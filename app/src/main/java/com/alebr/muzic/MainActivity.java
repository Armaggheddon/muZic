package com.alebr.muzic;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.MediaController;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    //Request code for permission, can be any arbitrary number
    private final int READ_EXTERNAL_STORAGE_REQUEST_CODE = 101;

    private MediaBrowserCompat mMediaBrowser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Sets the main theme for the activity
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        //If we are here we have the permission to read external storage
        mMediaBrowser = new MediaBrowserCompat(
                this,
                new ComponentName( this, MusicService.class),
                mConnectionCallback,
                null);
    }

    private final MediaBrowserCompat.ConnectionCallback mConnectionCallback = new MediaBrowserCompat.ConnectionCallback(){
        @Override
        public void onConnected() {
            MediaSessionCompat.Token token = mMediaBrowser.getSessionToken();
            MediaControllerCompat mediaController = null;
            try{
                mediaController = new MediaControllerCompat(
                        MainActivity.this, token);
            }catch (RemoteException e){
                e.printStackTrace();
            }
            MediaControllerCompat.setMediaController(MainActivity.this, mediaController);
            buildTransportControls();
            //super.onConnected();
        }
    };

    private void buildTransportControls(){
        //TODO: init the UI accordingly to the current playbackstate
        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(MainActivity.this);
        //TODO: sync initial state of the UI
        mediaController.registerCallback(mControllerCallback);
    }

    private MediaControllerCompat.Callback mControllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            //TODO: handle playbackstate change
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
            //TODO: handle metadata change, use to update ui accordingly
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == READ_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //We have the permission to read to external storage
                Log.d(TAG, "onRequestPermissionsResult: PERMISSION GRANTED");
                mMediaBrowser.connect();
            } else {
                //TODO: add strings to strings.xml
                // the user denied the permission no permission
                Log.d(TAG, "onRequestPermissionsResult: PERMISSION_DENIED");
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle("Permission")
                        .setMessage("Read external storage permission is required to play music")
                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                        READ_EXTERNAL_STORAGE_REQUEST_CODE);
                            }
                        })
                        .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //Quit the application, it does not work without the permission
                                finish();
                            }
                        });
                alert.create().show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //If we dont have the permission ask for them
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    READ_EXTERNAL_STORAGE_REQUEST_CODE);
        }
        else{
            //Permission is GRANTED so we can connect to the MusicService
            mMediaBrowser.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Set the volume buttons to handle the multimedia stream instead of the notification or other
        //sounds coming from the device
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onStop() {
        super.onStop();
        //If the activity has a media controller, unregister the callback from the session
        if(MediaControllerCompat.getMediaController(MainActivity.this) != null){
            MediaControllerCompat.getMediaController(MainActivity.this).unregisterCallback(mControllerCallback);
        }
        //Disconnect from the MusicService
        mMediaBrowser.disconnect();
    }
}
