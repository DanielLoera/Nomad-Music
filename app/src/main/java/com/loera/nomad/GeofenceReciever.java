package com.loera.nomad;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

/**
 * Created by Daniel on 11/7/2015.
 * <p/>
 * :)
 */
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
                    MainActivity.occupied = true;
                }else if (transition == Geofence.GEOFENCE_TRANSITION_EXIT){

                    Log.i(TAG,"Exited geofence " + g.getRequestId());
                    Toast.makeText(context,"Exited geofence " + g.getRequestId(),Toast.LENGTH_LONG).show();
                    MainActivity.occupied = false;

                }

            }


        }

    }
}
