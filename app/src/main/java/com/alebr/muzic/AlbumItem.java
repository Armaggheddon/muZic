package com.alebr.muzic;

import android.annotation.SuppressLint;
import android.net.Uri;

public class AlbumItem {

    private long id;
    private String idString;
    private String name;

    @SuppressLint("DefaultLocale")
    public AlbumItem(long id, String name){
        this.id = id;
        this.name = name;

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
