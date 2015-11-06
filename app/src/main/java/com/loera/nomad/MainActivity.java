package com.loera.nomad;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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

import java.util.LinkedList;

public class MainActivity extends AppCompatActivity {

    GoogleApiClient playServices;
    LinkedList<Geofence> geofences;
    LocationRequest locationRequest;
    Location currentLocation;
    Context context;
    PendingIntent mGeofencePendingIntent;
    public static boolean occupied;
    private final String TAG = "Main Activity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        occupied = false;
        context = this;

        geofences = new LinkedList<>();

        createLocationRequest();
        buildGoogleApiClient();
        setupButtons();

        playServices.connect();


    }

    public void updateUI() {

        LinearLayout layout = (LinearLayout) findViewById(R.id.contentMain);

        TextView text = new TextView(context);
        text.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        text.setText("Geofence added at:\n" +
                "Latitude: " + currentLocation.getLatitude() + "\n" +
                "Longitude: " + currentLocation.getLongitude());
        layout.addView(text);

    }

    public Geofence getCurrentGeofence() {

        return new Geofence.Builder()
                .setRequestId("TEST").setCircularRegion(currentLocation.getLatitude(), currentLocation.getLongitude(), 10.0f).setExpirationDuration(Geofence.NEVER_EXPIRE)
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

                currentLocation = location;

            }
        });
    }

    public void addGeofence(Geofence g) {

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
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        return PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }

    public void setupButtons() {

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentLocation != null) {
                    if (!occupied) {
                        updateUI();
                        addGeofence(getCurrentGeofence());
                        occupied = true;

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

                Log.i(TAG, "Removed geofences");

            }

            @Override
            public void onUnresolvableFailure(Status status) {
                Log.i(TAG, "Could not remove geofences, error: " + status.toString());
            }
        }); // Result processed in onResult().
    }

}


