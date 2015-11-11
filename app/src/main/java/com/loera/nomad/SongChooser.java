package com.loera.nomad;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

/**
 * Created by Daniel on 11/9/2015.
 * <p/>
 * :)
 */
public class SongChooser extends DialogFragment {

    final float FONT_SIZE = 25.0f;

    View view;
    Context context;
    String foundFile;
    SongListener songListener;

    final String TAG = "Song Chooser";

    public interface SongListener{

        void onRecieveSong(File song);
        void noSelectionMade();

    }


    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().getWindow().setTitle("Choose A Song");
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

    public void onDismiss(DialogInterface dialogInterface){

        super.onDismiss(dialogInterface);

        if(foundFile == null)
            songListener.noSelectionMade();

    }


    public void showFileList(String path){

        final File currentPath = new File(path);


        final LinearLayout layout = (LinearLayout) view.findViewById(R.id.fileLayout);

        TextView back = (TextView)view.findViewById(R.id.songChooserBackButton);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                layout.removeAllViews();
                showFileList(currentPath.getParentFile().getAbsolutePath());

            }
        });


        if(currentPath.isDirectory()){

            File[] files = currentPath.listFiles();

            if(files!=null)
            for(File f: files){
                 final File currentFile  = f;

                if(f.isDirectory() || f.getName().endsWith(".mp3")){

                    TextView text = new TextView(context);
                    text.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    text.setText(f.getName());
                    text.setTextSize(FONT_SIZE);
                    int padding  =(int) getResources().getDimension(R.dimen.padding_text_view);
                    text.setPadding(padding,padding,padding,padding);

                    if(f.isDirectory())
                        text.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_folder,0,0,0);
                    else
                        text.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_file_music,0,0,0);

                    text.setCompoundDrawablePadding(15);

                    text.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (currentFile.isDirectory()) {

                                layout.removeAllViews();
                                showFileList(currentFile.getAbsolutePath());

                            } else {

                                songListener.onRecieveSong(currentFile);
                                foundFile = currentFile.getAbsolutePath();
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
