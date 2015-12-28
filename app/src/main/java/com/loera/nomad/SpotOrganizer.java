package com.loera.nomad;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;

public class SpotOrganizer extends AppCompatActivity implements OnMapReadyCallback {

    HashMap<String,MusicSpot> spots;
    ArrayList<Integer> ids;
    GoogleMap googleMap;
    Context context;
    float density;
    Point screen;
    LinearLayout layout;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spot_organizer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        context = this;

        Intent intent = getIntent();
        screen = new Point();


        spots =(HashMap) intent.getSerializableExtra("spots");
        ids = intent.getIntegerArrayListExtra("ids");
        density = getResources().getDisplayMetrics().density;
        getWindowManager().getDefaultDisplay().getSize(screen);


        MapFragment map = (MapFragment) getFragmentManager().findFragmentById(R.id.mapOrgFragment);
        map.getMapAsync(this);

    }

    public void showUI(){

        googleMap.clear();

        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                spots.get(ids.get(0)).getLatlng(), 18.0f));

        int accentColorDark = getResources().getColor(R.color.colorAccentDark);

        layout = (LinearLayout)findViewById(R.id.spotsLinearLayout);

        int id = 0;

        for(int n : ids){
            MusicSpot m = spots.get(n);
            googleMap.addCircle(getCircle(m, accentColorDark));
            googleMap.addMarker(getMarker(m));
            layout.addView(getSpotView(m, id));
            id++;
        }

    }

    private View getSpotView(MusicSpot m,final int id ){

        final LinearLayout sidewaysLinearLayout = new LinearLayout(context);
        sidewaysLinearLayout.setId(id);
        sidewaysLinearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        sidewaysLinearLayout.setOrientation(LinearLayout.HORIZONTAL);

        TextView view = new TextView(this);
        view.setGravity(Gravity.CENTER);
        view.setClickable(true);
        view.setMaxWidth((int) (screen.x * .75));
        view.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        view.setText(m.getSongName() + " - " + m.getArtistName());
        view.setTextSize(5 * density);
        view.setId(id);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                spots.get(ids.get(v.getId())).getLatlng(), 18.0f)
                );
            }
        });

        sidewaysLinearLayout.addView(view);

        final ImageButton deleteButton = new ImageButton(context);
        deleteButton.setId(id);
        deleteButton.setScaleType(ImageView.ScaleType.FIT_XY);
        deleteButton.setImageResource(R.drawable.ic_trash);
        deleteButton.setClickable(true);
        deleteButton.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        deleteButton.setX(sidewaysLinearLayout.getWidth() - deleteButton.getWidth());
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final MusicSpot m = spots.get(ids.get(v.getId()));
                AlertDialog.Builder dialog = new AlertDialog.Builder(context);
                dialog.setTitle("Delete Music Spot");
                dialog.setMessage("Are you sure you want to delete the spot:\n" +
                        m.getSongName() + " ?");

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

        return sidewaysLinearLayout;
    }

    public void removeSpot(MusicSpot m){
        MainActivity.toRemove.add(m.getId());
        spots.remove(m.getId());
        ids.remove(new Integer(m.getId()));
        if(ids.size() == 0){
            onBackPressed();
        }
    }

    private MarkerOptions getMarker(MusicSpot m){
        MarkerOptions mark = new MarkerOptions();

        mark.title(m.getSongName());
        mark.position(m.getLatlng());

        return mark;
    }

    private CircleOptions getCircle(MusicSpot m,int stroke){
        CircleOptions circle = new CircleOptions();
        circle.center(m.getLatlng());
        circle.strokeWidth(10);
        circle.strokeColor(stroke);
        circle.fillColor(0x40C6096B);
        circle.radius(m.getRadius());

        return circle;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.setBuildingsEnabled(false);
        googleMap.setMyLocationEnabled(false);
        googleMap.getUiSettings().setScrollGesturesEnabled(false);
        this.googleMap = googleMap;
        showUI();

    }
}
