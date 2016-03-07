package com.loera.nomad;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.ui.IconGenerator;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class SpotOrganizer extends AppCompatActivity implements OnMapReadyCallback {

    HashMap<Integer, MusicSpot> spots;
    ArrayList<Integer> ids;
    GoogleMap googleMap;
    Context context;
    float density;
    Point screen;
    LinearLayout layout;
    String location;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spot_organizer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        context = this;

        Intent intent = getIntent();
        screen = new Point();

        spots = (HashMap<Integer,MusicSpot>)(intent.getSerializableExtra("spots"));
        location = intent.getStringExtra("location");
        ids = new ArrayList<>();
        ids.addAll(spots.keySet());

        density = getResources().getDisplayMetrics().density;
        getWindowManager().getDefaultDisplay().getSize(screen);


        MapFragment map = (MapFragment) getFragmentManager().findFragmentById(R.id.mapOrgFragment);
        map.getMapAsync(this);

    }

    public void showUI() {

        googleMap.clear();

        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                spots.get(ids.get(0)).getLatlng(), 18.0f));

        int accentColorDark = getResources().getColor(R.color.colorAccentDark);

        layout = (LinearLayout) findViewById(R.id.spotsLinearLayout);

        int id = 0;

        for (int n : ids) {
            MusicSpot m = spots.get(n);
            googleMap.addCircle(getCircle(m, accentColorDark));
            googleMap.addMarker(getMarker(m));
            layout.addView(getSpotView(m, id));
            id++;
        }

    }

    private View getSpotView(MusicSpot m, final int id) {

        final LinearLayout sidewaysLinearLayout = new LinearLayout(context);
        sidewaysLinearLayout.setId(id);
        sidewaysLinearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        sidewaysLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        sidewaysLinearLayout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView spotInfo = new TextView(this);
        spotInfo.setGravity(Gravity.CENTER);
        spotInfo.setClickable(true);
        spotInfo.setMaxWidth((int) (screen.x * .75));
        spotInfo.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        spotInfo.setText(m.getMessage());
        spotInfo.setTextSize(5 * density);
        spotInfo.setId(id);
        spotInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                spots.get(ids.get(v.getId())).getLatlng(), 18.0f)
                );
            }
        });

        new SetVisitorCount(spotInfo).execute();

        RelativeLayout spotInfoLayout = new RelativeLayout(context);

        RelativeLayout.LayoutParams rel = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        rel.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        spotInfo.setLayoutParams(rel);
        spotInfoLayout.addView(spotInfo);

        final ImageButton deleteButton = new ImageButton(context);
        deleteButton.setId(m.getId());
        deleteButton.setScaleType(ImageView.ScaleType.FIT_XY);
        deleteButton.setImageResource(R.drawable.ic_trash);
        deleteButton.setClickable(true);
        deleteButton.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final MusicSpot m = spots.get(deleteButton.getId());
                AlertDialog.Builder dialog = new AlertDialog.Builder(context);
                dialog.setTitle("Delete Music Spot");
                dialog.setMessage("Are you sure you want to delete the spot:\n" +
                        "\"" + m.getSongName() + "\"?");

                dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        layout.removeView(sidewaysLinearLayout);
                        removeSpot(m);
                    }
                });
                dialog.setNegativeButton("Cancel", null);

                AlertDialog d = dialog.create();
                d.show();

            }
        });

        sidewaysLinearLayout.addView(deleteButton);
        sidewaysLinearLayout.addView(spotInfoLayout);

        return sidewaysLinearLayout;
    }

    public void removeSpot(MusicSpot m) {
        MainActivity.toRemove.add(m.getId());
        if (ids.size() == 1) {
            onBackPressed();
        }else{
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    spots.get(ids.get(0)).getLatlng(), 18.0f));
        }
        ids.remove(new Integer(m.getId()));
    }

    private MarkerOptions getMarker(MusicSpot m) {
        MarkerOptions mark = new MarkerOptions();


        mark.position(m.getLatlng());
        IconGenerator iconGenerator = new IconGenerator(this);
        iconGenerator.makeIcon("\""+m.getMessage()+"\"");
        iconGenerator.setStyle(IconGenerator.STYLE_BLUE);
        mark.icon(BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon()));

        return mark;
    }

    private CircleOptions getCircle(MusicSpot m, int stroke) {
        CircleOptions circle = new CircleOptions();
        circle.center(m.getLatlng());
        circle.strokeWidth(10);
        circle.strokeColor(stroke);
        circle.fillColor(0x40C6096B);
        circle.radius(m.getRadius());


        return circle;
    }

    private class SetVisitorCount extends AsyncTask<Void,Void,Void>{

        TextView textView;
        int id;
        int count;

        public SetVisitorCount(TextView view){
            this.textView = view;
        }

        public void onPreExecute(){
            id = spots.get(ids.get(textView.getId())).getId();
        }

        @Override
        protected Void doInBackground(Void... params) {
            StringBuilder result = new StringBuilder();
            URL url = null;
            try {

                url = new URL("http://www.dannyloera.com/nomad/get_visitor_count.php?location="+location+"&id="+id);

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestMethod("GET");

                urlConnection.connect();

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                count = Integer.parseInt(result.toString());

                Log.i("Server", "Count for " + id + " on " + location +": " + count);

                urlConnection.disconnect();
            }catch(Exception e){

                e.printStackTrace();
            }
            return null;
        }

        public void onPostExecute(Void d){

            Log.i("SERVER", "getting visitor count for " + spots.get(ids.get(textView.getId())).getId()+ " from " + location);
            textView.setText(textView.getText()+"\nVisitors: "+ count);

        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.setBuildingsEnabled(false);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        googleMap.setMyLocationEnabled(false);
        googleMap.getUiSettings().setScrollGesturesEnabled(false);
        this.googleMap = googleMap;
        showUI();

    }
}
