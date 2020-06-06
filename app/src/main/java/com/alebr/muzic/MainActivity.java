package com.alebr.muzic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.app.Activity;
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
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity implements MediaBrowserProvider, ListFragment.FragmentListListener, QueueFragment.QueueFragmentListener {

    private static final String FRAGMENT_TO_REMOVE_SAVED_STATE_KEY = "fragment_tag_to_remove";

    @Override
    public void onPlayableItemClicked(String parentId, long positionInQueue) {
        //Called when a playable item is clicked on the fragment
        MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().playFromMediaId(parentId, null);
        MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().skipToQueueItem(positionInQueue);
        MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().play();
    }

    @Override
    public void onBrowsableItemClicked(String mediaId) {

        ListFragment playableFragment = ListFragment.newInstance(mediaId);
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction()
                .replace(R.id.fragment_container, playableFragment, mediaId)
                .addToBackStack(mediaId)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
        mToolbar.setNavigationIcon(R.drawable.ic_baseline_arrow);
        mToolbar.hideOverflowMenu();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mToolbar.setNavigationIcon(null);
        mToolbar.setTitle(R.string.app_name);
    }

    @Override
    public MediaBrowserCompat getMediaBrowser() {
        return mMediaBrowser;
    }

    @Override
    public void onQueueItemClicked(long positionInQueue) {
        MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().skipToQueueItem(positionInQueue);
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

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            mToolbar.setTitle(R.string.app_name);
            mToolbar.setNavigationIcon(null);

            String FRAGMENT_TAG = null;
            Fragment fragment = null;
            switch (item.getItemId()){
                case R.id.albums:

                    fragment = albumFragment;
                    FRAGMENT_TAG = ListFragment.ALBUM_FRAGMENT_TAG;

                    break;
                case R.id.artists:

                    fragment = artistFragment;
                    FRAGMENT_TAG = ListFragment.ARTIST_FRAGMENT_TAG;

                    break;
                case R.id.songs:

                    fragment = songsFragment;
                    FRAGMENT_TAG = ListFragment.SONG_FRAGMENT_TAG;

                    break;
                case R.id.queue_nav:

                    fragment = queueFragment;
                    FRAGMENT_TAG = QueueFragment.QUEUE_FRAGMENT_TAG;

                    break;
            }

            if(fragment != null){
                if(getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG) != fragment) {
                    getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, fragment, FRAGMENT_TAG)
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .commit();
                }else
                    getSupportFragmentManager().popBackStack();
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


        if(savedInstanceState != null){
            //Remove the fragment that was previously being shown allowing to restart the fragment
            //and have the content displayed instead of a white/black empty screen to avoid this
            //remove the fragment that was being displayed before the theme change
            String fragmentTag = savedInstanceState.getString(FRAGMENT_TO_REMOVE_SAVED_STATE_KEY);

            Fragment fragmentToRemove = getSupportFragmentManager().findFragmentByTag(fragmentTag);
            if(fragmentToRemove != null)
                getSupportFragmentManager().beginTransaction().remove(fragmentToRemove).commit();
        }

        mToolbar = findViewById(R.id.toolbar);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSupportFragmentManager().popBackStack();
                mToolbar.setNavigationIcon(null);
                mToolbar.setTitle(R.string.app_name);
            }
        });
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.option){
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                }
                return true;
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
                Intent intent = new Intent(MainActivity.this, FullPlayerActivity.class);
                startActivityForResult(intent, FullPlayerActivity.FULL_PLAYER_ACTIVITY_RESULT);
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

            boolean at_least_one_fragment = false;
            if(getSupportFragmentManager().getFragments().size() != 0)
                at_least_one_fragment = true;

            if(!at_least_one_fragment){

                //Update the current item selected in the bottom navigation view
                ((BottomNavigationView)findViewById(R.id.navigation)).setSelectedItemId(R.id.albums);

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, albumFragment, ListFragment.ALBUM_FRAGMENT_TAG)
                        .setTransition( FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();
            }


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
            title_text.setText(mediaController.getMetadata().getDescription().getTitle());
            album_image.setImageBitmap(mediaController.getMetadata().getDescription().getIconBitmap());
            motionLayout.transitionToEnd();
        }

        if (mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PAUSED){
            MediaMetadataCompat metadata = mediaController.getMetadata();
            if (metadata != null) {
                play_pause_button.setImageResource(R.drawable.ic_play);
                title_text.setText(metadata.getDescription().getTitle());
                album_image.setImageBitmap(metadata.getDescription().getIconBitmap());
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


    @RequiresApi(Build.VERSION_CODES.M) //Need to check only on version later than M since before permissions are asked at install time
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
            //Permission is GRANTED so we can connect to the MusicService if not already connected
            if(!mMediaBrowser.isConnected())
                mMediaBrowser.connect();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        //If a fragment is currently active save its tag in the saved state.
        //It the user changes theme and the value of the setting is default then the UI will
        //be redrawn every time the theme changes. To prevent the fragment not being shown due to
        //the fact that it exist but does not get instantiated again because it is not destroyed
        //save its tag to allow to remove it in the on create and then can be recreated.
        String fragmentTag = null;

        //Since at any time we are showing only one fragment, when we find the first fragment we break
        //because there arent any other fragments
        for(Fragment fragment : getSupportFragmentManager().getFragments()){
            fragmentTag = fragment.getTag();
            break;
        }

        Log.d(TAG, "onSaveInstanceState: " + fragmentTag);

        outState.putString(FRAGMENT_TO_REMOVE_SAVED_STATE_KEY, fragmentTag);

        super.onSaveInstanceState(outState);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == FullPlayerActivity.FULL_PLAYER_ACTIVITY_RESULT){
            if(resultCode == Activity.RESULT_OK){
                if(data.getBooleanExtra(FullPlayerActivity.METADATA_NOT_AVAILABLE, false)){
                    motionLayout.transitionToStart();
                }
            }
        }

    }
}
