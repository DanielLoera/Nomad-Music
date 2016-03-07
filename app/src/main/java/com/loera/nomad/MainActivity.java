package com.loera.nomad;

import android.Manifest;
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
import android.content.pm.PackageManager;
import android.content.res.Configuration;
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
import android.support.percent.PercentRelativeLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.maps.android.SphericalUtil;
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
import java.io.FileNotFoundException;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Scanner;

import io.codetail.animation.ViewAnimationUtils;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, PlayerNotificationCallback, PlayerStateCallback, ConnectionStateCallback, SpotCreator.SongListener {

    static boolean TEST_MODE = false;
    private final String TAG = "Main Activity";

    GoogleApiClient playServices;
    LocationRequest locationRequest;

    Context context;
    PendingIntent mGeofencePendingIntent;
    private static int occupied;

    GoogleMap googleMap;

    LatLng currentLatLng;
    LatLng referenceLatLng;

    NotificationCompat.Builder notification;
    NotificationManager notMgr;

    HashMap<Integer, MusicSpot> musicSpots;
    static LinkedList<Integer> toRemove = new LinkedList<>();

    GeofenceReciever reciever;

    Player musicPlayer;
    static int touchY;
    static long touchTime;
    ActionBarDrawerToggle toggle;
    Menu optionsMenu;

    DiscreteSeekBar seekBar;
    DiscreteSeekBar radiusSeekBar;
    CircleOptions currentCircle;

    android.os.Handler seekUpdater, circleUpdater, serverUpdater, songTimeUpdater;
    Runnable secondCheck, circleCheck, serverCheck, songTimeCheck;

    private final static int NO_SONG_PLAYING = -1;
    private final static int PLAY = 1;
    private final static int PAUSE = 2;
    private final static int FAST_FORWARD = 3;
    private final static int REWIND = 4;

    static boolean expanded = false;
    static boolean slidePlayer = false;
    static boolean addingGeofence = false;
    static boolean UISetup = false;
    static boolean followGps = false;
    static int playing = NO_SONG_PLAYING;

    private boolean spotifyLoggedIn = false;
    public String responseToken;
    public static final String CLIENT_ID = "7f54406569184c16b37376116cb6307c";
    public static final String REDIRECT_URI = "http://dannyloera.com";
    PlayerState playerState;

    final int COLOR_START = Color.parseColor("#00C853");
    final int COLOR_END = Color.parseColor("#F50057");

    SharedPreferences prefs;
    private String currentCity;
    private boolean paused;
    private float closedPlayerY;
    private float currentY;
    private PercentRelativeLayout player;
    private DrawerLayout drawerLayout;
    private RelativeLayout locationPlay;
    private RelativeLayout radiusChooser;

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

        setupSpotify();
    }

    public void onResume() {
        super.onResume();
        if (toRemove.size() > 0) { //Music spots need to be removed
            Log.i(TAG, "Removing deleted spots");
            stopMonitoringGeofences(); //disconnect all geofences
            googleMap.clear();//delete previous circles
            for (int s : toRemove) { //remove only ids necessary
                musicSpots.remove(s);
                new RemoveMusicSpotFromServer(s).execute();
            }
            if (!musicSpots.isEmpty()) {//if there are any geofences left, monitor them.
                monitorNearbySpots();
            }
            File saved = new File(context.getExternalFilesDir(ACTIVITY_SERVICE), "saved.txt");
            if (!musicSpots.isEmpty()) {
                String offlineSpots = "";
                ArrayList<MusicSpot> mspots = new ArrayList<>(musicSpots.values());
                for (int i = 0; i < mspots.size() - 1; i++)
                    offlineSpots += mspots.get(i).toString() + "\n";
                offlineSpots += mspots.get(mspots.size() - 1).toString();
                try {
                    saved.delete();
                    saved.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                saveMusicSpotOffline(offlineSpots);
            } else {
                saved.delete();
            }
            toRemove.clear();
            redrawCircles();
        }
    }

    public void readInstanceState(Bundle savedInstanceState) {

        if (savedInstanceState == null) {
            musicSpots = new HashMap<>();
        } else {

            musicSpots = (HashMap) (savedInstanceState.getSerializable("musicSpots"));
            occupied = savedInstanceState.getInt("occupied");
        }
    }

    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);

        state.putSerializable("musicSpots", musicSpots);
        state.putInt("occupied", occupied);
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (player == null)
            player = (PercentRelativeLayout) findViewById(R.id.player);
        if (hasFocus && player.getVisibility() == View.INVISIBLE) {
            setupUI();
        }

    }

    private CircleOptions getCircle(MusicSpot m) {
        CircleOptions circle = new CircleOptions();
        circle.center(m.getLatlng());
        circle.strokeWidth(10);
        int[] colors = getColorFromTime(m.getTime());
        circle.fillColor(colors[0]);
        circle.strokeColor(colors[1]);
        circle.radius(m.getRadius());

        return circle;
    }

    private int[] getColorFromTime(long time) {
        long difference = (System.currentTimeMillis() / 1000L) - time;
        float percentage = difference / 432000.0f;

        int color = interpolateColor(COLOR_START, COLOR_END,
                percentage);

        return new int[]{color, darkenColor(color)};
    }


    private float interpolate(final float a, final float b,
                              final float proportion) {
        return (a + ((b - a) * proportion));
    }

    private int interpolateColor(final int a, final int b,
                                 final float proportion) {
        final float[] hsva = new float[3];
        final float[] hsvb = new float[3];
        Color.colorToHSV(a, hsva);
        Color.colorToHSV(b, hsvb);
        for (int i = 0; i < 3; i++) {
            hsvb[i] = interpolate(hsva[i], hsvb[i], proportion);
        }

        int color = Color.HSVToColor(hsvb);
        color = Color.argb(50, Color.red(color), Color.green(color), Color.blue(color));

        return color;
    }

    private int darkenColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f; // value component
        color = Color.HSVToColor(hsv);
        return color;
    }

    public void onPostCreate(Bundle savedInstanceState) {

        super.onPostCreate(savedInstanceState);
        //pretty arrow!!
        toggle.syncState();

    }

    public void onDestroy() {
        Spotify.destroyPlayer(musicPlayer);
        musicPlayer = null;
        stopMusic();
        Log.i(TAG, "destroyed");
        unregisterReceiver(reciever);
        super.onDestroy();

    }

    public void requestPermission(String[] permissions) {
        ActivityCompat.requestPermissions(this, permissions, 1337);
    }


    /***
     * SPOTIFY METHODS
     */

    public void setupSpotify() {

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
        if (occupied != 0) {
            playMusicSpot(musicSpots.get(occupied));
        }
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
        if (eventType == EventType.PAUSE) {

            if (playerState.positionInMs == playerState.durationInMs) { // song has ended
                setPlayOrPauseIcons(PLAY);
                stopSeek();
                seekBar.setProgress(0);
            } else if (eventType == EventType.TRACK_CHANGED) {

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
                Log.i(TAG, "Authentication token received.");
                responseToken = response.getAccessToken();
                prefs.edit().putString("spotify_token", responseToken).commit();
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
            } else if (response.getType() == AuthenticationResponse.Type.ERROR) {
                Log.i(TAG, "Error occured while logging in:  " + response.getError());
                AuthenticationClient.clearCookies(context);
            } else {
                Log.i(TAG, "User cancelled login.");
            }
        }
    }

    /**
     * LOCATION/MAP METHODS
     */

    public Geofence getCurrentGeofence(Location currentLocation, int id) {

        return new Geofence.Builder()
                .setRequestId(id + "")
                .setCircularRegion(currentLocation.getLatitude(), currentLocation.getLongitude(), (float) currentCircle.getRadius())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT).build();

    }

    public Geofence getGeofenceFromSpot(MusicSpot m) {

        return new Geofence.Builder()
                .setRequestId(m.getId() + "")
                .setCircularRegion(m.getLatlng().latitude, m.getLatlng().longitude, (float) m.getRadius())
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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(playServices, locationRequest, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                Log.i(TAG, "Location updated");

                if (!addingGeofence) {

                    boolean firstLocationGrab = currentLatLng == null;

                    if (location != null) {
                        String tempCity = getCity(location);
                        if (tempCity != null) {
                            currentCity = tempCity;
                            if (serverUpdater == null)
                                startServerUpdates();
                        }
                    }

                    currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                    if (referenceLatLng == null)
                        referenceLatLng = currentLatLng;
                    else if (SphericalUtil.computeDistanceBetween(referenceLatLng, currentLatLng) >= 500) {
                        //if current LatLng farther than 500 meters from reference LatLng
                        Toast.makeText(context, "New Refernce Point at: Lat: " + currentLatLng.latitude + " Long: " + currentLatLng.longitude
                                , Toast.LENGTH_SHORT).show();
                        referenceLatLng = currentLatLng;
                        monitorNearbySpots();
                    }

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
        List<Address> addresses = new ArrayList<>();

        try {
            addresses = coder.getFromLocation(location.getLatitude(), location.getLongitude(), 10);

        } catch (IOException e) {
            e.printStackTrace();

        }

        if (addresses.isEmpty())
            return null;

        String city = null;

        Address address = addresses.get(0);
        if (address.getLocality() != null) city = address.getLocality();
        else if (address.getSubAdminArea() != null) city = address.getSubAdminArea();

        return city;

    }

    public void redrawCircles() {
        googleMap.clear();
        for (MusicSpot m : musicSpots.values())
            googleMap.addCircle(getCircle(m));
    }

    public void monitorGeofence(Geofence g) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        }
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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        }
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
                        stopMonitoringGeofences();

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
        // calling addGeofences() and stopMonitoringGeofences().
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }

    public void stopMonitoringGeofences() {
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        }
        googleMap.setMyLocationEnabled(true);


        this.googleMap = googleMap;


    }

    @Override
    public void noSelectionMade() {
        addingGeofence = false;
        redrawCircles();
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
            new IncrementVisitorCountOnSpot(id).execute();
            Log.i(TAG, "Entered Geofence id: " + occupied);
            if (spotifyLoggedIn) {
                if(occupied != playing)
                playMusicSpot(musicSpots.get(occupied));
            }

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
     * SERVER METHODS / CLASSES
     */

    public void startServerUpdates() {
        serverUpdater = new Handler();

        serverCheck = new Runnable() {
            @Override
            public void run() {
                Log.i("Server", "server update from " + currentCity + " fetched.");
                new getMusicSpotsFromServer().execute();
                serverUpdater.postDelayed(serverCheck, (1000 * 60) * 5);
            }
        };

        serverCheck.run();
    }

    public void stopServerUpdates() {
        serverUpdater.removeCallbacks(serverCheck);
    }

    public class getMusicSpotsFromServer extends AsyncTask<Void, Void, Void> {

        MusicSpot[] spots;


        public JSONArray getSpotArray() throws JSONException {

            StringBuilder result = new StringBuilder();
            URL url = null;
            try {

                Log.i("Server", "Attempting to retrieve spots from " + currentCity);

                url = new URL("http://www.dannyloera.com/nomad/get_music_spots.php?location=" + currentCity);

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
            } catch (Exception e) {

                e.printStackTrace();
            }

            return new JSONArray(result.toString());

        }

        public MusicSpot getMusicSpotFromJSON(JSONObject j) throws JSONException {

            MusicSpot m = new MusicSpot(j.getString("song name"), j.getString("song link"), j.getDouble("latitude"), j.getDouble("longitude"));
            m.setId(j.getInt("id"));
            m.setDurationInMils(j.getLong("song duration"));
            m.setArtLink(j.getString("art link"));
            m.setArtistName(j.getString("artist name"));
            m.setAlbumName(j.getString("album name"));
            m.setRadius(j.getDouble("radius"));
            m.setVisits(j.getInt("visits"));
            m.setMessage(j.getString("message"));
            m.setTime(j.getLong("time"));

            return m;

        }

        @Override
        protected Void doInBackground(Void... params) {


            try {
                JSONArray jsonArray = getSpotArray();

                if (jsonArray.length() > 0) {

                    spots = new MusicSpot[jsonArray.length()];

                    for (int i = 0; i < jsonArray.length(); i++) {

                        spots[i] = getMusicSpotFromJSON((JSONObject) jsonArray.get(i));

                    }

                }


            } catch (JSONException e) {
                e.printStackTrace();
            }


            return null;
        }

        public void onPostExecute(Void result) {
            int count = 0;
            if (spots != null) {
                for (MusicSpot m : spots) {
                    if (!musicSpots.containsKey(m.getId())) {
                        count++;
                        musicSpots.put(m.getId(), m);
                    }
                }
                Log.i("Server", count + " new spots added.");
                redrawCircles();
                monitorNearbySpots();
            }
        }
    }

    private void monitorNearbySpots() {
        ArrayList<Integer> ids = new ArrayList<>(musicSpots.keySet());
        stopMonitoringGeofences();
        if (ids.size() <= 100) { // monitor all spots, there are less than 100
            for (Integer i : ids) {
                monitorGeofence(getGeofenceFromSpot(musicSpots.get(i)));
            }
        } else {//monitor only the nearest 100 spots
            PriorityQueue<MusicSpot> top = new PriorityQueue<>(ids.size(), new SpotComparator(currentLatLng));
            top.addAll(musicSpots.values());
            for (int i = 0; i < 100; i++) {
                monitorGeofence(getGeofenceFromSpot(top.poll()));
            }
        }
    }

    private class SpotComparator implements Comparator<MusicSpot> {
        LatLng toCompare;

        public SpotComparator(LatLng toCompare) {
            this.toCompare = toCompare;
        }

        @Override
        public int compare(MusicSpot lhs, MusicSpot rhs) {
            double distance1 = lhs.distanceBetween(toCompare);
            double distance2 = rhs.distanceBetween(toCompare);

            return distance1 >= distance2 ? distance1 == distance2 ? 0 : 1 : -1;
        }

        @Override
        public boolean equals(Object object) {
            return false;
        }
    }

    public class addMusicSpotToServer extends AsyncTask<Void, Void, Void> {

        MusicSpot spot;

        public addMusicSpotToServer(MusicSpot m) {
            this.spot = m;
        }

        private class NameValuePair {

            private String name;
            private String value;

            public NameValuePair(String name, String value) {

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


        private String getQuery(List<NameValuePair> params) throws UnsupportedEncodingException {
            StringBuilder result = new StringBuilder();
            boolean first = true;

            for (NameValuePair pair : params) {
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

                pairs.add(new NameValuePair("location", currentCity));
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
                pairs.add(new NameValuePair("message", spot.getMessage()));

                OutputStream os = urlConnection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(getQuery(pairs));
                writer.flush();
                writer.close();
                os.close();

                urlConnection.connect();

                switch (urlConnection.getResponseCode()) {

                    case 1:
                        Log.i("Server", "Spot " + spot.getId() + " succesfully added to server!");

                    case 0:
                        Log.i("Server", "Spot could not be added to server. :(");
                }

                urlConnection.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    public class RemoveMusicSpotFromServer extends AsyncTask<Void, Void, Void> {

        int id;

        public RemoveMusicSpotFromServer(int id) {
            this.id = id;
        }

        @Override
        protected Void doInBackground(Void... params) {

            StringBuilder result = new StringBuilder();
            URL url = null;
            try {

                Log.i(TAG, "Removing " + id + " from " + currentCity + " table on nomadmusic database.");

                url = new URL("http://www.dannyloera.com/nomad/remove_spot.php?location=" + currentCity + "&id=" + id);

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
            } catch (Exception e) {

                e.printStackTrace();
            }

            Log.i(TAG, result.toString());


            return null;
        }
    }

    public class IncrementVisitorCountOnSpot extends AsyncTask<Void, Void, Void> {

        String id;

        public IncrementVisitorCountOnSpot(String id) {
            this.id = id;
        }

        @Override
        protected Void doInBackground(Void... params) {
            StringBuilder result = new StringBuilder();
            URL url = null;
            try {
                url = new URL("http://www.dannyloera.com/nomad/new_visitor.php?location=" + currentCity + "&id=" + id);

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
            } catch (Exception e) {

                e.printStackTrace();
            }

            Log.i("Increment Spot", result.toString());

            return null;
        }
    }


    /**
     * UI METHODS
     */

    public void setupUI() {
        UISetup = true;
        Display display = getWindowManager().getDefaultDisplay();
        final Point point = new Point();
        display.getSize(point);
        //bottomBar and animation
        setupButtons();
        TextView text = (TextView) findViewById(R.id.bottomBar);

        final float bottomBarSize = text.getHeight();

        final RelativeLayout contentMain = (RelativeLayout) findViewById(R.id.contentMain);

        closedPlayerY = contentMain.getHeight() - bottomBarSize;
        player.setY(closedPlayerY);
        player.setX(0);

        player.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (playing != NO_SONG_PLAYING) {

                    currentY = event.getRawY();

                    if (event.getAction() == MotionEvent.ACTION_DOWN && !slidePlayer) {
                        slidePlayer = true;
                        touchY = (int) event.getY();
                        touchTime = System.currentTimeMillis();
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE && slidePlayer) {
                        currentY = currentY - bottomBarSize - touchY;
                        if (currentY > 0 && currentY < closedPlayerY)
                            player.setY(currentY);
                    } else if (event.getAction() == MotionEvent.ACTION_UP && slidePlayer) {

                        if (expanded)
                            showPlayer(false);
                        else
                            showPlayer(true);

                        slidePlayer = false;
                    }
                } else {
                    Snackbar.make(contentMain, "No Music Spot detected :(", Snackbar.LENGTH_SHORT).show();
                }
                return false;
            }
        });
        setupSeekBar();
        player.setVisibility(View.VISIBLE);//UI DONE!!
    }

    private void showPlayer(boolean show) {
        float finalY = show ? 0 : closedPlayerY;

        float beginY = finalY == 0 ? closedPlayerY : 0;

        float speed = getSpeedOfSwipe(touchTime, System.currentTimeMillis(), beginY, currentY, closedPlayerY);

        if (speed > 200) {
            speed = 200;
        }

        ObjectAnimator animator = ObjectAnimator.ofFloat(player, "y", finalY);
        animator.setDuration((long) speed);
        animator.start();

        expanded = !expanded;

        animateButtonsOnExpand();
    }

    public void updateNotification(int state) {


        MusicSpot current = musicSpots.get(playing);

        if(current != null){
        notification = new NotificationCompat.Builder(context);
        notification.setSmallIcon(R.drawable.ic_headphones);
        notification.setAutoCancel(true);

        notification.setContentTitle(current.getSongName());

        notification.setContentText(current.getArtistName() + " - " + current.getAlbumName());

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


        int playOrPauseIcon = state == PLAY ? R.drawable.ic_play : R.drawable.ic_pause;

        String playOrPauseText = state == PLAY ? "Play" : "Pause";

        notification.addAction(playOrPauseIcon, playOrPauseText, playPausePend);
        notification.addAction(R.drawable.ic_stop, "Stop", stopPend);
        notification.setContentIntent(launchPend);

        notMgr.notify(001, notification.build());
        }

    }

    public void setupButtons() {

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (googleMap != null)
                    if (currentLatLng != null) {
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

        ImageButton but = (ImageButton) findViewById(R.id.ffButton);
        but.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startSongTimeUpdate(FAST_FORWARD);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    stopSongTimeUpdate();
                }

                return false;
            }
        });

        but = (ImageButton) findViewById(R.id.rwButton);
        but.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startSongTimeUpdate(REWIND);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    stopSongTimeUpdate();
                }

                return false;
            }
        });
    }

    private void startSongTimeUpdate(int update) {
        final int change = update == FAST_FORWARD ? 5500 : -5500;
        setPlayOrPauseIcons(PLAY);
        stopSeek();
        songTimeUpdater = new Handler();
        songTimeCheck = new Runnable() {
            @Override
            public void run() {
                musicPlayer.pause();
                seekBar.setProgress(seekBar.getProgress() + change);
                musicPlayer.seekToPosition(seekBar.getProgress());
                musicPlayer.resume();
                songTimeUpdater.postDelayed(songTimeCheck, 100);
            }
        };
        songTimeCheck.run();
    }

    private void stopSongTimeUpdate() {
        songTimeUpdater.removeCallbacks(songTimeCheck);
        setPlayOrPauseIcons(PAUSE);
        musicPlayer.seekToPosition(seekBar.getProgress());
        startSeek(seekBar.getProgress());
    }


    public void setPlayOrPauseIcons(int state) {
        final ImageButton fab = (ImageButton) findViewById(R.id.play_pause);
        final ImageButton fab2 = (ImageButton) findViewById(R.id.bar_play_pause);

        if (state == PLAY) {
            fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_play));
            fab2.setImageDrawable(getResources().getDrawable(R.drawable.ic_play));
        } else {
            fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause));
            fab2.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause));
        }

        if (playing != NO_SONG_PLAYING)
            updateNotification(state);

    }

    public void setupSeekBar() {
        RelativeLayout controls = (RelativeLayout) findViewById(R.id.controlsLayout);
        locationPlay = (RelativeLayout)findViewById(R.id.locationPlayLayout);
        radiusChooser = (RelativeLayout)findViewById(R.id.radiusChooserLayout);
        radiusSeekBar = (DiscreteSeekBar)findViewById(R.id.radiusChooserSeekbar);
        seekBar = (DiscreteSeekBar) findViewById(R.id.musicSeekBar);
        seekBar.setY(controls.getY() - (seekBar.getHeight() / 2));
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
                setPlayOrPauseIcons(PAUSE);
                startSeek(stoppedTime);


            }
        });

    }

    public void
    startSeek(final long initialSeekPosition) {
        seekBar.setProgress((int) initialSeekPosition);
        seekBar.setMax((int) musicSpots.get(occupied).getDurationInMils());
        seekBar.setVisibility(View.VISIBLE);
        seekUpdater = new android.os.Handler();

        final long startTime = System.currentTimeMillis();

        secondCheck = new Runnable() {
            @Override
            public void run() {
                //update progress
                seekBar.setProgress((int) (initialSeekPosition + (System.currentTimeMillis() - startTime)));
                if (seekBar.getProgress() >= seekBar.getMax()) {
                    //song has finished playing
                    setPlayOrPauseIcons(PLAY);
                    seekBar.setProgress(0);
                    musicPlayer.pause();
                    paused = true;
                } else {
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

        float amountMoved = (float) (barButton.getWidth() * 1.5);
        if (expanded) {

            ObjectAnimator moveRight = ObjectAnimator.ofFloat(barButton, "x", barButton.getX() + amountMoved);
            moveRight.setDuration(400);
            moveRight.start();
            fab.hide();
            hideMessage();

        } else {

            ObjectAnimator moveRight = ObjectAnimator.ofFloat(barButton, "x", barButton.getX() - amountMoved);
            moveRight.setDuration(200);
            moveRight.start();
            fab.show();
            optionsMenu.findItem(R.id.maptype).setIcon(R.drawable.ic_map);
        }

    }

    public float getSpeedOfSwipe(long touchTime, long currentTime, float firstTouch, float secondTouch, float sizeOfBox) {

        float slope = ((currentTime - touchTime) / (secondTouch - firstTouch));

        float distanceLeft = sizeOfBox - secondTouch;

        return Math.abs(slope * distanceLeft);

    }

    public void setupDrawer() {

        drawerLayout = (DrawerLayout) findViewById(R.id.homeDrawerLayout);

        drawerLayout.setScrimColor(ContextCompat.getColor(context,android.R.color.transparent));
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

        drawerLayout.addDrawerListener(toggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        Display d = getWindowManager().getDefaultDisplay();
        Point p = new Point();
        d.getSize(p);

        final String[] drawerTitles = {
                "History", //0
                "Your Spots", //1
                "Settings"}; //2
        final int[] drawerIcons = {R.drawable.ic_account_box_grey600_36dp,
                R.drawable.ic_pencil_grey600_36dp,
                R.drawable.ic_fast_forward_grey600_36dp};

        LinearLayout drawer = (LinearLayout) findViewById(R.id.homeDrawer);
        LayoutInflater inflater = LayoutInflater.from(context);
        for(int i = 0;i<drawerIcons.length;i++){
            View layoutview = inflater.inflate(R.layout.drawer_item,null);
            layoutview.setId(i);
            ((TextView)layoutview.findViewById(R.id.drawerItemText)).setText(drawerTitles[i]);
            ((ImageView)layoutview.findViewById(R.id.drawerItemIcon)).setImageResource(drawerIcons[i]);
            layoutview.setOnClickListener(new DrawerOnClickListener());
            drawer.addView(layoutview);
        }
    }

    private class DrawerOnClickListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            switch(v.getId()){
                case 1:
                    File spotsFile = new File(context.getExternalFilesDir(Context.ACTIVITY_SERVICE), "saved.txt");
                    if(spotsFile.exists()){
                        drawerLayout.closeDrawer(Gravity.LEFT);
                        Intent intent = new Intent(context,SpotOrganizer.class);
                        HashMap<Integer,MusicSpot> savedSpots = getSpotsFromFile(spotsFile);
                        intent.putExtra("location",currentCity);
                        intent.putExtra("spots",savedSpots);
                        context.startActivity(intent);

                    }else{
                        Snackbar.make(v,"You dont have any spots, yet :)",Snackbar.LENGTH_SHORT).show();
                    }

                    break;
            }
        }
    }

    private HashMap<Integer,MusicSpot> getSpotsFromFile(File f){
        HashMap<Integer,MusicSpot> saved = new HashMap<>();
        try {
            Scanner s = new Scanner(f);

            while(s.hasNextLine()){
                MusicSpot m = new MusicSpot();
                m.setId(Integer.parseInt(s.nextLine()));
                m.setLatt(Double.parseDouble(s.nextLine()));
                m.setLongi(Double.parseDouble(s.nextLine()));
                m.setRadius(Double.parseDouble(s.nextLine()));
                m.setDurationInMils(Long.parseLong(s.nextLine()));
                m.setMessage(s.nextLine());
                m.setSongName(s.nextLine());
                m.setSongLink(s.nextLine());
                m.setAlbumName(s.nextLine());
                m.setArtistName(s.nextLine());
                m.setArtLink(s.nextLine());
                m.setTime(Long.parseLong(s.nextLine()));
                saved.put(m.getId(),m);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return saved;
    }



    public void showRadiusChooser() {

        locationPlay.setVisibility(View.INVISIBLE);
        radiusChooser.setVisibility(View.VISIBLE);
        int startX =(int)(radiusChooser.getWidth() * .90);
        int startY = radiusChooser.getTop() + radiusChooser.getHeight()/2;
        int finalRadius = (int)(radiusChooser.getWidth()*1.5);
        Animator anim = ViewAnimationUtils.createCircularReveal(radiusChooser,startX,startY,0,finalRadius);
        anim.setDuration(800);
        anim.start();
        int accentColorDark = getResources().getColor(R.color.colorAccentDark);


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
                redrawCircles();
                googleMap.addCircle(currentCircle);

            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {
                currentCircle.center(googleMap.getCameraPosition().target);
                currentCircle.radius(seekBar.getProgress());

                redrawCircles();
                googleMap.addCircle(currentCircle);
            }
        });
        showCirclePreview();
    }

    public void hideRadiusChooser() {
        radiusChooser.setVisibility(View.INVISIBLE);
        locationPlay.setVisibility(View.VISIBLE);
        int startX = (int)(locationPlay.getWidth()*.15);
        int startY = locationPlay.getTop() + locationPlay.getHeight()/2;
        int finalRadius = (int)(locationPlay.getWidth()*1.5);
        Animator anim = ViewAnimationUtils.createCircularReveal(locationPlay,startX,startY,0,finalRadius);
        anim.setDuration(800);
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
        getSong();
    }


    /**
     * MUSIC PLAYER METHODS
     */

    public void playMusicSpot(MusicSpot m) {

        if (playing != NO_SONG_PLAYING) {
            replaceSongWith(m);
        } else {
            playing = m.getId();
            try {
                musicPlayer.play(m.getSongLink());
                setPlayOrPauseIcons(PAUSE);
            } catch (Exception e) {
                e.printStackTrace();
            }
            startSeek(0);
        }
        updatePlayerUI(m);
    }

    public void getSong() {
        new SpotCreator().show(getFragmentManager(), "Create a Spot");
    }

    public void stopMusic() {
        if (notMgr != null)
            notMgr.cancelAll();
        if (musicPlayer != null) {
            musicPlayer.pause();
            musicPlayer.seekToPosition(0);
            stopSeek();
            seekBar.setProgress(0);
        }
        playing = NO_SONG_PLAYING;
        setPlayOrPauseIcons(PLAY);
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

    public void updatePlayerUI(MusicSpot m) {

        TextView songName = (TextView) findViewById(R.id.songText);
        TextView albumAndArtist = (TextView) findViewById(R.id.artsitAlbumText);


        String songTitle = m.getSongName();
        if (songTitle != null) {
            songName.setText(songTitle);
        }

        String artist = m.getArtistName();
        String album = m.getAlbumName();

        if (artist != null) {
            albumAndArtist.setText(artist + " - ");
        }

        if (album != null) {
            albumAndArtist.setText(albumAndArtist.getText() + album);
        }

        new GetAlbumArtFromLink(m.getArtLink()).execute();

    }

    private class GetAlbumArtFromLink extends AsyncTask<Void, Void, Void> {

        String link;
        Bitmap art;

        public GetAlbumArtFromLink(String link) {
            this.link = link;
        }


        @Override
        protected Void doInBackground(Void... params) {
            try {

                HttpURLConnection con = (HttpURLConnection) new URL(link).openConnection();
                con.connect();
                art = BitmapFactory.decodeStream(con.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        public void onPostExecute(Void result) {

            ImageView albumArt = (ImageView) findViewById(R.id.artImageView);

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

    public MusicSpot getSpotFromCreator(SpotifyTrack song, String message) {
        MusicSpot m = new MusicSpot(song.getName(), song.getUri(), currentCircle.getCenter().latitude,
                currentCircle.getCenter().longitude);
        m.setMessage(message);
        m.setRadius(currentCircle.getRadius());
        m.setAlbumName(song.getAlbum());
        m.setArtistName(song.getArtist());
        m.setArtLink(song.getArt());
        m.setDurationInMils(song.getDurationInMils());
        m.setTime(System.currentTimeMillis() / 1000L);
        m.setId();

        return m;

    }

    @Override
    public void onRecieveSong(SpotifyTrack song, String message) {

        //create new Music Spot from Spotify Track and current location

        MusicSpot m = getSpotFromCreator(song, message);

        musicSpots.put(m.getId(), m);
        redrawCircles();

        //monitor new spot

        monitorGeofence(getGeofenceFromSpot(m));

        //send spot to server
        new addMusicSpotToServer(m).execute();

        addingGeofence = false;
        saveMusicSpotOffline(m.toString());
    }

    private void saveMusicSpotOffline(String spot) {
        try {
            File spotsFile = new File(context.getExternalFilesDir(Context.ACTIVITY_SERVICE), "saved.txt");
            if (!spotsFile.exists()) {
                spotsFile.createNewFile();
                Log.i(TAG, "spots file created");
            }
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(spotsFile.getAbsoluteFile(), true)));
            out.println(spot);
            out.close();
            Log.i(TAG, "saved to spot file");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void playOrPause(View v) {

        Log.i(TAG, "play or pause");


        if (musicPlayer != null && occupied != 0) {

            if (paused) {
                paused = false;
                musicPlayer.resume();
                setPlayOrPauseIcons(PAUSE);
                startSeek(seekBar.getProgress());
                Log.i(TAG, "Resuming Song");
            } else {
                paused = true;
                musicPlayer.pause();
                setPlayOrPauseIcons(PLAY);
                stopSeek();
                Log.i(TAG, "Pausing Song");
            }
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
     * MISC METHODS
     */

    public void onConfigurationChanged(Configuration newConfig) {

        super.onConfigurationChanged(newConfig);
        toggle.onConfigurationChanged(newConfig);

    }

    public boolean onCreateOptionsMenu(Menu menu) {

        optionsMenu = menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {


        if (toggle.onOptionsItemSelected(item)) {
            return true;
        } else if (item.getItemId() == R.id.maptype && googleMap != null) {

            if (!expanded) {
                boolean mapNormal = googleMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL;
                if (mapNormal)
                    googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                else
                    googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            } else {

                int vis = (findViewById(R.id.artImageView)).getVisibility();
                if (vis == View.VISIBLE) {
                    showMessage();
                } else {
                    hideMessage();
                }

            }


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

    private void showMessage() {

        ImageView art = (ImageView) findViewById(R.id.artImageView);
        TextView text = (TextView) findViewById(R.id.artText);
        MusicSpot currentSpot = musicSpots.get(occupied);

        ColorDrawable color = (ColorDrawable) art.getBackground();
        art.setVisibility(View.GONE);
        text.setVisibility(View.VISIBLE);
        text.setBackground(color);
        text.setText("\n\n\"" + currentSpot.getMessage() + "\"\n\nVisits: " + (currentSpot.getVisits() + 1));

        optionsMenu.findItem(R.id.maptype).setIcon(R.drawable.ic_account_box_white_36dp);

    }

    private void hideMessage() {
        ImageView art = (ImageView) findViewById(R.id.artImageView);
        TextView text = (TextView) findViewById(R.id.artText);
        text.setVisibility(View.GONE);
        art.setVisibility(View.VISIBLE);

        optionsMenu.findItem(R.id.maptype).setIcon(R.drawable.ic_book_open_page_variant_white_36dp);
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


