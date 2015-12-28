package com.loera.nomad;

/**
 * Created by Daniel on 12/20/2015.
 * <p/>
 * :)
 */
public class SpotifyTrack {

    private String album, artist,name,art,uri;
    private long durationInMils;

    public void setAlbum(String a){album = a;}

    public  void setArtist(String a){artist = a;}

    public void setName(String n){name = n;}

    public void setArt(String a){art = a;}

    public String getAlbum(){return album;}

    public String getArtist(){return artist;}

    public String getName(){return name;}

    public String getArt(){return art;}

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public long getDurationInMils() {
        return durationInMils;
    }

    public void setDurationInMils(long durationInMils) {
        this.durationInMils = durationInMils;
    }
}
