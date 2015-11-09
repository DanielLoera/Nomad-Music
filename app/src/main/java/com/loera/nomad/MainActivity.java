package com.loera.nomad;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvingResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,SongChooser.SongListener{

    static boolean TEST_MODE = false;

    GoogleApiClient playServices;
    LinkedList<MusicSpot> musicSpots;
    LocationRequest locationRequest;
    Location currentLocation;
    Context context;
    PendingIntent mGeofencePendingIntent;
    public static boolean occupied;
    private final String TAG = "Main Activity";
    MapFragment map;
    GoogleMap googleMap;
    Marker currentMarker;
    LatLng currentLatLng;

    List<CircleOptions> circles;

    final int GEOFENCE_RADIUS = 45;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        map = (MapFragment) getFragmentManager().findFragmentById(R.id.mapFragment);
        map.getMapAsync(this);



        occupied = false;
        context = this;

        musicSpots = new LinkedList<>();
        circles = new LinkedList<>();
        createLocationRequest();

        buildGoogleApiClient();
        setupButtons();

        playServices.connect();


    }

    public void updateUI() {

        LinearLayout layout = (LinearLayout) findViewById(R.id.contentMain);

        TextView text = new TextView(context);
        text.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        text.setText("Geofence added with song: " + musicSpots.getLast().getSongName());
        layout.addView(text);

    }

    public Geofence getCurrentGeofence(Location currentLocation) {

        return new Geofence.Builder()
                .setRequestId(currentLocation.getLatitude()+","+currentLocation.getLongitude())
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

                boolean zoomCamera = false;
                if (currentLocation == null)
                    zoomCamera = true;

                currentLocation = location;
                currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                MarkerOptions marker = new MarkerOptions()
                        .position(currentLatLng);

                googleMap.clear();
                addCircles();
                googleMap.addMarker(marker);

                if (zoomCamera)
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14.0f));

                Log.i(TAG, "Location updated, Marker updated");

            }
        });
    }

    public void addCircles(){

        for(CircleOptions c:circles)
            googleMap.addCircle(c);

    }

    public void addMusicSpot(Geofence g) {

        addCircleAroundCurrentPosition();

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

                Log.i(TAG, "Monitoring Geofence Failed\nStatus: " + status.toString());

            }
        });

    }

    public void getSong(){

       SongChooser chooser = new SongChooser();
        chooser.show(getFragmentManager(),"TEST");

    }

    public void addCircleAroundCurrentPosition(){



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

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
       // Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        Intent intent = new Intent("com.aol.android.geofence.ACTION_RECEIVE_GEOFENCE");
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }

    public void setupButtons() {

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentLocation != null) {
                    if (!occupied) {
                        getSong();
                    } else {

                        Snackbar.make(view, "Current Position Occupied", Snackbar.LENGTH_LONG).show();

                    }
                } else
                    Snackbar.make(view, "Waiting on location...", Snackbar.LENGTH_LONG).show();
            }
        });

    }


    public void onDestroy() {
        super.onDestroy();

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

            }
        });

        this.googleMap = googleMap;



    }

    @Override
    public void onRecieveSong(File song) {

        musicSpots.add(new MusicSpot(song.getName(),song,currentLatLng));
        addMusicSpot(getCurrentGeofence(currentLocation));
        occupied = true;
        updateUI();

    }
}


