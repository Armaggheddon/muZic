package com.alebr.muzic;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MediaBrowserProvider, ListFragment.FragmentListListener, QueueFragment.QueueListener{

    @Override
    public void onPlayableItemClicked(String caller, String mediaId, long id) {
        //Called when a playable item is clicked on the fragment
        MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().playFromMediaId(caller, null);
        MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().skipToQueueItem(id);
        MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().play();
    }

    @Override
    public void onBrowsableItemClicked(String caller, String mediaId, long id) {

        ListFragment playableFragment = ListFragment.newInstance(mediaId);
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction()
                .replace(R.id.fragment_container, playableFragment)
                .addToBackStack(mediaId)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
        mToolbar.setNavigationIcon(R.drawable.ic_baseline_arrow);

    }

    @Override
    public MediaBrowserCompat getMediaBrowser() {
        return mMediaBrowser;
    }

    @Override
    public void onQueueItemClicked(String stringId, long id) {
        MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().skipToQueueItem(id);
    }

    @Override
    public void setToolbarTitle(String title) {
        mToolbar.setTitle(title);
    }

    private final ListFragment albumFragment = ListFragment.newInstance(MusicLibrary.ALBUMS);
    private final ListFragment artistFragment = ListFragment.newInstance(MusicLibrary.ARTISTS);
    private final ListFragment songsFragment = ListFragment.newInstance(MusicLibrary.SONGS);
    private final QueueFragment queueFragment = new QueueFragment();

    private static final String TAG = "MainActivity";

    //Request code for permission, can be any arbitrary number
    private final int READ_EXTERNAL_STORAGE_REQUEST_CODE = 101;

    private MediaBrowserCompat mMediaBrowser;
    private final BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @SuppressLint("DefaultLocale")
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            mToolbar.setTitle(R.string.app_name);
            mToolbar.setNavigationIcon(null);
            Fragment fragment = null;
            switch (item.getItemId()){
                case R.id.albums:

                    fragment = albumFragment;

                    break;
                case R.id.artists:

                    fragment = artistFragment;

                    break;
                case R.id.songs:

                    fragment = songsFragment;

                    break;
                case R.id.queue_nav:

                    fragment = queueFragment;

                    break;
            }

            if(fragment != null){
                getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .setTransition( FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();
            }

            return true;
        }
    };

    private MaterialToolbar mToolbar;
    private TextView title_text;
    private ImageView album_image;
    private FloatingActionButton play_pause_button;
    private ConstraintLayout smallPlayerLayout;
    private MotionLayout motionLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Sets the main theme for the activity
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mToolbar = findViewById(R.id.toolbar);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSupportFragmentManager().popBackStack();
                mToolbar.setNavigationIcon(null);
                mToolbar.setTitle(R.string.app_name);
            }
        });

        //If we are here we have the permission to read external storage
        mMediaBrowser = new MediaBrowserCompat(
                this,
                new ComponentName( this, MusicService.class),
                mConnectionCallback,
                null);

        final BottomNavigationView navigationView = findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(mOnNavigationListener);
        navigationView.setOnNavigationItemReselectedListener(null);

        smallPlayerLayout = findViewById(R.id.small_player);
        smallPlayerLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Launch full screen player", Toast.LENGTH_SHORT).show();
            }
        });

        motionLayout = findViewById(R.id.main_layout);

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
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    albumFragment).commit();
            buildTransportControls();
            super.onConnected();
        }
    };

    private void buildTransportControls(){

        album_image = findViewById(R.id.album_art_small_player);
        title_text = findViewById(R.id.title_small_player);
        play_pause_button = findViewById(R.id.play_pause_small_player);

        play_pause_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(MediaControllerCompat.getMediaController(MainActivity.this).getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING){
                    MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().pause();
                    play_pause_button.setImageResource(R.drawable.ic_play);
                }else{
                    MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().play();
                    play_pause_button.setImageResource(R.drawable.ic_pause);
                }
            }
        });
        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(MainActivity.this);

        if (mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING){
            play_pause_button.setImageResource(R.drawable.ic_pause);
            motionLayout.transitionToEnd();
            title_text.setText(mediaController.getMetadata().getDescription().getTitle());
            album_image.setImageBitmap(mediaController.getMetadata().getDescription().getIconBitmap());
        }

        if (mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PAUSED){
            MediaMetadataCompat metadata = mediaController.getMetadata();
            if (metadata != null) {
                play_pause_button.setImageResource(R.drawable.ic_play);
                title_text.setText(mediaController.getMetadata().getDescription().getTitle());
                album_image.setImageBitmap(mediaController.getMetadata().getDescription().getIconBitmap());
                motionLayout.transitionToEnd();
            }
        }

        mediaController.registerCallback(mControllerCallback);
    }

    private MediaControllerCompat.Callback mControllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            play_pause_button.setImageResource((state.getState()==PlaybackStateCompat.STATE_PLAYING) ? R.drawable.ic_pause : R.drawable.ic_play);
            if(motionLayout.getCurrentState() == motionLayout.getStartState())
                motionLayout.transitionToEnd();
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);

            album_image.setImageBitmap(metadata.getDescription().getIconBitmap());
            title_text.setText(metadata.getDescription().getTitle());

        }
    };

    @RequiresApi(Build.VERSION_CODES.M)
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
                //Since permissions are requested at runtime only on Android API 23+

                boolean showRationale = shouldShowRequestPermissionRationale(permissions[0]);
                if(!showRationale){
                    //The user clicked on the "never show again" checkbox on the dialog for permissions
                    androidx.appcompat.app.AlertDialog.Builder alert = new androidx.appcompat.app.AlertDialog.Builder(this);
                    alert.setTitle("Permission")
                            .setMessage("In order to play the music on your device the app requires the permission to read external storage.")
                            .setPositiveButton("GO TO SETTINGS", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //Set a dialog to
                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                                    intent.setData(uri);
                                    startActivity(intent);
                                }
                            }).setCancelable(false);
                    alert.create().show();
                }
                else {
                    androidx.appcompat.app.AlertDialog.Builder alert = new AlertDialog.Builder(this);
                    alert.setTitle("Permission")
                            .setMessage("In order to play the music on your device the app needs the permission to read external storage.")
                            .setPositiveButton("Try again", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                            READ_EXTERNAL_STORAGE_REQUEST_CODE);
                                }
                            });
                    //Dont allow the user to dismiss the dialog by touching outside the dialog window
                    alert.setCancelable(false);
                    alert.create().show();
                }
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
