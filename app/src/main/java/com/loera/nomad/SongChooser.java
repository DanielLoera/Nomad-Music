package com.loera.nomad;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

/**
 * Created by Daniel on 11/9/2015.
 * <p/>
 * :)
 */
public class SongChooser extends DialogFragment {

    final float FONT_SIZE = 30.0f;

    View view;
    Context context;
    String foundFile = "None";
    SongListener songListener;

    final String TAG = "Song Chooser";

    public interface SongListener{

        public void onRecieveSong(File song);
    }


    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return inflater.inflate(R.layout.song_chooser_layout,container,false);

    }

    public void onAttach(Activity activity){
        super.onAttach(activity);
        songListener = (SongListener)activity;

    }

    public void onStart(){

        super.onStart();

        view = getView();
        context = getActivity();
        showFileList(Environment.getExternalStorageDirectory().getAbsolutePath());

    }


    public void showFileList(String path){

        final File currentPath = new File(path);


        final LinearLayout layout = (LinearLayout) view.findViewById(R.id.fileLayout);

        TextView back = new TextView(context);
        back.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        back.setText("Back");
        back.setTextSize(FONT_SIZE);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                layout.removeAllViews();
                showFileList(currentPath.getParentFile().getAbsolutePath());

            }
        });

        layout.addView(back);


        if(currentPath.isDirectory()){

            File[] files = currentPath.listFiles();

            if(files!=null)
            for(File f: files){
                 final File currentFile  = f;
                Log.i(TAG,"found file/directory " + f.getName());

                if(f.isDirectory() || f.getName().endsWith(".mp3")){

                    TextView text = new TextView(context);
                    text.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    text.setText(f.getName());
                    text.setTextSize(FONT_SIZE);

                    text.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (currentFile.isDirectory()) {

                                layout.removeAllViews();
                                showFileList(currentFile.getAbsolutePath());

                            } else {

                                songListener.onRecieveSong(currentFile);
                                dismiss();

                            }
                        }
                    });

                    layout.addView(text);
                }


            }

        }else{

            Log.i(TAG,"Not a directory");
        }

    }
}
