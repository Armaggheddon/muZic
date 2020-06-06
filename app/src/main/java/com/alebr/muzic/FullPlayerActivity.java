package com.alebr.muzic;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.palette.graphics.Palette;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadata;
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

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.concurrent.TimeUnit;

public class FullPlayerActivity extends AppCompatActivity implements QueueFragment.QueueFragmentListener {

    public static int FULL_PLAYER_ACTIVITY_RESULT = 555;
    public static final String METADATA_NOT_AVAILABLE = "is_metadata_available";

    private static final String TAG = "FullPlayerActivity";

    private ImageView albumImage, skipToPreviousButton, skipToNextButton, showQueueButton, hideQueueButton;
    private TextView titleTextView, artistTextView, elapsedTimeTextView, leftTimeTextView;
    private FloatingActionButton playPauseButton;
    private ValueAnimator mSeekBarAnimator;
    private boolean isTracking = false;
    private SeekBar seekBar;
    private MotionLayout motionLayout;
    private MaterialToolbar mToolbar;
    private MediaBrowserCompat mBrowser;

    private int previousColor;

    @Override
    public void onQueueItemClicked(long positionInQueue) {
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

        previousColor = android.R.attr.windowBackground;

        mBrowser = new MediaBrowserCompat(
                this,
                new ComponentName(
                        this, MusicService.class),
                connectionCallback,
                null
                );

    }

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

