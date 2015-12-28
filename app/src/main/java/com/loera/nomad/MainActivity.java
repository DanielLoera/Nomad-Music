package com.loera.nomad;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
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
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.PlayerStateCallback;
import com.spotify.sdk.android.player.Spotify;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, PlayerNotificationCallback,PlayerStateCallback, ConnectionStateCallback ,SongChooser.SongListener, ListView.OnItemClickListener{

    static boolean TEST_MODE = false;
    private final String TAG = "Main Activity";

    GoogleApiClient playServices;
    LocationRequest locationRequest;

    Context context;
    PendingIntent mGeofencePendingIntent;
    private static int occupied;

    GoogleMap googleMap;

    LatLng currentLatLng;

    NotificationCompat.Builder notification;
    NotificationManager notMgr;

    ArrayList<Integer> ids = new ArrayList<>();
    List<CircleOptions> circles;
    HashMap<Integer, MusicSpot> musicSpots;
    static LinkedList<Integer> toRemove = new LinkedList<>();

    GeofenceReciever reciever;

    Player musicPlayer;
    static int touchY;
    static long touchTime;
    ActionBarDrawerToggle toggle;

    static String[] currentSong;

    DiscreteSeekBar seekBar;
    DiscreteSeekBar radiusSeekBar;
    CircleOptions currentCircle;

    android.os.Handler seekUpdater, circleUpdater, serverUpdater;
    Runnable secondCheck, circleCheck, serverCheck;

    static boolean expanded = false;
    static boolean slidePlayer = false;
    static boolean addingGeofence = false;
    static boolean UISetup = false;
    static boolean followGps = false;
    static boolean playing = false;

    private boolean spotifyLoggedIn = false;
    public String responseToken;
    public static final String CLIENT_ID = "7f54406569184c16b37376116cb6307c";
    public static final String REDIRECT_URI = "http://dannyloera.com";
    PlayerState playerState;

    SharedPreferences prefs;
    private String currentCity;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new LogWriter().execute();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        context = this;
        prefs = this.getPreferences(Context.MODE_PRIVATE);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }


        setupDrawer();
        readInstanceState(savedInstanceState);

        reciever = new GeofenceReciever();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.aol.android.geofence.ACTION_RECEIVE_GEOFENCE");
        filter.addAction("com.nomad.ACTION_NOTIFICATION_PRESS");
        registerReceiver(reciever, filter);
        notMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);


        MapFragment map = (MapFragment) getFragmentManager().findFragmentById(R.id.mapFragment);
        map.getMapAsync(this);
        createLocationRequest();
        buildGoogleApiClient();

        playServices.connect();


    }

    public void onResume(){
        super.onResume();
        if(toRemove.size() > 0){ //Music spots need to be removed
            Log.i(TAG,"Removing deleted spots");
            removeGeofences(); //disconnect all geofences
            circles.clear();//delete previous circles
            for(int s: toRemove){ //remove only ids necessary
                ids.remove(new Integer(s));
                new RemoveMusicSpotFromServer(s).execute();
            }
            if (ids.size() > 0) {//if there are any geofences left, monitor them.
                monitorCurrentMusicSpots();
            }
            toRemove.clear();
            googleMap.clear();
            addCircles();
        }
    }

    public void readInstanceState(Bundle savedInstanceState){

        if (savedInstanceState == null) {
            musicSpots = new HashMap<>();
            circles = new LinkedList<>();
            ids = new ArrayList<>();
        } else {

            musicSpots = (HashMap) savedInstanceState.getSerializable("musicSpots");
            occupied = savedInstanceState.getInt("occupied");
            circles = getCirclesFromList(savedInstanceState.getStringArrayList("circles"));
            ids = savedInstanceState.getIntegerArrayList("names");
        }
    }

    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);

        state.putSerializable("musicSpots", musicSpots);
        state.putInt("occupied", occupied);
        state.putStringArrayList("circles", getCirclesArrayList());
        state.putIntegerArrayList("ids", ids);
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        RelativeLayout player = (RelativeLayout) findViewById(R.id.player);
        if (hasFocus && player.getVisibility() == View.INVISIBLE) {
            setupUI();
            if(!spotifyLoggedIn)
                setupSpotify();
        }

    }

    public void monitorCurrentMusicSpots() {
        LinkedList<Geofence> fences = new LinkedList<>();

        for (int i = 0; i < ids.size(); i++) {

            MusicSpot m = musicSpots.get(ids.get(i));

            LatLng latLng = m.getLatlng();

            Location loc = new Location("Nomad");
            loc.setLongitude(latLng.longitude);
            loc.setLatitude(latLng.latitude);

            Geofence g = getGeofenceFromSpot(m);

            fences.add(g);
            circles.add(getCircle(m));
        }
        monitorGeofence(fences);
    }

    private CircleOptions getCircle(MusicSpot m){
        CircleOptions circle = new CircleOptions();
        circle.center(m.getLatlng());
        circle.strokeWidth(10);
        circle.strokeColor(getResources().getColor(R.color.colorAccentDark));
        circle.fillColor(0x40C6096B);
        circle.radius(m.getRadius());

        return circle;
    }

    public ArrayList<String> getCirclesArrayList() {
        ArrayList<String> l = new ArrayList();

        for (CircleOptions c : circles) {

            String s = c.getCenter().latitude + "," + c.getCenter().longitude + "," +
                    c.getRadius() + "," + c.getFillColor() + "," + c.getStrokeColor() + ","
                    + c.getStrokeWidth();
            l.add(s);

        }


        return l;

    }

    public LinkedList<CircleOptions> getCirclesFromList(ArrayList<String> l) {

        LinkedList<CircleOptions> list = new LinkedList<>();

        for (String s : l) {

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

    public void onPostCreate(Bundle savedInstanceState) {

        super.onPostCreate(savedInstanceState);
        //pretty arrow!!
        toggle.syncState();

    }

    public void onDestroy() {

        stopMusic();
        Log.i(TAG, "destroyed");
        unregisterReceiver(reciever);
        super.onDestroy();

    }


    /***
     *
     * SPOTIFY METHODS
     *
     */

    public void setupSpotify(){

        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                REDIRECT_URI);
        builder.setScopes(new String[]{"streaming"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, 6969, request);
    }

    @Override
    public void onPlayerState(PlayerState playerState) {
       this.playerState = playerState;
    }

    @Override
    public void onLoggedIn() {
        Log.i(TAG, "User Succesfully logged in.");
        spotifyLoggedIn = true;
    }

    @Override
    public void onLoggedOut() {
        spotifyLoggedIn = false;
        Log.i(TAG, "User Succesfully logged out, asking again.");
        setupSpotify();
    }

    @Override
    public void onLoginFailed(Throwable throwable) {
        Log.i(TAG, "User failed login. " + throwable.getLocalizedMessage());

        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle("Login Failed");
        dialog.setMessage("Failed to login to spotify:\n" + throwable.getLocalizedMessage());
        dialog.setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AuthenticationClient.clearCookies(context);
                musicPlayer.logout();
                dialog.dismiss();
            }
        });
        dialog.create().show();

    }

    @Override
    public void onTemporaryError() {

    }

    @Override
    public void onConnectionMessage(String s) {
        Log.i(TAG, "New connection message: " + s);
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        if(eventType == EventType.PAUSE){

            if(playerState.positionInMs == playerState.durationInMs){ // song has ended
                setPlayOrPauseIcons("play");
                stopSeek();
                seekBar.setProgress(0);
            }else if(eventType == EventType.TRACK_CHANGED){

                this.playerState = playerState;

            }

        }
    }
    @Override
    public void onPlaybackError(ErrorType errorType, String s) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == 6969) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                Log.i(TAG,"Authentication token received.");
                responseToken = response.getAccessToken();
                prefs.edit().putString("spotify_token",responseToken).commit();
                Config playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);
                Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                    @Override
                    public void onInitialized(Player player) {
                        musicPlayer = player;
                        musicPlayer.addConnectionStateCallback(MainActivity.this);
                        musicPlayer.addPlayerNotificationCallback(MainActivity.this);

                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                    }
                });
            }else if(response.getType() == AuthenticationResponse.Type.ERROR){
                Log.i(TAG,"Error occured while logging in:  " + response.getError());
                AuthenticationClient.clearCookies(context);
            }else{
                Log.i(TAG,"User cancelled login.");
            }
        }
    }

    /**
     *
     * LOCATION/MAP METHODS
     *
     */

    public Geofence getCurrentGeofence(Location currentLocation, int id) {

        return new Geofence.Builder()
                .setRequestId(id+"")
                .setCircularRegion(currentLocation.getLatitude(), currentLocation.getLongitude(), (float) currentCircle.getRadius())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT).build();

    }

    public Geofence getGeofenceFromSpot(MusicSpot m){

        return new Geofence.Builder()
                .setRequestId(m.getId()+"")
                .setCircularRegion(m.getLatlng().latitude, m.getLatlng().longitude, (float)m.getRadius())
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

                Log.i(TAG, "Location updated");

                if (!addingGeofence) {

                    boolean firstLocationGrab = googleMap.getMyLocation() == null;

                    if(googleMap.getMyLocation() != null){
                        currentCity = getCity(googleMap.getMyLocation());
                        if(serverUpdater == null)
                            startServerUpdates();
                    }

                    currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());


                    if (followGps || firstLocationGrab) {
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18.0f));
                    }

                    if (occupied == 0)
                        updateLocationUI(location);

                }
            }
        });
    }

    public String getCity(Location location) {
        Geocoder coder = new Geocoder(context);
        List<Address> addresses = new ArrayList();

        try {
            addresses = coder.getFromLocation(location.getLatitude(), location.getLongitude(), 10);

        } catch (IOException e) {
            e.printStackTrace();

        }
        return  addresses.get(0).getLocality();

    }

    public void addCircles() {

        for (CircleOptions c : circles)
            googleMap.addCircle(c);

    }

    public void monitorGeofence(Geofence g) {

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

    public void monitorGeofence(LinkedList<Geofence> g) {

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

    protected synchronized void buildGoogleApiClient() {
        playServices = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Toast.makeText(context, "Connected to Google Play Services", Toast.LENGTH_SHORT).show();
                        removeGeofences();

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

    public void removeGeofences() {
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
    public void onMapReady(final GoogleMap googleMap) {
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.setBuildingsEnabled(false);
        googleMap.setMyLocationEnabled(true);


        this.googleMap = googleMap;


    }

    @Override
    public void noSelectionMade() {
        addingGeofence = false;
        googleMap.clear();
        addCircles();
    }

    public class GeofenceReciever extends BroadcastReceiver {

        final String TAG = "GeofenceReciever";

        Context context;
        Intent broadcastIntent = new Intent();

        @Override
        public void onReceive(Context context, Intent intent) {
            this.context = context;

            String buttonPress = intent.getStringExtra("button");

            broadcastIntent.addCategory("EnterOrExit");
            if (buttonPress == null)
                enterOrExit(intent);
            else
                buttonPressed(buttonPress);

        }

        public void enterOrExit(Intent intent) {

            GeofencingEvent event = GeofencingEvent.fromIntent(intent);

            if (event.hasError()) {
                Log.i(TAG, "error in geofence intent " + event.getErrorCode() + "");
            } else {


                List<Geofence> geofences = event.getTriggeringGeofences();
                int transition = event.getGeofenceTransition();


                for (int count = 0; count < geofences.size(); count++) {

                    Geofence g = geofences.get(count);

                    if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {

                        Log.i(TAG, "Entered geofence " + g.getRequestId());
                        Toast.makeText(context, "Entered geofence " + g.getRequestId(), Toast.LENGTH_LONG).show();
                        onGeofenceEntered(g.getRequestId());
                    } else if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {

                        Log.i(TAG, "Exited geofence " + g.getRequestId());
                        Toast.makeText(context, "Exited geofence " + g.getRequestId(), Toast.LENGTH_LONG).show();
                        onGeofenceExited(g.getRequestId());

                    }

                }


            }

        }

        public void onGeofenceEntered(String id) {
            occupied = Integer.parseInt(id);
            MusicSpot spot = musicSpots.get(occupied);

            Log.i(TAG, "Entered Geofence id: " + occupied);

             if (playing) {
                replaceSongWith(spot);
            }else {
                 playing  = true;
                 playMusicSpot(spot);
                 startSeek(0);
             }

            updatePlayerUI();
            updateNotification();

        }

        public void onGeofenceExited(String id) {
            occupied = 0;
        }

        public void buttonPressed(String buttonPressed) {

            switch (buttonPressed) {

                case "playOrPause":
                    playOrPause(null);
                    break;
                case "stop":
                    stopMusic();
                    break;
                default:
                    Log.i(TAG, "Unknown button press");

            }

        }
    }


    /**
     *
     * SERVER METHODS / CLASSES
     *
     */

    public void startServerUpdates(){
        serverUpdater = new Handler();

        serverCheck = new Runnable() {
            @Override
            public void run() {
                Log.i("Server","server update from " + currentCity +" fetched.");
                new getMusicSpotsFromServer().execute();
                serverUpdater.postDelayed(serverCheck,(1000 * 60) * 5);
            }
        };

        serverCheck.run();
    }

    public void stopServerUpdates(){
        serverUpdater.removeCallbacks(serverCheck);
    }

    public class getMusicSpotsFromServer extends AsyncTask<Void,Void,Void>{

        MusicSpot[] spots;


        public JSONArray getSpotArray()throws JSONException{

            StringBuilder result = new StringBuilder();
            URL url = null;
            try {
                url = new URL("http://www.dannyloera.com/nomad/get_music_spots.php?location="+currentCity);

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestMethod("GET");

                urlConnection.connect();

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                urlConnection.disconnect();
            }catch(Exception e){

                e.printStackTrace();
            }

            return new JSONArray(result.toString());

        }

        public MusicSpot getMusicSpotFromJSON(JSONObject j)throws JSONException{

            MusicSpot m = new MusicSpot(j.getString("song name"),j.getString("song link"),j.getDouble("latitude"),j.getDouble("longitude"));
            m.setId(j.getInt("id"));
            m.setDurationInMils(j.getLong("song duration"));
            m.setArtLink(j.getString("art link"));
            m.setArtistName(j.getString("artist name"));
            m.setAlbumName(j.getString("album name"));
            m.setRadius(j.getDouble("radius"));


            return m;

        }

        @Override
        protected Void doInBackground(Void... params) {


            try {
                JSONArray jsonArray = getSpotArray();

                if(jsonArray.length() > 0){

                    spots = new MusicSpot[jsonArray.length()];

                    for(int i = 0;i<jsonArray.length();i++){

                        spots[i] = getMusicSpotFromJSON((JSONObject)jsonArray.get(i));

                    }

                }



            } catch (JSONException e) {
                e.printStackTrace();
            }


            return null;
        }

        public void onPostExecute(Void result){
            int count = 0;
            if(spots != null) {
                for (MusicSpot m : spots) {
                    if (!musicSpots.containsKey(m.getId())) {
                        count++;
                        musicSpots.put(m.getId(), m);
                        ids.add(m.getId());
                        monitorGeofence(getGeofenceFromSpot(m));
                        circles.add(getCircle(m));
                    }
                }
                Log.i("Server",count + " new spots added.");
                googleMap.clear();
                addCircles();
            }
        }
    }

    public class addMusicSpotToServer extends AsyncTask<Void,Void,Void>{

        MusicSpot spot;

        public addMusicSpotToServer(MusicSpot m){
            this.spot = m;
        }

        private class NameValuePair{

            private String name;
            private String value;

            public NameValuePair(String name,String value){

                this.name = name;
                this.value = value;
            }

            public String getValue() {
                return value;
            }

            public String getName() {
                return name;
            }
        }


        private String getQuery(List<NameValuePair> params) throws UnsupportedEncodingException
        {
            StringBuilder result = new StringBuilder();
            boolean first = true;

            for (NameValuePair pair : params)
            {
                if (first)
                    first = false;
                else
                    result.append("&");

                result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
            }

            return result.toString();
        }

        @Override
        protected Void doInBackground(Void... params) {


            try {
                URL url = new URL("http://www.dannyloera.com/nomad/add_music_spot.php");

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setReadTimeout(10000);
                urlConnection.setConnectTimeout(15000);
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);

                List<NameValuePair> pairs = new ArrayList<>();

                pairs.add(new NameValuePair("location",currentCity));
                pairs.add(new NameValuePair("id", spot.getId() + ""));
                pairs.add(new NameValuePair("latitude", spot.getLatlng().latitude + ""));
                pairs.add(new NameValuePair("longitude", spot.getLatlng().longitude + ""));
                pairs.add(new NameValuePair("radius", spot.getRadius() + ""));
                pairs.add(new NameValuePair("duration", spot.getDurationInMils() + ""));
                pairs.add(new NameValuePair("songLink", spot.getSongLink()));
                pairs.add(new NameValuePair("artLink", spot.getArtLink()));
                pairs.add(new NameValuePair("songName", spot.getSongName()));
                pairs.add(new NameValuePair("artistName", spot.getArtistName()));
                pairs.add(new NameValuePair("albumName", spot.getAlbumName()));

                OutputStream os = urlConnection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(getQuery(pairs));
                writer.flush();
                writer.close();
                os.close();

                urlConnection.connect();

                switch (urlConnection.getResponseCode()){

                    case 1:
                        Log.i(TAG,"Spot "+spot.getId()+" succesfully added to server!");

                    case 0:
                        Log.i(TAG,"Spot could not be added to server. :(");
                }

                urlConnection.disconnect();
            }catch(IOException e){
                e.printStackTrace();
            }

            return null;
        }
    }

    public class RemoveMusicSpotFromServer extends  AsyncTask<Void,Void,Void>{

        int id;

        public RemoveMusicSpotFromServer(int id){
            this.id = id;
        }

        @Override
        protected Void doInBackground(Void... params) {

            StringBuilder result = new StringBuilder();
            URL url = null;
            try {

                Log.i(TAG, "Removing " + id + " from " + currentCity + " table on nomadmusic database.");

                url = new URL("http://www.dannyloera.com/nomad/remove_spot.php?location="+currentCity+"&id="+id);

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestMethod("GET");

                urlConnection.connect();

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }


                urlConnection.disconnect();
            }catch(Exception e){

                e.printStackTrace();
            }

            Log.i(TAG,result.toString());


            return null;
        }
    }


    /**
     *
     * UI METHODS
     *
     */

    public void setupUI() {
        UISetup = true;
        final RelativeLayout player = (RelativeLayout) findViewById(R.id.player);
        FrameLayout map = (FrameLayout) findViewById(R.id.mapLayout);
        Display display = getWindowManager().getDefaultDisplay();
        final Point point = new Point();
        display.getSize(point);
        boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
        boolean hasHomeKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME);

        final float height = point.y;
        final float width = point.x;
        //bottomBar and animation
        TextView text = (TextView) findViewById(R.id.infoBG);
        if (!(hasBackKey && hasHomeKey))
            setScaledHeight(text, width, height, 21, 128);
        else
            setScaledHeight(text, width, height, 117, 640);
        text = (TextView) findViewById(R.id.songText);
        text.setSelected(true);
        setScaledHeight(text, width, height, 43, 480);
        text = (TextView) findViewById(R.id.artsitAlbumText);
        text.setSelected(true);
        setScaledHeight(text, width, height, 37, 640);
        RelativeLayout controls = (RelativeLayout) findViewById(R.id.controlsLayout);
        controls.getLayoutParams().height = (int) (height - player.getLayoutParams().height) - controls.getLayoutParams().height;
        float buttonHeight = (height - controls.getY()) / 2;
        setupButtons((int) buttonHeight / 2);
        text = (TextView) findViewById(R.id.bottomBar);
        if (!(hasBackKey && hasHomeKey))
            setScaledHeight(text, width, height, 11, 128);
        else
            setScaledHeight(text, width, height, 67, 640);
        (findViewById(R.id.bottom_bar_layout)).getLayoutParams().height = text.getLayoutParams().height;
        ImageView image = (ImageView) findViewById(R.id.albumArt);
        if (!(hasBackKey && hasHomeKey))
            image.getLayoutParams().height = (int) (height * 7 / 16);
        else
            image.getLayoutParams().height = (int) (height * 73 / 160);

        final float bottomBarSize = text.getLayoutParams().height;

        final RelativeLayout contentMain = (RelativeLayout) findViewById(R.id.contentMain);

        final float closedPlayerY = contentMain.getHeight() - bottomBarSize;
        map.getLayoutParams().height = (int) closedPlayerY;
        player.setY(closedPlayerY);
        player.setX(0);

        text.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (occupied != 0) {

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

                        Log.i(TAG, "Speed is " + speed);

                        ObjectAnimator animator = ObjectAnimator.ofFloat(player, "y", finalY);
                        animator.setDuration((long) speed);
                        animator.start();

                        expanded = !expanded;

                        animateButtonsOnExpand();

                        if (!expanded && currentLatLng != null)
                            googleMap.animateCamera(CameraUpdateFactory.newLatLng(currentLatLng));

                        slidePlayer = false;
                    }
                } else {

                    Snackbar.make(contentMain, "No Music Spot detected :(", Snackbar.LENGTH_SHORT).show();
                }
                return false;
            }
        });

        setupSeekBar();

        player.setVisibility(View.VISIBLE);

    }

    public void updateNotification() {

        notification = new NotificationCompat.Builder(context);
        notification.setSmallIcon(R.drawable.ic_headphones);
        notification.setAutoCancel(true);

        notification.setContentTitle(currentSong[0]);

        notification.setContentText(currentSong[1] + " - " + currentSong[2]);

        Intent playPauseIntent = new Intent();
        playPauseIntent.setAction("com.nomad.ACTION_NOTIFICATION_PRESS");
        playPauseIntent.putExtra("button", "playOrPause");
        PendingIntent playPausePend = PendingIntent.getBroadcast(context, 001, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent stopIntent = new Intent();
        stopIntent.setAction("com.nomad.ACTION_NOTIFICATION_PRESS");
        stopIntent.putExtra("button", "stop");
        PendingIntent stopPend = PendingIntent.getBroadcast(context, 002, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent launchAppIntent = new Intent(context, MainActivity.class);
        launchAppIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent launchPend = PendingIntent.getActivity(context, 003, launchAppIntent, 0);


        int playOrPauseIcon = playing ? R.drawable.ic_pause : R.drawable.ic_play;

        String playOrPauseText = playOrPauseIcon == R.drawable.ic_pause ? "Pause" : "Play";

        notification.addAction(playOrPauseIcon, playOrPauseText, playPausePend);
        notification.addAction(R.drawable.ic_stop, "Stop", stopPend);
        notification.setContentIntent(launchPend);

        notMgr.notify(001, notification.build());

    }

    public void setupButtons(int imageButtonHeight) {

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (googleMap != null)
                    if (googleMap.getMyLocation() != null) {
                        if (!addingGeofence) {
                            fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_check));
                            addingGeofence = true;
                            showRadiusChooser();
                        } else {
                            fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_plus));
                            hideRadiusChooser();
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

        but.setX(but.getX() - (imageButtonHeight / 2));
    }

    public void setPlayOrPauseIcons(String state) {
        final ImageButton fab = (ImageButton) findViewById(R.id.play_pause);
        final ImageButton fab2 = (ImageButton) findViewById(R.id.bar_play_pause);

        if (state.equals("play")) {
            fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_play));
            fab2.setImageDrawable(getResources().getDrawable(R.drawable.ic_play));
        } else {
            fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause));
            fab2.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause));
        }


    }

    public void setupSeekBar() {

        RelativeLayout text = (RelativeLayout) findViewById(R.id.controlsLayout);

        float topOfView = text.getY();

        seekBar = (DiscreteSeekBar) findViewById(R.id.musicSeekBar);

        float seekBarHeight = seekBar.getHeight();
        seekBar.setY(topOfView - (seekBarHeight));
        seekBar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

                stopSeek();

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {

                int stoppedTime = seekBar.getProgress();

                musicPlayer.pause();
                musicPlayer.seekToPosition(stoppedTime);
                musicPlayer.resume();
                setPlayOrPauseIcons("pause");
                updateNotification();
                startSeek(stoppedTime);


            }
        });

    }

    public void startSeek(final long initialSeekPosition) {
        seekBar.setProgress((int) initialSeekPosition);
        seekBar.setMax((int)musicSpots.get(occupied).getDurationInMils());
        seekBar.setVisibility(View.VISIBLE);
        seekUpdater = new android.os.Handler();

        final long startTime = System.currentTimeMillis();

        secondCheck = new Runnable() {
            @Override
            public void run() {
                //update progress
                seekBar.setProgress((int) (initialSeekPosition + (System.currentTimeMillis() - startTime)));
                if(seekBar.getProgress() >= seekBar.getMax()){
                    //song has finished playing
                    playing = false;
                    setPlayOrPauseIcons("play");
                    seekBar.setProgress(0);
                    musicPlayer.pause();
                }else{
                seekUpdater.postDelayed(secondCheck, 1000);
                }
            }
        };

        secondCheck.run();

    }

    void stopSeek() {
        seekUpdater.removeCallbacks(secondCheck);
    }

    public void animateButtonsOnExpand() {

        ImageButton barButton = (ImageButton) findViewById(R.id.bar_play_pause);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        float amountMoved = (float) (barButton.getLayoutParams().width * 1.5);
        if (expanded) {

            ObjectAnimator moveRight = ObjectAnimator.ofFloat(barButton, "x", barButton.getX() + amountMoved);
            moveRight.setDuration(400);
            moveRight.start();
            fab.hide();

        } else {

            ObjectAnimator moveRight = ObjectAnimator.ofFloat(barButton, "x", barButton.getX() - amountMoved);
            moveRight.setDuration(200);
            moveRight.start();
            fab.show();

        }

    }

    public float getSpeedOfSwipe(long touchTime, long currentTime, float firstTouch, float secondTouch, float sizeOfBox) {

        float slope = ((currentTime - touchTime) / (secondTouch - firstTouch));

        float distanceLeft = sizeOfBox - secondTouch;

        return Math.abs(slope * distanceLeft);

    }

    public void setScaledHeight(TextView text, float width, float height, double numerator, double denominator) {

        text.getLayoutParams().height = (int) (height * numerator / denominator);

    }

    public void setupDrawer() {

         DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.homeDrawerLayout);

        drawerLayout.setScrimColor(getResources().getColor(android.R.color.transparent));
        toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open_drawer, R.string.close_drawer) {

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

        drawerLayout.setDrawerListener(toggle);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        Display d = getWindowManager().getDefaultDisplay();
        Point p = new Point();
        d.getSize(p);

        final String[] drawerItems = {"Home","Music Spots", "Settings"};

        final ListView drawer = (ListView)findViewById(R.id.homeDrawer);
        drawer.getLayoutParams().width = (int)(p.x * .80);
        drawer.setAdapter(new ListAdapter() {
            @Override
            public boolean areAllItemsEnabled() {
                return true;
            }

            @Override
            public boolean isEnabled(int position) {
                return true;
            }

            @Override
            public void registerDataSetObserver(DataSetObserver observer) {

            }

            @Override
            public void unregisterDataSetObserver(DataSetObserver observer) {

            }

            @Override
            public int getCount() {
                return 3;
            }

            @Override
            public Object getItem(int position) {
                return drawer.getChildAt(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView text = new TextView(context);
                text.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                text.setText(drawerItems[position]);
                text.setPadding(20, 15, 15, 15);
                text.setTextSize(8 * getResources().getDisplayMetrics().density);
                return text;
            }

            @Override
            public int getItemViewType(int position) {
                return 0;
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }
        });
        drawer.setOnItemClickListener(this);
    }

    public void showRadiusChooser() {

        Display display = getWindowManager().getDefaultDisplay();
        Point displaySize = new Point();
        display.getSize(displaySize);

        FrameLayout mapLayout = (FrameLayout) findViewById(R.id.mapLayout);
        TextView bottomBar = (TextView) findViewById(R.id.bottomBar);
        radiusSeekBar = new DiscreteSeekBar(context);
        final int primaryColor = getResources().getColor(R.color.colorPrimary);
        final int accentColor = getResources().getColor(R.color.colorAccent);
        int accentColorDark = getResources().getColor(R.color.colorAccentDark);
        radiusSeekBar.setLayoutParams(new LinearLayout.LayoutParams(displaySize.x, bottomBar.getLayoutParams().height));

        radiusSeekBar.setBackgroundColor(primaryColor);
        radiusSeekBar.setTrackColor(accentColorDark);
        radiusSeekBar.setScrubberColor(accentColor);
        radiusSeekBar.setThumbColor(accentColor, accentColor);

        radiusSeekBar.setMin(45);
        radiusSeekBar.setMax(500);
        radiusSeekBar.setProgress(45);

        currentCircle = new CircleOptions();
        currentCircle.center(googleMap.getCameraPosition().target);
        currentCircle.strokeWidth(10);
        currentCircle.strokeColor(accentColorDark);
        currentCircle.fillColor(0x40C6096B);
        currentCircle.radius(45);

        googleMap.addCircle(currentCircle);


        radiusSeekBar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                currentCircle.center(googleMap.getCameraPosition().target);
                currentCircle.radius(value);
                googleMap.clear();
                addCircles();
                googleMap.addCircle(currentCircle);

            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {
                currentCircle.center(googleMap.getCameraPosition().target);
                currentCircle.radius(seekBar.getProgress());

                googleMap.clear();
                googleMap.addCircle(currentCircle);
                addCircles();


            }
        });

        mapLayout.addView(radiusSeekBar);
        float initialY = radiusSeekBar.getY();
        radiusSeekBar.setY(0 - radiusSeekBar.getLayoutParams().height);

        ObjectAnimator anim = ObjectAnimator.ofFloat(radiusSeekBar, "y", initialY);
        anim.setDuration(500);
        anim.start();

        showCirclePreview();
    }

    public void hideRadiusChooser() {

        ObjectAnimator anim = ObjectAnimator.ofFloat(radiusSeekBar, "y", -radiusSeekBar.getLayoutParams().height);
        anim.setDuration(500);
        anim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                getSong();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        anim.start();
        hideCirclePreview();
    }

    public void showCirclePreview() {

        circleUpdater = new Handler();

        circleCheck = new Runnable() {
            @Override
            public void run() {
                googleMap.clear();
                currentCircle.center(googleMap.getCameraPosition().target);
                googleMap.addCircle(currentCircle);
                circleUpdater.postDelayed(circleCheck, 250);
            }
        };

        circleCheck.run();

    }

    public void hideCirclePreview() {

        circleUpdater.removeCallbacks(circleCheck);

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        DrawerLayout layout = (DrawerLayout)findViewById(R.id.homeDrawerLayout);
        if(position == 1){
            if(!ids.isEmpty()) {
                layout.closeDrawer(Gravity.LEFT);
                Intent intent = new Intent(this, SpotOrganizer.class);
                intent.putExtra("spots", musicSpots);
                intent.putIntegerArrayListExtra("ids",ids);
                startActivity(intent);
            }

        }
    }

    /**
     *
     * MUSIC PLAYER METHODS
     *
     */

    public void playMusicSpot(MusicSpot m) {

        try {
            musicPlayer.play(m.getSongLink());
            setPlayOrPauseIcons("pause");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getSong() {
        SongChooser chooser = new SongChooser();
        chooser.show(getFragmentManager(), "Song Chooser");
    }

    public void stopMusic() {
        if (notMgr != null)
            notMgr.cancelAll();
        if (musicPlayer != null) {
            musicPlayer.pause();
            Spotify.destroyPlayer(musicPlayer);
            musicPlayer = null;
        }
        setPlayOrPauseIcons("play");
    }

    public void updateLocationUI(Location location) {

        TextView locationText = (TextView) findViewById(R.id.locationText);
        //Not Used Yet.
        // TextView claimedByText = (TextView)findViewById(R.id.claimedByText);

        Geocoder geocoder = new Geocoder(context);
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 10);
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                locationText.setText(address.getAddressLine(0));
            } else {
                locationText.setText("Unknown Location");
            }
        } catch (IOException e) {

            e.printStackTrace();

        }


    }

    public void updatePlayerUI() {

        currentSong = new String[]{"Unknown", "Unknown", "Unknown"};

        TextView songName = (TextView) findViewById(R.id.songText);
        TextView albumAndArtist = (TextView) findViewById(R.id.artsitAlbumText);

        MusicSpot current = musicSpots.get(occupied);

        String songTitle = current.getSongName();
        if (songTitle != null) {
            songName.setText(songTitle);
            currentSong[0] = songTitle;
        }

        String artist = current.getArtistName();
        String album =  current.getAlbumName();

        if (artist != null) {
            albumAndArtist.setText(artist + " - ");
            currentSong[1] = artist;
        }

        if (album != null) {
            albumAndArtist.setText(albumAndArtist.getText() + album);
            currentSong[2] = album;
        }

        new GetAlbumArtFromLink(current.getArtLink()).execute();

    }

    private class GetAlbumArtFromLink extends AsyncTask<Void,Void,Void>{

        String link;
        Bitmap art;

        public GetAlbumArtFromLink(String link){
            this.link = link;
        }


        @Override
        protected Void doInBackground(Void... params) {
            try {

                HttpURLConnection con = (HttpURLConnection)new URL(link).openConnection();
                con.connect();
                art = BitmapFactory.decodeStream(con.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        public void onPostExecute(Void result){

            ImageView albumArt = (ImageView) findViewById(R.id.albumArt);

            int color = getAverageColor(art);
            albumArt.setBackground(new ColorDrawable(color));
            albumArt.setImageBitmap(art);

        }
    }

    public int getAverageColor(Bitmap b) {

        long totalPixels = 0;
        long red = 0;
        long green = 0;
        long blue = 0;

        for (int y = 0; y < b.getHeight(); y++) {
            for (int x = 0; x < b.getWidth(); x++) {
                totalPixels++;
                int color = b.getPixel(x, y);
                red += Color.red(color);
                green += Color.green(color);
                blue += Color.blue(color);
            }
        }
        return Color.rgb((int) (red / totalPixels), (int) (green / totalPixels), (int) (blue / totalPixels));
    }

    @Override
    public void onRecieveSong(SpotifyTrack song) {

        //create new Music Spot from Spotify Track and current location

        MusicSpot m = new MusicSpot(song.getName(), song.getUri(), currentCircle.getCenter().latitude,
                currentCircle.getCenter().longitude);

        m.setRadius(currentCircle.getRadius());
        m.setAlbumName(song.getAlbum());
        m.setArtistName(song.getArtist());
        m.setArtLink(song.getArt());
        m.setDurationInMils(song.getDurationInMils());
        m.setId();

        ids.add(m.getId());
        musicSpots.put(m.getId(), m);
        circles.add(currentCircle);
        addCircles();

        //monitor new spot

        monitorGeofence(getGeofenceFromSpot(m));
        updateLocationUI(googleMap.getMyLocation());

        //send spot to server
        new addMusicSpotToServer(m).execute();


        addingGeofence = false;
    }


    public void playOrPause(View v) {

        Log.i(TAG, "play or pause");


        if (musicPlayer != null && occupied != 0) {

            if(!playing) {
                playing = true;
                musicPlayer.resume();
                setPlayOrPauseIcons("pause");
                startSeek(seekBar.getProgress());
            } else {
                playing = false;
                musicPlayer.pause();
                setPlayOrPauseIcons("play");
                stopSeek();
            }
            updateNotification();
        } else if (occupied == 0) {

            Toast.makeText(context, "Go find a Music Spot to play some jams!", Toast.LENGTH_SHORT).show();

        }
    }

    public void replaceSongWith(MusicSpot m) {

        musicPlayer.pause();
        try {
            musicPlayer.play(m.getSongLink());
            stopSeek();
            startSeek(0);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     *
     * MISC METHODS
     *
     *
     */

    public void onConfigurationChanged(Configuration newConfig) {

        super.onConfigurationChanged(newConfig);
        toggle.onConfigurationChanged(newConfig);

    }

    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {


        if (toggle.onOptionsItemSelected(item)) {
            return true;
        } else if (item.getItemId() == R.id.maptype && googleMap != null) {
            boolean mapNormal = googleMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL;
            if (mapNormal)
                googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            else
                googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);


        } else if (item.getItemId() == R.id.gpsAction && googleMap != null && !addingGeofence) {

            followGps = !followGps;

            String answer = followGps ? "following" : "not following";

            TextView dumbView = (TextView) findViewById(R.id.bottomBar);

            Snackbar.make(dumbView, "Map " + answer + " your location", Snackbar.LENGTH_SHORT).show();

            if (followGps)
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18.0f));


        }

        return super.onOptionsItemSelected(item);

    }

    private class LogWriter extends AsyncTask<Void, Void, Void> {


        @Override
        protected Void doInBackground(Void... params) {
            try {
                Process process = Runtime.getRuntime().exec("logcat -d");
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));


                File logFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/NomadLogData.txt");
                if (!logFile.exists())
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


