package com.alebr.muzic;

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
}
