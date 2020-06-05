package com.alebr.muzic;

import android.annotation.SuppressLint;
import android.net.Uri;

/**
 * Holds all the useful information about a song
 */

public class SongItem {

    private long id;
    private String idString;
    private String title;
    private String artist;
    private long artistId;
    private String album;
    private long albumId;
    private long duration;
    private Uri songUri;
    private Uri albumArtUri;

    /**
     * Constructor of the item. It creates a idString appending {@param id} to
     * {@value com.alebr.muzic.MusicLibrary#SONG_}
     * @param id
     *          The id of the song obtained from {@value android.provider.MediaStore.Audio.Media#_ID}
     * @param title
     *          The title of the song
     * @param artist
     *          The name of the artist
     * @param artistId
     *          The artist id of the artist obtained from {@value android.provider.MediaStore.Audio.Media#ARTIST_ID}
     * @param album
     *          The album name
     * @param albumId
     *          The album id of the album obtained from {@value android.provider.MediaStore.Audio.Media#ALBUM_ID}
     * @param duration
     *          The duration in milliseconds of the song obtained from
     *          {@value android.provider.MediaStore.Audio.Media#DURATION}
     * @param songUri
     *          The absolute path to the playable song item {@link MusicLibrary}
     * @param albumArtUri
     *          The absolute path to the album art in the storage {@link MusicLibrary}
     */

    /* Suppress because it is not a user visible string, no need to format to "DefaultLocale" */
    @SuppressLint("DefaultLocale")
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

        /* Builds the idString as "song_id" */
        idString = String.format("%s%d", MusicLibrary.SONG_, id);
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
