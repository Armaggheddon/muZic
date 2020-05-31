package com.alebr.muzic;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import java.io.IOException;

public class MusicPlayer {

    private static final String TAG = "MusicPlayer";

    private static final float DEFAULT_VOLUME = 1.0f;
    public static final float DUCKING_VOLUME = 0.7f;

    //Flag indicating if the current song is ended or not
    public static boolean is_end_of_song = false;

    //The song path of the current song being played, used to compare with new paths to know if the
    //song to be played is a new one or the one that is/was already being played
    public static String currentSongPath;

    //Completation listener to listen when a song ends
    private CompletationListener mCompletationListener = new CompletationListener();
    final class CompletationListener implements MediaPlayer.OnCompletionListener{
        @Override
        public void onCompletion(MediaPlayer mp) {
            //When the song currently playing ends, notify the MusicService using the command
            //skipToNext(), further controls for this state are leaved to the skipToNext() method itself
            //Update the flag to tell the end of the song is reached
            is_end_of_song = true;
            mSession.getController().getTransportControls().skipToNext();
        }
    }

    private MediaSessionCompat mSession;
    private MediaPlayer mPlayer;

    private Context context;

    public MusicPlayer(Context context, MediaSessionCompat mediaSessionCompat){
        this.context = context;
        mSession = mediaSessionCompat;
    }

    /**
     * Utility method that parses the Uri in a string and calls play with the parsed parameter
     * @param uriPath the Uri to the song to reproduce
     */
    public void play(Uri uriPath){
        play(String.valueOf(uriPath));
    }

    /**
     * Initialize the MediaPlayer object based on the parameter path and starts the playback.
     * @param path the String path to the song, it has to be a String in order to do the comparison
     *             with the currentSongPath
     */
    public void play(String path){
        //If mPlayer is null is the first time that this method is called
        if(mPlayer == null){
            prepareMPlayer(path);
        }
        //mPlayer is not null so we check if the path given is the same as currentSongPath, if it is,
        //it means that is a new path to reproduce, we then release the current mPlayer and create
        //a new one with the new path
        else if (!path.equals(currentSongPath)){
            mPlayer.release();
            prepareMPlayer(path);
        }
        //Start the playback if the player is not already playing
        if(!mPlayer.isPlaying())
            mPlayer.start();
        //Set end of song to false
        is_end_of_song = false;
        mPlayer.setOnCompletionListener(mCompletationListener);
    }

    /**
     * Prepares the MediaPlayer object (mPlayer) with the given path and sets the correct options
     * @param path the String path that points to the song itself
     */
    private void prepareMPlayer(String path){
        try{
            mPlayer = new MediaPlayer();
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            //Call setDataSource and then prepare to be sure the MediaPlayer instance gets the
            //correct output source, otherwise it is not granted to get the correct output source
            mPlayer.setDataSource(context, Uri.parse(path));
            mPlayer.prepare();
        }catch (IOException e){
            Log.e(TAG, "play: ", e);
        }
        //Update the path we store locally
        currentSongPath = path;
    }

    /**
     * Pause the playback, checks if mPlayer is not null, if is null, no playback is active then
     * there is no need to pause
     */
    public void pause(){
        if(mPlayer != null)
            mPlayer.pause();
    }

    /**
     * Stop the mPlayer and release the resources used
     */
    public void stop(){
        //Default way to fully release a MediaPlayer instance
        //see https://developer.android.com/guide/topics/media/mediaplayer#releaseplayer
        mPlayer.stop();
        mPlayer.release();
        mPlayer = null;

    }

    /**
     * Seeks to a certain point in the song if mPlayer is not null
     * @param ms the milliseconds in the song to seek to
     */
    public void seekTo(long ms){
        if(mPlayer != null)
            mPlayer.seekTo(((int) ms));
    }

    /**
     * Retrieve if mPlayer is not null the current position in the song
     * @return the current position in the song, 0 if mPlayer is null
     */
    public long getPosition(){
        return (mPlayer != null) ? mPlayer.getCurrentPosition() : 0;
    }

    /**
     * Gets the information about the duration of the song
     * @return the duration of the song, 0 if mPlayer is null
     */
    public long getDuration(){
        return (mPlayer != null) ? mPlayer.getDuration() : 0;
    }

    /**
     * Set the volume to be DEFAULT_VOLUME for both left and right volumes
     */
    public void setDefaultVolume(){
        mPlayer.setVolume(DEFAULT_VOLUME, DEFAULT_VOLUME);
    }

    /**
     * Set the volume to be DUCKING_VOLUME for both left and right volumes
     */
    public void setDuckingVolume(){
        mPlayer.setVolume(DUCKING_VOLUME, DUCKING_VOLUME);
    }
}
