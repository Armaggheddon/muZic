package com.alebr.muzic;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.motion.widget.MotionLayout;

import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.graphics.Bitmap;
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

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.concurrent.TimeUnit;

public class FullPlayerActivity extends AppCompatActivity implements QueueFragment.QueueListener{

    @Override
    public void onQueueItemClicked(String stringId, long id) {
        MediaControllerCompat.getMediaController(FullPlayerActivity.this).getTransportControls().skipToQueueItem(id);
        MediaControllerCompat.getMediaController(FullPlayerActivity.this).getTransportControls().play();
    }

    @Override
    public MediaBrowserCompat getMediaBrowser() {
        return mBrowser;
    }

    private static final String TAG = "FullPlayerActivity";

    private ImageView backButton, albumImage, skipToPreviousButton, skipToNextButton, showQueueButton, hideQueueButton;
    private TextView titleTextView, albumTextView, elapsedTimeTextView, leftTimeTextView;
    private FloatingActionButton playPauseButton;
    private ValueAnimator mSeekBarAnimator;
    private boolean isTracking = false;
    private SeekBar seekBar;
    private MotionLayout motionLayout;
    private MediaBrowserCompat mBrowser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_player);

        motionLayout = findViewById(R.id.full_player_layout);

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
            buildTransportControls();
        }
    };

    private void buildTransportControls(){

        backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSeekBarAnimator.removeAllUpdateListeners();
                mSeekBarAnimator.cancel();
                mSeekBarAnimator = null;
                finish();
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
        albumTextView = findViewById(R.id.album_textview);
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
            }
        });

        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(FullPlayerActivity.this);

        MediaMetadataCompat mediaMetadata = mediaController.getMetadata();
        PlaybackStateCompat pbState = mediaController.getPlaybackState();
        MediaDescriptionCompat description = mediaMetadata.getDescription();

        String title = description.getTitle().toString();
        String album = description.getDescription().toString();
        Bitmap albumArt = description.getIconBitmap();

        titleTextView.setText(title);
        albumTextView.setText(album);
        albumImage.setImageBitmap(albumArt);

        int currentProgress = (int) pbState.getPosition();
        int maxProgress = (int) (mediaMetadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));

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

    private AnimatorUpdateListener mAnimatorListener = new AnimatorUpdateListener();
    class AnimatorUpdateListener implements ValueAnimator.AnimatorUpdateListener{
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            if(isTracking){
                mSeekBarAnimator.cancel();
                return;
            }
            int newValue = (Integer) animation.getAnimatedValue();
            //The value is not sent constantly for every value, so since we dont need to update the
            //seconds displayed every second when we are at least 800ms in one seconds update the
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
            String title = description.getTitle().toString();
            String album = description.getDescription().toString();
            Bitmap albumArt = description.getIconBitmap();

            titleTextView.setText(title);
            albumTextView.setText(album);
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