package com.armaggheddon.muzic.ui;

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
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

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

import com.armaggheddon.muzic.MusicService;
import com.armaggheddon.muzic.R;
import com.armaggheddon.muzic.library.MusicLibrary;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * MainActivity, handles the {@link R.layout#activity_main} layout with the controls for
 * {@link R.layout#small_player} and a 4 tab navigation with fragments, 3 of type
 * {@link ListFragment} and one of type {@link QueueFragment}
 */

public class MainActivity extends AppCompatActivity implements MediaBrowserProvider, ListFragment.FragmentListListener, QueueFragment.QueueFragmentListener {

    private static final String TAG = "MainActivity";

    /* Key for extra arguments that has the fragment tag to remove and update */
    private static final String FRAGMENT_TO_REMOVE_SAVED_STATE_KEY = "fragment_tag_to_remove";

    /* Key for extra arguments for launcher shortcuts */
    public static final String LAUNCHER_SHORTCUTS_INTENT_KEY = "launcher_intent_key";

    /* Request code for storage permission, can be any arbitrary number */
    private final int READ_EXTERNAL_STORAGE_REQUEST_CODE = 101;

    /* Get the fragments that will ba used from the activity */
    private static final ListFragment albumFragment = ListFragment.newInstance(MusicLibrary.ALBUMS);
    private static final ListFragment artistFragment = ListFragment.newInstance(MusicLibrary.ARTISTS);
    private static final ListFragment songsFragment = ListFragment.newInstance(MusicLibrary.SONGS);
    private final QueueFragment queueFragment = new QueueFragment();

    private MediaBrowserCompat mMediaBrowser;

    /* Ui widgets */
    private MaterialToolbar mToolbar;
    private TextView title_text;
    private ImageView album_image;
    private ImageView play_pause_button;
    private MotionLayout motionLayout;

    /*
    Default fragment that will be launched when the application launch with the navigation item to
    select to align the navigation with the fragment opened and the fragment TAG
    */
    private Fragment defaultFragment = albumFragment;
    private int defaultItemInNavigation = R.id.albums_nav;
    private String defaultFragmentTag = ListFragment.ALBUM_FRAGMENT_TAG;

    /* Callback methods to notify the session and the activity of what is being clicked in the fragment */
    @Override
    public void onPlayableItemClicked(String parentId, long positionInQueue) {

        /*
        Called when a playable item is clicked in a fragment. Call playFromMediaId to create a
        queue of related items, skip to the selected item and start the playback
        */
        MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().playFromMediaId(parentId, null);
        MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().skipToQueueItem(positionInQueue);
        MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().play();
    }

    @Override
    public void onBrowsableItemClicked(String mediaId) {
        /*
        Called when a browsable item is clicked.
        Create a new ListFragment and replace the current one with the new one that subscribes
        to the parentId clicked in the ListFragment that registered the click event.
        Add the new fragment to the backstack to allow the back button to restore the previous
        fragment
         */
        ListFragment playableFragment = ListFragment.newInstance(mediaId);
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction()
                .replace(R.id.fragment_container, playableFragment, mediaId)
                .addToBackStack(mediaId)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();

        /* Show on the toolbar a back button that allows to pop the back stack */
        mToolbar.setNavigationIcon(R.drawable.ic_baseline_arrow);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        /*
        Override onBackPressed to remove the navigation icon on the toolbar when the back button is
        clicked and update the toolbar title to the default one
         */
        mToolbar.setNavigationIcon(null);
        mToolbar.setTitle(R.string.app_name);
    }

    @Override
    public MediaBrowserCompat getMediaBrowser() {

        /* Return the MediaBrowserCompat that the activity has */
        return mMediaBrowser;
    }

    @Override
    public void onQueueItemClicked(long positionInQueue) {

        /* Called when an item in the QueueFragment is clicked. Skip to the queue item clicked */
        MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().skipToQueueItem(positionInQueue);
    }

    @Override
    public void setToolbarTitle(String title) {

        /* Called from ListFragment when a browsable item is clicked, this allow a contextual navigation */
        mToolbar.setTitle(title);
    }

    private final BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            /* Reset the title back to the default one and remove the back icon */
            mToolbar.setTitle(R.string.app_name);
            mToolbar.setNavigationIcon(null);

            String FRAGMENT_TAG = null;
            Fragment fragment = null;
            switch (item.getItemId()) {
                case R.id.albums_nav:

                    /* The album icon on the navigation bar is clicked, get the Album fragment */
                    fragment = albumFragment;
                    FRAGMENT_TAG = ListFragment.ALBUM_FRAGMENT_TAG;

                    break;
                case R.id.artists_nav:

                    /* The artist icon on the navigation bar is clicked, get the Artist fragment */
                    fragment = artistFragment;
                    FRAGMENT_TAG = ListFragment.ARTIST_FRAGMENT_TAG;

                    break;
                case R.id.songs_nav:

                    /* The song icon on the navigation bar is clicked, get the Song fragment */
                    fragment = songsFragment;
                    FRAGMENT_TAG = ListFragment.SONG_FRAGMENT_TAG;

                    break;
                case R.id.queue_nav:

                    /* The queue icon on the navigation bar is clicked, get the Queue fragment */
                    fragment = queueFragment;
                    FRAGMENT_TAG = QueueFragment.QUEUE_FRAGMENT_TAG;

                    break;
            }

            /* If the fragment is not null (one valid item was clicked) */
            if (fragment != null) {

                /* If the new fragment to launch is different from the previous one */
                if (getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG) != fragment) {

                    /* Remove all fragments from the back stack to cancel the navigation */
                    getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

                    /* Replace the view with the new fragment and apply a fade transition */
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, fragment, FRAGMENT_TAG)
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                            .commit();
                } else
                    /* Else we are already on the fragment clicked just pop the back stack to return to the "root" */
                    getSupportFragmentManager().popBackStack();
            }

            /* Return true to tell that the event has been handled */
            return true;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        /* Replace the SplashScreen theme with the AppTheme*/
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Get any intent, if it is not null */
        Intent openIntent = getIntent();
        if (openIntent != null) {

            /*
            The user launched the application using a launcher shortcut published get the data in
            the intent. The value is the fragment TAG to launch
            */
            String intentdata = openIntent.getStringExtra(LAUNCHER_SHORTCUTS_INTENT_KEY);

            /* If the intent data is not null */
            if (intentdata != null) {

                /* Check for what fragment needs to be opened and set the default values */
                defaultFragmentTag = intentdata;
                switch (intentdata) {
                    case ListFragment.ARTIST_FRAGMENT_TAG:
                        defaultFragment = artistFragment;
                        defaultItemInNavigation = R.id.artists_nav;
                        break;
                    case ListFragment.SONG_FRAGMENT_TAG:
                        defaultFragment = songsFragment;
                        defaultItemInNavigation = R.id.songs_nav;
                        break;
                    case QueueFragment.QUEUE_FRAGMENT_TAG:
                        defaultFragment = queueFragment;
                        defaultItemInNavigation = R.id.queue_nav;
                        break;
                }
            }
        }

        /*
        If a configuration changes, such as a theme change we can restore the previous savedState
        that is the current visible fragment.
        */
        if (savedInstanceState != null) {
            /*
            Remove the fragment that was previously visible allowing to restart the fragment
            and have the content displayed instead of a white/black empty screen. To avoid this
            remove the fragment that was being displayed before the theme change so we can restart
            it on activity recreation
             */
            String fragmentTag = savedInstanceState.getString(FRAGMENT_TO_REMOVE_SAVED_STATE_KEY);

            Fragment fragmentToRemove = getSupportFragmentManager().findFragmentByTag(fragmentTag);

            /* If the fragment to remove is not null */
            if (fragmentToRemove != null) {

                /* Get the fragment data and set it to the default values before removing it */
                defaultFragment = fragmentToRemove;
                defaultFragmentTag = defaultFragment.getTag();
                if (defaultFragmentTag != null) {
                    switch (defaultFragmentTag) {
                        case ListFragment.ARTIST_FRAGMENT_TAG:
                            defaultItemInNavigation = R.id.artists_nav;
                            break;
                        case ListFragment.SONG_FRAGMENT_TAG:
                            defaultItemInNavigation = R.id.songs_nav;
                            break;
                        case QueueFragment.QUEUE_FRAGMENT_TAG:
                            defaultItemInNavigation = R.id.queue_nav;
                            break;
                    }

                    /* Remove the fragment */
                    getSupportFragmentManager().beginTransaction().remove(fragmentToRemove).commit();
                }
            }
        }

        mToolbar = findViewById(R.id.toolbar);

        /*
        Set the navigation click listener for the back button on the toolbar.
        This listener only works when the back arrow icon is set
        */
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSupportFragmentManager().popBackStack();

                /* Restore the default toolbar status */
                mToolbar.setNavigationIcon(null);
                mToolbar.setTitle(R.string.app_name);
            }
        });

        /* Set the on click listener for the options button on the toolbar */
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.option) {

                    /* Launch the option activity */
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                }

                /* Return true to tell that the event was handled */
                return true;
            }
        });

        /* If the execution is here the application has the "read external storage permission" */
        mMediaBrowser = new MediaBrowserCompat(
                this,
                new ComponentName(this, MusicService.class),
                mConnectionCallback,
                null);


        final BottomNavigationView navigationView = findViewById(R.id.navigation);

        /* Set the onClick listener for the items in the view */
        navigationView.setOnNavigationItemSelectedListener(mOnNavigationListener);

        /* Get a reference to the small player layout */
        ConstraintLayout smallPlayerLayout = findViewById(R.id.small_player);
        smallPlayerLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /* When the layout is clicked launch the activity FullPlayerActivity */
                Intent intent = new Intent(MainActivity.this, FullPlayerActivity.class);

                /* Start the activity for the result to receive, when the activity ends, the result */
                startActivityForResult(intent, FullPlayerActivity.FULL_PLAYER_ACTIVITY_RESULT);
            }
        });

        /* Get a reference to the "root" of the layout used by MainActivity */
        motionLayout = findViewById(R.id.main_layout);

    }

    private final MediaBrowserCompat.ConnectionCallback mConnectionCallback = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            MediaSessionCompat.Token token = mMediaBrowser.getSessionToken();
            MediaControllerCompat mediaController = null;
            try {

                /* Get a new media controller that allows the activity to send transport controls to the session */
                mediaController = new MediaControllerCompat(
                        MainActivity.this, token);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            MediaControllerCompat.setMediaController(MainActivity.this, mediaController);

            /*
            If a fragment already exists set the flag to true to avoid replacing an existing
            fragment
            */
            boolean at_least_one_fragment = false;
            if (getSupportFragmentManager().getFragments().size() != 0)
                at_least_one_fragment = true;

            /* If no fragments are active */
            if (!at_least_one_fragment) {

                /* Update the current item selected in the bottom navigation view */
                ((BottomNavigationView) findViewById(R.id.navigation)).setSelectedItemId(defaultItemInNavigation);

                /*
                Replace the view with the fragment, if savedInstanceState is null the default fragment
                launched is AlbumFragment (ListFragment) but if the activity was destroyed and
                recreated (for example a theme change) then the default fragment is the last active
                fragment. Default fragment can also be the fragment that represent the launcher
                shortcut clicked ( Artist, Songs, Queue)
                */
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, defaultFragment, defaultFragmentTag)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commit();
            }

            /* Initialize the views in the correct state */
            buildTransportControls();
            super.onConnected();
        }
    };

    /**
     * Initialize the views to match the current playback state and metadata
     */
    private void buildTransportControls() {

        album_image = findViewById(R.id.album_art_small_player);
        title_text = findViewById(R.id.title_small_player);
        play_pause_button = findViewById(R.id.play_pause_small_player);

        play_pause_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /* If the current state is PLAYING then the click event represent a pause action */
                if (MediaControllerCompat.getMediaController(MainActivity.this).getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING) {
                    MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().pause();

                    /* Update the image on the button to represent the new action */
                    play_pause_button.setImageResource(R.drawable.ic_play);
                }

                /* Else the event is in state PAUSED, then the the click event represent a play action */
                else {
                    MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().play();

                    /* Update the image on the button to represent the new action */
                    play_pause_button.setImageResource(R.drawable.ic_pause);
                }
            }
        });

        /* Initialize the small player layout elements */
        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(MainActivity.this);

        /*
        If the current state is PLAYING set play_pause_button in the play state (pause icon), set
        the album image on the image view, se the title of the song and then animate the small
        player to be visible with a slide up animation
        */
        if (mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING) {
            play_pause_button.setImageResource(R.drawable.ic_pause);
            title_text.setText(mediaController.getMetadata().getDescription().getTitle());
            album_image.setImageBitmap(mediaController.getMetadata().getDescription().getIconBitmap());
            motionLayout.transitionToEnd();
        }

        /*
        If the state is PAUSED but not STOPPED,... then set play_pause_button in the paused state
        (play icon), set the album image on the image view, set the title of the song and then
        animate the small player to be visible with a slide_up_animation
        */
        if (mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PAUSED) {
            MediaMetadataCompat metadata = mediaController.getMetadata();
            if (metadata != null) {
                play_pause_button.setImageResource(R.drawable.ic_play);
                title_text.setText(metadata.getDescription().getTitle());
                album_image.setImageBitmap(metadata.getDescription().getIconBitmap());
                motionLayout.transitionToEnd();
            }
        }

        /* Register a callback to receive updates from the session */
        mediaController.registerCallback(mControllerCallback);
    }

    private MediaControllerCompat.Callback mControllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);

            /* When the playback state changes update the icons on the play_pause_button */
            play_pause_button.setImageResource((state.getState() == PlaybackStateCompat.STATE_PLAYING) ? R.drawable.ic_pause : R.drawable.ic_play);

            /* If the current session state is PAUSED or PLAYING */
            if (state.getState() == PlaybackStateCompat.STATE_PAUSED || state.getState() == PlaybackStateCompat.STATE_PLAYING) {

                /*
                If the current state of the layout is in the started state animate the to the end
                state (small player layout visible)
                */
                if (motionLayout.getCurrentState() == motionLayout.getStartState())
                    motionLayout.transitionToEnd();
            }

            /* Hide the small player if the session state changes to stopped */
            if (state.getState() == PlaybackStateCompat.STATE_STOPPED) {

                /*
                If the current state of the layout is in the end state animate to the start state
                (small player layout hidden)
                */
                if (motionLayout.getCurrentState() == motionLayout.getEndState())
                    motionLayout.transitionToStart();
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);

            /* When the metadata changes update the title and the album art */
            album_image.setImageBitmap(metadata.getDescription().getIconBitmap());
            title_text.setText(metadata.getDescription().getTitle());

        }


    };


    /* Only check on version later than M since before permissions are asked at install time */
    @RequiresApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == READ_EXTERNAL_STORAGE_REQUEST_CODE) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                /* We have the permission to read to external storage */
                //Log.d(TAG, "onRequestPermissionsResult: permission granted");
                mMediaBrowser.connect();
            } else {
                /* The user denied the permission */
                //Log.d(TAG, "onRequestPermissionsResult: permission denied");

                /*
                This allows to know if the user clicked on "Never show again" toggle. If it is the
                case the system dialog asking for permission will not be displayed again and the
                application has no chance on obtaining the permission from the system.
                If the flag showRationale is set to false then this is the case.
                Then we have to route the user to the application settings and make him manually
                give the permission to read external storage
                */
                boolean showRationale = shouldShowRequestPermissionRationale(permissions[0]);
                if (!showRationale) {

                    /*
                    The user clicked on the "never show again"
                    Create a dialog with the information on why we need the permission with a button
                    to route the user to the application settings page.
                    Set the cancelable flag on the dialog to false to disable the chance to dismiss
                    the dialog clicking outside of the layout
                    */
                    androidx.appcompat.app.AlertDialog.Builder alert = new androidx.appcompat.app.AlertDialog.Builder(this);
                    alert.setTitle(getString(R.string.permission_denied_title))
                            .setMessage(getString(R.string.permission_denied_never_show_again_message))
                            .setPositiveButton(getString(R.string.permission_denied_never_show_again_button_text), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    /*
                                    Create an intent that launches the settings page of the
                                    application. In this page the user has to give the permission
                                    manually. It is also possible to delete the application data
                                    to make the system ask for the permission at launch time one
                                    more time
                                    */
                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                                    intent.setData(uri);
                                    startActivity(intent);
                                }
                            })
                            .setCancelable(false);
                    alert.create().show();
                } else {

                    /*
                    It is possible to ask for the permission again, build a dialog informing the
                    user why the permission is necessary. Display a dialog with a button to try
                    again to give the permission requested. Set the dialog not cancelable when the
                    user clicks outside the layout
                    */
                    androidx.appcompat.app.AlertDialog.Builder alert = new AlertDialog.Builder(this);
                    alert.setTitle(getString(R.string.permission_denied_title))
                            .setMessage(getString(R.string.permission_denied_message))
                            .setPositiveButton(getString(R.string.permission_denied_button_text), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    /* Ask again for the permission */
                                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                            READ_EXTERNAL_STORAGE_REQUEST_CODE);
                                }
                            });
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

        /* If we dont have the permission */
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    READ_EXTERNAL_STORAGE_REQUEST_CODE);
        } else {
            /*
            The application has the permission to read external storage so we can connect to the
            MusicService if not already connected
            */
            if (!mMediaBrowser.isConnected())
                mMediaBrowser.connect();
        }
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        /*
        If a fragment is currently active save its tag in the saved state.
        It the user changes theme and the value of the setting is default then the UI will
        be recreated every time the theme changes. To prevent the fragment being instantiated
        2 times or showing a blank fragment save the tag of the fragment that is active.
        When onCreate is called by retrieving savedInstanceState the previously active fragment
        can be restored
         */
        String fragmentTag = null;

        /*
        Because the application only shows one fragment at a time when the first fragment is found
        the loop can be stopped
        */
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            fragmentTag = fragment.getTag();
            break;
        }

        //Log.d(TAG, "onSaveInstanceState: fragment TAG saved " + fragmentTag);

        /* Put the fragment TAG in the bundle */
        outState.putString(FRAGMENT_TO_REMOVE_SAVED_STATE_KEY, fragmentTag);

        super.onSaveInstanceState(outState);
    }


    @Override
    protected void onResume() {
        super.onResume();

        /* Set the volume buttons to handle the multimedia stream */
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onStop() {
        super.onStop();

        /* If the activity has a media controller, unregister the callback from the session */
        if (MediaControllerCompat.getMediaController(MainActivity.this) != null) {
            MediaControllerCompat.getMediaController(MainActivity.this).unregisterCallback(mControllerCallback);
        }

        /* Disconnect from the MusicService */
        mMediaBrowser.disconnect();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        /* When FullPlayerActivity returns false it means the session is disconnected */
        if (requestCode == FullPlayerActivity.FULL_PLAYER_ACTIVITY_RESULT) {
            if (resultCode == Activity.RESULT_OK) {
                if (data.getBooleanExtra(FullPlayerActivity.METADATA_NOT_AVAILABLE_ARGS_KEY, false)) {

                    /* Then hide the small player layout if is in end state (small player visible) */
                    if (motionLayout.getCurrentState() == motionLayout.getEndState())
                        motionLayout.transitionToStart();
                }
            }
        }

    }
}
