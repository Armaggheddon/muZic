package com.alebr.muzic;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import androidx.media.session.MediaButtonReceiver;

import com.alebr.muzic.ui.FullPlayerActivity;
import com.alebr.muzic.ui.MainActivity;

/**
 * Manages the correct set up of the notification and builds the
 * {@link MediaNotificationManager#createContentIntent()} pending intent that launches
 * {@link FullPlayerActivity}
 */

public class MediaNotificationManager {

    /* The maximum TAG length is 23 chars, but the class name has 24, so the "r" is removed  :( */
    private static final String TAG = "MediaNotificationManage";
    public static final String CHANNEL_ID = "MuzicApplication";

    /*
    NOTIFICATION_ID cannot be 0
    Since we use only one notification at a time, we don't need to generate the ID,
    in fact by using the same ID we can update the notification that is currently
    being shown
     */
    public static final int NOTIFICATION_ID = 100;

    /* Can ba a random value and it is */
    public static final int REQUEST_CODE = 244;

    private MusicService mService;

    /* Keep a final instance of the actions since are always the same, and are only swapped in/out */
    private final NotificationManager mNotificationManager;
    private final NotificationCompat.Action mPlayAction;
    private final NotificationCompat.Action mPauseAction;
    private final NotificationCompat.Action mSkipNextAction;
    private final NotificationCompat.Action mSkipPreviousAction;

