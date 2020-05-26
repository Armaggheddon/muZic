package com.alebr.muzic;

import android.os.Bundle;

import androidx.annotation.NonNull;

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;

import androidx.media.MediaBrowserServiceCompat;

import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides a MediaBrowser through a service. It exposes the media library to a browsing
 * client, through the onGetRoot and onLoadChildren methods. It also creates a MediaSession and
 * exposes it through its MediaSession.Token, which allows the client to create a MediaController
 * that connects to and send control commands to the MediaSession remotely. This is useful for
 * user interfaces that need to interact with your media session, like Android Auto. You can
 * (should) also use the same service from your app's UI, which gives a seamless playback
 * experience to the user.
 * <p>
 * To implement a MediaBrowserService, you need to:
 *
 * <ul>
 *
 * <li> Extend {@link MediaBrowserServiceCompat}, implementing the media browsing
 *      related methods {@link MediaBrowserServiceCompat#onGetRoot} and
 *      {@link MediaBrowserServiceCompat#onLoadChildren};
 * <li> In onCreate, start a new {@link MediaSessionCompat} and notify its parent
 *      with the session's token {@link MediaBrowserServiceCompat#setSessionToken};
 *
 * <li> Set a callback on the {@link MediaSessionCompat#setCallback(MediaSessionCompat.Callback)}.
 *      The callback will receive all the user's actions, like play, pause, etc;
 *
 * <li> Handle all the actual music playing using any method your app prefers (for example,
 *      {@link android.media.MediaPlayer})
 *
 * <li> Update playbackState, "now playing" metadata and queue, using MediaSession proper methods
 *      {@link MediaSessionCompat#setPlaybackState(android.support.v4.media.session.PlaybackStateCompat)}
 *      {@link MediaSessionCompat#setMetadata(android.support.v4.media.MediaMetadataCompat)} and
 *      {@link MediaSessionCompat#setQueue(java.util.List)})
 *
 * <li> Declare and export the service in AndroidManifest with an intent receiver for the action
 *      android.media.browse.MediaBrowserService
 *
 * </ul>
 * <p>
 * To make your app compatible with Android Auto, you also need to:
 *
 * <ul>
 *
 * <li> Declare a meta-data tag in AndroidManifest.xml linking to a xml resource
 *      with a &lt;automotiveApp&gt; root element. For a media app, this must include
 *      an &lt;uses name="media"/&gt; element as a child.
 *      For example, in AndroidManifest.xml:
 *          &lt;meta-data android:name="com.google.android.gms.car.application"
 *              android:resource="@xml/automotive_app_desc"/&gt;
 *      And in res/values/automotive_app_desc.xml:
 *          &lt;automotiveApp&gt;
 *              &lt;uses name="media"/&gt;
 *          &lt;/automotiveApp&gt;
 *
 * </ul>
 */
public class MusicService extends MediaBrowserServiceCompat {

    private static final String TAG = "MusicService";

    private MediaSessionCompat mSession;
    //Since we use this object a lot create a cached version
    private PlaybackStateCompat.Builder mStateBuilder;
    private boolean isServiceStarted = false;
    private MediaNotificationManager mMediaNotificationManager;
    private MusicLibrary mMusicLibrary;
    private MusicPlayer mMusicPlayer;

    @Override
    public void onCreate() {
        super.onCreate();

        mSession = new MediaSessionCompat(this, TAG);
        //Action that an app must support for Android Auto
        //https://developer.android.com/training/cars/media#required-actions
        mStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY|
                                PlaybackStateCompat.ACTION_PAUSE|
                                PlaybackStateCompat.ACTION_STOP|
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS|
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT|
                                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID|
                                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                );
        //To add custom action just uncomment the following line
        /*
        mStateBuilder.addCustomAction( new PlaybackStateCompat.CustomAction.Builder(
                "STRING_ACTION",
                "Charsequence_name",
                R.drawable.ic_settings
        ).build());
         */
        mSession.setPlaybackState(mStateBuilder.build());
        mSession.setCallback(new MediaSessionCallback());
        setSessionToken(mSession.getSessionToken());

        mMediaNotificationManager = new MediaNotificationManager(this);
        mMusicLibrary = new MusicLibrary(this);
        mMusicPlayer = new MusicPlayer(this);
    }

    @Override
    public void onDestroy() {
        mSession.release();
    }

    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName,
                                 int clientUid,
                                 Bundle rootHints) {
        return new BrowserRoot(MusicLibrary.BROWSER_ROOT, null);
    }

    @Override
    public void onLoadChildren(@NonNull final String parentMediaId,
                               @NonNull final Result<List<MediaItem>> result) {

        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        //Given parentMediaId we return the correct children to show
        switch (parentMediaId){
            case MusicLibrary.BROWSER_ROOT:
                mediaItems = mMusicLibrary.getRootItems();
                break;
            case MusicLibrary.ALBUMS:
                mediaItems = mMusicLibrary.getBrowsableItems(MusicLibrary.ALBUMS);
                break;
            case MusicLibrary.ARTISTS:
                mediaItems = mMusicLibrary.getBrowsableItems(MusicLibrary.ARTISTS);
                break;
            case MusicLibrary.SONGS:
                mediaItems = mMusicLibrary.getBrowsableItems(MusicLibrary.SONGS);
                break;
            default:
                //The user clicked on an album, artist or a single song.
                //So we search in the music library for all the related elements
                //given the parentMediaId clicked
                mediaItems = mMusicLibrary.getMediaItemsFromParentId(parentMediaId);
                break;
        }
        result.sendResult(mediaItems);
    }

    private final class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
        }

        @Override
        public void onSkipToQueueItem(long queueId) {
        }

        @Override
        public void onSeekTo(long position) {
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
        }

        @Override
        public void onPause() {
        }

        @Override
        public void onStop() {
        }

        @Override
        public void onSkipToNext() {
        }

        @Override
        public void onSkipToPrevious() {
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
        }

        @Override
        public void onPlayFromSearch(final String query, final Bundle extras) {
        }
    }
}
