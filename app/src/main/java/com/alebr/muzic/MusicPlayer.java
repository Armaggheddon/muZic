package com.alebr.muzic;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import java.io.IOException;

/**
 * Handles all the operation required to correctly play audio with {@link MediaPlayer}
 */

public class MusicPlayer {

    private static final String TAG = "MusicPlayer";

    private MediaSessionCompat mSession;
    private MediaPlayer mPlayer;

    private Context context;

    private static final float DEFAULT_VOLUME = 1.0f;
    private static final float DUCKING_VOLUME = 0.7f;

    /* Tells is the song being currently played is finished or not */
    public static boolean is_end_of_song = false;

    /*
    The path to the song that is being played, it is compared to new paths to know if the new song
    is the same as the old one (the no changes are required) or if it is different update the
    song being played
     */
    public static String currentSongPath;

    /* Completation listener called when MediaPlayer reaches the end of a song */
    private CompletationListener mCompletationListener = new CompletationListener();
    final class CompletationListener implements MediaPlayer.OnCompletionListener{
        @Override
        public void onCompletion(MediaPlayer mp) {
            /*
            When the song being played ends, call skipToNext in MusicService that will do the
            appropriate operations
             */
            is_end_of_song = true;
            mSession.getController().getTransportControls().skipToNext();
        }
    }

    public MusicPlayer(Context context, MediaSessionCompat mediaSessionCompat){
        this.context = context;
        mSession = mediaSessionCompat;
    }

    /**
     * Utility method that calls {@link MusicPlayer#play(String)} converting {@param uriPath} to
     * String
     * @param uriPath
     *          The Uri of the song to play
     */
    public void play(Uri uriPath){
        play(String.valueOf(uriPath));
    }

    /**
     * Handles the path given and does the correct operation to start the playback
     * @param path
     *          The path as a string of the file to play
     */
    public void play(String path){

        /* If is null it is the first time that is being called, then call prepareMPlayer(path) */
        if(mPlayer == null){
            prepareMPlayer(path);
        }

        /* If the path given is different release the player and call prepareMPlayer(path) */
        else if (!path.equals(currentSongPath)){
            mPlayer.release();
            prepareMPlayer(path);
        }

        /* If the player is not playing start the playback */
        if(!mPlayer.isPlaying())
            mPlayer.start();

        /* Update the flag */
        is_end_of_song = false;

        /* Listen at when the song ends */
        mPlayer.setOnCompletionListener(mCompletationListener);
    }

    /**
     * Prepares the MediaPlayer object {@link MusicPlayer#mPlayer} with the given path and sets the correct options
     * @param path
     *          The String path {@link MusicPlayer#play(String)}
     */
    private void prepareMPlayer(String path){
        try{
            mPlayer = new MediaPlayer();
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            /*
            Initialize the MediaPlayer making two different calls to setDataSource() and prepare().
            On the methods that automatically call prepare() it is not guaranteed to be called and
            can cause issues in the output source
            (es. audio being played from device speakers instead of the cars speakers when connected with Android Auto)
             */
            mPlayer.setDataSource(context, Uri.parse(path));
            mPlayer.prepare();
        }catch (IOException e){
            Log.e(TAG, "play: ", e);
        }
        /* Update the path */
        currentSongPath = path;
    }

    /**
     * Pause the playback if {@link MusicPlayer#mPlayer} is not null
     */
    public void pause(){
        if(mPlayer != null)
            mPlayer.pause();
    }

    /**
     * Stop the playback and release the resources used by {@link MediaPlayer} to play audio
     * @see "https://developer.android.com/guide/topics/media/mediaplayer#releaseplayer"
     */
    public void stop(){
        if(mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
    }

    /**
     * Seeks to {@param ms} in milliseconds in the song being played
     * @param ms
     *          The milliseconds to seek to
     */
    public void seekTo(long ms){
        if(mPlayer != null)
            mPlayer.seekTo(((int) ms));
    }

    /**
     * Get the milliseconds elapsed from the start of the song
     * @return
     *          The current position in milliseconds in the song, 0 if {@link MusicPlayer#mPlayer} is null
     */
    public long getPosition(){
        return (mPlayer != null) ? mPlayer.getCurrentPosition() : 0;
    }


    /**
     * Set the volume to {@value DEFAULT_VOLUME} for both left and right volumes
     */
    public void setDefaultVolume(){
        mPlayer.setVolume(DEFAULT_VOLUME, DEFAULT_VOLUME);
    }

    /**
     * Set the volume to {@value DUCKING_VOLUME} for both left and right volumes
     */
    public void setDuckingVolume(){
        mPlayer.setVolume(DUCKING_VOLUME, DUCKING_VOLUME);
    }
}
