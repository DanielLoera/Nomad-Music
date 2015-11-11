package com.loera.nomad;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Daniel on 11/9/2015.
 * <p/>
 * :)
 */
public class MusicSpot implements Parcelable {

    private String songName;
    private String songFile;
    private Double latt;
    private Double longi;

    public MusicSpot(String s1, String s2, double l,double l2){

        songName = s1;
        songFile = s2;
        latt = l;
        longi = l2;

    }

    public String getSongName(){
        return songName;
    }
    public String getSongFile(){
        return songFile;
    }
    public LatLng getLatLng(){
        return new LatLng(latt,longi);
    }

    public String getId(){

        return songName;

    }

    protected MusicSpot(Parcel in) {
        songName = in.readString();
        songFile = in.readString();
        latt = in.readByte() == 0x00 ? null : in.readDouble();
        longi = in.readByte() == 0x00 ? null : in.readDouble();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(songName);
        dest.writeString(songFile);
        if (latt == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeDouble(latt);
        }
        if (longi == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeDouble(longi);
        }
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<MusicSpot> CREATOR = new Parcelable.Creator<MusicSpot>() {
        @Override
        public MusicSpot createFromParcel(Parcel in) {
            return new MusicSpot(in);
        }

        @Override
        public MusicSpot[] newArray(int size) {
            return new MusicSpot[size];
        }
    };
}