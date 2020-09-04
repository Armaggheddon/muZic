package com.armaggheddon.muzic.ui;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.content.ContextCompat;
import androidx.palette.graphics.Palette;

import com.armaggheddon.muzic.MusicService;
import com.armaggheddon.muzic.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.concurrent.TimeUnit;

/**
 * Handles the {@link R.layout#activity_full_player} layout with the controls that allows to
 * send actions to {@link MusicService}
 */
public class FullPlayerActivity extends AppCompatActivity implements QueueFragment.QueueFragmentListener {

    /* Activity result code to publish to MainActivity the result code */
    public static int FULL_PLAYER_ACTIVITY_RESULT = 555;

    /* Key for the argument to send in the activity result */
    public static final String METADATA_NOT_AVAILABLE_ARGS_KEY = "is_metadata_available";

    /* Activity log tag*/
    private static final String TAG = "FullPlayerActivity";

    /*
    hideQueueButton is used to animate to the start state hiding QueueFragment
    */
    private ImageView albumImage;
    private ImageView hideQueueButton;

    /*
    elapsedTimeTextView is used to display the current position in the song being played,
    leftTimeTextView is uset to display the time left to reach the end of the song being played
    */
    private TextView titleTextView, artistTextView, elapsedTimeTextView, leftTimeTextView;
    private FloatingActionButton playPauseButton;

    /*
    Value animator used to animate the position update for the seek bar and for the
    text views that represent the elapsed time and left time
    */
    private ValueAnimator mTimeAnimator;

    /* Flag that allows mTimeAnimator to know if the seek bar is being updated by the user to avoid pushing updates */
    private boolean isTracking = false;
    private SeekBar seekBar;
    private MotionLayout motionLayout;
    private MaterialToolbar mToolbar;
    private MediaBrowserCompat mBrowser;

    /* Used to animate from the previous color to the current color for the background */
    private int previousColor;

    /* Listener to listen to new values being updated */
    private AnimatorUpdateListener mAnimatorListener = new AnimatorUpdateListener();

    /**
     * Handles the item in the queue being clicked, updates the item being currently played, and
     * calls skipToQueueItem so {@link MusicService} can update the metadata
     * @param positionInQueue
     *          The position of the item clicked. Since the items are shown in the same order as
     *          they are in the playback queue the position also represents the id of the queue
     */
    @Override
    public void onQueueItemClicked(long positionInQueue) {
        /* Update the current position in the queue and play */
        MediaControllerCompat.getMediaController(FullPlayerActivity.this).getTransportControls().skipToQueueItem(positionInQueue);
        MediaControllerCompat.getMediaController(FullPlayerActivity.this).getTransportControls().play();
    }


    @Override
    public MediaBrowserCompat getMediaBrowser() {
        return mBrowser;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_player);

        motionLayout = findViewById(R.id.full_player_root);

        mToolbar = findViewById(R.id.full_player_toolbar);

