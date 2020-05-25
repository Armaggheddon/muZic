package com.alebr.muzic;

import android.net.Uri;

public class SongItem {
    /*
    Questa classe fornisce un oggetto di tipo SongItem che contiene tutte le
    principali informazioni su un brano necessarie alla riproduzione ed
    all'impostazione dei corretti metdati per la sessione
     */

    private long id;            //Univoco per ogni brano
    private String title;
    private String artist;
    private String album;
    private long albumId;       //Univoco per ogni album
    private long duration;      //Durata del brano
    private Uri songUri;        //Uri del brano
    private Uri albumArtUri;    //Uri dell'immagine dell'album

    public SongItem(long id, String title, String artist, String album, long albumId, long duration,
                    Uri songUri, Uri albumArtUri){
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.albumId = albumId;
        this.duration = duration;
        this.songUri = songUri;
        this.albumArtUri = albumArtUri;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public long getAlbumId() {
        return albumId;
    }

    public long getDuration() {
        return duration;
    }

    public Uri getSongUri() {
        return songUri;
    }

    public Uri getAlbumArtUri() {
        return albumArtUri;
    }
}
