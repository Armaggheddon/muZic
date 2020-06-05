package com.alebr.muzic;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MusicLibrary {
    /**
     * Holds all the information about the music data in the device storage, it handles the retrieving
     * and all operations related to the library, it also allows for helper methods to get the
     * items already prebuilt/pre-formatted for the MediaSessionCompat in the MusicService class
     */

    private static final String TAG = "MusicLibrary";

    //Android auto styling for grids instead of list, in order are the KEY and the VALUE to
    //assign to the bundle of the MediaItem when built
    /*
    public static final String CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT";
    public static final int CONTENT_STYLE_GRID_ITEM_HINT_VALUE = 2;
     */

    //Cache the FLAGS since they are used often, also creates a custom flag FLAG_PLAYLIST holding
    // both the flags telling Android Auto that the items in the MediaItems should
    // be treated as a Playlist instead of a list (or grid)
    public static final int FLAG_PLAYABLE = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE;
    public static final int FLAG_BROWSABLE = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE;
    public static final int FLAG_PLAYLIST = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE | MediaBrowserCompat.MediaItem.FLAG_PLAYABLE;

    //Main categories of the media library, it is navigated from the BROWSER_ROOT ->(ALBUMS & ARTIST & SONGS)
    //The EMPTY_ROOT is used for clients that are not allowed to use the MusicService
    public static final String EMPTY_ROOT = "EMPTY_ROOT";
    public static final String BROWSER_ROOT = "ROOT_AUTO";
    public static final String ALBUMS = "Albums";
    public static final String ARTISTS = "Artists";
    public static final String SONGS = "Songs";
    //Represents the first part of the id of items, the full id is obtained merging song_ with the
    //long value of the id
    public static final String SONG_ = "song_";
    public static final String ALBUM_ = "album_";
    public static final String ARTIST_ = "artist_";

    public static final String DURATION_ARGS_EXTRA = "duration";
    public static final String ALBUM_ART_URI_ARGS_EXTRA = "album_art_uri";

    private static final String IC_ALBUM = "android.resource://com.alebr.muzic/drawable/ic_album";
    private static final String IC_ARTIST = "android.resource://com.alebr.muzic/drawable/ic_artist";
    private static final String IC_SONG = "android.resource://com.alebr.muzic/drawable/ic_audiotrack";

    //Album art path to build the path to the album art
    private static final String ALBUM_ART_URI = "content://media/external/audio/albumart";

    //Holds all the songs on the device
    private List<SongItem> songs = new ArrayList<>();
    //Holds all the albums main data on the device
    private List<AlbumItem> albums = new ArrayList<>();
    //Holds just the albumID value which is a Long for faster check in the albums
    private List<Long> albumIds = new ArrayList<>();
    //Same as for albums and albumIds
    private List<ArtistItem> artists= new ArrayList<>();
    private List<Long> artistIds = new ArrayList<>();

    private final Context context;

    /**
     * Constructor of the class, it also initialize the media retrieving process to avoid waiting
     * @param context is used to retrieve the data from the memory since we need a contentResolver
     */
    public MusicLibrary(Context context){
        this.context = context;
        initLibrary();
    }

    /**
     * Retrieve the data for all the songs in the device storage
     * -ID : unique identifier of the song
     * -TITLE : the title of the song
     * -ALBUM : the album name of the song
     * -ARTIST : the artist name of the song
     * -ARTIST_ID : unique identifier of the artist
     * -ALBUM_ID : unique identifier of the album
     * -DURATION : the length in ms of the song
     */
    private void initLibrary(){

        //The column DURATION was added back in API level 1, the columns so exists before Q, as shown
        //in the link below.
        //https://github.com/AndroidSDKSources/android-sdk-sources-for-api-level-1/blob/c77731af5068b85a350e768757d229cae00f8098/android/provider/MediaStore.java#L292
        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + "=1";

        try(Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null)){
            //Cache the column ids since they are always the same and used in every iteration
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
                //Build the songUri (the song itself to play) and the albumArtUri (the image of the album)
                Uri songUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                Uri albumArtUri = ContentUris.withAppendedId(Uri.parse(ALBUM_ART_URI), albumId);

                //If artistIds doesn't have this artistId add the artist data to a list of
                //ArtistItems that contains the artistId (unique) and the artist name.
                //The class (ArtistItem) also generates an IdString that is used to understand
                // what it is being asked from the MusicService (later in the class).
                //The artistId is also added to the list of artistIds for a faster comparison.
                //The same applies fot the album data in AlbumItem (+ the Uri of the album art)
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
                            album));
                    albumIds.add(albumId);
                }
                //Add to the songs list a new instance of SongItem that holds all the useful
                //information about the song itself
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
        }catch (NullPointerException e){
            Log.e(TAG, "initLibrary: ", e);
        }
        //Sort the songs, albums and artists alphabetically using a comparator.
        //Since the comparator is only used at this point there is no need to cache it
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

    /**
     * Returns a bitmap representation of the Uri given as parameter. The image is also resized to
     * be 320x320 to match Android Auto default size
     * https://developer.android.com/guide/topics/media-apps/working-with-a-media-session#maintain-state
     * @param albumArtUri the uri pointing to the album art image in the storage
     * @return bitmap the bitmap pointed by the albumArtUri, null if an IOException occurs
     */
    public Bitmap loadAlbumArt(Uri albumArtUri){
        //If albumArtUri is null return the default album icon
        if (albumArtUri == null)
            return getDefaultLargeIcon();

        Bitmap bitmap = null;
        try{
            ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(albumArtUri, "r");
            if(pfd != null){
                FileDescriptor fd = pfd.getFileDescriptor();
                BitmapFactory.Options options = new BitmapFactory.Options();
                //Reduce the sampling size to reduce memory consumption
                //read 1 pixel every options.inSampleSize pixel/pixels
                options.inSampleSize = 1;
                //Scale it down to be 320x320 since it will be anyway scaled down to that resolution
                bitmap = BitmapFactory.decodeFileDescriptor(fd, new Rect(0, 0, 320, 320), options);
                pfd.close();
            }
        }catch (IOException e){
            e.printStackTrace();
        }

        return (bitmap != null) ? bitmap : getDefaultLargeIcon();
    }

    private Bitmap getDefaultLargeIcon(){
        Drawable drawable = context.getResources().getDrawable(R.drawable.ic_default_album_art_with_bg);

        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }


    /**
     * Creates the browsable root elements from where to start to navigate.
     * @return a list of mediaItems that holds the information of the categories
     */
    public List<MediaBrowserCompat.MediaItem> getRootItems(){
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        //Create the main categories for the media: Albums, Artists, Songs
        mediaItems.add(generateBrowsableOrPlaylistItem(
                ALBUMS,
                ALBUMS,
                String.valueOf(albums.size()),
                Uri.parse(IC_ALBUM),
                FLAG_BROWSABLE));
        mediaItems.add(generateBrowsableOrPlaylistItem(
                ARTISTS,
                ARTISTS,
                String.valueOf(artists.size()),
                Uri.parse(IC_ARTIST),
                FLAG_BROWSABLE));
        mediaItems.add(generateBrowsableOrPlaylistItem(
                SONGS,
                SONGS,
                String.valueOf(songs.size()),
                Uri.parse(IC_SONG),
                FLAG_PLAYLIST));
        return mediaItems;
    }


    /**
     * Creates the items for Android Auto and default clients. For Android Auto,
     * since Songs is a Playlist it creates the PLAYLIST items only for Albums and Artists for
     * the parentId given (Android Auto never gets in "case SONGS").
     * For default clients it creates the items as BROWSABLE for ALBUMS and ARTISTS
     * (with an added image Uri for the artist) and as PLAYABLE for the songs
     * @param parentId the parent ID clicked to get in this category which can be ALBUMS or ARTISTS or SONGS
     * @return the mediaItems as Playlists playable, an empty list if the parentId does not exist
     */
    public List<MediaBrowserCompat.MediaItem> getItemsFromParentId(String parentId){
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        switch (parentId){
            case ALBUMS:
                //Create the children for ALBUMS, for every album in albums create the item as
                //BROWSABLE or PLAYLIST, and if is NOT Android Auto assign the iconUri to the item
                //to allow later retrieving in the UI to show the right album image. Same applies for ARTISTS
                for(AlbumItem album : albums){
                     mediaItems.add(generateBrowsableOrPlaylistItem(
                             album.getIdString(),
                             album.getName(),
                             "",
                             null,
                             FLAG_PLAYLIST));
                }
                break;
            case ARTISTS:
                for(ArtistItem artist : artists){
                    mediaItems.add(generateBrowsableOrPlaylistItem(
                            artist.getIdString(),
                            artist.getName(),
                            "",
                            null,
                            FLAG_PLAYLIST));
                }
                break;
            case SONGS:
                //Called when a client subscribes to SONGS, it does not happen with Android Auto as client
                for(SongItem song : songs){
                    mediaItems.add(generatePlayableItem(
                            song.getIdString(),
                            song.getTitle(),
                            song.getArtist(),
                            song.getAlbum(),
                            song.getAlbumArtUri(),
                            song.getSongUri()));
                }
                break;
            default:
                //This should not happen, if we get in here there is an error with the parentId value
                Log.d(TAG, "getItemsFromParentId: DEFAULT ERROR" + parentId);
        }
        return mediaItems;
    }

    /**
     * This method is called only if the client is NOT Android Auto. Based on the parentId we receive
     * we build the children to be displayed/played
     * @param parentId the parentId as a String, it is the unique identifier of the item
     * @return a list of MediaItems with the children of the parentId given
     */
    public List<MediaBrowserCompat.MediaItem> getMediaItemsFromParentId(String parentId){
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

        //The item is none of the above, we look then at the parentId to understand what is
        //the item clicked, since the ids are in the form "song_id", "album_id", "artist_id"
        //we just need to check what substring id is

        if(parentId.contains(ALBUM_)){
            //Get just the id value (just the number as string)
            String string_id = parentId.substring(ALBUM_.length());
            //The parent id is an album, we then send back the songs that share the same album
            for(SongItem songItem : songs){
                //If the album ID (long) equals the ID at the end of the parentID "album_id"
                if (String.valueOf(songItem.getAlbumId()).equals(string_id)){
                    mediaItems.add(generatePlayableItem(
                            songItem.getIdString(),
                            songItem.getTitle(),
                            songItem.getArtist(),
                            songItem.getAlbum(),
                            songItem.getAlbumArtUri(),
                            songItem.getSongUri()));
                }
            }
        }
        else if(parentId.contains(ARTIST_)){
            String string_id = parentId.substring(ARTIST_.length());
            for(SongItem songItem : songs){
                if(String.valueOf(songItem.getArtistId()).equals(string_id)){
                    mediaItems.add(generatePlayableItem(
                            songItem.getIdString(),
                            songItem.getTitle(),
                            songItem.getArtist(),
                            songItem.getAlbum(),
                            songItem.getAlbumArtUri(),
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
                            songItem.getAlbumArtUri(),
                            songItem.getSongUri()));
                }
            }
        }
        return mediaItems;
    }

    /**
     * Creates a MediaItem that is flagged as PLAYABLE with all the parameters given
     * @param id the unique id of the playable item, since is a song is in the form <song_id> (es "song_1")
     * @param title the title of the song
     * @param artist the name of the artist
     * @param album the name of the album
     * @param albumUri the Uri to the album image
     * @param mediaUri the Uri to the song itself
     * @return a MediaItem with all the data given with the PLAYABLE flag set
     */
    private MediaBrowserCompat.MediaItem generatePlayableItem(String id, String title, String artist, String album, Uri albumUri, Uri mediaUri){
        MediaDescriptionCompat.Builder mediaDescriptionBuilder = new MediaDescriptionCompat.Builder();

        //Dont set mediaDescriptionBuilder.setIconBitmap() to preserve resource consumption, it can
        //loaded from the memory one at a time when needed, just set the uri to get the image
        mediaDescriptionBuilder.setMediaId(id)
                //Set the title of the song
                .setTitle(title)
                //Set the artist name
                .setSubtitle(artist)
                //Set the album name
                .setDescription(album)
                //Set the iconUri of the bitmap
                .setIconUri(albumUri)
                //Set the mediaUri of the song itself
                .setMediaUri(mediaUri);
        //Return a new MediaItem with the flag PLAYABLE indicating that the item can be playable
        //so se MediaBrowserServiceCompat can start the player screen
        return new MediaBrowserCompat.MediaItem(mediaDescriptionBuilder.build(), FLAG_PLAYABLE);
    }

    /**
     * Creates the QueueItem for the MusicService for all the songs ready to be assigned to the MediaSessionCompat
     * @return a list of QueueItems that holds the information for all the songs in the queue
     */
    public List<MediaSessionCompat.QueueItem> getSongsQueue(){

        List<MediaSessionCompat.QueueItem> queueItems = new ArrayList<>();
        //Assign a position to every QueueItem to know its position in the queue
        int queuePosition = 0;
        for(SongItem songItem : songs){

            //Add extra data as DURATION and ALBUM_URI
            Bundle extras = new Bundle();
            extras.putLong(DURATION_ARGS_EXTRA, songItem.getDuration());
            extras.putLong("POSITION", queuePosition);
            extras.putString(ALBUM_ART_URI_ARGS_EXTRA, songItem.getAlbumArtUri().toString());

            //Build the queueItem from MediaSessionCompat.Builder()
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

    /**
     * Creates the queue for the album given as parameter with the songs in that album
     * @param albumId the albumId string as <album_id> (es "album_1")
     * @return the list of QueueItems with all the songs in the albumId album
     */
    //Suppress because it is not a user visible string, no need to format to "DefaultLocale"
    @SuppressLint("DefaultLocale")
    public List<MediaSessionCompat.QueueItem> getAlbumIdQueue(String albumId){
        List<MediaSessionCompat.QueueItem> queueItems = new ArrayList<>();
        int queuePosition = 0;
        for(SongItem songItem : songs){
            //Check if the song album matches the same albumId building the albumId string from the
            //songItem as "album_" + the albumId in the songItem
            if(albumId.equals(String.format("%s%d", ALBUM_, songItem.getAlbumId()))) {
                //Create the QueueItem and add some extra data
                Bundle extras = new Bundle();
                extras.putLong(DURATION_ARGS_EXTRA, songItem.getDuration());
                extras.putLong("POSITION", queuePosition);
                extras.putString(ALBUM_ART_URI_ARGS_EXTRA, songItem.getAlbumArtUri().toString());

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


    public List<MediaSessionCompat.QueueItem> getAlbumQueueFromQuery(String query){
        String albumId = null;

        for(AlbumItem item : albums){
            if(query.equalsIgnoreCase(item.getName())){
                albumId = item.getIdString();
                break;
            }
        }

        return (albumId != null) ? getAlbumIdQueue(albumId) : null;
    }

    public List<MediaSessionCompat.QueueItem> getArtistQueueFromQuery(String query){
        String artistId = null;

        for(ArtistItem item : artists){
            if(query.equalsIgnoreCase(item.getName())){
                artistId = item.getIdString();
                break;
            }
        }

        return (artistId != null) ? getArtistIdQueue(artistId) : null;
    }

    /**
     * Does the same as getAlbumIdQueue but with the artists (see method above)
     * @param artistId the artistId string as <artist_id> (es "artist_1")
     * @return the list of QueueItems with all the songs with artistId as artist
     */
    //Suppress because it is not a user visible string, no need to format to "DefaultLocale"
    @SuppressLint("DefaultLocale")
    public List<MediaSessionCompat.QueueItem> getArtistIdQueue(String artistId){
        List<MediaSessionCompat.QueueItem> queueItems = new ArrayList<>();
        int queuePosition = 0;
        for(SongItem songItem : songs){
            //Check if the song artist matches the same artistId building the artistId string from the
            //songItem as "artist_" + the artistId in the songItem
            if(artistId.equals(String.format("%s%d", ARTIST_, songItem.getArtistId()))) {
                Bundle extras = new Bundle();
                extras.putLong( DURATION_ARGS_EXTRA, songItem.getDuration());
                extras.putLong("POSITION", queuePosition);
                extras.putString(ALBUM_ART_URI_ARGS_EXTRA, songItem.getAlbumArtUri().toString());

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

    /**
     * Returns a list of QueueItem holding all the data that matches the query. The query is an user given string
     * that does not match the internal ID scheme ("album_id", "artist_id", "song_id") so we need to check in all
     * the library for a match.
     * The query gets filtered in the following order:
     *         -if a song title matches the query, we return a List of QueueItems holding the song as the first and only item
     *         -if a song artist matches the query we return a list of QueueItems holding all the songs of that artist
     *         -if a song album mathces the query we return a list of QueueItems holding all the songs in the album
     * @param query the query string containing the raw data passed from user or Google Assistant
     * @return a list of QueueItems with all the matches, if no matches an empty list is returned
     */
    //Suppress because it is not a user visible string, no need to format to "DefaultLocale"
    @SuppressLint("DefaultLocale")
    public List<MediaSessionCompat.QueueItem> getSearchResult(String query){

        List<MediaSessionCompat.QueueItem> queueItems = new ArrayList<>();

        for(SongItem songItem : songs){
            if(songItem.getTitle().equalsIgnoreCase(query)){
                Bundle extras = new Bundle();
                //Add the extras for DURATION and ALBUM_URI
                extras.putLong(DURATION_ARGS_EXTRA, songItem.getDuration());
                extras.putLong("POSITION", 0);
                extras.putString(ALBUM_ART_URI_ARGS_EXTRA, songItem.getAlbumArtUri().toString());

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
                //Just need to return the result that the method give us passing as parameter the
                //album_id build from the match, same applies for the artist match
                return getAlbumIdQueue(String.format("%s%d", ALBUM_, songItem.getAlbumId()));
            }if(songItem.getArtist().equalsIgnoreCase(query)){
                 return getArtistIdQueue(String.format("%s%d", ARTIST_, songItem.getArtistId()));
            }
        }
        //No matches in the library
        return null;
    }

    /**
     * Creates a custom MediaItem from the data passed setting all the necessary data,
     * icon and iconUri are exclusive as parameters to avoid issues in the showing of the
     * data in Android Auto UI
     * @param id the id to assign to the item
     * @param title the title to give to the item
     * @param subtitle the subtitle to give to the item
     * @param iconUri the icon Uri for the icon (used for the main categories, ALBUMS, ARTISTS, SONGS)
     * @param flag the flags to use to build the MediaItem as shown in the top of the class
     * @return the MediaItem built from the data given
     */
    private MediaBrowserCompat.MediaItem generateBrowsableOrPlaylistItem(String id, String title, String subtitle, Uri iconUri, int flag){

        MediaDescriptionCompat.Builder mediaDescriptionBuilder = new MediaDescriptionCompat.Builder();
        //Set the id to category name, set the title to category, the subtitle to che number of items
        //in the category and the icon
        mediaDescriptionBuilder.setMediaId(id)
                .setTitle(title)
                .setSubtitle( subtitle);
        if(iconUri != null)
            mediaDescriptionBuilder.setIconUri(iconUri);

        //To avoid showing a large list for the SONG category, set both the flags to handle the item
        //as a playlist, so when clicked we start playing audio from the first item.
        //In this way we can also load one image at a time using resources in a more frendly way
        return new MediaBrowserCompat.MediaItem(mediaDescriptionBuilder.build(), flag);
    }
}