            QueueFragment queueFragment = QueueFragment.newInstance(false);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, queueFragment)
                    .commit();

            MediaControllerCompat.setMediaController(FullPlayerActivity.this, mediaControllerCompat);
            if(MediaControllerCompat.getMediaController(FullPlayerActivity.this).getMetadata() != null)
                buildTransportControls();
            else{
                Intent returnIntent = new Intent();
                returnIntent.putExtra(METADATA_NOT_AVAILABLE, true);
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        }
    };

    /* Suppress because it is a string built to represent time, no need to format for "DefaultLocale" */
    @SuppressLint("DefaultLocale")
    private void buildTransportControls(){

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mSeekBarAnimator != null){
                    mSeekBarAnimator.removeAllUpdateListeners();
                    mSeekBarAnimator.cancel();
                    mSeekBarAnimator = null;
                }
                if(MediaControllerCompat.getMediaController(FullPlayerActivity.this).getPlaybackState().getState() == PlaybackStateCompat.STATE_STOPPED){
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra(METADATA_NOT_AVAILABLE, true);
                    setResult(Activity.RESULT_OK, returnIntent);
                    finish();
                }
                else {
                    finish();
                }
            }
        });

        albumImage = findViewById(R.id.album_imageview);
        skipToPreviousButton = findViewById(R.id.skip_previous_button);
        skipToPreviousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaControllerCompat.getMediaController(FullPlayerActivity.this).getTransportControls().skipToPrevious();
            }
        });
        skipToNextButton = findViewById(R.id.skip_next_button);
        skipToNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaControllerCompat.getMediaController(FullPlayerActivity.this).getTransportControls().skipToNext();
            }
        });
        showQueueButton = findViewById(R.id.open_queue_button);
        showQueueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                motionLayout.transitionToEnd();
            }
        });

        hideQueueButton = findViewById(R.id.close_queue_button);
        hideQueueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(motionLayout.getCurrentState() == motionLayout.getEndState()){
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
                int pbState = MediaControllerCompat.getMediaController(FullPlayerActivity.this).getPlaybackState().getState();
                if (pbState == PlaybackStateCompat.STATE_PLAYING){
                    MediaControllerCompat.getMediaController(FullPlayerActivity.this).getTransportControls().pause();
                }else {
                    MediaControllerCompat.getMediaController(FullPlayerActivity.this).getTransportControls().play();
                }
            }
        });

        seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isTracking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isTracking = false;
                MediaControllerCompat.getMediaController(FullPlayerActivity.this).getTransportControls().seekTo(seekBar.getProgress());
                int currentProgress = seekBar.getProgress();
                int maxProgress = seekBar.getMax();

                elapsedTimeTextView.setText(String.format("%02d:%02d",
                        (int) TimeUnit.MILLISECONDS.toMinutes(currentProgress),
                        (int) TimeUnit.MILLISECONDS.toSeconds(currentProgress) % 60));

                int leftTime = maxProgress - currentProgress;

                leftTimeTextView.setText(String.format("%02d:%02d",
                        (int) TimeUnit.MILLISECONDS.toMinutes(leftTime),
                        (int) TimeUnit.MILLISECONDS.toSeconds(leftTime) % 60));
            }
        });

        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(FullPlayerActivity.this);

        MediaMetadataCompat mediaMetadata = mediaController.getMetadata();
        PlaybackStateCompat pbState = mediaController.getPlaybackState();

        String title = mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
        String artist = mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);

        Bitmap albumArt = mediaMetadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART);

        setBackgroundAsync(albumArt);

        titleTextView.setText(title);
        artistTextView.setText(artist);
        albumImage.setImageBitmap(albumArt);

        int currentProgress = (int) pbState.getPosition();
        int maxProgress = (int) (mediaMetadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
        Log.d(TAG, "buildTransportControls: " + maxProgress);

        elapsedTimeTextView.setText(String.format("%02d:%02d",
                (int) TimeUnit.MILLISECONDS.toMinutes(currentProgress),
                (int) TimeUnit.MILLISECONDS.toSeconds(currentProgress) % 60));

        int leftTime = maxProgress - currentProgress;

        leftTimeTextView.setText(String.format("%02d:%02d",
                (int) TimeUnit.MILLISECONDS.toMinutes(leftTime),
                (int) TimeUnit.MILLISECONDS.toSeconds(leftTime) % 60));

        seekBar.setMax(maxProgress);
        seekBar.setProgress(currentProgress);
        if(pbState.getState() == PlaybackStateCompat.STATE_PLAYING){
            playPauseButton.setImageResource(R.drawable.ic_pause);

            mSeekBarAnimator = ValueAnimator.ofInt((int) pbState.getPosition(), seekBar.getMax())
                    .setDuration(seekBar.getMax() - pbState.getPosition());
            mSeekBarAnimator.setInterpolator( new LinearInterpolator());
            mSeekBarAnimator.addUpdateListener(mAnimatorListener);
            mSeekBarAnimator.start();
        }else {
            playPauseButton.setImageResource(R.drawable.ic_play);
        }
        mediaController.registerCallback(controllerCallback);
    }


    private void setBackgroundAsync(Bitmap image){

        Palette.from(image).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(@Nullable Palette palette) {

                Palette.Swatch vibrantSwatch = palette.getDominantSwatch();

                int colorTop = android.R.attr.windowBackground;
                int colorButton = android.R.attr.colorControlNormal;

                if(vibrantSwatch != null){
                    colorTop = palette.getDominantColor(android.R.attr.windowBackground);
                    colorButton = vibrantSwatch.getBodyTextColor();

                }

                Drawable backIcon = getDrawable(R.drawable.ic_baseline_arrow_black);
                Drawable closeQueueIcon = getDrawable(R.drawable.ic_baseline_close);
                backIcon.setTint(colorButton);
                closeQueueIcon.setTint(colorButton);

                mToolbar.setNavigationIcon(backIcon);

                hideQueueButton.setImageDrawable(closeQueueIcon);


                final ValueAnimator colorAnimator = ValueAnimator.ofArgb(previousColor, colorTop);


                colorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int updateColor = (int) animation.getAnimatedValue();
                        GradientDrawable gradientDrawable = new GradientDrawable(
                                GradientDrawable.Orientation.TOP_BOTTOM,
                                new int[]{updateColor, android.R.attr.windowBackground, android.R.attr.windowBackground});
                        motionLayout.setBackground(gradientDrawable);
                    }
                });
                colorAnimator.start();
                previousColor = colorTop;
            }
        });
    }

    private AnimatorUpdateListener mAnimatorListener = new AnimatorUpdateListener();
    class AnimatorUpdateListener implements ValueAnimator.AnimatorUpdateListener{
        //Suppress because it is a string built to represent time, no need to format for "DefaultLocale"
        @SuppressLint("DefaultLocale")
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            if(isTracking){
                mSeekBarAnimator.cancel();
                return;
            }
            int newValue = (Integer) animation.getAnimatedValue();
            //The value is not sent constantly for every value, so since we dont need to update the
            //seconds displayed every second when we are at least 500ms in one seconds update the
            //values
            if(newValue%1000 >= 800) {
                //Log.d(TAG, "onAnimationUpdate: " + newValue);
                int elapsedSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(newValue) % 60;
                int elapsedMinutes = (int) TimeUnit.MILLISECONDS.toMinutes(newValue);
                int leftTime = seekBar.getMax() - newValue;
                int leftSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(leftTime) % 60;
                int leftMinutes = (int) TimeUnit.MILLISECONDS.toMinutes(leftTime);
                elapsedTimeTextView.setText(String.format("%02d:%02d", elapsedMinutes, elapsedSeconds));
                leftTimeTextView.setText(String.format("%02d:%02d", leftMinutes, leftSeconds));
            }
            seekBar.setProgress(newValue);
        }
    }

    MediaControllerCompat.Callback controllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);

            if(mSeekBarAnimator != null){
                mSeekBarAnimator.cancel();
                mSeekBarAnimator = null;
            }
            seekBar.setProgress((int)state.getPosition());
            if(state.getState() == PlaybackStateCompat.STATE_PLAYING){
                playPauseButton.setImageResource(R.drawable.ic_pause);
                mSeekBarAnimator = ValueAnimator.ofInt((int) state.getPosition(), seekBar.getMax())
                        .setDuration(seekBar.getMax()-state.getPosition());
                mSeekBarAnimator.setInterpolator( new LinearInterpolator());
                mSeekBarAnimator.addUpdateListener( mAnimatorListener);
                mSeekBarAnimator.start();
            }else {
                playPauseButton.setImageResource(R.drawable.ic_play);
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);

            MediaDescriptionCompat description = metadata.getDescription();

            seekBar.setMax((int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));

            String title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
            String artist = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);

            Bitmap albumArt = description.getIconBitmap();

            setBackgroundAsync(albumArt);

            titleTextView.setText(title);
            artistTextView.setText(artist);
            albumImage.setImageBitmap(albumArt);

        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if(!mBrowser.isConnected()){
            mBrowser.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(MediaControllerCompat.getMediaController(FullPlayerActivity.this) != null){
            MediaControllerCompat.getMediaController(FullPlayerActivity.this).unregisterCallback(controllerCallback);
        }
        mBrowser.disconnect();

        if(mSeekBarAnimator != null){
            mSeekBarAnimator.cancel();
            mSeekBarAnimator.removeAllUpdateListeners();
            mSeekBarAnimator = null;
        }
    }
}