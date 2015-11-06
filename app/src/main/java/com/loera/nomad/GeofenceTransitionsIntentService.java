package com.loera.nomad;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

/**
 * Created by Daniel on 11/6/2015.
 * <p/>
 * :)
 */
public class GeofenceTransitionsIntentService extends IntentService {
    private final String TAG = "Transition Service";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public GeofenceTransitionsIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {

            Log.e(TAG, geofencingEvent.getErrorCode()+"");
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ) {

            MainActivity.occupied = true;
            Log.i(TAG,"Entered a Geofence");

        } else if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT){

            MainActivity.occupied = false;
            Log.i(TAG,"Exited a Geofence");
        }else{

            Log.i(TAG,"Something went wrong in monitoring geofences");
        }
    }
}
