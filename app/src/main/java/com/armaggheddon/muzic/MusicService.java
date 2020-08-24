package com.armaggheddon.muzic;

import android.Manifest;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.media.MediaBrowserServiceCompat;

import com.armaggheddon.muzic.library.MusicLibrary;

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

    private static final String CUSTOM_ACTION_REPLAY = "replay";

    private static final String CUSTOM_ACTION_RANDOM_SONG_IN_QUEUE = "random";
    public static final String CUSTOM_ACTION_ADD_TO_QUEUE_END = "end_of_queue";
    public static final String CUSTOM_ACTION_ADD_TO_QUEUE_NEXT = "next_of_queue";
    public static final String CUSTOM_ACTION_REMOVE_FROM_QUEUE = "remove_from_queue";

    private MediaSessionCompat mSession;

    /* The state builder is used a lod in the session, to avoid wasting resources creating it multiple times cache a instance */
    private PlaybackStateCompat.Builder mStateBuilder;

    /* Flag used to know if the Notification service is already started or not */
    private boolean isServiceStarted = false;

    private MediaNotificationManager mMediaNotificationManager;
    private MusicLibrary mMusicLibrary;
    private MusicPlayer mMusicPlayer;
    private PackageValidator mPackageValidator;

    /* True if the client connected to the session is Android Auto */
    private boolean IS_CAR_CONNECTED;

    /* If true tells the session that the user has not granted the permission to read external storage */
    private boolean PERMISSION_NOT_GRANTED = false;

    /* Receiver for audio becoming noisy with filter and custom receiver class */
    private IntentFilter mNoisyFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private BecomingNoisyReceiver mNoisyReceiver = new BecomingNoisyReceiver();

    private final class BecomingNoisyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                //Headphones or audio source changed to phone speakers, so pause the Playback
                mSession.getController().getTransportControls().pause();
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mSession = new MediaSessionCompat(this, TAG);

        /*
        An application that support Android Auto should support all the actions described here
        https://developer.android.com/training/cars/media#required-actions and also set the initial
        playback state to paused
        */
        mStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                PlaybackStateCompat.ACTION_STOP |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_SEEK_TO |
                                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID |
                                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                )
                .setState(PlaybackStateCompat.STATE_PAUSED,
                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                        1.0f);

        /*
        Build the "Replay" custom action for Android Auto that allows to restart a song */
        mStateBuilder.addCustomAction(
                new PlaybackStateCompat.CustomAction.Builder(
                        CUSTOM_ACTION_REPLAY,
                        getString(R.string.custom_action_replay),
                        R.drawable.ic_replay).build()
        );

        /* Build the "Random" custom action for Android Auto that allows to set a random song in the current queue */
        mStateBuilder.addCustomAction(
                new PlaybackStateCompat.CustomAction.Builder(
                        CUSTOM_ACTION_RANDOM_SONG_IN_QUEUE,
                        getString(R.string.custom_action_random_song_in_queue),
                        R.drawable.ic_dice).build()
        );

        /*
        The following lines are called automatically if MediaBrowserServiceCompat is used so there
        is no need to set these flags
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
         */
        mSession.setPlaybackState(mStateBuilder.build());
        mSession.setCallback(new MediaSessionCallback());
        setSessionToken(mSession.getSessionToken());

        /* Check for the permission */
        checkForPermissions();

        /*
        Build the objects that handle the media notification, the music library, the player and
        the PackageValidator that validates clients connected checking in a white list
        */
        mMediaNotificationManager = new MediaNotificationManager(this);

        /*
        If the application is started the first time in Android Auto we have no permissions and
        the application will crash (it is a rare behaviour that an user uses an app the first time
        on Android Auto). Still, this behaviour is not completely incorrect because the application
        can't work without the permission to read the external storage.
        To override this behaviour check if we have the permissions, if we dont set the error state
        and do not load the library because will cause the application to crash.
        By calling setErrorState is possible to display a message to the user about the error
        */
        if (PERMISSION_NOT_GRANTED)
            setErrorState();
        else
            mMusicLibrary = new MusicLibrary(this);

        mMusicPlayer = new MusicPlayer(this, mSession);
        mPackageValidator = new PackageValidator(this);
    }

    /**
     * Check if the {@value Manifest.permission#READ_EXTERNAL_STORAGE} is granted or not,
     * if is not granted updates the value of the flag
     */
    private void checkForPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED)
            PERMISSION_NOT_GRANTED = true;
    }

    /**
     * Set the session state to {@value PlaybackStateCompat#STATE_ERROR} and a message that tells
     * the user to open the application and grant the permission described in
     * {@link R.string#permission_not_available_auto_error_message}
     */
    private void setErrorState() {
        mStateBuilder.setState(
                PlaybackStateCompat.STATE_ERROR,
                PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                1.0f
        ).setErrorMessage(PlaybackStateCompat.ERROR_CODE_APP_ERROR,
                getString(R.string.permission_not_available_auto_error_message));
        mSession.setPlaybackState(mStateBuilder.build());
    }


    /**
     * Handle case when user swipes the app away the application from the recents apps list by
     * stopping the service (and any ongoing playback) releasing all the resources used
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
        mMusicPlayer.stop();
        stopNotification();
        mSession.getController().getTransportControls().stop();
        mSession.release();
        unregisterMNoisyReceiver();
    }

    /**
     * Removes the notification and stops tracking the session. If the session
     * was destroyed this has no effect
     */
    @Override
    public void onDestroy() {

        mMusicPlayer.stop();
        stopNotification();
        mSession.getController().getTransportControls().stop();
        mSession.release();
        unregisterMNoisyReceiver();
    }

    /**
     * There is currently no API function to check if a receiver is registered or not, then the only
     * safe option is to try unregistering the receiver in a try/catch block to avoid unexpected
     * behaviours
     */
    private void unregisterMNoisyReceiver() {
        try {
            unregisterReceiver(mNoisyReceiver);
        } catch (IllegalArgumentException e) {
            //Log.d(TAG, "unregisterMNoisyReceiver: mNoisyReceiver was already unregistered");
        }
    }

    /**
     * Utility method to stop the notification, checks if the notification service is running and
     * if it is the case stops the service and updates the flag {@link MusicService#isServiceStarted}
     */
    private void stopNotification() {
        if (isServiceStarted) {
            isServiceStarted = false;
            mMediaNotificationManager.getNotificationManager().cancel(MediaNotificationManager.NOTIFICATION_ID);
            stopForeground(true);
        }
    }

    /**
     * Called when a client asks to connect, returns a browsable root only if the client is allowed.
     * The allowed clients are described in {@link R.xml#allawed_media_browser_callers}
     *
     * @param clientPackageName The package name of the client that asked to connect
     * @param clientUid         The client unique id
     * @param rootHints         A hint for building the browser root
     * @return A browser root, is a valid one {@value MusicLibrary#BROWSER_ROOT} if the client
     * is a allowed by {@link PackageValidator}, or {@value MusicLibrary#EMPTY_ROOT}
     * if the client is not allowed
     */
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName,
                                 int clientUid,
                                 Bundle rootHints) {

        /* If the permissions are not granted set the session in an error state */
        if (PERMISSION_NOT_GRANTED) {
            setErrorState();
        } else {

            /* We have the permissions */
            if (!mPackageValidator.isCallerAllowed(this, clientPackageName, clientUid)) {

                /* The request comes from an unknown client, return an empty browser root */
                return new MediaBrowserServiceCompat.BrowserRoot(MusicLibrary.EMPTY_ROOT, null);
            }

            /*
            Now the client is in the white list
            True if the client is Android Auto
            False if client is not Android Auto
             */
            IS_CAR_CONNECTED = mPackageValidator.isValidCarPackage(clientPackageName);

            /* Return the valid browser root */
            return new BrowserRoot(MusicLibrary.BROWSER_ROOT, null);
        }
        return new BrowserRoot(MusicLibrary.EMPTY_ROOT, null);
    }

    /**
     * Called every time the client clicks (for Android Auto) an item that has the flag
     * {@value MediaBrowserCompat.MediaItem#FLAG_BROWSABLE}
     *
     * @param parentMediaId The id of the item clicked, the same that was set by
     *                      {@link MusicLibrary#getRootItems()}
     * @param result        The list of MediaItems on which to publish the related items
     */
    @Override
    public void onLoadChildren(@NonNull final String parentMediaId,
                               @NonNull final Result<List<MediaItem>> result) {

        final List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

        /* If the parentMediaId is EMPTY_ROOT the client is not allowed so return an empty list */
        if (parentMediaId.equals(MusicLibrary.EMPTY_ROOT)) {
            //Return an empty list since the caller is an untrusted client
            result.sendResult(mediaItems);
        }

        /* Else the client was allowed to browse the content */
        else {

            /*
            Calling detach() on result to load the data in another thread and send the data when is ready,
            the current implementation does not use this behaviour because the item are already
            loaded in the memory and building the item is a fast operation
             */
            switch (parentMediaId) {
                case MusicLibrary.BROWSER_ROOT:
                    mediaItems.addAll(mMusicLibrary.getRootItems());
                    result.sendResult(mediaItems);
                    break;
                case MusicLibrary.ALBUMS:

                    /* The item clicked is the "Albums" category showed in the main screen */
                    mediaItems.addAll(mMusicLibrary.getItemsFromParentId(MusicLibrary.ALBUMS));
                    result.sendResult(mediaItems);

                    break;
                case MusicLibrary.ARTISTS:

                    /* The item clicked is the "Artists" category showed in the main screen */
                    mediaItems.addAll(mMusicLibrary.getItemsFromParentId(MusicLibrary.ARTISTS));
                    result.sendResult(mediaItems);
                    break;
                case MusicLibrary.SONGS:

                    /* The item clicked is the "Songs" category showed in the main screen */
                    mediaItems.addAll(mMusicLibrary.getItemsFromParentId(MusicLibrary.SONGS));
                    result.sendResult(mediaItems);
                    break;
                default:

                    /*
                    The parentMediaId is none of the above, then build the data given the parentMediaId.
                    The item clicked can be a specific album such as "Album A" or a specific artist
                    so retrieve the songs in that album or from that artist
                    */
                    mediaItems.addAll(mMusicLibrary.getAlbumArtistItemsFromParentId(parentMediaId));
                    result.sendResult(mediaItems);
                    break;
            }
            /*
            No need to return result, all the above methods publishes the update when the data is
            ready
            */
        }
    }


    /**
     * Callback of MediaSession that handles all the actions passed by
     * {@link androidx.media.session.MediaButtonReceiver} such as play, pause,
     * stop, skip to next, skip to previous ... and the custom actions defined
     * in {@link MusicService#onCreate()}
     */
    private final class MediaSessionCallback extends MediaSessionCompat.Callback {

        /* Get the AudioManager to manage audio events */
        private AudioManager mAudioManager = (AudioManager) MusicService.this.getSystemService(Context.AUDIO_SERVICE);

        /* Handler used to cancel the call to onStop() */
        private Handler mHandler = new Handler();
        /* Is executed 30 seconds after AUDIOFOCUS_LOSS is received */
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
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_GAIN:

                        /* Set the volume back to the default value */
                        mMusicPlayer.setDefaultVolume();

                        /*
                        onPlay is not called here, otherwise the playback would start "automatically"
                        as soon as the focus is gained, leave to the user this action
                        */
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS:

                        /*
                        The focus is permanently lost, pause the playback and schedule a onStop()
                        call to be executed in 30 seconds from now.
                        This allows the user, if changes idea, to cancel the scheduled task if
                        any operation on the session is executed
                        */
                        onPause();
                        mHandler.postDelayed(delayedStopRunnable, 30000);
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:

                        /* The focus loss is just temporary so just pause the playback */
                        onPause();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:

                        /* Lower the playback audio to allow ducking behaviour */
                        mMusicPlayer.setDuckingVolume();
                        break;
                }
            }
        };

        /* A local copy of the current queue in use to the session */
        private List<MediaSessionCompat.QueueItem> mQueue = new ArrayList<>();

        /* The current item in the queue that is being played or selected */
        private int mQueuePosition = 0;

        /**
         * When the play button is clicked, it might be the notification play button, the one on the
         * car, the one on headphones or every connected device sending a play command.
         * The method handles the AudioFocus request and setting the notification for the media being
         * currently played, as well as registering the listener for ACTION_AUDIO_BECOMING_NOISY.
         * onPlay() is executed only if the queue is not null or empty
         */
        @Override
        public void onPlay() {

            /* If there is no queue or is empty call onPause to set the current playback state to paused */
            if (mQueue == null || mQueue.size() == 0) {
                onPause();
                return;
            }

            /* If the result is AUDIOFOCUS_GAIN we have the focus and can start the playback */
            if (requestAudioFocus() == AudioManager.AUDIOFOCUS_GAIN) {

                /* Set the session as active */
                mSession.setActive(true);

                /* Start the playback */
                mMusicPlayer.play(mQueue.get(mQueuePosition).getDescription().getMediaUri());

                /* Update the playback state */
                setCorrectPlaybackState(
                        PlaybackStateCompat.STATE_PLAYING,
                        mMusicPlayer.getPosition(),
                        mQueuePosition);

                /* Start building the notification */
                Notification notification = mMediaNotificationManager.getNotification(
                        mSession.getController().getMetadata(),
                        mSession.getController().getPlaybackState(),
                        mSession.getSessionToken());

                /* If the service is not started yet */
                if (!isServiceStarted) {

                    /* Start the foreground service fot the background playback and update the flag */
                    ContextCompat.startForegroundService(
                            MusicService.this,
                            new Intent(MusicService.this, MusicService.class));
                    isServiceStarted = true;
                }

                /* If the service is already in the started state just start it in the foreground */
                startForeground(MediaNotificationManager.NOTIFICATION_ID, notification);

                /* Register the receiver */
                registerReceiver(mNoisyReceiver, mNoisyFilter);

                /*
                If the previous AudioFocus state was AUDIOFOCUS_LOSS, now the user interacted with
                the session asking to start the playback, so remove the scheduled call to onStop()
                */
                mHandler.removeCallbacks(delayedStopRunnable);
            }
        }

        /**
         * Asks for AudioFocus based on the version of Android that the device is running in the most
         * appropriate way for each version
         *
         * @return The int value representing the result of the request
         */
        private int requestAudioFocus() {
            int focusResult;

            /* For Android O or later */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                /* Build the attributes based on the session needs */
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build();

                /* Build the request and set a callback to listen to future focus changes */
                AudioFocusRequest audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setOnAudioFocusChangeListener(afChangeListener)
                        .setAudioAttributes(audioAttributes)
                        .build();

                /* Ask for the audio focus */
                focusResult = mAudioManager.requestAudioFocus(audioFocusRequest);
            }

            /* For android before O */
            else {

                /* Ask for the audio focus and set the callback for future focus changes */
                focusResult = mAudioManager.requestAudioFocus(
                        afChangeListener,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN);
            }
            return focusResult;
        }

        /**
         * When a queue item is clicked in the queue view of Android Auto or a specific item in the
         * queue is asked by the application, this method is called.
         *
         * @param queueId The position in the queue of the item selected
         */
        @Override
        public void onSkipToQueueItem(long queueId) {

            /* Check for the queue not being null or empty */
            if (mQueue != null && mQueue.size() != 0) {

                /* If the item asked is the current one, just restart the song */
                if (mQueuePosition == queueId) {
                    onSeekTo(0);
                }

                /* Else skip to the selected item if the position is not bigger than mQueue.size() */
                else {
                    if (queueId < mQueue.size()) {

                        /* Set the current position in the queue */
                        mQueuePosition = (int) queueId;
                        setMetadataFromQueueItem(mQueue.get(mQueuePosition));

                        /* Delegate the work to start the new song to onPlay() */
                        onPlay();
                    }
                }
            }
        }

        /**
         * Seeks to a pont in the song specified by the parameter given
         *
         * @param position The value in milliseconds where to seek to
         */
        @Override
        public void onSeekTo(long position) {

            /*
            Update the playback state with the current position and set as state the current state
            so seeking to a specific point does not set change the state to a new one that does
            not reflect the previous one
            */
            setCorrectPlaybackState(
                    mSession.getController().getPlaybackState().getState(),
                    position,
                    mQueuePosition);

            /* Update the player position */
            mMusicPlayer.seekTo(position);
        }

        /**
         * This method is called when a {@value MusicLibrary#FLAG_PLAYABLE} item is clicked, or
         * when a {@value MusicLibrary#FLAG_PLAYLIST} is clicked.
         * Handles the playback state update and the queue set-up based on what type of client is
         * connected
         *
         * @param mediaId The mediaId of the item clicked, it is unique for every element
         * @param extras  The Bundle with styling and other components, not used in this implementation
         */
        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {

            /* If the client connected is Android Auto */
            if (IS_CAR_CONNECTED) {

                /*
                If the mediaId is MusicLibrary.SONGS the item asked is the song playlist, return a
                playlist with all the songs.
                For ALBUM_ the mediaId is built as "album_id" so if the mediaId contains "album_"
                we return a queue with the songs in the album, the same for ARTIST_
                */
                if (mediaId.equals(MusicLibrary.SONGS)) {

                    /* Initialize the queue for the SONGS and call onPlay to start the playback */
                    initQueue(mMusicLibrary.getSongsQueue(), true);
                    onPlay();
                } else if (mediaId.contains(MusicLibrary.ALBUM_)) {
                    initQueue(mMusicLibrary.getAlbumIdQueue(mediaId), true);
                    onPlay();
                } else if (mediaId.contains(MusicLibrary.ARTIST_)) {
                    initQueue(mMusicLibrary.getArtistIdQueue(mediaId, null), true);
                    onPlay();
                }
                /* ELse is a forbidden state for Android Auto clients */
                //Log.d(TAG, "onPlayFromMediaId: no matches found for \"" + mediaId + "\"");
            } else {

                /*
                If the mediaId is MusicLibrary.SONGS the client subscribed to the songs
                so initialize the queue with the requested items, the same applies for the others.
                The main difference from Android Auto clients is that the playback is not automatically
                started this this behaviour allows the session to track what items are being browsed
                and build the correct queue based on the item clicked
                 */
                if (mediaId.equals(MusicLibrary.SONGS)) {
                    initQueue(mMusicLibrary.getSongsQueue(), true);
                } else if (mediaId.contains(MusicLibrary.ALBUM_)) {
                    initQueue(mMusicLibrary.getAlbumIdQueue(mediaId), true);
                } else if (mediaId.contains(MusicLibrary.ARTIST_)) {
                    initQueue(mMusicLibrary.getArtistIdQueue(mediaId, null), true);
                }

                /* Else is a forbidden state and should never happen */
                //Log.d(TAG, "onPlayFromMediaId: no matches found for \"" + mediaId + "\"");
            }
        }


        /**
         * The client asked to pause the playback, update the playback state and the notification
         */
        @Override
        public void onPause() {

            /* If the current state is STATE_PLAYING */
            if (mSession.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING) {

                /* Update the current playback state */
                setCorrectPlaybackState(
                        PlaybackStateCompat.STATE_PAUSED,
                        mMusicPlayer.getPosition(),
                        mQueuePosition);

                /* Get a new notification with the play button instead of the pause */
                Notification notification = mMediaNotificationManager.getNotification(
                        mSession.getController().getMetadata(),
                        mSession.getController().getPlaybackState(),
                        mSession.getSessionToken());

                /* Update the notification */
                mMediaNotificationManager.getNotificationManager()
                        .notify(MediaNotificationManager.NOTIFICATION_ID, notification);

                /* Pause the playback */
                mMusicPlayer.pause();

                /* Unregister the receiver for ACTION_AUDIO_BECOMING_NOISY*/
                unregisterReceiver(mNoisyReceiver);

                /* Dont remove the foreground service of the notification */
                stopForeground(false);
            }
        }

        /**
         * The service is asked to stop, so release all the resources that holds and set the new
         * state. The operations are described at
         *
         * @see "https://developer.android.com/guide/topics/media-apps/audio-app/mediasession-callbacks"
         */
        @Override
        public void onStop() {

            /* Remove the notification and abandon audio focus */
            mAudioManager.abandonAudioFocus(afChangeListener);

            /* Update the playback state as STOPPED */
            setCorrectPlaybackState(
                    PlaybackStateCompat.STATE_STOPPED,
                    mMusicPlayer.getPosition(),
                    -1);

            /* Stop the MediaBrowserServiceCompat */
            stopSelf();

            /* Set the session as inactive */
            mSession.setActive(false);

            /* Release the MediaPlayer used to play audio */
            mMusicPlayer.stop();

            /* Stop the notification service */
            stopForeground(false);

            /* Update the flag */
            isServiceStarted = false;
        }

        /**
         * Handles the "prepare" request to prepare the session.
         * Creates a queue from {@link MusicLibrary#getSongsQueue()} with all the songs available
         * ,set the current position in the queue to be a random position and set the
         * playback state as {@value PlaybackStateCompat#STATE_PAUSED} as described in the documentation
         *
         * @see "https://developer.android.com/training/cars/media#initial-playback-state"
         */
        @Override
        public void onPrepare() {
            super.onPrepare();

            /* If the queue is null or empty initialize the queue with a queue from all the songs */
            if (mQueue == null || mQueue.isEmpty()) {
                List<MediaSessionCompat.QueueItem> queueItems = mMusicLibrary.getSongsQueue();
                Random random = new Random();

                /* Set the current position randomly */
                mQueuePosition = random.nextInt(queueItems.size());

                /* Initialize the queue */
                initQueue(queueItems, false);
            }

            /* Else the queue is not empty so there is no need to build a default queue */
            setCorrectPlaybackState(
                    PlaybackStateCompat.STATE_PAUSED,
                    0,
                    mQueuePosition);

            /* Set the session active */
            mSession.setActive(true);
        }

        /**
         * The client asked the next item in the queue.
         * This method is also called by {@link MusicPlayer} when the current song being played ends.
         */
        @Override
        public void onSkipToNext() {

            /*
            On Android API 27+ double clicking the play button (for example the button on headphones)
            calls this method, if a client connects to our service and perform this operation with
            a null queue the service would crash
             */
            if (mQueue != null) {

                /* If the current item is the last one in the queue */
                if (mQueuePosition + 1 == mQueue.size()) {

                    /*
                    The current item is already the last one in the queue, so is not possible to skip to
                    the next one.
                    If the end of the song is also reached, and the current PlaybackState is not STATE_STOPPED
                    call onPause() to pause the playback and call stop() on mMusicPlayer to release the
                    resources being held by the MediaPlayer
                    */
                    if (MusicPlayer.is_end_of_song &&
                            (mSession.getController().getPlaybackState().getState() != PlaybackStateCompat.STATE_STOPPED)) {
                        onPause();
                        mMusicPlayer.stop();
                    }
                } else {

                    /*
                    Check if mQueuePosition + 2
                    (+2 is to align the value to mQueue.size() that starts from 1 and adding the next item)
                    is a valid position, if it is update the queue position
                    */
                    if (mQueuePosition + 2 <= mQueue.size()) {
                        mQueuePosition++;

                        /* Update the metadata to represent the current item being played */
                        setMetadataFromQueueItem(mQueue.get(mQueuePosition));

                        /* Delegate the play */
                        onPlay();
                    }
                }
            }
        }

        /**
         * The client asked the previous item in the queue to play, if the current item is
         * already the first one we call
         * {@link MusicService#MediaBrowserServiceCompat#onSeekTo(long)} )} with
         * {@param position} = 0 else the current queue position is updated
         */
        @Override
        public void onSkipToPrevious() {

            /* If is the first item in the queue */
            if (mQueuePosition == 0) {

                /* Rewind the current track to the begin */
                setCorrectPlaybackState(PlaybackStateCompat.STATE_PLAYING, 0, mQueuePosition);
                mMusicPlayer.seekTo(0);
            } else {

                /* Update the current queue item being played */
                mQueuePosition--;

                /* Update the metadata to represent the current item in the queue */
                setMetadataFromQueueItem(mQueue.get(mQueuePosition));

                onPlay();
            }
        }

        /**
         * Used to respond to custom actions in the Android Auto UI or other clients
         *
         * @param action The string representing the action, the same used when setting the custom
         *               action in {@link MusicService#onCreate()}
         * @param extras The Bundle holding extra data useful to better respond to the event, not used
         *               in this implementation
         */
        @Override
        public void onCustomAction(String action, Bundle extras) {

            /* Check what custom action is sent */
            switch (action) {
                case CUSTOM_ACTION_REPLAY:
                    /* Delegate the work to correctly set everything to onSeekTo, because replay is just a "seek to position 0" operation */
                    onSeekTo(0);
                    break;
                case CUSTOM_ACTION_RANDOM_SONG_IN_QUEUE:

                    /* If the queue exists and has a number of items greater than 1 */
                    if (mQueue != null && mQueue.size() > 1) {

                        /*
                        Get a random position to skip to in the queue, the bound value is
                        exclusive so mQueue.size() is safe to use
                        */
                        Random random = new Random();
                        int randomPosition = random.nextInt(mQueue.size());

                        /* Delegate the work to do to onSkipToQueueItem */
                        onSkipToQueueItem(randomPosition);
                    }
                    break;
                case CUSTOM_ACTION_ADD_TO_QUEUE_END:
                    /* Get the mediaId as "song_x" and add the queue item obtained from MusicLibrary */
                    String mediaId = extras.getString(MusicLibrary.SONG_);
                    mQueue.add(mMusicLibrary.createQueueItemFromMediaId(mediaId, mQueue.size()));
                    /* Tell the session that the queue has been updated */
                    mSession.setQueue(mQueue);
                    break;
                case CUSTOM_ACTION_ADD_TO_QUEUE_NEXT:
                    mediaId = extras.getString(MusicLibrary.SONG_);
                    mQueue.add(mMusicLibrary.createQueueItemFromMediaId(mediaId, mQueuePosition));
                    mSession.setQueue(mQueue);
                    break;
                    /* TODO: implement a method that allows to restore a deleted item
                case "position":
                    mediaId = extras.getString(MusicLibrary.SONG_);
                    int position = extras.getInt("position");
                    mQueue.add(position, mMusicLibrary.createQueueItemFromMediaId(mediaId, position));
                    mSession.setQueue(mQueue);
                    break;

                     */
                case CUSTOM_ACTION_REMOVE_FROM_QUEUE:
                    mediaId = extras.getString(MusicLibrary.SONG_);
                    for(MediaSessionCompat.QueueItem item : mQueue){
                        if(item.getDescription().getMediaId().equalsIgnoreCase(mediaId)){
                            mQueue.remove(item);
                            break;
                        }
                    }
                    mSession.setQueue(mQueue);
                    break;
            }
        }

        /**
         * Called when the user uses Google Assistant to query for something in Android Auto
         *
         * @param query  The raw query string the user says to the Assistant
         * @param extras The Bundle holding extra information such as EXTRA_MEDIA_FOCUS to understand,
         *               if available (is not always available), if the query refers to Album, Artist
         *               Song item, or other categories described in {@link MediaStore}
         */
        @Override
        public void onPlayFromSearch(final String query, final Bundle extras) {

            //Log.d(TAG, "onPlayFromSearch: user raw query " + query);
            //Log.d(TAG, "onPlayFromSearch: Assistant parsed query " + extras.get(MediaStore.EXTRA_MEDIA_FOCUS));

            List<MediaSessionCompat.QueueItem> queueItems = new ArrayList<>();

            if (TextUtils.isEmpty(query)) {

                /*
                The user provided generic string for example "Play music", we then build a queue
                of all the songs, only if the current queue is not null or empty
                 */
                if (mQueue == null || mQueue.size() == 0) {
                    List<MediaSessionCompat.QueueItem> songQueue = mMusicLibrary.getSongsQueue();

                    /* Only if songQueue is not null */
                    if (songQueue != null)
                        queueItems.addAll(mMusicLibrary.getSongsQueue());
                }
            } else {

                /* Get the extra data about the query */
                String mediaFocus = extras.getString(MediaStore.EXTRA_MEDIA_FOCUS);
                if (TextUtils.equals(mediaFocus, MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE)) {

                    /* Build a queue with the songs of the artist queried */
                    String artistQuery = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST);
                    List<MediaSessionCompat.QueueItem> artistQueue = mMusicLibrary.getArtistQueueFromQuery(artistQuery);
                    if (artistQueue != null)
                        queueItems.addAll(artistQueue);
                } else if (TextUtils.equals(mediaFocus, MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE)) {

                    /* Build a queue with the songs of the album queried */
                    String albumQuery = extras.getString(MediaStore.EXTRA_MEDIA_ALBUM);
                    List<MediaSessionCompat.QueueItem> albumQueue = mMusicLibrary.getAlbumQueueFromQuery(albumQuery);
                    if (albumQueue != null)
                        queueItems.addAll(albumQueue);
                } else if (TextUtils.equals(mediaFocus, MediaStore.Audio.Media.ENTRY_CONTENT_TYPE)){

                    /* Build a queue with the song asked in the fist position and other songs from the same artist */
                    String songQuery = extras.getString(MediaStore.EXTRA_MEDIA_TITLE);
                    List<MediaSessionCompat.QueueItem> songsQueue = mMusicLibrary.getSongsQueueFromQuery(songQuery);
                    if (songsQueue != null)
                        queueItems.addAll(songsQueue);
                }
            }

            /* If the queue is not empty */
            if (queueItems.size() != 0) {

                /* Initialize the queue and start the playback */
                initQueue(queueItems, true);
                onPlay();
            } else {

                /*
                If no results were found the default implementation is to set the playback state
                to paused
                */
                onPause();
            }
        }

        /**
         * Sets the Queue for the session given a list of QueueItems and a flag
         * indicating if the current mQueuePosition is to be set to 0 or not, it also manages setting
         * the metadata for the session with the method
         * {@link MusicService#MediaBrowserServiceCompat#setMetadataFromQueueItem(MediaSessionCompat.QueueItem)}
         *
         * @param queueItems             The list of the QueueItems to set
         * @param default_queue_position True if mQueuePosition needs to be 0, false else
         */
        private void initQueue(List<MediaSessionCompat.QueueItem> queueItems, boolean default_queue_position) {

            /* Clear the previous queue */
            mQueue.clear();

            /* If we are asked to set the current position to 0 */
            if (default_queue_position)
                mQueuePosition = 0;

            /* Add all the queue items to the local queue */
            mQueue.addAll(queueItems);

            /* Assign the queue to the session */
            mSession.setQueue(mQueue);

            /* Update the metadata */
            setMetadataFromQueueItem(mQueue.get(mQueuePosition));
        }

        /**
         * Sets the playback state, it is a utility method that allows to reduce the amount of identical
         * code being repeated multiple times. It also updates the session about the current item in
         * the queue being played.
         *
         * @param playbackState The {@link PlaybackStateCompat} state to set
         * @param timeElapsed   The current position of the playback in milliseconds
         * @param queueId       The queueId to set as the current active item. If is -1 it is not set
         */
        private void setCorrectPlaybackState(int playbackState, long timeElapsed, long queueId) {

            /* The playback speed is always 1.0 */
            mStateBuilder.setState(
                    playbackState,
                    timeElapsed,
                    1.0f
            );

            /* If queueId is a valid value */
            if (queueId != -1) {

                /* Set the current active item */
                mStateBuilder.setActiveQueueItemId(queueId);
            }

            /* Set the state to the session */
            mSession.setPlaybackState(mStateBuilder.build());
        }


        /**
         * Utility method that extracts the data from a {@link MusicService#MediaBrowserServiceCompat#mQueue}
         * and uses the {@link MusicService#MediaBrowserServiceCompat#setMetadataFromQueueItem(MediaSessionCompat.QueueItem)}
         * to set the metadata to the session
         *
         * @param queueItem The from which extract the data
         */
        private void setMetadataFromQueueItem(MediaSessionCompat.QueueItem queueItem) {

            MediaDescriptionCompat data = queueItem.getDescription();
            /*
            Gets the following data:
                -Song title
                -Song album (description)
                -Song artist (subtitle)
                -Song duration
                -Song Uri to play from
                -Album art bitmap

            Checks for non-null values on the data to avoid unexpected behaviours
            */
            setMetadata(
                    (data.getTitle() != null) ? data.getTitle().toString() : "",
                    (data.getSubtitle() != null) ? data.getSubtitle().toString() : "",
                    (data.getDescription() != null) ? data.getDescription().toString() : "",
                    (data.getExtras() != null) ? data.getExtras().getLong(MusicLibrary.DURATION_ARGS_EXTRA) : 0,
                    (data.getMediaUri() != null) ? data.getMediaUri().toString() : "",
                    mMusicLibrary.loadAlbumArt(Uri.parse((data.getExtras() != null) ? data.getExtras().getString(MusicLibrary.ALBUM_ART_URI_ARGS_EXTRA) : null)));
        }

        /**
         * Builds a {@link MediaMetadataCompat} object with all the data necessary to display and
         * consume by clients and assigns it to the session
         *
         * @param title    The title of the song
         * @param artist   The name of the artist
         * @param album    The name of the album
         * @param duration The duration in milliseconds of the song
         * @param mediaUri The Uri of the song itself used to play the song by {@link MusicService#mMusicPlayer}
         * @param albumArt The album art of the song
         */
        private void setMetadata(String title, String artist, String album, long duration, String mediaUri, Bitmap albumArt) {

            /* Get a metadata builder and put all the data inside */
            MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
            metadataBuilder.putText(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                    .putText(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                    .putText(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, mediaUri)
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt);

            /* Update the session metadata */
            mSession.setMetadata(metadataBuilder.build());
        }
    }
}
