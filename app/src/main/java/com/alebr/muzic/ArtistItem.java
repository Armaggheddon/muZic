package com.alebr.muzic;

import android.annotation.SuppressLint;

public class ArtistItem {

    private String idString;
    private long id;
    private String name;

    @SuppressLint("DefaultLocale")
    public ArtistItem(long id, String name){
        this.id = id;
        this.name = name;

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
