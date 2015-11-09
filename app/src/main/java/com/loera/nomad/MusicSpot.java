package com.loera.nomad;

import com.google.android.gms.maps.model.LatLng;

import java.io.File;

/**
 * Created by Daniel on 11/9/2015.
 * <p/>
 * :)
 */
public class MusicSpot {

    private String songName;
    private File songFile;
    private LatLng latLng;

    public MusicSpot(String s1, File s2, LatLng l){

        songName = s1;
        songFile = s2;
        latLng = l;

    }

    public String getSongName(){
        return songName;
    }
    public File getSongFile(){
        return songFile;
    }
    public LatLng getLatLng(){
        return latLng;
    }
}