        /* Set the back click listener for the back button being clicked in the toolbar */
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /* Remove the listener to the Animator and delete it */
                if(mTimeAnimator != null){
                    mTimeAnimator.removeAllUpdateListeners();
                    mTimeAnimator.cancel();
                    mTimeAnimator = null;
                }
                finish();
            }
        });

        previousColor = android.R.attr.windowBackground;

        mBrowser = new MediaBrowserCompat(
                this,
                new ComponentName(
                        this, MusicService.class),
                connectionCallback,
                null
                );

    }

    /* Callback to receive the onConnected event when the Activity connects to MusicService */
    private final MediaBrowserCompat.ConnectionCallback connectionCallback = new MediaBrowserCompat.ConnectionCallback(){
        @Override
        public void onConnected() {
            MediaSessionCompat.Token token = mBrowser.getSessionToken();

            MediaControllerCompat mediaControllerCompat = null;
            try {
                mediaControllerCompat = new MediaControllerCompat(
                        FullPlayerActivity.this, token);

            }catch (RemoteException e){
                Log.e(TAG, "onConnected: ", e);
            }

            /* When we are connected create and add the fragment for viewing the current queue */
            QueueFragment queueFragment = QueueFragment.newInstance(false);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, queueFragment)
                    .commit();

            MediaControllerCompat.setMediaController(FullPlayerActivity.this, mediaControllerCompat);
            if(MediaControllerCompat.getMediaController(FullPlayerActivity.this).getMetadata() != null)
                buildTransportControls();
            else{

                /*
                If the metadata is not available the service was destroyed and restarted,
                but no metadata is available so is not possible to use the commands in the activity
                such as play/pause, so close the activity notifying MainActivity about the event.
                Set RESULT_OK because the event was correctly handled and the activity did not crash
                */
                Intent returnIntent = new Intent();
                returnIntent.putExtra(METADATA_NOT_AVAILABLE_ARGS_KEY, true);
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        }
    };

    /**
     * Handles setting up all the views in an initial state that represents the current PlaybackState
     * and the sets the click listeners
     */
    /* Suppress because it is a string built to represent time, no need to format for "DefaultLocale" */
    @SuppressLint("DefaultLocale")
    private void buildTransportControls(){

        /* Calling setNavigationOnClickListener again replaces the previous listener in onCreate() */
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(mTimeAnimator != null){
                    mTimeAnimator.removeAllUpdateListeners();
                    mTimeAnimator.cancel();
                    mTimeAnimator = null;
                }
                if(MediaControllerCompat.getMediaController(FullPlayerActivity.this).getPlaybackState().getState() == PlaybackStateCompat.STATE_STOPPED){

                    /*
                    If the state is STATE_STOPPED the MusicService is in STOPPED state wich means
                    that there is no metadata available and no queue. Return to MainActivity telling
                    about the event happened to avoid prohibited states for events from the
                    "small player".
                    Result code is RESULT_OK because the event has been handled and the activity did
                    not crash
                    */
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra(METADATA_NOT_AVAILABLE_ARGS_KEY, true);
                    setResult(Activity.RESULT_OK, returnIntent);
                    finish();
                }
                else {

                    /* No prohibited states, just close the current activity */
                    finish();
                }
            }
        });

        albumImage = findViewById(R.id.album_imageview);
        ImageView skipToPreviousButton = findViewById(R.id.skip_previous_button);
        skipToPreviousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /*
                Skip to the previous track, this happens only if the current item is not the first
                one, if is the case the track is restarted
                */
                MediaControllerCompat.getMediaController(FullPlayerActivity.this).getTransportControls().skipToPrevious();
            }
        });
        ImageView skipToNextButton = findViewById(R.id.skip_next_button);
        skipToNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /*
                Skip to the next track, this happens only if the current item is not the last one,
                if is the case nothing happens
                */
                MediaControllerCompat.getMediaController(FullPlayerActivity.this).getTransportControls().skipToNext();
            }
        });
        ImageView showQueueButton = findViewById(R.id.open_queue_button);
        showQueueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /* Transition to end state to open the QueueFragment and show the queue and the close queue button */
                motionLayout.transitionToEnd();
            }
        });

        hideQueueButton = findViewById(R.id.close_queue_button);
        hideQueueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /* If the current state of the layout is the end state (QueueFragment visible */
                if(motionLayout.getCurrentState() == motionLayout.getEndState()){

                     /* Then transition to the start state hiding the queue and the close queue button*/
                    motionLayout.transitionToStart();
                }
            }
        });

        titleTextView = findViewById(R.id.title_textview);
        artistTextView = findViewById(R.id.artist_textview);
        elapsedTimeTextView = findViewById(R.id.time_elapsed);
        leftTimeTextView = findViewById(R.id.time_left);

        playPauseButton = findViewById(R.id.play_pause_button);
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /* Get the current state */
                int pbState = MediaControllerCompat.getMediaController(FullPlayerActivity.this).getPlaybackState().getState();

                /* If the current state is STATE_PLAYING then the click event represents a pause */
                if (pbState == PlaybackStateCompat.STATE_PLAYING){
                    MediaControllerCompat.getMediaController(FullPlayerActivity.this).getTransportControls().pause();
                }else {
                    MediaControllerCompat.getMediaController(FullPlayerActivity.this).getTransportControls().play();
                }
            }
        });

        seekBar = findViewById(R.id.seekBar);

        /* Listen for the seek bar being changed by the user */
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

                /* Set the flag to true so updates to the animator are not applied to the seek bar */
                isTracking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                /* Set the flag to false since the user has stopped moving the seek bar */
                isTracking = false;

                /* Seek to the position set from the user and update the MusicService */
                MediaControllerCompat.getMediaController(FullPlayerActivity.this).getTransportControls().seekTo(seekBar.getProgress());

                /*
                The new position  for the TextViews and the SeekBar is updated
                in onPlaybackStateChanged an on onMetadataChanged
                */
            }
        });

        /* Set the initial state for the views */
        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(FullPlayerActivity.this);

        MediaMetadataCompat mediaMetadata = mediaController.getMetadata();
        PlaybackStateCompat pbState = mediaController.getPlaybackState();

        String title = mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
        String artist = mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);

        Bitmap albumArt = mediaMetadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART);

        /* Set the background gradient */
        setBackgroundAsync(albumArt);

        titleTextView.setText(title);
        artistTextView.setText(artist);
        albumImage.setImageBitmap(albumArt);

        /* Get the current position and the duration of the song both in milliseconds */
        int currentProgress = (int) pbState.getPosition();
        int maxProgress = (int) (mediaMetadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));

        /* Format the text as 00:00 as <minutes>:<seconds> */
        elapsedTimeTextView.setText(String.format("%02d:%02d",
                (int) TimeUnit.MILLISECONDS.toMinutes(currentProgress),
                (int) TimeUnit.MILLISECONDS.toSeconds(currentProgress) % 60));

        /* Get the left */
        int leftTime = maxProgress - currentProgress;

        leftTimeTextView.setText(String.format("%02d:%02d",
                (int) TimeUnit.MILLISECONDS.toMinutes(leftTime),
                (int) TimeUnit.MILLISECONDS.toSeconds(leftTime) % 60));

        /* Set the maximum progress to the seek bar as DURATION */
        seekBar.setMax(maxProgress);
        seekBar.setProgress(currentProgress);

        /*
        If the state is playing set the pause icon on play_pause_button and start the animator
        for elapsedTimeTextView, leftTimeTextView and seekBar
        */
        if(pbState.getState() == PlaybackStateCompat.STATE_PLAYING){
            playPauseButton.setImageResource(R.drawable.ic_pause);

            /* Animate from current position to the maximum position in (max-current) milliseconds */
            mTimeAnimator = ValueAnimator.ofInt( currentProgress, seekBar.getMax())
                    .setDuration(seekBar.getMax() - currentProgress);
            mTimeAnimator.setInterpolator( new LinearInterpolator());
            mTimeAnimator.addUpdateListener(mAnimatorListener);
            mTimeAnimator.start();
        }

        /* Else the current state is PAUSED so set the play_pause_button the play icon */
        else {
            playPauseButton.setImageResource(R.drawable.ic_play);
        }

        /* Register a callback to receive state updates */
        mediaController.registerCallback(controllerCallback);
    }

    /**
     * Sets the background to the view building a {@link GradientDrawable} from the colors in the
     * {@param image} given
     * @param image
     *              The image used to retrieve the information about the colors to use
     */
    private void setBackgroundAsync(Bitmap image){

        /* This operation is created in a background thread, onGenerated is called when the data is ready */
        Palette.from(image).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(@Nullable Palette palette) {

                Palette.Swatch dominantSwatch = null;
                if(palette != null)
                    dominantSwatch = palette.getDominantSwatch();

                /*Set the default color to use in the top of the view and for the buttons in case no data is available */
                int colorTop = android.R.attr.windowBackground;
                int colorButton = android.R.attr.colorControlNormal;

                /* If the swatch is not null then update the values */
                if (dominantSwatch != null) {
                    colorTop = palette.getDominantColor(android.R.attr.windowBackground);
                    colorButton = dominantSwatch.getBodyTextColor();
                }

                /* Get the 2 drawables for the icons that needs to have a proper color to avoid visibility issues */
                Drawable backIcon = ContextCompat.getDrawable(FullPlayerActivity.this, R.drawable.ic_baseline_arrow_black);
                Drawable closeQueueIcon = ContextCompat.getDrawable(FullPlayerActivity.this, R.drawable.ic_baseline_close);

                /* Set tint applies a tint on the drawable, in fact changing the color if the drawable is black */
                if (backIcon != null)
                    backIcon.setTint(colorButton);
                if (closeQueueIcon != null)
                    closeQueueIcon.setTint(colorButton);

                /* Update the icons */
                mToolbar.setNavigationIcon(backIcon);
                hideQueueButton.setImageDrawable(closeQueueIcon);

                /* Build a value animator to animate the change in color from previous color to the new one */
                final ValueAnimator colorAnimator = ValueAnimator.ofArgb(previousColor, colorTop);

                /* Add a lister on value update */
                colorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {

                        /* Get the current value being pushed */
                        int updateColor = (int) animation.getAnimatedValue();

                        /*
                        Build a gradient drawable with top colo the updateColor and mid,bot the
                        default one of the view in order to achive the start of the fade effect
                        at 33% from the top of the view
                        */
                        GradientDrawable gradientDrawable = new GradientDrawable(
                                GradientDrawable.Orientation.TOP_BOTTOM,
                                new int[]{updateColor, android.R.attr.windowBackground, android.R.attr.windowBackground});

                        /* Update the layout to achieve a transition effect */
                        motionLayout.setBackground(gradientDrawable);
                    }
                });
                colorAnimator.start();

                /* Update the current color used so when a new track is selected we can animate from this color */
                previousColor = colorTop;
            }
        });
    }


    /**
     * The listener for updates on the value to animate
     */
    class AnimatorUpdateListener implements ValueAnimator.AnimatorUpdateListener{
        /* Suppress because it is a string built to represent time, no need to format for "DefaultLocale" */
        @SuppressLint("DefaultLocale")
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {

            /*
            If the user is interacting cancel any update on the seek bar because changing the
            position would cause the seek bar to return back to where it was canceling any user
            position updates
            */
            if(isTracking){
                mTimeAnimator.cancel();
                return;
            }
            int newValue = (Integer) animation.getAnimatedValue();
            /*
            Since we need to update the views that represents the time elapsed and time left it is
            useless to update for every milliseconds because the lowest time unit used in the
            views is seconds. To avoid overloading the UI Thread that has to redraw the views every
            time are updated, we just update the values in a smaller window of 200 milliseconds.
            This choice causes a small delay on the update on the time values but is better for
            performance
             */
            if(newValue%1000 >= 800) {

                /* Build the time values to display on the views as 00:00 -> <minutes>:<seconds> */
                int elapsedSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(newValue) % 60;
                int elapsedMinutes = (int) TimeUnit.MILLISECONDS.toMinutes(newValue);
                int leftTime = seekBar.getMax() - newValue;
                int leftSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(leftTime) % 60;
                int leftMinutes = (int) TimeUnit.MILLISECONDS.toMinutes(leftTime);

                /* Update the time that has elapsed from the start current song */
                elapsedTimeTextView.setText(String.format("%02d:%02d", elapsedMinutes, elapsedSeconds));

                /* Update the time tha is left to the end of the current song */
                leftTimeTextView.setText(String.format("%02d:%02d", leftMinutes, leftSeconds));
            }

            /* Update the current progress of the seek bar */
            seekBar.setProgress(newValue);
        }
    }

    /**
     * Handles state changes on the session and updates all the views with the right data
     */
    MediaControllerCompat.Callback controllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);

            /* A new state is available, so cancel any updates on the views */
            if(mTimeAnimator != null){
                mTimeAnimator.cancel();
                mTimeAnimator = null;
            }

            /* Get the current position in the song */
            int currentPosition = (int) state.getPosition();

            /* Update the views with the new value and start the animator */
            seekBar.setProgress(currentPosition);

            /* If the state is PLAYING change the icon on playPauseButton and start the value animator */
            if(state.getState() == PlaybackStateCompat.STATE_PLAYING){
                playPauseButton.setImageResource(R.drawable.ic_pause);
                mTimeAnimator = ValueAnimator.ofInt( currentPosition, seekBar.getMax())
                        .setDuration(seekBar.getMax()-currentPosition);
                mTimeAnimator.setInterpolator( new LinearInterpolator());
                mTimeAnimator.addUpdateListener( mAnimatorListener);
                mTimeAnimator.start();
            }

            /* Else just change the current icon on playPauseButton */
            else {
                playPauseButton.setImageResource(R.drawable.ic_play);
            }
        }


        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);

            /* Metadata changed, which means that the song changed and the data about it */
            MediaDescriptionCompat description = metadata.getDescription();

            /* Update the maximum value of the seek bar to be the duration of the new song */
            seekBar.setMax((int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));

            /* Update the title and the artist name in the text views */
            String title = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
            String artist = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);

            Bitmap albumArt = description.getIconBitmap();

            /* Update the background color */
            setBackgroundAsync(albumArt);

            titleTextView.setText(title);
            artistTextView.setText(artist);
            albumImage.setImageBitmap(albumArt);
        }
    };


    @Override
    protected void onStart() {
        super.onStart();

        /* On start check if we are connected, iif not connect again */
        if(!mBrowser.isConnected()){
            mBrowser.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        /* On stop release the controller and unregister the receiver */
        if(MediaControllerCompat.getMediaController(FullPlayerActivity.this) != null){
            MediaControllerCompat.getMediaController(FullPlayerActivity.this).unregisterCallback(controllerCallback);
        }

        /* Disconnect from the browser */
        mBrowser.disconnect();

        /* Remove the listener to the updates of the value */
        if(mTimeAnimator != null){
            mTimeAnimator.cancel();
            mTimeAnimator.removeAllUpdateListeners();
            mTimeAnimator = null;
        }
    }
}