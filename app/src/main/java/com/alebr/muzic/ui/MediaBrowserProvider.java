package com.alebr.muzic.ui;

import android.support.v4.media.MediaBrowserCompat;

/**
 * Gives a public interface to send a {@link MediaBrowserCompat} instance between fragments
 * hosted by {@link MainActivity} and {@link FullPlayerActivity}
 */
interface MediaBrowserProvider {
    MediaBrowserCompat getMediaBrowser();
}
