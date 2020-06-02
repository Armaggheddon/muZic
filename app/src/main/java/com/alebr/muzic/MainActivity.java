package com.alebr.muzic;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.annotation.SuppressLint;
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
    public void onItemClicked(String id, long position) {
        MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().playFromMediaId(id, null);
        MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().play();
    }

    @Override
    public MediaBrowserCompat getMediaBrowser() {
        return mMediaBrowser;
    }

    @Override
    public void onQueueItemClicked(String id, long position) {
        MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().skipToQueueItem(position+1);
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
            navigationHistory.clear();
            mToolbar.setTitle(R.string.app_name);
            FragmentManager manager = getSupportFragmentManager();
            switch (item.getItemId()){
                case R.id.albums:
                    /*
                    mMediaBrowser.subscribe(MusicLibrary.ALBUMS, mSubscriptionCallback);
                    LAST_ITEM_CLICKED = R.id.albums;

                     */


                    manager.beginTransaction()
                            .replace(R.id.fragment_container, albumFragment)
                            .setTransition( FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .commit();

                    return true;
                case R.id.artists:
                    //mMediaBrowser.subscribe(MusicLibrary.ARTISTS, mSubscriptionCallback);
                    LAST_ITEM_CLICKED = R.id.artists;
                    manager.beginTransaction()
                            .replace(R.id.fragment_container, artistFragment)
                            .setTransition( FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .commit();
                    return true;
                case R.id.songs:
                    //mMediaBrowser.subscribe(MusicLibrary.SONGS, mSubscriptionCallback);
                    LAST_ITEM_CLICKED = R.id.songs;
                    manager.beginTransaction()
                            .replace(R.id.fragment_container, songsFragment)
                            .setTransition( FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .commit();
                    return true;
                case R.id.queue_nav:

                    LAST_ITEM_CLICKED = R.id.queue_nav;
                    manager.beginTransaction()
                            .replace(R.id.fragment_container, queueFragment)
                            .setTransition( FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .commit();
                    return true;
            }
            return false;
        }
    };

    private int LAST_ITEM_CLICKED;
    private ArrayList<String> navigationHistory = new ArrayList<>();

    private MaterialToolbar mToolbar;
    private TextView title_text;
    private ImageView album_image;
    private FloatingActionButton play_pause_button;
    private ConstraintLayout smallPlayerLayout;
    private MotionLayout motionLayout;

    private ListView listview;
    private ArrayAdapter<CustomListItem> listAdapter;
    private List<CustomListItem> elements = new ArrayList<>();

    /*
    private MediaBrowserCompat.SubscriptionCallback mSubscriptionCallback = new MediaBrowserCompat.SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children) {
            super.onChildrenLoaded(parentId, children);

            Log.d(TAG, "onChildrenLoaded: " + parentId);
            List<CustomListItem> tempElements = new ArrayList<>();
            for(MediaBrowserCompat.MediaItem item : children){
                tempElements.add(
                        new CustomListItem(
                                item.getMediaId(),
                                item.getDescription().getTitle().toString()
                        )
                );
            }

            elements.clear();
            elements.addAll(tempElements);
            listAdapter.notifyDataSetChanged();

            mMediaBrowser.unsubscribe(parentId);
        }
    };*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Sets the main theme for the activity
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mToolbar = findViewById(R.id.toolbar);
        //mToolbar.setTitle(R.string.app_name);

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


        /*
        listview = findViewById(R.id.listView);
        listAdapter = new ArrayAdapter<CustomListItem>(
                this,
                android.R.layout.simple_list_item_1,
                elements){
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = view.findViewById(android.R.id.text1);
                text.setText(elements.get(position).getTitle());
                return view;
            }
        };
        listview.setAdapter(listAdapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //TODO: fix MusicService.java
                if (listAdapter.getItem(position).getId().contains(MusicLibrary.ALBUM_) || listAdapter.getItem(position).getId().contains(MusicLibrary.ARTIST_)){
                    navigationHistory.add(listAdapter.getItem(position).getId());
                    mMediaBrowser.subscribe(listAdapter.getItem(position).getId(), mSubscriptionCallback);
                }
                else if (listAdapter.getItem(position).getId().contains(MusicLibrary.SONG_)){

                    //If the last subscription we make was songs
                    if(LAST_ITEM_CLICKED == R.id.songs){
                        MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().playFromMediaId(MusicLibrary.SONGS, null);
                        MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().skipToQueueItem(id);
                        MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().play();
                    }else {
                        if(LAST_ITEM_CLICKED != R.id.queue_nav)
                            MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().playFromMediaId(navigationHistory.get(navigationHistory.size()-1), null);
                        MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().skipToQueueItem(id);
                        MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().play();
                    }
                }
            }
        });

         */

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
            //mMediaBrowser.subscribe(MusicLibrary.ALBUMS, mSubscriptionCallback);
            super.onConnected();
        }
    };

    private void buildTransportControls(){
        //TODO: init the UI accordingly to the current playbackstate

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
        //TODO: sync initial state of the UI
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
