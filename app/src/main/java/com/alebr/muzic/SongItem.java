package com.alebr.muzic;

import android.net.Uri;

public class SongItem {
    /*
    This class represent a single song in the library, it holds all the main
    information useful for the session state and the playback state
     */

    private long id;//Unique for every song
    private String idString;
    private String title;
    private String artist;
    private long artistId;
    private String album;
    private long albumId;       //Unique for every album
    private long duration;
    private Uri songUri;        //Uri of the song
    private Uri albumArtUri;    //Uri of the album art (the cover of the album)

    public SongItem(long id, String title, String artist, long artistId, String album, long albumId, long duration,
                    Uri songUri, Uri albumArtUri){
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.artistId = artistId;
        this.album = album;
        this.albumId = albumId;
        this.duration = duration;
        this.songUri = songUri;
        this.albumArtUri = albumArtUri;

        idString = String.format("song_%d", id);
    }

    public long getId() {
        return id;
    }

    public String getIdString(){return idString;}

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public long getArtistId(){return artistId;}

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
