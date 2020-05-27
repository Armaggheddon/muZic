package com.alebr.muzic;

import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.core.graphics.BitmapCompat;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MusicLibrary {
    /*
    This class handles all the music library related tasks, such as retrieving the data,
    creating the content tree, ...
     */

    private static final String TAG = "MusicLibrary";

    //Android auto styling for grids instead of list
    public static final String CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT";
    public static final int CONTENT_STYLE_GRID_ITEM_HINT_VALUE = 2;
    public static final int FLAG_PLAYABLE = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE;
    public static final int FLAG_BROWSABLE = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE;
    public static final int FLAG_PLAYLIST = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE | MediaBrowserCompat.MediaItem.FLAG_PLAYABLE;

    //This are the categories that the user sees when opens the app on his phone or on Android Auto
    public static final String BROWSER_ROOT = "root";
    public static final String ALBUMS = "Albums";
    public static final String ARTISTS = "Artists";
    public static final String SONGS = "Songs";

    public static final String ALBUM_ART_URI = "content://media/external/audio/albumart";

    //Save the songs in a Map where the key is the song ID which is unique
    private List<SongItem> songs = new ArrayList<>();
    //List of the available album names
    private List<AlbumItem> albums = new ArrayList<>();
    //Used to know which albums are already in the list
    private List<Long> albumIds = new ArrayList<>();
    private List<ArtistItem> artists= new ArrayList<>();
    private List<Long> artistIds = new ArrayList<>();
    //Map containing as the key the album URI and as value the bitmap of the album itself
    //private Map<String, Bitmap> album_art = new HashMap<>();

    private final Context context;

    public MusicLibrary(Context context){
        this.context = context;
        initLibrary();
    }

    private void initLibrary(){
        final String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION
        };
        //final String sortOrder = MediaStore.Audio.Media.DEFAULT_SORT_ORDER + " ASC";

        try(Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null)){
            //Cache the column ids since they are always the same
            int idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID);
            int artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            int durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            while(cursor.moveToNext()){
                long id = cursor.getLong(idCol);
                String title = cursor.getString(titleCol);
                String album = cursor.getString(albumCol);
                String artist = cursor.getString(artistCol);
                long artistId = cursor.getLong(artistIdCol);
                long albumId = cursor.getLong(albumIdCol);
                long duration = cursor.getLong(durationCol);
                Uri songUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                Uri albumArtUri = ContentUris.withAppendedId(Uri.parse(ALBUM_ART_URI), albumId);

                //Check in the list of artists if we added the artist, if not add it
                if(!artistIds.contains(artistId)){
                    artists.add(
                            new ArtistItem(
                                    artistId,
                                    artist));
                    artistIds.add(artistId);
                }
                if(!albumIds.contains(albumId)){
                    albums.add(new AlbumItem(
                            albumId,
                            album,
                            albumArtUri));
                    //Update the list of added albums
                    albumIds.add(albumId);
                    //loadAlbumArtAsync(albumArtUri);
                }

                songs.add(
                        new SongItem(
                                id,
                                title,
                                artist,
                                artistId,
                                album,
                                albumId,
                                duration,
                                songUri,
                                albumArtUri
                        ));
            }
        }
        Log.d(TAG, "run: LOADING FINISHED");

        //Sort the songs alphabetically, do the same for the other lists
        Collections.sort(songs, new Comparator<SongItem>() {
            @Override
            public int compare(SongItem o1, SongItem o2) {
                return o1.getTitle().compareToIgnoreCase(o2.getTitle());
            }
        });
        Collections.sort(albums, new Comparator<AlbumItem>() {
            @Override
            public int compare(AlbumItem o1, AlbumItem o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        Collections.sort(artists, new Comparator<ArtistItem>() {
            @Override
            public int compare(ArtistItem o1, ArtistItem o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
    }
    /*
    private void loadAlbumArtAsync(final Uri albumArtUri){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(albumArtUri, "r");
                    if(pfd != null){
                        FileDescriptor fd = pfd.getFileDescriptor();
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        //Reduce the sampling size to reduce memory consumption
                        //read one pixel every 4
                        options.inSampleSize = 4;
                        album_art.put(String.valueOf(albumArtUri), BitmapFactory.decodeFileDescriptor(fd, new Rect(0, 0, 320, 320), options));
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }).start();
    }
     */


    public Bitmap loadAlbumArt(Uri albumArtUri){
        Bitmap bitmap = null;
        try{
            ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(albumArtUri, "r");
            if(pfd != null){
                FileDescriptor fd = pfd.getFileDescriptor();
                BitmapFactory.Options options = new BitmapFactory.Options();
                //Reduce the sampling size to reduce memory consumption
                //read one pixel every 2
                options.inSampleSize = 1;
                bitmap = BitmapFactory.decodeFileDescriptor(fd, new Rect(0, 0, 320, 320), options);
                //album_art.put(String.valueOf(albumArtUri), BitmapFactory.decodeFileDescriptor(fd, new Rect(0, 0, 320, 320), options));
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return bitmap;
    }


    public List<MediaBrowserCompat.MediaItem> getRootItems(){
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        //Create the main categories for the media
        mediaItems.add(generateBrowsableMediaItem(ALBUMS, ALBUMS, String.valueOf(albums.size()), null, Uri.parse("android.resource://com.alebr.muzic/drawable/ic_album"), FLAG_BROWSABLE));
        mediaItems.add(generateBrowsableMediaItem(ARTISTS, ARTISTS, String.valueOf(artists.size()), null, Uri.parse("android.resource://com.alebr.muzic/drawable/ic_artist"), FLAG_BROWSABLE));
        mediaItems.add(generateBrowsableMediaItem(SONGS, SONGS, String.valueOf(songs.size()), null, Uri.parse("android.resource://com.alebr.muzic/drawable/ic_audiotrack"), FLAG_PLAYLIST));
        return mediaItems;
    }

    public List<MediaBrowserCompat.MediaItem> getBrowsableItems(String parentId){
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        switch (parentId){
            case ALBUMS:
                //Add the mediaItems as albums
                for(AlbumItem album : albums){
                     mediaItems.add(generateBrowsableMediaItem(
                             album.getIdString(),
                             album.getName(),
                             "",
                             null,
                             null,
                             FLAG_PLAYLIST));
                }
                break;
            case ARTISTS:
                for(ArtistItem artist : artists){
                    mediaItems.add(generateBrowsableMediaItem(
                            artist.getIdString(),
                            artist.getName(),
                            "",
                            null,
                            null,
                            FLAG_PLAYLIST));
                }
                break;
                /*
            case SONGS:
                for(SongItem song : songs){
                    mediaItems.add(generatePlayableItem(
                            song.getIdString(),
                            song.getTitle(),
                            song.getArtist(),
                            song.getAlbum(),
                            loadAlbumArt(song.getAlbumArtUri()),
                            song.getAlbumArtUri(),
                            song.getSongUri(),
                            FLAG_PLAYABLE));
                }
                break;

                 */
            default:
                //This should not happen, if we get in here there is an error with the parentId value
        }
        return mediaItems;
    }


/*
    public List<MediaBrowserCompat.MediaItem> getMediaItemsFromParentId(String parentId){
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

        Log.d(TAG, "getMediaItemsFromParentId: " + parentId);

        //The item is none of the above, we look then at the parentId to understand what is
        //the item clicked, since the ids are in the form "song_id", "album_id", "artist_id"
        //we just need to check what substring has the parent id
        String song_string = "song_";
        String album_string = "album_";
        String artist_string = "artist_";

        if(parentId.contains(album_string)){
            String string_id = parentId.substring(album_string.length());
            //Since the album art is the same for all the songs in the album, we can cache it
            Bitmap album_art_local = null;
            for(AlbumItem album : albums){
                if (album.getIdString().equals(parentId)) {
                    album_art_local = loadAlbumArt(album.getAlbumArtUri());
                    //We can break the loop since all the songs in one album have the same art
                    break;
                }
            }
            //The parent id is an album, we then send back the songs that share the same album
            for(SongItem songItem : songs){
                //If the album ID (long) equals the ID at the end of the parentID "album_id"
                if (String.valueOf(songItem.getAlbumId()).equals(string_id)){
                    mediaItems.add(generatePlayableItem(
                            songItem.getIdString(),
                            songItem.getTitle(),
                            songItem.getArtist(),
                            songItem.getAlbum(),
                            album_art_local,
                            songItem.getAlbumArtUri(),
                            songItem.getSongUri(),
                            FLAG_PLAYLIST));
                }
            }
            album_art_local = null;
        }
        else if(parentId.contains(artist_string)){
            String string_id = parentId.substring(artist_string.length());
            for(SongItem songItem : songs){
                if(String.valueOf(songItem.getArtistId()).equals(string_id)){
                    mediaItems.add(generatePlayableItem(
                            songItem.getIdString(),
                            songItem.getTitle(),
                            songItem.getArtist(),
                            songItem.getAlbum(),
                            loadAlbumArt(songItem.getAlbumArtUri()),
                            songItem.getAlbumArtUri(),
                            songItem.getSongUri(),
                            FLAG_PLAYLIST));
                }
            }
        }
        else{
            //The parent id clicked was a song, just return the song with the id specified
            /*
            for(SongItem songItem : songs){
                if(songItem.getIdString().equals(parentId)){
                    mediaItems.add(generatePlayableItem(
                            songItem.getIdString(),
                            songItem.getTitle(),
                            songItem.getArtist(),
                            songItem.getAlbum(),
                            album_art.get(songItem.getAlbumArtUri().toString()),
                            songItem.getAlbumArtUri(),
                            songItem.getSongUri()));
                }
            }
             */
/*
        }
        return mediaItems;
    }
*/
    private MediaBrowserCompat.MediaItem generatePlayableItem(String id, String title, String artist, String album, Bitmap icon, Uri albumUri, Uri mediaUri, int flag){
        MediaDescriptionCompat.Builder mediaDescriptionBuilder = new MediaDescriptionCompat.Builder();

        mediaDescriptionBuilder.setMediaId(id)
                //Set the title of the song
                .setTitle(title)
                //Set the artist name
                .setSubtitle(artist)
                //Set the album name
                .setDescription(album)
                //Set the icon bitmap of the song
                .setIconBitmap(icon)
                //Set the iconUri of the bitmap
                //.setIconUri(albumUri)
                //Set the mediaUri of the song itself
                .setMediaUri(mediaUri);
        //Return a new MediaItem with the flag PLAYABLE indicating that the item can be playable
        //so se MediaBrowserServiceCompat can start the player screen
        return new MediaBrowserCompat.MediaItem(mediaDescriptionBuilder.build(), flag);
    }

    public List<MediaSessionCompat.QueueItem> getSongsQueue(){
        /*
        Returns a list of QueueItem for the playback with all the information useful for the metadata
        and the playback itself
         */
        List<MediaSessionCompat.QueueItem> queueItems = new ArrayList<>();
        int queuePosition = 0;
        for(SongItem songItem : songs){

            Bundle extras = new Bundle();
            extras.putLong("DURATION", songItem.getDuration());
            extras.putString("ALBUM_URI", songItem.getAlbumArtUri().toString());

            queueItems.add(
                    new MediaSessionCompat.QueueItem(
                            new MediaDescriptionCompat.Builder().setMediaId(songItem.getIdString())
                                    .setMediaUri(songItem.getSongUri())
                            .setTitle(songItem.getTitle())
                            .setSubtitle(songItem.getArtist())
                            .setDescription(songItem.getAlbum())
                            .setExtras(extras).build(),
                            queuePosition++));
        }
        return queueItems;
    }

    public List<MediaSessionCompat.QueueItem> getAlbumIdQueue(String albumId){
        //albumId is in the form "album_id" where id is the unique number
        List<MediaSessionCompat.QueueItem> queueItems = new ArrayList<>();
        int queuePosition = 0;
        for(SongItem songItem : songs){
            if(albumId.equals(String.format("album_%d", songItem.getAlbumId()))) {
                Bundle extras = new Bundle();
                extras.putLong("DURATION", songItem.getDuration());
                extras.putString("ALBUM_URI", songItem.getAlbumArtUri().toString());

                queueItems.add(
                        new MediaSessionCompat.QueueItem(
                                new MediaDescriptionCompat.Builder().setMediaId(songItem.getIdString())
                                        .setMediaUri(songItem.getSongUri())
                                        .setTitle(songItem.getTitle())
                                        .setSubtitle(songItem.getArtist())
                                        .setDescription(songItem.getAlbum())
                                        .setExtras(extras).build(),
                                queuePosition++));
            }
        }
        return queueItems;
    }

    public List<MediaSessionCompat.QueueItem> getArtistIdQueue(String artistId){
        List<MediaSessionCompat.QueueItem> queueItems = new ArrayList<>();
        int queuePosition = 0;
        for(SongItem songItem : songs){
            if(artistId.equals(String.format("artist_%d", songItem.getArtistId()))) {
                Bundle extras = new Bundle();
                extras.putLong("DURATION", songItem.getDuration());
                extras.putString("ALBUM_URI", songItem.getAlbumArtUri().toString());

                queueItems.add(
                        new MediaSessionCompat.QueueItem(
                                new MediaDescriptionCompat.Builder().setMediaId(songItem.getIdString())
                                        .setMediaUri(songItem.getSongUri())
                                        .setTitle(songItem.getTitle())
                                        .setSubtitle(songItem.getArtist())
                                        .setDescription(songItem.getAlbum())
                                        .setExtras(extras).build(),
                                queuePosition++));
            }
        }
        return queueItems;
    }

    public List<MediaSessionCompat.QueueItem> getSearchResult(String query){
        /*
        The query gets filtered by the data in the list,
        -if a song title matches the query, we return a List of QueueItems holding the song as the first and only item
        -if a song artist matches the query we return a list of QueueItems holding all the songs of that artist
        -if a song album mathces the query we return a list of QueueItems holding all the songs in the album
         */
        List<MediaSessionCompat.QueueItem> queueItems = new ArrayList<>();
        for(SongItem songItem : songs){
            if(songItem.getTitle().equalsIgnoreCase(query)){
                Bundle extras = new Bundle();
                extras.putLong("DURATION", songItem.getDuration());
                extras.putString("ALBUM_URI", songItem.getAlbumArtUri().toString());

                queueItems.add(
                        new MediaSessionCompat.QueueItem(
                                new MediaDescriptionCompat.Builder().setMediaId(songItem.getIdString())
                                        .setMediaUri(songItem.getSongUri())
                                        .setTitle(songItem.getTitle())
                                        .setSubtitle(songItem.getArtist())
                                        .setDescription(songItem.getAlbum())
                                        .setExtras(extras).build(),
                                0));
                return queueItems;
            }if (songItem.getAlbum().equalsIgnoreCase(query)){
                Log.d(TAG, "getSearchResult: ALBUM" + query);
                return getAlbumIdQueue(String.format("album_%d", songItem.getAlbumId()));
            }if(songItem.getArtist().equalsIgnoreCase(query)){
                Log.d(TAG, "getSearchResult: ARTIST" + query);
                 return getArtistIdQueue(String.format("artist_%d", songItem.getArtistId()));
            }
        }
        return null;
    }

    /*
    Generates browsable media item given an id and a count
     */
    private MediaBrowserCompat.MediaItem generateBrowsableMediaItem(String id, String title, String subtitle,Bitmap icon, Uri iconUri, int flag){

        MediaDescriptionCompat.Builder mediaDescriptionBuilder = new MediaDescriptionCompat.Builder();
        //Set the id to category name, set the title to category, the subtitle to che number of items
        //in the category and the icon
        mediaDescriptionBuilder.setMediaId(id)
                .setTitle(title)
                .setSubtitle( subtitle);
        if(icon != null)
            mediaDescriptionBuilder.setIconBitmap(icon);
        if(iconUri != null)
            mediaDescriptionBuilder.setIconUri(iconUri);

        //To avoid showing a large list for the SONG category, set both the flags to handle the item
        //as a playlist, so when clicked we start playing audio from the first item.
        //In this way we can also load one image at a time using resources in a more frendly way
        return new MediaBrowserCompat.MediaItem(mediaDescriptionBuilder.build(), flag);
    }
}
