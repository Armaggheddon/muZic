package com.alebr.muzic.library;

import android.annotation.SuppressLint;

/**
 * Holds all the useful information about an album
 */

public class AlbumItem {

    private long id;
    private String idString;
    private String name;

    /**
     * Constructor of the item. It creates a idString appending {@param id} to
     * {@value MusicLibrary#ALBUM_}
     * @param id
     *          The id of the album obtained from {@value android.provider.MediaStore.Audio.Media#ARTIST_ID}
     * @param name
     *          The name of the artist
     */
    /* Suppress because it is not a user visible string, no need to format to "DefaultLocale" */
    @SuppressLint("DefaultLocale")
    public AlbumItem(long id, String name){
        this.id = id;
        this.name = name;

        /* Builds the idString as "album_id" */
        idString = String.format("%s%d", MusicLibrary.ALBUM_, id);
    }

    public String getIdString(){
        return idString;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
