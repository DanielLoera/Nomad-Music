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
    private String songLink;
    private String albumName;
    private String artistName;
    private String artLink;
    private Double latt;
    private Double longi;
    private double radius;
    private long durationInMils;
    private int id;

    public MusicSpot(String s1, String s2, double l,double l2){

        songName = s1;
        songLink = s2;
        latt = l;
        longi = l2;

    }

    public String getSongName(){
        return songName;
    }
    public String getSongLink(){
        return songLink;
    }

    public LatLng getLatlng(){return  new LatLng(latt,longi);}

    protected MusicSpot(Parcel in) {
        songName = in.readString();
        songLink = in.readString();
        albumName = in.readString();
        artistName = in.readString();
        artLink = in.readString();
        durationInMils = in.readLong();
        radius = in.readDouble();
        latt = in.readByte() == 0x00 ? null : in.readDouble();
        longi = in.readByte() == 0x00 ? null : in.readDouble();
        id = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(songName);
        dest.writeString(songLink);
        dest.writeString(albumName);
        dest.writeString(artistName);
        dest.writeString(artLink);
        dest.writeLong(durationInMils);
        dest.writeDouble(radius);
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
        dest.writeInt(id);
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

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getArtLink() {
        return artLink;
    }

    public void setArtLink(String artLink) {
        this.artLink = artLink;
    }

    public long getDurationInMils() {
        return durationInMils;
    }

    public void setDurationInMils(long durationInMils) {
        this.durationInMils = durationInMils;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public int getId(){return id;}

    public void setId() {
        this.id = ((int)(songLink.hashCode() / (Math.random() * 42)) + (getLastThree(latt) + getLastThree(longi)));
    }

    public void setId(int id){
        this.id = id;
    }

    private int getLastThree(double d){

        String dS = d+"";

        String ans = ""+ dS.charAt(dS.length()-3) + dS.charAt(dS.length()-2) +dS.charAt(dS.length()-2);

        return Integer.parseInt(ans);
    }
}