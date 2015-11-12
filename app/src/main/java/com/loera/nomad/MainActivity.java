package com.loera.nomad;

import android.animation.ObjectAnimator;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvingResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,SongChooser.SongListener, ListView.OnItemClickListener{

    static boolean TEST_MODE = false;
    private final String TAG = "Main Activity";
    final int GEOFENCE_RADIUS = 45;


    GoogleApiClient playServices;
    LocationRequest locationRequest;

    Context context;
    PendingIntent mGeofencePendingIntent;
    private static String occupied = "none";

    GoogleMap googleMap;

    LatLng currentLatLng;
    Location currentLocation;

    ArrayList<String> names;
    List<CircleOptions> circles;
    HashMap<String,MusicSpot> musicSpots;

    GeofenceReciever reciever;

    MediaPlayer musicPlayer;
    static int touchY;
    static long touchTime;
    ActionBarDrawerToggle toggle;

    static boolean expanded = false;
    static boolean slidePlayer = false;
    static boolean addingGeofence = false;
    static boolean UISetup = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new LogWriter().execute();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setElevation(0);
        context = this;
        setupDrawer();
        if(savedInstanceState == null){
        musicSpots = new HashMap<>();
        circles = new LinkedList<>();
            names = new ArrayList<>();
        }else{

            musicSpots = (HashMap) savedInstanceState.getSerializable("musicSpots");
            occupied = savedInstanceState.getString("occupied");
            circles = getCirclesFromList(savedInstanceState.getStringArrayList("circles"));
            names = savedInstanceState.getStringArrayList("names");
        }

        reciever = new GeofenceReciever();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.aol.android.geofence.ACTION_RECEIVE_GEOFENCE");
        registerReceiver(reciever,filter);

        MapFragment map = (MapFragment) getFragmentManager().findFragmentById(R.id.mapFragment);
        map.getMapAsync(this);
        createLocationRequest();
        buildGoogleApiClient();

        playServices.connect();

    }

    public void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);

        state.putSerializable("musicSpots", musicSpots);
        state.putString("occupied", occupied);
        state.putStringArrayList("circles", getCirclesArrayList());
        state.putStringArrayList("names", names);

    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(!UISetup){
            UISetup = true;
            setupPlayerUI();
        }
    }

    public void monitorPreviousGeofences(ArrayList<String> list){
        LinkedList<Geofence> fences = new LinkedList<>();

        for(int i = 0;i<list.size();i++){

            LatLng latLng = circles.get(i).getCenter();

            Location loc = new Location("Nomad");
            loc.setLongitude(latLng.longitude);
            loc.setLatitude(latLng.latitude);

            Geofence g = getCurrentGeofence(loc,list.get(i));

            fences.add(g);
        }
        addMusicSpots(fences);
    }

    public ArrayList<String> getCirclesArrayList(){
        ArrayList<String> l = new ArrayList();

        for(CircleOptions c:circles){

            String s = c.getCenter().latitude+","+c.getCenter().longitude+","+
                    c.getRadius()+","+c.getFillColor()+","+c.getStrokeColor()+","
                    +c.getStrokeWidth();
            l.add(s);

        }


        return l;

    }

    public LinkedList<CircleOptions> getCirclesFromList(ArrayList<String> l){

        LinkedList<CircleOptions> list = new LinkedList<>();

        for(String s : l){

            Scanner sc = new Scanner(s);
            sc.useDelimiter(",");

            CircleOptions circle = new CircleOptions();
            circle.center(new LatLng(sc.nextDouble(), sc.nextDouble()));
            circle.radius(sc.nextDouble());
            circle.fillColor(sc.nextInt());
            circle.strokeColor(sc.nextInt());
            circle.strokeWidth(sc.nextFloat());

            list.add(circle);
        }
        return list;
    }


    public void setupPlayerUI(){

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        final RelativeLayout player = (RelativeLayout) findViewById(R.id.player);

        FrameLayout map = (FrameLayout) findViewById(R.id.mapLayout);

        Display display = getWindowManager().getDefaultDisplay();
        final Point point = new Point();
        display.getSize(point);
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");

        float navigationBarHeight;
        if (resourceId > 0) {
            navigationBarHeight = resources.getDimensionPixelSize(resourceId);
        }else {
            navigationBarHeight =  0;
        }

        boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
        boolean hasHomeKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME);

        final float height = point.y;
        final float width = point.x;
        //bottomBar and animation
        TextView text = (TextView)findViewById(R.id.infoBG);
        if(!(hasBackKey && hasHomeKey))
        setScaledHeight(text,width,height,21,128);
        else
            setScaledHeight(text,width,height,117,640);
        text = (TextView)findViewById(R.id.songText);
        setScaledHeight(text,width,height,43,480);
        text = (TextView)findViewById(R.id.artsitAlbumText);
        setScaledHeight(text,width,height,37,640);
        RelativeLayout controls = (RelativeLayout)findViewById(R.id.controlsLayout);
        controls.getLayoutParams().height = (int)(height - player.getLayoutParams().height) - controls.getLayoutParams().height;
        float buttonHeight =  (height - controls.getY())/2;
        setupButtons((int)buttonHeight/2);
        text = (TextView) findViewById(R.id.bottomBar);
        if(!(hasBackKey && hasHomeKey))
        setScaledHeight(text,width,height,11,128);
        else
            setScaledHeight(text,width,height,67,640);
        ImageView image = (ImageView)findViewById(R.id.albumArt);
        if(!(hasBackKey && hasHomeKey))
        image.getLayoutParams().height = (int)(height*7/16);
        else
            image.getLayoutParams().height = (int)(height*73/160);

        final float bottomBarSize = text.getLayoutParams().height;

        RelativeLayout contentMain = (RelativeLayout)findViewById(R.id.contentMain);

        final float closedPlayerY = contentMain.getHeight() - bottomBarSize;
        map.getLayoutParams().height = (int)closedPlayerY;
        player.setY(closedPlayerY);
        player.setX(0);

        text.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float currentY = event.getRawY();


                if (event.getAction() == MotionEvent.ACTION_DOWN && !slidePlayer) {
                    slidePlayer = true;
                    touchY = (int) event.getY();
                    touchTime = System.currentTimeMillis();
                } else if (event.getAction() == MotionEvent.ACTION_MOVE && slidePlayer) {
                    currentY = currentY - bottomBarSize - touchY;
                    if (currentY > 0 && currentY < closedPlayerY)
                        player.setY(currentY);
                } else if (event.getAction() == MotionEvent.ACTION_UP && slidePlayer) {

                    float finalY = expanded ? closedPlayerY : 0;

                    float beginY = finalY == 0 ? closedPlayerY : 0;

                    float speed = getSpeedOfSwipe(touchTime, System.currentTimeMillis(), beginY, currentY, closedPlayerY);

                    if (speed > 200) {
                        speed = 200;
                    }

                    Log.i(TAG,"Speed is " + speed);

                    ObjectAnimator animator = ObjectAnimator.ofFloat(player, "y", finalY);
                    animator.setDuration((long) speed);
                    animator.start();

                    expanded = !expanded;

                   animateButtonsOnExpand();

                    if (!expanded && currentLatLng != null)
                        googleMap.animateCamera(CameraUpdateFactory.newLatLng(currentLatLng));

                    slidePlayer = false;
                }

                return false;
            }
        });

        player.setVisibility(View.VISIBLE);
    }

    public void animateButtonsOnExpand(){

        ImageButton barButton = (ImageButton)findViewById(R.id.bar_play_pause);
        float amountMoved = (float) (barButton.getLayoutParams().width * 1.5);
        if(expanded){

            ObjectAnimator moveRight = ObjectAnimator.ofFloat(barButton,"x",barButton.getX() + amountMoved);
            moveRight.setDuration(400);
            moveRight.start();

        }else{

            ObjectAnimator moveRight = ObjectAnimator.ofFloat(barButton,"x",barButton.getX() - amountMoved);
            moveRight.setDuration(200);
            moveRight.start();

        }

    }

    public float getSpeedOfSwipe(long touchTime,long currentTime, float firstTouch,float secondTouch,float sizeOfBox){

        float slope = ((currentTime-touchTime)/(secondTouch-firstTouch));

        float distanceLeft = sizeOfBox - secondTouch;

        return Math.abs(slope * distanceLeft);

    }

    public void setScaledHeight(TextView text,float width,float height, double numerator,double denominator){

        text.getLayoutParams().height = (int)(height*numerator/denominator);

    }

    public void setupDrawer(){

        final DrawerLayout layout = (DrawerLayout)findViewById(R.id.homeDrawerLayout);

        toggle = new ActionBarDrawerToggle(this,layout,R.string.open_drawer,R.string.close_drawer){

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu();
            }
        };

        layout.setDrawerListener(toggle);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);


    }

    public void onPostCreate(Bundle savedInstanceState){

        super.onPostCreate(savedInstanceState);
        toggle.syncState();

    }

    public void onDestroy() {
        unregisterReceiver(reciever);
        super.onDestroy();
        if(musicPlayer!=null){
        musicPlayer.stop();
        musicPlayer.release();
        musicPlayer = null;
        }
    }

    public void updateUI(MusicSpot m) {


    }

    public Geofence getCurrentGeofence(Location currentLocation,String name) {

        return new Geofence.Builder()
                .setRequestId(name)
                .setCircularRegion(currentLocation.getLatitude(), currentLocation.getLongitude(), GEOFENCE_RADIUS)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT).build();

    }

    public void createLocationRequest() {

        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    }

    public void startLocationUpdates() {

        LocationServices.FusedLocationApi.requestLocationUpdates(playServices, locationRequest, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                if (!addingGeofence) {

                    boolean zoomCamera = currentLocation == null;

                    currentLocation = location;
                    currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                    MarkerOptions marker = new MarkerOptions()
                            .position(currentLatLng);

                    googleMap.clear();
                    addCircles();
                    googleMap.addMarker(marker);

                    if(zoomCamera)
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16.0f));

                    Log.i(TAG, "Location updated, Marker updated");

                }
            }
        });
    }

    public void addCircles(){

        for(CircleOptions c:circles)
            googleMap.addCircle(c);

    }

    public void addMusicSpots(Geofence g) {

        LocationServices.GeofencingApi.addGeofences(
                playServices,
                getGeofencingRequest(g),
                getGeofencePendingIntent()
        ).setResultCallback(new ResolvingResultCallbacks<Status>(this, 1) {
            @Override
            public void onSuccess(Status status) {
                Log.i(TAG, "Monitoring Geofence");
            }

            @Override
            public void onUnresolvableFailure(Status status) {

                Log.i(TAG, "Monitoring Geofence Failed Status: " + status.toString());

            }
        });

    }
    public void addMusicSpots(LinkedList<Geofence> g) {

        LocationServices.GeofencingApi.addGeofences(
                playServices,
                getGeofencingRequest(g),
                getGeofencePendingIntent()
        ).setResultCallback(new ResolvingResultCallbacks<Status>(this, 1) {
            @Override
            public void onSuccess(Status status) {
                Log.i(TAG, "Monitoring Geofence");
            }

            @Override
            public void onUnresolvableFailure(Status status) {

                Log.i(TAG, "Monitoring Geofence Failed Status: " + status.toString());

            }
        });

    }

    public void getSong(){
        addingGeofence = true;
       SongChooser chooser = new SongChooser();
        chooser.show(getFragmentManager(), "TEST");

    }

    public void addCircleAroundCurrentPosition(String name){

        String fillString = "#9D45BA";
        String strokeString = "#5358DB";


        CircleOptions circle = new CircleOptions().center(currentLatLng)
                .radius(GEOFENCE_RADIUS).fillColor(Color.parseColor(fillString))
                .strokeColor(Color.parseColor(strokeString))
                .strokeWidth(8.0f);

        googleMap.addCircle(circle);
        circles.add(circle);


        Log.i(TAG, "Circle added around current position");
    }

    protected synchronized void buildGoogleApiClient() {
        playServices = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Toast.makeText(context, "Connected to Google Play Services", Toast.LENGTH_SHORT).show();
                        removeGeofences();
                        if(!names.isEmpty())
                            monitorPreviousGeofences(names);
                        startLocationUpdates();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Toast.makeText(context, "Failed to Connect", Toast.LENGTH_SHORT).show();
                    }
                })
                .addApi(LocationServices.API)
                .build();
    }

    private GeofencingRequest getGeofencingRequest(Geofence g) {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofence(g);
        return builder.build();
    }
    private GeofencingRequest getGeofencingRequest(LinkedList<Geofence> g) {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(g);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
       // Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        Intent intent = new Intent("com.aol.android.geofence.ACTION_RECEIVE_GEOFENCE");
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }

    public void setupButtons(int imageButtonHeight) {

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentLocation != null) {
                    if (occupied.equals("none")) {
                        getSong();
                    } else {

                        Snackbar.make(view, "Current Position Occupied", Snackbar.LENGTH_LONG).show();

                    }
                } else
                    Snackbar.make(view, "Waiting on location...", Snackbar.LENGTH_LONG).show();
            }
        });

        ImageButton but = (ImageButton) findViewById(R.id.play_pause);

        but.getLayoutParams().height = imageButtonHeight;
        but.getLayoutParams().width = imageButtonHeight;

        but = (ImageButton) findViewById(R.id.bar_play_pause);
        but.getLayoutParams().height = imageButtonHeight;
        but.getLayoutParams().width = imageButtonHeight;
        but.setY(but.getY() + (imageButtonHeight / 4));
        but.setX(but.getX() - (imageButtonHeight / 4));

    }

    public void removeGeofences(){
        LocationServices.GeofencingApi.removeGeofences(
                playServices,
                // This is the same pending intent that was used in addGeofences().
                getGeofencePendingIntent()
        ).setResultCallback(new ResolvingResultCallbacks<Status>(this, 3) {
            @Override
            public void onSuccess(Status status) {

                Log.i(TAG, "Removed Geofences");

            }

            @Override
            public void onUnresolvableFailure(Status status) {
                Log.i(TAG, "Could not remove Geofences, error: " + status.toString());
            }
        }); // Result processed in onResult().

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.setBuildingsEnabled(false);

        googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                addingGeofence = true;
                currentLatLng = latLng;
                currentLocation = new Location("Nomad");
                currentLocation.setLatitude(latLng.latitude);
                currentLocation.setLongitude(latLng.longitude);
                getSong();
            }
        });

        this.googleMap = googleMap;



    }

    @Override
    public void onRecieveSong(File song) {

        names.add(song.getName());
        MusicSpot m = new MusicSpot(song.getName(),song.getAbsolutePath(),currentLatLng.latitude,currentLatLng.longitude);
        musicSpots.put(m.getSongName(), m);
        addMusicSpots(getCurrentGeofence(currentLocation, m.getSongName()));
        addCircleAroundCurrentPosition(m.getSongName());
        occupied = m.getSongName();
        updateUI(m);

        addingGeofence = false;

    }

    @Override
    public void noSelectionMade() {
        addingGeofence = false;
    }
    public void playOrPause(View v){

        Log.i(TAG, "play or pause");


        if(musicPlayer!= null && occupied != null){

            if(!musicPlayer.isPlaying()){
                musicPlayer.start();
                setPlayOrPauseIcons("pause");
            }else{
                musicPlayer.pause();
                setPlayOrPauseIcons("play");
            }

        }else if( musicPlayer == null && !occupied.equals("none")){

            createPlayerFromSpot(musicSpots.get(occupied));

        }
    }

    public void createPlayerFromSpot(MusicSpot m){

     ;
        musicPlayer = new MediaPlayer();
        musicPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                setPlayOrPauseIcons("play");
            }
        });
        musicPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try{
        musicPlayer.setDataSource(m.getSongFile());
            setPlayOrPauseIcons("pause");
            musicPlayer.prepare();
            musicPlayer.start();
            musicPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        }catch(IOException e){
            e.printStackTrace();
        }

    }

    public void setPlayOrPauseIcons(String state){
        final ImageButton fab  = (ImageButton) findViewById(R.id.play_pause);
        final ImageButton fab2 = (ImageButton) findViewById(R.id.bar_play_pause);

        if(state.equals("play")){
        fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_play));
        fab2.setImageDrawable(getResources().getDrawable(R.drawable.ic_play));
        }else{
            fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause));
            fab2.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause));
        }


    }

    public void replaceSongWith(MusicSpot m){

        musicPlayer.pause();
        musicPlayer.stop();
        musicPlayer.release();
        musicPlayer = new MediaPlayer();
        try{
        musicPlayer.setDataSource(m.getSongFile());
        musicPlayer.prepare();
            musicPlayer.start();
        }catch(IOException e){
            e.printStackTrace();
        }

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.i(TAG, "LOL");
    }


    public class GeofenceReciever extends BroadcastReceiver {

        final String TAG = "GeofenceReciever";

        Context context;
        Intent broadcastIntent = new Intent();
        @Override
        public void onReceive(Context context, Intent intent) {
            this.context = context;
            broadcastIntent.addCategory("EnterOrExit");
            enterOrExit(intent);

        }

        public void enterOrExit(Intent intent){

            GeofencingEvent event = GeofencingEvent.fromIntent(intent);

            if(event.hasError()){
                Log.i(TAG,"error in geofence intent " + event.getErrorCode()+"");
            }else{


                List<Geofence> geofences = event.getTriggeringGeofences();
                int transition = event.getGeofenceTransition();



                for(int count = 0;count<geofences.size();count++){

                    Geofence g = geofences.get(count);

                    if(transition == Geofence.GEOFENCE_TRANSITION_ENTER){

                        Log.i(TAG,"Entered geofence " + g.getRequestId());
                        Toast.makeText(context,"Entered geofence " + g.getRequestId(),Toast.LENGTH_LONG).show();
                        onGeofenceEntered(g.getRequestId());
                    }else if (transition == Geofence.GEOFENCE_TRANSITION_EXIT){

                        Log.i(TAG,"Exited geofence " + g.getRequestId());
                        Toast.makeText(context,"Exited geofence " + g.getRequestId(),Toast.LENGTH_LONG).show();
                        onGeofenceExited(g.getRequestId());

                    }

                }


            }

        }

        public void onGeofenceEntered(String id) {
            occupied = id;
            MusicSpot spot = musicSpots.get(id);

            if(musicPlayer==null){
                createPlayerFromSpot(spot);
            }else if(musicPlayer.isPlaying()){
                replaceSongWith(spot);
            }


        }

        public void onGeofenceExited(String id) {
            occupied = "none";

        }
    }

    public void onConfigurationChanged(Configuration newConfig){

        super.onConfigurationChanged(newConfig);
        toggle.onConfigurationChanged(newConfig);

    }

    public boolean onOptionsItemSelected(MenuItem item){

        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);

    }

        private class LogWriter extends AsyncTask<Void,Void,Void>{


            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Process process = Runtime.getRuntime().exec("logcat -d");
                    BufferedReader bufferedReader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));


                    File logFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/NomadLogData.txt");
                    if(!logFile.exists())
                        logFile.createNewFile();
                    else
                        logFile.delete();
                    FileWriter logWrite = new FileWriter(logFile);
                    PrintWriter log = new PrintWriter(logWrite);
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        log.println(line);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }
        }


}


