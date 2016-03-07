 package com.loera.nomad;

 import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

 /**
 * Created by Daniel on 11/9/2015.
 * <p/>
 * :)
 */
public class SpotCreator extends DialogFragment {
     float density;

    View view;
    Context context;
    boolean pressed;
    SongListener songListener;
     SpotifyTrack[] tracks;
     SpotifyTrack selectedStrack;


    final String TAG = "Song Chooser";
     private LayoutInflater inflater;
     private LinearLayout layout;

     public interface SongListener{

        void onRecieveSong(SpotifyTrack song,String message);
        void noSelectionMade();

    }


    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().getWindow().setTitle("Create a Spot");
        return inflater.inflate(R.layout.song_chooser_layout,container,false);

    }
    private class SpotifySearch extends AsyncTask<Void,Void,Void>{

        String JSONURL;
        JSONArray items;

        public SpotifySearch(String text){
            JSONURL = formatText(text);
        }

        private String formatText(String s){
            String ans = "";
            String[] pieces = s.split(" ");
            for(int i = 0;i<pieces.length-1;i++){
                ans+= pieces[i] + "%20";
            }
            ans+= pieces[pieces.length-1];

            ans = "https://api.spotify.com/v1/search?q=" + ans + "&type=track";
            return ans;
        }

        private JSONObject getJSONFromString()throws IOException, JSONException{
            StringBuilder result = new StringBuilder();
            URL url = new URL(JSONURL);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            int code = urlConnection.getResponseCode();

            if (code == 200) {
                Log.i(TAG, "JSON successfully received.");
            }else{
                Log.i(TAG, "JSON error occurred.");
            }

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }

            urlConnection.disconnect();

            Log.i(TAG,result.toString());

            return new JSONObject(result.toString());


        }

        @Override
        protected Void doInBackground(Void... params) {
            try {

                JSONObject requestJSON = getJSONFromString();
                JSONObject tracks = requestJSON.getJSONObject("tracks");
                items = tracks.getJSONArray("items");

            }catch(Exception e) {
                e.printStackTrace();
            }
            return null;
            }

        public void onPostExecute(Void result){

            displayResults(items);

        }

    }

    public void onAttach(Activity activity){
        super.onAttach(activity);
        songListener = (SongListener)activity;
    }

    public void onStart(){

        super.onStart();

        view = getView();
        context = getActivity();
        inflater = LayoutInflater.from(context);
        density = getResources().getDisplayMetrics().density;
        SearchView search = (SearchView)view.findViewById(R.id.searchBox);
        layout = (LinearLayout) view.findViewById(R.id.fileLayout);
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                new SpotifySearch(query).execute();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

    }

    public void displayResults(JSONArray items){
        layout.removeAllViews();
        tracks = getTracksFromJSONArray(items);

        View[] textViews = getTextViewsFromTracks();

        for(View t:textViews){
            layout.addView(t);
        }


    }

     public View[] getTextViewsFromTracks(){

         View[] textViews = new View[tracks.length];

         for(int i = 0;i<textViews.length;i++){
             SpotifyTrack currentTrack = tracks[i];
             View result = inflater.inflate(R.layout.spotify_search_result,layout,false);
             result.setVisibility(View.INVISIBLE);
             ((TextView)result.findViewById(R.id.resultInfo)).setText(currentTrack.getName() + " - " + currentTrack.getArtist());
             result.setId(i);
             result.setOnClickListener(new View.OnClickListener() {
                 @Override
                 public void onClick(View v) {
                     selectedStrack = tracks[v.getId()];
                     showMessageCreator();
                 }
             });

             new SetTextViewArt(currentTrack.getArt(),result).execute();

             textViews[i] = result;

         }

         return textViews;

     }

     private void showMessageCreator() {
        SearchView search = (SearchView)view.findViewById(R.id.searchBox);
         ScrollView results = (ScrollView)view.findViewById(R.id.songResultsScroll);
         RelativeLayout messageCreator = (RelativeLayout)view.findViewById(R.id.messageCreator);
         search.setVisibility(View.GONE);
         results.setVisibility(View.GONE);
         messageCreator.setVisibility(View.VISIBLE);
         final EditText text = (EditText) view.findViewById(R.id.spotEditText);
         Button messageCreatorButton = (Button)view.findViewById(R.id.messageCreatorButton);
         messageCreatorButton.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                pressed = true;
                 songListener.onRecieveSong(selectedStrack,text.getText().toString());
                 dismiss();
             }
         });
     }


     public SpotifyTrack[] getTracksFromJSONArray(JSONArray a){

         SpotifyTrack[] tracks = new SpotifyTrack[a.length()];

         for(int i = 0;i< tracks.length;i++){
             try {
                 JSONObject currentJSON = a.getJSONObject(i);
                 SpotifyTrack currentTrack = new SpotifyTrack();

                 JSONObject album = currentJSON.getJSONObject("album");
                 JSONArray images = album.getJSONArray("images");
                 JSONArray artists = currentJSON.getJSONArray("artists");

                 String albumName = album.getString("name");
                 String name = currentJSON.getString("name");
                 String uri = currentJSON.getString("uri");
                 String art = ((JSONObject)images.get(0)).getString("url");
                 String artistName = ((JSONObject)artists.get(0)).getString("name");

                 currentTrack.setAlbum(albumName);
                 currentTrack.setArt(art);
                 currentTrack.setArtist(artistName);
                 currentTrack.setName(name);
                 currentTrack.setUri(uri);
                 currentTrack.setDurationInMils(currentJSON.getLong("duration_ms"));
                 tracks[i] = currentTrack;


             } catch (JSONException e) {
                 e.printStackTrace();
             }
         }

         return tracks;
     }

     private class SetTextViewArt extends AsyncTask<Void,Void,Void>{

         String link;
         Bitmap art;
         View view;

         public SetTextViewArt(String link,View view){
             this.link = link;
             this.view = view;
         }


         @Override
         protected Void doInBackground(Void... params) {
             try {

                 HttpURLConnection con = (HttpURLConnection)new URL(link).openConnection();
                 con.connect();
                 art = BitmapFactory.decodeStream(con.getInputStream());
             } catch (IOException e) {
                 e.printStackTrace();
             }
             return null;
         }

         public void onPostExecute(Void result){

             if(!pressed) {
                 ImageView albumArt = (ImageView)view.findViewById(R.id.resultAlbumArt);
                         albumArt.setImageBitmap(art);
                 albumArt.getLayoutParams().width = view.getHeight();
                 view.setVisibility(View.VISIBLE);
             }
         }
     }

     public void onDismiss(DialogInterface dialogInterface){

         super.onDismiss(dialogInterface);

         if(!pressed)
             songListener.noSelectionMade();

     }
}