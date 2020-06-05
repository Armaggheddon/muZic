package com.alebr.muzic;

import android.annotation.SuppressLint;

/**
 * Holds all the useful information about an artist
 */

public class ArtistItem {

    private String idString;
    private long id;
    private String name;

    /**
     * Constructor of the item. It creates a idString appending {@param id} to
     * {@value com.alebr.muzic.MusicLibrary#ARTIST_}
     * @param id
     *          The id of the artist obtained from {@value android.provider.MediaStore.Audio.Media#ARTIST_ID}
     * @param name
     *          The name of the artist
     */
    /* Suppress because it is not a user visible string, no need to format to "DefaultLocale" */
    @SuppressLint("DefaultLocale")
    public ArtistItem(long id, String name){
        this.id = id;
        this.name = name;

        /* Builds the idString as "artist_id" */
        idString = String.format("%s%d", MusicLibrary.ARTIST_, id);
    }

    public String getIdString() {
        return idString;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
