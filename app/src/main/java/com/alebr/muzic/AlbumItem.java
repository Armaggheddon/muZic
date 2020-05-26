package com.alebr.muzic;

import android.net.Uri;

public class AlbumItem {

    private long id;
    private String idString;
    private String name;
    private Uri albumArtUri;

    public AlbumItem(long id, String name, Uri albumArtUri){
        this.id = id;
        this.name = name;
        this.albumArtUri = albumArtUri;

        idString = String.format("album_%d", id);
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

    public Uri getAlbumArtUri() {
        return albumArtUri;
    }

    public String getAlbumArtUriAsString() {
        return String.valueOf(albumArtUri);
    }
}
