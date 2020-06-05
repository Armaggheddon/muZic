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

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import androidx.media.session.MediaButtonReceiver;

/**
 * Manages the correct set up of the notification and the building process of the notification itself
 */

public class MediaNotificationManager {

    private static final String TAG = "MediaNotificationManager";
    public static final String CHANNEL_ID = "muZic";
    /*
    NOTIFICATION_ID cannot be 0
    Since we use only one notification at a time, we don't need to generate the ID,
    in fact by using the same ID we can update the notification that is currently
    being shown
     */
    public static final int NOTIFICATION_ID = 100;

    /* Can ba a random value and it is*/
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

        //TODO : check why creating a back stack causes the activity original stack to be destroyed and recreated

        Intent openActivity = new Intent(mService, FullPlayerActivity.class);

        /* Launch as SINGLE_TOP to prevent the launch of multiple activities on top of each other */
        openActivity.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        /* Create the TaskStackBuilder that inflates the back stack adding MainActivity */
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mService);
        stackBuilder.addNextIntentWithParentStack(openActivity);

        /* Use UPDATE_CURRENT to update the pending intent currently pending if exists */
        return stackBuilder.getPendingIntent(
                REQUEST_CODE, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent createContentIntent2(){
        Intent openActivity = new Intent(mService, FullPlayerActivity.class);
        //Open the activity as single top, to prevent the launch of multipla activities on top
        //of each others
        openActivity.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(
                mService, REQUEST_CODE, openActivity, PendingIntent.FLAG_CANCEL_CURRENT);
    }


    /**
     * Creates the notification channel for {@value Build.VERSION_CODES#O} and later if it does not
     * already exists
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel(){

        /* If the channel does not exist */
        if (mNotificationManager.getNotificationChannel(CHANNEL_ID) == null){

            /* User visible channel name */
            CharSequence name = mService.getString(R.string.app_name);

            /* User visible channel description */
            String description = mService.getString(R.string.channel_description);

            /* Set the importance to LOW to not send notification sound when the notification is pushed or updated */
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    name,
                    importance
            );

            /*
            In the latest android versions when an application has a notification a dot on the
            application icon is shown on the launcher telling the user about the event as long
            as the notification is removed. As a music player the notification
            would always be available as long as the playback is active so setting the value to
            false disables this behaviour
            */
            channel.setShowBadge(false);
            channel.setDescription(description);
            mNotificationManager.createNotificationChannel(channel);
        }
        /* Else the channel already exists, no need to create a new one */
    }



}
