package com.alebr.muzic;

import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.util.Log;

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
    private Map<String, Bitmap> album_art = new HashMap<>();

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
                    loadAlbumArtAsync(albumArtUri);
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

    private void loadAlbumArtAsync(final Uri albumArtUri){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(albumArtUri, "r");
                    if(pfd != null){
                        FileDescriptor fd = pfd.getFileDescriptor();
                        album_art.put(String.valueOf(albumArtUri), BitmapFactory.decodeFileDescriptor(fd));
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public List<MediaBrowserCompat.MediaItem> getRootItems(){
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        //Create the main categories for the media
        mediaItems.add(generateBrowsableMediaItem(ALBUMS, ALBUMS, String.valueOf(albums.size()), BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_pause)));
        mediaItems.add(generateBrowsableMediaItem(ARTISTS, ARTISTS, String.valueOf(artists.size()), BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_pause)));
        mediaItems.add(generateBrowsableMediaItem(SONGS, SONGS, String.valueOf(songs.size()), BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_pause)));

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
                             album_art.get(album.getAlbumArtUriAsString())));
                }
                break;
            case ARTISTS:
                for(ArtistItem artist : artists){
                    mediaItems.add(generateBrowsableMediaItem(
                            artist.getIdString(),
                            artist.getName(),
                            "",
                            null));
                }
                break;
            case SONGS:
                for(SongItem song : songs){
                    mediaItems.add(generatePlayableItem(
                            song.getIdString(),
                            song.getTitle(),
                            song.getArtist(),
                            song.getAlbum(),
                            album_art.get(song.getAlbumArtUri().toString()),
                            song.getSongUri()));
                }
                break;
            default:
                //This should not happen, if we get in here there is an error with the parentId value
        }
        return mediaItems;
    }

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
            Bitmap album_art_local = album_art.get(String.format("%s/%s", ALBUM_ART_URI, string_id));
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
                            songItem.getSongUri()));
                }
            }
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
                            album_art.get(songItem.getAlbumArtUri().toString()),
                            songItem.getSongUri()));
                }
            }
        }
        else{
            //The parent id clicked was a song, just return the song with the id specified
            for(SongItem songItem : songs){
                if(songItem.getIdString().equals(parentId)){
                    mediaItems.add(generatePlayableItem(
                            songItem.getIdString(),
                            songItem.getTitle(),
                            songItem.getArtist(),
                            songItem.getAlbum(),
                            album_art.get(songItem.getAlbumArtUri().toString()),
                            songItem.getSongUri()));
                }
            }
        }

        return mediaItems;
    }

    private MediaBrowserCompat.MediaItem generatePlayableItem(String id, String title, String artist, String album, Bitmap icon, Uri mediaUri){
        MediaDescriptionCompat.Builder mediaDescriptionBuilder = new MediaDescriptionCompat.Builder();

        mediaDescriptionBuilder.setMediaId(id)
                .setTitle(title)
                .setSubtitle(artist)
                .setDescription(album)
                .setIconBitmap(icon)
                .setMediaUri(mediaUri);
        return new MediaBrowserCompat.MediaItem(mediaDescriptionBuilder.build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
    }

    /*
    Generates browsable media item given an id and a count
     */
    private MediaBrowserCompat.MediaItem generateBrowsableMediaItem(String id, String title, String subtitle, Bitmap icon){
        MediaDescriptionCompat.Builder mediaDescriptionBuilder = new MediaDescriptionCompat.Builder();
        //Set the id to category name, set the title to category, the subtitle to che number of items
        //in the category and the icon
        mediaDescriptionBuilder.setMediaId(id)
                .setTitle(title)
                .setSubtitle( subtitle);
        if(icon != null)
            mediaDescriptionBuilder.setIconBitmap(icon);
        return new MediaBrowserCompat.MediaItem(mediaDescriptionBuilder.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }
}