    public MediaNotificationManager(MusicService service){
        mService = service;
        mNotificationManager = (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);

        /* Build the actions */
        mPlayAction = new NotificationCompat.Action(
                R.drawable.ic_play,
                mService.getString(R.string.play_text),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                        mService,
                        PlaybackStateCompat.ACTION_PLAY));
        mPauseAction = new NotificationCompat.Action(
                R.drawable.ic_pause,
                mService.getString(R.string.pause_text),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                        mService,
                        PlaybackStateCompat.ACTION_PAUSE));
        mSkipNextAction = new NotificationCompat.Action(
                R.drawable.ic_skip_next,
                mService.getString(R.string.skip_next_text),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                        mService,
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT));
        mSkipPreviousAction = new NotificationCompat.Action(
                R.drawable.ic_skip_previous,
                mService.getString(R.string.skip_previous_text),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                        mService,
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS));

        /* Cancel all notifications already to handle the case where the service was killed and restarted by the system*/
        mNotificationManager.cancelAll();
    }

    /**
     * Returns the {@link MediaNotificationManager#mNotificationManager} used
     * @return
     *          Returns the {@link MediaNotificationManager#mNotificationManager}
     */
    public NotificationManager getNotificationManager(){return mNotificationManager;}

    /**
     * Simplifies the process of building the notification calling
     * {@link MediaNotificationManager#buildNotification(MediaDescriptionCompat, PlaybackStateCompat, MediaSessionCompat.Token)}
     * @param metadata
     *          The metadata of the song being currently played
     * @param state
     *          The current state of the playback:
     *          - {@value android.support.v4.media.session.PlaybackStateCompat#STATE_PLAYING}
     *          - {@value android.support.v4.media.session.PlaybackStateCompat#STATE_PAUSED}
     * @param token
     *          The session token in {@link MusicService}
     * @return
     *          Returns a notification with the data representing the current {@param state} with
     *          the {@param metadata}
     */
    public Notification getNotification(MediaMetadataCompat metadata,
                                        PlaybackStateCompat state,
                                        MediaSessionCompat.Token token){
        MediaDescriptionCompat description = metadata.getDescription();
        NotificationCompat.Builder builder = buildNotification(description, state, token);
        return builder.build();
    }

    /**
     * Called by
     * {@link MediaNotificationManager#getNotification(MediaMetadataCompat, PlaybackStateCompat, MediaSessionCompat.Token)}.
     * Handles the creation of the channel if necessary and creates the notification
     * @param description
     *          The description extracted from {@link MediaMetadataCompat}
     * @param state
     *          The current state of the playback:
     *          - {@value android.support.v4.media.session.PlaybackStateCompat#STATE_PLAYING}
     *          - {@value android.support.v4.media.session.PlaybackStateCompat#STATE_PAUSED}
     * @param token
     *          The session token in {@link MusicService}
     * @return
     *          Returns a notification with the data representing the current {@param state} with
     *          the {@param description}
     */
    private NotificationCompat.Builder buildNotification(MediaDescriptionCompat description,
                                                         PlaybackStateCompat state,
                                                         MediaSessionCompat.Token token){

        /* If the app is running on Android O or later create the Notification channel */
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)createChannel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mService, CHANNEL_ID);

        /* Sets in order: title, album name, artist, and the album cover */
        builder.setContentTitle(description.getTitle())
                .setContentText(description.getDescription())
                .setSubText(description.getSubtitle())
                .setLargeIcon(description.getIconBitmap());

        /* Set the style to set the appearance of the notification based on the album art being played */
        builder.setStyle(
                new androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(token)
                /* The number 1 represent the action play/pause, 0 for skip previous, 2 for skip next */
                .setShowActionsInCompactView(1)
                /* For android L set the cancel button for the notification (notifications were not swipable) */
                .setShowCancelButton(true)
                        /* When the cancel button is clicked call ACTION_STOP on the MusicService */
                        .setCancelButtonIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                mService,
                                PlaybackStateCompat.ACTION_STOP)))
                /* Set the default notification background color */
                .setColor(ContextCompat.getColor(mService, R.color.colorPrimary))
                /* Set the small icon that is showed in the notification bar */
                .setSmallIcon(R.drawable.ic_app_icon)
                .setContentIntent(createContentIntent())
                /* When the notification is swiped away */
                .setDeleteIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                mService,
                                PlaybackStateCompat.ACTION_STOP))
                /* Set visibility public to show the notification on the lock screen*/
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        /* Set the actions to set to the notification */
        builder.addAction(mSkipPreviousAction);

        /* Set the action based on the current playback state, mActionPause if the state is STATE_PLAYING */
        builder.addAction(
                state.getState() == PlaybackStateCompat.STATE_PLAYING ?
                        mPauseAction:
                        mPlayAction
        );
        builder.addAction(mSkipNextAction);
        return builder;
    }

    /**
     * Build a pending intent that launches {@link FullPlayerActivity} when the notification is
     * clicked. It also builds a back stack with {@link MainActivity} since is the parent as described
     * in the manifest
     * @return
     *          Returns a PendingIntent that starts {@link FullPlayerActivity} and adds
     *          {@link MainActivity} in the back stack
     */

    private PendingIntent createContentIntent(){
        /*
        The default behaviour when creating an intent with TaskStackBuilder destroys the currently
        visible or available views and recreates them representing the stack described in the
        manifest of the application. The current stack is, from bottom to top
        MainActivity --> FullPlayerActivity with FullPlayerActivity being shown on top
         */

        Intent openActivity = new Intent(mService, FullPlayerActivity.class);

        /* Launch as SINGLE_TOP to prevent the launch of multiple activities on top of each other */
        openActivity.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        /* Create the TaskStackBuilder that inflates the back stack adding MainActivity */
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mService);
        stackBuilder.addNextIntentWithParentStack(openActivity);

        /* Use UPDATE_CURRENT to update the pending intent currently pending if exists */
        return stackBuilder.getPendingIntent(
                REQUEST_CODE, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    /**
     * Creates the notification channel for {@value Build.VERSION_CODES#O} and later if it does not
     * already exists
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel(){

        /* If the channel does not exist */
        if (mNotificationManager.getNotificationChannel(CHANNEL_ID) == null){

            Log.d(TAG, "createChannel: " +
                    "creating new notification channel with channelId " + CHANNEL_ID);

            /* User visible channel name */
            CharSequence name = mService.getString(R.string.app_name);

            /* User visible channel description */
            String description = mService.getString(R.string.channel_description);

            /* Set the importance to LOW to avoid sound when the notification is pushed or updated */
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    name,
                    importance
            );

            /*
            Starting with Android 8.0, notification badges (also known as notification dots)
            appear on a launcher icon when the associated app has an active notification.
            To avoid this behaviour set the value to false since the notification in a media app
            is showed as long as is in the play state
            */
            channel.setShowBadge(false);
            channel.setDescription(description);
            mNotificationManager.createNotificationChannel(channel);
        }

        /* Else the channel already exists, just use the existing one */
        else
            Log.d(TAG, "createChannel: " +
                    "using existing notification channel with channelId " + CHANNEL_ID);
    }
}
