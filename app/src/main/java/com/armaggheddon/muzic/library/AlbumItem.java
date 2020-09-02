package com.armaggheddon.muzic.library;

import android.annotation.SuppressLint;
import android.net.Uri;

/**
 * Holds all the useful information about an album
 */

public class AlbumItem {

    private long id;
    private String idString;
    private String name;
    private Uri albumArt;

    /**
     * Constructor of the item. It creates a idString appending {@param id} to
     * {@value MusicLibrary#ALBUM_}
     * @param id
     *          The id of the album obtained from {@value android.provider.MediaStore.Audio.Media#ARTIST_ID}
     * @param name
     *          The name of the artist
     */
    //TODO: comment
    /* Suppress because it is not a user visible string, no need to format to "DefaultLocale" */
    @SuppressLint("DefaultLocale")
    public AlbumItem(long id, String name, Uri albumArt){
        this.id = id;
        this.name = name;
        this.albumArt = albumArt;

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

    public Uri getAlbumArt(){return albumArt;}
}
