package com.alebr.muzic;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.media.MediaBrowserServiceCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
    //True if the client is Android Auto, else is false, used to differentiate how to handle
    //the call to onPlayFromMediaId(...)
    private boolean IS_CAR_CONNECTED;

    @Override
    public void onCreate() {
        super.onCreate();

        mSession = new MediaSessionCompat(this, TAG);
        //Action that an app must support for Android Auto
        //https://developer.android.com/training/cars/media#required-actions
        //also set the initial PlaybackState to paused
        mStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY|
                                PlaybackStateCompat.ACTION_PAUSE|
                                PlaybackStateCompat.ACTION_PLAY_PAUSE|
                                PlaybackStateCompat.ACTION_STOP|
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS|
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT|
                                PlaybackStateCompat.ACTION_SEEK_TO|
                                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID|
                                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                ).setState(PlaybackStateCompat.STATE_PAUSED,
                        0,
                        1.0f);
        //To add custom action see the link below
        //https://developer.android.com/guide/topics/media-apps/working-with-a-media-session#custom-action
        //The following lines are necessary only for android 5.0 or before, since we are targeting
        //android 5.0+ the call is implicit
        //mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mSession.setPlaybackState(mStateBuilder.build());
        mSession.setCallback(new MediaSessionCallback());
        setSessionToken(mSession.getSessionToken());

        mMediaNotificationManager = new MediaNotificationManager(this);
        mMusicLibrary = new MusicLibrary(this);
        mMusicPlayer = new MusicPlayer(this, mSession);
        mPackageValidator = new PackageValidator(this);
    }

    @Override
    public void onDestroy() {
        mSession.release();
        mSession.getController().getTransportControls().stop();
    }

    /**
     * Called on connection with a client, returns a browsable root only if the client is allowed.
     * The allowed clients are described in allowed_media_browser_callers.xml
     * @param clientPackageName the package name of the client
     * @param clientUid the client unique id
     * @param rootHints a hint for building the browser root
     * @return a browser root, is a valid one BROWSER_ROOT if the client is a valid one,
     * or EMPTY_ROOT if the client is not allowed
     */
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
            //The client is Android Auto
            Log.d(TAG, "onGetRoot: ANDROID AUTO CONNECTED");
            IS_CAR_CONNECTED = true;
        }else {
            //The client is the phone app
            Log.d(TAG, "onGetRoot: APPLICATION CONNECTED");
            IS_CAR_CONNECTED = false;
        }
        return new BrowserRoot(MusicLibrary.BROWSER_ROOT, null);
    }

    /**
     * Called every time the clients clicks on an item with the flag BROWSABLE
     * @param parentMediaId the id if the item clicked
     * @param result the list of MediaItems with the result
     */
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
                            mediaItems.addAll(mMusicLibrary.getItemsFromParentId(MusicLibrary.ALBUMS));
                            result.sendResult(mediaItems);
                        }
                    }).start();

                    break;
                case MusicLibrary.ARTISTS:

                    result.detach();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mediaItems.addAll(mMusicLibrary.getItemsFromParentId(MusicLibrary.ARTISTS));
                            result.sendResult(mediaItems);
                        }
                    }).start();

                    break;
                case MusicLibrary.SONGS:
                    result.detach();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mediaItems.addAll(mMusicLibrary.getItemsFromParentId(MusicLibrary.SONGS));
                            result.sendResult(mediaItems);
                        }
                    }).start();
                    break;
                default:
                    //The user clicked on an album, artist or a single song
                    //So we search in the music library for all the related elements
                    //given the parentMediaId clicked and return a list of MediaItems representing
                    //the parentMediaId
                    result.detach();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mediaItems.addAll(mMusicLibrary.getMediaItemsFromParentId(parentMediaId));
                            result.sendResult(mediaItems);
                        }
                    }).start();
                    break;
            }
            //Do not return anything since we detach the result to be updated on its separated thread
        }
    }

    /**
     * Callback of MediaSession handling all the actions passed by the session such as play, pause,
     * stop, skip to next, skip to previous ...
     */
    private final class MediaSessionCallback extends MediaSessionCompat.Callback {

        private AudioManager mAudioManager = (AudioManager) MusicService.this.getSystemService(Context.AUDIO_SERVICE);
        private Handler mHandler = new Handler();
        //Runnable to stop the session after 30 seconds
        private Runnable delayedStopRunnable = new Runnable() {
            @Override
            public void run() {
                onStop();
            }
        };
        //Audio focus change listener
        private AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                switch (focusChange){
                    case AudioManager.AUDIOFOCUS_GAIN:
                        mMusicPlayer.setDefaultVolume();
                        //Dont call onPlay, otherwise the playback will start automatically as soon
                        //as we get the focus
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS:
                        //Pause the playback for 30 seconds, if no state changes happens, call onStop()
                        onPause();
                        mHandler.postDelayed(delayedStopRunnable, 30000);
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        onPause();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        //Lower the volume to allow ducking
                        mMusicPlayer.setDuckingVolume();
                        break;
                }
            }
        };

        //Receiver for audio becoming noisy with filter and custom receiver class
        private IntentFilter mNoisyFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        private BecomingNoisyReceiver mNoisyReceiver = new BecomingNoisyReceiver();
        private final class BecomingNoisyReceiver extends BroadcastReceiver{
            @Override
            public void onReceive(Context context, Intent intent) {
                if(AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())){
                    //Headphones or audio source changed to phone speakers, so pause the Playback
                    onPause();
                }
            }
        };

        //Instances of the QueueItem used for queue management
        private List<MediaSessionCompat.QueueItem> mQueue = new ArrayList<>();
        //Position of the item that is being currently played
        private int mQueuePosition = 0;

        /**
         * When the play button is clicked, it might be the notification play button, the one on the
         * car, the one on headphones or every connected device sending a play command.
         * The method handles the AudioFocus request and setting the notification for the media being
         * currently played, as well as the listener for ACTION_AUDIO_BECOMIG_NOISY
         */
        @Override
        public void onPlay() {

            //If the result is AUDIOFOCUS_GAIN we have the focus and can start the playback
            if(requestAudioFocus() == AudioManager.AUDIOFOCUS_GAIN) {

                //Set the session as active
                mSession.setActive(true);

                //Start the playback
                mMusicPlayer.play(mQueue.get(mQueuePosition).getDescription().getMediaUri());

                //Update the PlaybackState
                setCorrectPlaybackState(
                        PlaybackStateCompat.STATE_PLAYING,
                        mMusicPlayer.getPosition());

                //Get the notification
                Notification notification = mMediaNotificationManager.getNotification(
                        mSession.getController().getMetadata(),
                        mSession.getController().getPlaybackState(),
                        mSession.getSessionToken());
                //If the service is not started
                if (!isServiceStarted) {
                    //Start the foreground service and update the flag isServiceStarted
                    ContextCompat.startForegroundService(
                            MusicService.this,
                            new Intent(MusicService.this, MusicService.class));
                    isServiceStarted = true;
                }
                //Start the foreground service for the notification
                startForeground(MediaNotificationManager.NOTIFICATION_ID, notification);

                //Register the receiver
                registerReceiver(mNoisyReceiver, mNoisyFilter);
                //Remove the stop runnable if is started since the user interacted with our app
                //and we have regained AudioFocus
                mHandler.removeCallbacks(delayedStopRunnable);
            }
        }

        /**
         * Asks for AudioFocus based on the version of the OS the device is running
         * @return the int value representing the result of the request
         */
        private int requestAudioFocus(){
            int focusResult = 0;

            //For SDK version later than 26 (Android O and later)
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                //Build the attributes for the request
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build();
                //Build the request itself setting the listener for focus change
                AudioFocusRequest audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setOnAudioFocusChangeListener(afChangeListener)
                        .setAudioAttributes(audioAttributes)
                        .build();
                //Ask for the request
                focusResult = mAudioManager.requestAudioFocus(audioFocusRequest);
            }
            //For SDK version before than 26 (Android N and before)
            else{
                //Ask for the request setting the focus change listener
                focusResult = mAudioManager.requestAudioFocus(
                        afChangeListener,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN);
            }
            return focusResult;
        }

        /**
         * When a queue item is clicked in the queue view of Android Auto, this method is called
         * @param queueId the position in the queue of the item selected
         */
        @Override
        public void onSkipToQueueItem(long queueId) {
            /*
            Called when the user clicks on a queue item in the view showing all the songs in the queue
            the call gives a queueId representing the position of the item clicked, we then
            retrieve the item from the queue and play it
             */
            Log.d(TAG, "onSkipToQueueItem: " + queueId);
            setCorrectPlaybackState(PlaybackStateCompat.STATE_PLAYING, 0);
            mQueuePosition = (int) queueId;
            setMetadataFromQueueItem(mQueue.get(mQueuePosition));

            onPlay();
        }

        /**
         * Seeks to a pont in the song specified by the parameter given
         * @param position the value in ms where to seek to
         */
        @Override
        public void onSeekTo(long position) {
            setCorrectPlaybackState(
                    mSession.getController().getPlaybackState().getState(),
                    position);
            mMusicPlayer.seekTo(position);
        }

        /**
         * This method is called when a Playable item is clicked, or when a PLAYLIST is clicked.
         * Handle correctly the playback and the queue set-up based on what type of client is
         * connected
         * @param mediaId the mediaId of the item clicked, it is unique for every element
         * @param extras bundle extra with styling and other components, not used in this implementation
         */
        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {

            //If the connected client is Android Auto
            if(IS_CAR_CONNECTED) {
                //If the mediaId equals MusicLibrary.SONGS the user clicked
                //the song playlist in the root so we set a queue to play all the songs in the selection
                if (mediaId.equals(MusicLibrary.SONGS)) {
                    initQueue(mMusicLibrary.getSongsQueue(), true);
                    onPlay();
                } else if (mediaId.contains(MusicLibrary.ALBUM_)) {
                    initQueue(mMusicLibrary.getAlbumIdQueue(mediaId), true);
                    onPlay();
                } else if (mediaId.contains(MusicLibrary.ARTIST_)) {
                    initQueue(mMusicLibrary.getArtistIdQueue(mediaId), true);
                    onPlay();
                } else {
                    //This case should not happen in an Android Auto client
                    Log.d(TAG, "onPlayFromMediaId: " + mediaId);
                }
            }else{
                //If the mediaId equals MusicLibrary.SONGS the user subscribed for the songs library
                //so initialize the queue with the requested items, the same applies for the others.
                //The main difference from Android Auto is that the playback is not automatically started,
                //this is in order to allow the service to track what items are being browsed and
                //create the appropriate queue based on the item clicked
                if (mediaId.equals(MusicLibrary.SONGS)) {
                    initQueue(mMusicLibrary.getSongsQueue(), true);
                } else if (mediaId.contains(MusicLibrary.ALBUM_)) {
                    initQueue(mMusicLibrary.getAlbumIdQueue(mediaId), true);
                } else if (mediaId.contains(MusicLibrary.ARTIST_)) {
                    initQueue(mMusicLibrary.getArtistIdQueue(mediaId), true);
                } else {
                    //This case should never happen
                    Log.d(TAG, "onPlayFromMediaId: " + mediaId);
                }
            }
        }

        /**
         * The client asked to pause the playback so update the playback state accordingly
         */
        @Override
        public void onPause() {
            //Set the session state to Paused
            setCorrectPlaybackState(
                    PlaybackStateCompat.STATE_PAUSED,
                    mMusicPlayer.getPosition());
            //Update the notification status with the pause button
            Notification notification = mMediaNotificationManager.getNotification(
                    mSession.getController().getMetadata(),
                    mSession.getController().getPlaybackState(),
                    mSession.getSessionToken());
            //Update the currently showing notification
            mMediaNotificationManager.getNotificationManager()
                    .notify(MediaNotificationManager.NOTIFICATION_ID, notification);
            //Pause the playback
            mMusicPlayer.pause();
            //Unregister the receiver since no audio is being played
            unregisterReceiver(mNoisyReceiver);

            stopForeground(false);
        }

        /**
         * The service is asked to stop, so release all the resources that holds and setting the
         * data and state correctly
         */
        @Override
        public void onStop() {
            //The order used is the same specified in the below developer resource
            //https://developer.android.com/guide/topics/media-apps/audio-app/mediasession-callbacks
            //Remove the notification and abandon audio focus
            mAudioManager.abandonAudioFocus(afChangeListener);
            stopSelf();
            mSession.setActive(false);
            setCorrectPlaybackState(
                    PlaybackStateCompat.STATE_STOPPED,
                    mMusicPlayer.getPosition());
            mSession.setActive(false);
            mMusicPlayer.stop();

            stopForeground(false);

            isServiceStarted = false;
        }

        /**
         * Handle prepare request to prepare the playback state, queue and metadata
         */
        @Override
        public void onPrepare() {
            super.onPrepare();
            //Called from both Android Auto and phone initialize a playback queue
            //with a random position
            //If the queue is null or empty initialize the queue with a queue from all the songs
            //this allow the pressing play button starts the playback with no issue
            if(mQueue == null || mQueue.isEmpty()) {
                List<MediaSessionCompat.QueueItem> queueItems = mMusicLibrary.getSongsQueue();
                Random random = new Random();
                mQueuePosition = random.nextInt(queueItems.size() - 1);
                initQueue(queueItems, false);
            }
            //Else the queue is not empty so there is no need to initialize the queue and just set
            //the state as Paused as described in the Android Auto guidelines
            //https://developer.android.com/training/cars/media#initial-playback-state
            setCorrectPlaybackState(
                    PlaybackStateCompat.STATE_PAUSED,
                    0);
            mSession.setActive(false);
        }

        /**
         * The client asked the next item in the queue to play, check if we reached the end of the
         * song being played, if is the case, set the state to paused
         */
        @Override
        public void onSkipToNext() {
            //mQueuePosition + 1 since it starts from 0 but the the size starts from 1 unless empty
            //so the last item in the queue is represented by mQueuePosition + 1

            if (mQueuePosition + 1 == mQueue.size()) {
                //cant skip to next song we are at the last one, then we do nothing
                //If is the end of queue end the song has finished playing call onStop() if it
                //was not already in the stopped state
                if (MusicPlayer.is_end_of_song &&
                        (mSession.getController().getPlaybackState().getState() != PlaybackStateCompat.STATE_STOPPED))
                    onStop();
            } else {
                //Move to the next QueueItem
                mQueuePosition++;
                //TODO: set state plyaing only if the current state was playing
                setCorrectPlaybackState(PlaybackStateCompat.STATE_PLAYING, 0);
                setMetadataFromQueueItem(mQueue.get(mQueuePosition));
                onPlay();
            }
        }

        /**
         * The client asked the previous item in the queue to play, handles the case when we are
         * playing the first item in the queue, where the playback is brought back to 0ms
         */
        @Override
        public void onSkipToPrevious() {
            if (mQueuePosition == 0) {
                //Can't skip to previous song, we are already at first one (0th item)
                //we can rewind the current song to the begin
                setCorrectPlaybackState(
                        PlaybackStateCompat.STATE_PLAYING,
                        0);
                mMusicPlayer.seekTo(0);
            } else {
                //Move to the previous QueueItem
                mQueuePosition--;
                setCorrectPlaybackState(PlaybackStateCompat.STATE_PLAYING, 0);
                setMetadataFromQueueItem(mQueue.get(mQueuePosition));
                onPlay();
            }
        }

        /**
         * Used to respond to custom actions in the Android Auto UI or other custom events
         * @param action the string representing the action, the same used when setting the custom
         *               action
         * @param extras Bundle holding extra data useful to better respond to the event
         */
        @Override
        public void onCustomAction(String action, Bundle extras) {
        }

        /**
         * Called when the user uses Google Assistant to query for something
         * @param query the raw query string the user say to the Assistant
         * @param extras Bundle holding extra information such as EXTRA_MEDIA_FOCUS to understand,
         *               if available (is not always available), if the query refers to Album, Artist
         *               Song item, or other categories described in the class MediaStore
         */
        @Override
        public void onPlayFromSearch(final String query, final Bundle extras) {

            Log.d(TAG, "onPlayFromSearch: " + query);
            Log.d(TAG, "onPlayFromSearch: " + extras.get(MediaStore.EXTRA_MEDIA_FOCUS));
            List<MediaSessionCompat.QueueItem> queueItems = mMusicLibrary.getSearchResult(query);
            if (queueItems == null || queueItems.size() == 0){
                //No results were available we do nothing
                return;
            }

            initQueue(queueItems, true);
            onPlay();
        }

        /**
         * Sets the Queue for the session in the correct way given a list of QueueItems and a flag
         * indicating if the current mQueuePosition is to be set to 0 or not, it also manages setting
         * the metadata for the session with the method setMetadataFromQueueItem
         * @param queueItems the list of the QueueItems to set
         * @param default_queue_position true if mQueuePosition is 0, false if is not to set
         */
        private void initQueue(List<MediaSessionCompat.QueueItem> queueItems, boolean default_queue_position){
            //TODO: see if is possible to assign duration here instead of passing trougth MediaStore
            // since the field DURATION is available only on Android 10+
            //Clear the previous queue
            mQueue.clear();
            //Set the new queue position to 0 since we are initializing the queue
            if(default_queue_position)
                mQueuePosition = 0;
            mQueue.addAll(queueItems);
            //Set the queue to the session
            mSession.setQueue(mQueue);
            //Update the metadata for the item that will be played
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

        /**
         * Parses all the data needed to setMetadata from a QueueItem
         * @param queueItem the QueueItem itself
         */
        private void setMetadataFromQueueItem(MediaSessionCompat.QueueItem queueItem){

            MediaDescriptionCompat data = queueItem.getDescription();
            setMetadata(
                    (data.getTitle()!=null)?data.getTitle().toString():"",
                    (data.getSubtitle()!=null)?data.getSubtitle().toString():"",
                    (data.getDescription()!=null)?data.getDescription().toString():"",
                    (data.getExtras()!=null)?data.getExtras().getLong("DURATION"):0,
                    (data.getMediaUri()!=null)?data.getMediaUri().toString():"",
                    mMusicLibrary.loadAlbumArtForAuto(Uri.parse((data.getExtras()!=null)?data.getExtras().getString("ALBUM_URI"):null))
            );
        }

        /**
         * Builds a MediametadataCompat with all the data necessary to display and consume by clients
         * and assigns it to the session
         * @param title of the song
         * @param artist name of the song
         * @param album name
         * @param duration in long milliseconds of the song
         * @param mediaUri Uri of the song itself
         * @param albumArt the album art of the song
         */
        private void setMetadata(String title, String artist, String album, long duration, String mediaUri, Bitmap albumArt){

            MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
            metadataBuilder.putText(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                    .putText(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                    .putText(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, mediaUri)
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt);
            mSession.setMetadata(metadataBuilder.build());
        }
    }
}
