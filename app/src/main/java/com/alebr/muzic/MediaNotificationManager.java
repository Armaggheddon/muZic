package com.alebr.muzic;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.session.PlaybackState;
import android.os.Build;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media.session.MediaButtonReceiver;

public class MediaNotificationManager {
    /*
    Handle sending multimedia notifications
    It manages the creation of a new channel (if not already created) and
    allows to edit the already published notification in order to represent correctly
    the current playback state
     */
    private static final String TAG = "MediaNotificationManager";
    public static final String CHANNEL_ID = "muZic";
    /*
    NOTIFICATION_ID cannot be 0
    Since we use only one notification at a time, we don't need to generate the ID,
    in fact by using the same ID we can update the notification that is currently
    being shown
     */
    public static final int NOTIFICATION_ID = 100;
    //REQUEST_CODE can be a random number
    public static final int REQUEST_CODE = 244;

    private MusicService mService;

    private final NotificationManager mNotificationManager;
    private final NotificationCompat.Action mPlayAction;
    private final NotificationCompat.Action mPauseAction;
    private final NotificationCompat.Action mSkipNextAction;
    private final NotificationCompat.Action mSkipPreviousAction;

    public MediaNotificationManager(MusicService service){
        mService = service;
        mNotificationManager = (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);

        //TODO: add the strings to the strings.xml file

        //Build the actions that will be used for the notification
        mPlayAction = new NotificationCompat.Action(
                R.drawable.ic_play,
                "PLAY",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                        mService,
                        PlaybackStateCompat.ACTION_PLAY));
        mPauseAction = new NotificationCompat.Action(
                R.drawable.ic_pause,
                "PAUSE",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                        mService,
                        PlaybackStateCompat.ACTION_PAUSE));
        mSkipNextAction = new NotificationCompat.Action(
                R.drawable.ic_skip_next,
                "NEXT",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                        mService,
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT));
        mSkipPreviousAction = new NotificationCompat.Action(
                R.drawable.ic_skip_previous,
                "PREVIOUS",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                        mService,
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS));

        //Cancel all notifications in order to handle the case where the
        //service was killed and restarted by the system
        mNotificationManager.cancelAll();
    }

    public NotificationManager getNotificationManager(){return mNotificationManager;}

    public Notification getNotification(MediaMetadataCompat metadata,
                                        PlaybackStateCompat state,
                                        MediaSessionCompat.Token token){
        MediaDescriptionCompat description = metadata.getDescription();
        NotificationCompat.Builder builder = buildNotification(description, state, token);
        return builder.build();
    }

    private NotificationCompat.Builder buildNotification(MediaDescriptionCompat description,
                                                         PlaybackStateCompat state,
                                                         MediaSessionCompat.Token token){
        //If the device has android oreo or later we must create the channel for notification
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)createChannel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mService, CHANNEL_ID);
        //Set in order the: title, album name, artist, and the album cover
        builder.setContentTitle(description.getTitle())
                .setContentText(description.getDescription())
                .setSubText(description.getSubtitle())
                .setLargeIcon(description.getIconBitmap());
        builder.setStyle(
                new androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(token)
                /*The number 1 represent the action play/pause, 0 for skip previous, 2 for skip next*/
                .setShowActionsInCompactView(1)
                /*For android L set the cancel button for the notification*/
                .setShowCancelButton(true)
                .setCancelButtonIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                mService,
                                PlaybackStateCompat.ACTION_STOP)))
                .setColor(ContextCompat.getColor(mService, R.color.colorPrimary))
                .setSmallIcon(R.drawable.ic_app_icon)
                .setContentIntent(createContentIntent())
                /*For android 6 and later since the user can swype away the notification*/
                .setDeleteIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                mService,
                                PlaybackStateCompat.ACTION_STOP))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        builder.addAction(mSkipPreviousAction);
        //If the state is playing add the pause action, else add the play action
        builder.addAction(
                state.getState() == PlaybackStateCompat.STATE_PLAYING ?
                        mPauseAction:
                        mPlayAction
        );
        builder.addAction(mSkipNextAction);
        return builder;
    }

    private PendingIntent createContentIntent(){
        Intent openActivity = new Intent(mService, MainActivity.class);
        //Open the activity as single top, to prevent the launch of multipla activities on top
        //of each others
        openActivity.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(
                mService, REQUEST_CODE, openActivity, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel(){
        //If the channel does not already exist, we need to create it
        if (mNotificationManager.getNotificationChannel(CHANNEL_ID) == null){
            //User visible channel name
            CharSequence name = "muZic";
            //User visible channel description
            String description = "Channel description";
            //TODO: find why it reproduces the notification sound on notification update
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    name,
                    importance
            );
            //Disable the dot on the launcher icon
            channel.setShowBadge(false);
            channel.setDescription(description);
            mNotificationManager.createNotificationChannel(channel);
        }
        //else, no need to create the channel, it already exists
    }



}
