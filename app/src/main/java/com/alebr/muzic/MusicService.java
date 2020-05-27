package com.alebr.muzic;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;

import androidx.media.MediaBrowserServiceCompat;

import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

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
    private PackageValidator mPackageValidator;

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
                                PlaybackStateCompat.ACTION_PLAY_PAUSE|
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
                R.drawable.ic_action
        ).build());
         */
        mSession.setPlaybackState(mStateBuilder.build());
        mSession.setCallback(new MediaSessionCallback());
        setSessionToken(mSession.getSessionToken());

        mMediaNotificationManager = new MediaNotificationManager(this);
        mMusicLibrary = new MusicLibrary(this);
        mMusicPlayer = new MusicPlayer(this);
        mPackageValidator = new PackageValidator(this);
    }

    @Override
    public void onDestroy() {
        mSession.release();
    }

    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName,
                                 int clientUid,
                                 Bundle rootHints) {
        //To allow just certain apps check if the package name of the client is allowed
        if (!mPackageValidator.isCallerAllowed(this, clientPackageName, clientUid)){
            //The request comes from an untrusted source, return an empty browser root
            return new MediaBrowserServiceCompat.BrowserRoot(MusicLibrary.EMPTY_ROOT, null);
        }
        if (mPackageValidator.isValidCarPackage(clientPackageName)){
            //Here we can adapt the music library to show a different subset when connected to the car
        }
        return new BrowserRoot(MusicLibrary.BROWSER_ROOT, null);
    }

    @Override
    public void onLoadChildren(@NonNull final String parentMediaId,
                               @NonNull final Result<List<MediaItem>> result) {

        final List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        //Given parentMediaId we return the correct children to show
        //Detach the result to load the data in another thread and send the data when all is loaded
        if (parentMediaId.equals(MusicLibrary.EMPTY_ROOT)) {
            //Return an empty list since the caller is an untrusted client
            result.sendResult(mediaItems);
        }
        else {
            //The caller is a trusted client we then return the appropriate MediaItems
            switch (parentMediaId) {
                case MusicLibrary.BROWSER_ROOT:
                    mediaItems.addAll(mMusicLibrary.getRootItems());
                    result.sendResult(mediaItems);
                    break;
                case MusicLibrary.ALBUMS:
                    result.detach();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mediaItems.addAll(mMusicLibrary.getBrowsableItems(MusicLibrary.ALBUMS));
                            result.sendResult(mediaItems);
                        }
                    }).start();

                    break;
                case MusicLibrary.ARTISTS:

                    result.detach();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mediaItems.addAll(mMusicLibrary.getBrowsableItems(MusicLibrary.ARTISTS));
                            result.sendResult(mediaItems);
                        }
                    }).start();

                    break;
                case MusicLibrary.SONGS:
                    result.detach();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mediaItems.addAll(mMusicLibrary.getBrowsableItems(MusicLibrary.SONGS));
                            result.sendResult(mediaItems);
                        }
                    }).start();
                    break;
                default:
                    //The user clicked on an album, artist or a single song.
                    //So we search in the music library for all the related elements
                    //given the parentMediaId clicked
                /*
                result.detach();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mediaItems.addAll(mMusicLibrary.getMediaItemsFromParentId(parentMediaId));
                        result.sendResult(mediaItems);
                    }
                }).start();

                break;

                 */
            }
            //result.sendResult(mediaItems);
        }
    }

    private final class MediaSessionCallback extends MediaSessionCompat.Callback {

        private List<MediaSessionCompat.QueueItem> mQueue = new ArrayList<>();
        private int mQueuePosition = 0;

        @Override
        public void onPlay() {
        }

        @Override
        public void onSkipToQueueItem(long queueId) {
            /*
            Called when the user clicks on a queue item in the view showing all the songs in the queue
            the call gives a queueId representing the position of the item clicked, we then
            retrieve the item from the queue and play it
             */
            Log.d(TAG, "onSkipToQueueItem: " + queueId);
            setCorrectPlaybackState(PlaybackStateCompat.STATE_PLAYING, 0);
            setMetadataFromQueueItem(mQueue.get((int)queueId));
            mQueuePosition = (int) queueId;
        }

        @Override
        public void onSeekTo(long position) {
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            /*
            This method gets called whenever a PLAYABLE item is clicked or an item with both the
            flags PLAYABLE and BROWSABLE is clicked, since in our library the only item with both
            these flags is the list of all songs, we check if the mediaId is SONGS.
            In this case we prepare a Queue to play and assign it to the session (methods)
             */
            Log.d(TAG, "onPlayFromMediaId: " + mediaId);
            //If the mediaId equals MusicLibrary.SONGS the user clicked
            //the song playlist in the root so we set a queue to play all the songs in the selection
            if(mediaId.equals(MusicLibrary.SONGS)) {
                mQueue.clear();
                mQueue.addAll(mMusicLibrary.getSongsQueue());
                mSession.setQueue(mQueue);
                mQueuePosition = 0;
                setCorrectPlaybackState(PlaybackStateCompat.STATE_PLAYING, 0);
                setMetadataFromQueueItem(mQueue.get(mQueuePosition));
            }
            else if(mediaId.contains("album_")){
                mQueue.clear();
                mQueue.addAll(mMusicLibrary.getAlbumIdQueue(mediaId));
                mSession.setQueue(mQueue);
                mQueuePosition = 0;
                setCorrectPlaybackState(PlaybackStateCompat.STATE_PLAYING, 0);
                setMetadataFromQueueItem(mQueue.get(mQueuePosition));

            }else if(mediaId.contains("artist_")){
                mQueue.clear();
                mQueue.addAll(mMusicLibrary.getArtistIdQueue(mediaId));
                mSession.setQueue(mQueue);
                mQueuePosition = 0;
                setCorrectPlaybackState(PlaybackStateCompat.STATE_PLAYING, 0);
                setMetadataFromQueueItem(mQueue.get(mQueuePosition));
            }
            else{
                //A single item is clicked, perhaps a song in an album, so we load the song with the
                //appropriate queue
                //Most likely the smartphone UI asked something to play
            }
        }

        @Override
        public void onPause() {
        }

        @Override
        public void onStop() {
        }

        @Override
        public void onSkipToNext() {
            if(mQueuePosition == mQueue.size()){
                //cant skip to next song we are at the last one
            }else{
                //Move to the next QueueItem
                mQueuePosition++;
                setCorrectPlaybackState(PlaybackStateCompat.STATE_PLAYING, 0);
                setMetadataFromQueueItem(mQueue.get(mQueuePosition));
            }
        }

        @Override
        public void onSkipToPrevious() {
            if(mQueuePosition == 0){
                //Can't skip to previous song, we are already at first one (0th item)
            }else{
                //Move to the previous QueueItem
                mQueuePosition--;
                setCorrectPlaybackState(PlaybackStateCompat.STATE_PLAYING, 0);
                setMetadataFromQueueItem(mQueue.get(mQueuePosition));
            }
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
        }


        @Override
        public void onPlayFromSearch(final String query, final Bundle extras) {

            Log.d(TAG, "onPlayFromSearch: " + query);
            List<MediaSessionCompat.QueueItem> queueItems = mMusicLibrary.getSearchResult(query);
            if (queueItems.size() == 0)
                return;
            mQueue.clear();
            mQueuePosition = 0;
            mQueue.addAll(queueItems);
            mSession.setQueue(mQueue);
            setCorrectPlaybackState(PlaybackStateCompat.STATE_PLAYING, 0);
            setMetadataFromQueueItem(mQueue.get(mQueuePosition));
        }

        private void setCorrectPlaybackState(int playbackState, long timeElapsed){
            //Since playback speed is always 1.0 no need to pass it as a parameter
            mStateBuilder.setState(
                    playbackState,
                    timeElapsed,
                    1.0f
            );
            mSession.setPlaybackState(mStateBuilder.build());
        }

        private void setMetadataFromQueueItem(MediaSessionCompat.QueueItem queueItem){
            /*
            Given a queueItem this method loads all the data and passes it to the setMetadata method
             */
            MediaDescriptionCompat data = queueItem.getDescription();
            setMetadata(
                    data.getTitle().toString(),
                    data.getSubtitle().toString(),
                    data.getDescription().toString(),
                    data.getExtras().getLong("DURATION"),
                    data.getMediaUri().toString(),
                    mMusicLibrary.loadAlbumArt(Uri.parse(data.getExtras().getString("ALBUM_URI")))
            );
        }

        private void setMetadata(String title, String artist, String album, long duration, String mediaUri, Bitmap albumArt){
            /*
            Builds a MediaMetadata object that holds all the information about the song being played
             */
            MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
            metadataBuilder.putText(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                    .putText(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                    .putText(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, mediaUri)
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt);
            mSession.setMetadata(metadataBuilder.build());
        }
    }
}
