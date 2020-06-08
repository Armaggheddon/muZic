package com.alebr.muzic;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
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

/**
 * Holds all the information about the music data in the device storage, it handles the retrieving
 * and all operations related to the library, it also implements helper methods to get the
 * items already prebuilt/pre-formatted for the {@link MusicService}
 */

public class MusicLibrary {


    private static final String TAG = "MusicLibrary";

    /*
    Android auto styling for grids instead of list, in order are the KEY and the VALUE to
    assign to the bundle of the MediaItem when built

    public static final String CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT";
    public static final int CONTENT_STYLE_GRID_ITEM_HINT_VALUE = 2;
     */

    /*
    Cache the FLAGS since they are used often, also creates a custom flag FLAG_PLAYLIST holding
    both the flags telling Android Auto that the item is a playlist and is not browsable
     */
    public static final int FLAG_PLAYABLE = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE;
    public static final int FLAG_BROWSABLE = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE;
    public static final int FLAG_PLAYLIST = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE | MediaBrowserCompat.MediaItem.FLAG_PLAYABLE;

    /*
    Main categories of the media library, it is navigated from the BROWSER_ROOT ->(ALBUMS & ARTIST & SONGS)
    The EMPTY_ROOT is used for clients that are not allowed to connect to MusicService and are not
    in the white list
     */
    public static final String EMPTY_ROOT = "EMPTY_ROOT";
    public static final String BROWSER_ROOT = "ROOT_AUTO";
    public static final String ALBUMS = "Albums";
    public static final String ARTISTS = "Artists";
    public static final String SONGS = "Songs";

    /*
    Represents the first part of the id of the items, the full id is obtained merging song_ with the
    long value of the id retrieved from MediaStore
     */
    public static final String SONG_ = "song_";
    public static final String ALBUM_ = "album_";
    public static final String ARTIST_ = "artist_";

    /* Extra arguments that are used to set more information about an item */
    public static final String DURATION_ARGS_EXTRA = "duration";
    public static final String ALBUM_ART_URI_ARGS_EXTRA = "album_art_uri";

    /* URI string to the images used for the main categories */
    private static final String IC_ALBUM = "android.resource://com.alebr.muzic/drawable/ic_album";
    private static final String IC_ARTIST = "android.resource://com.alebr.muzic/drawable/ic_artist";
    private static final String IC_SONG = "android.resource://com.alebr.muzic/drawable/ic_audiotrack";

    /* Album art path to build the path to the album art */
    private static final String ALBUM_ART_URI = "content://media/external/audio/albumart";

    /* Holds all the songs on the device and all of the information related */
    private List<SongItem> songs = new ArrayList<>();

    /* Holds the data about the albums on the device */
    private List<AlbumItem> albums = new ArrayList<>();

    /* Holds just the albumID value (long). This allows for to perform a fast check on the data */
    private List<Long> albumIds = new ArrayList<>();

    /* Holds the data about the artist */
    private List<ArtistItem> artists = new ArrayList<>();

    /* Holds just the artistID value (long). This list allows to perform a fast check on the data */
    private List<Long> artistIds = new ArrayList<>();

    /* The context used to retrieve a ContentProvider */
    private final Context context;

    /* The default bitmap used if the media has no album art */
    private Bitmap defaultBitmap;

    /**
     * Constructor of the class, it initialize the media library on a different thread and loads the
     * {@link MusicLibrary#defaultBitmap}
     *
     * @param context The context used to retrieve the data from the memory because
     *                we need a contentResolver
     */
    public MusicLibrary(Context context) {
        this.context = context;
        initLibrary();
        initDefaultBitmap(context.getDrawable(R.drawable.ic_default_album_art_with_bg));
    }

    /**
     * Retrieves the information used for the playback from the storage using a ContentResolver.
     * Builds the projection array for the elements to retrieve and a selection string to restrict
     * the search only on the elements that are flagged as music.
     * Creates a new thread to load the data. The thread will publish the results as they are available
     *
     * Retrieves the data for all the songs in the device storage
     * -ID : unique identifier of the song
     * -TITLE : the title of the song
     * -ALBUM : the album name of the song
     * -ARTIST : the artist name of the song
     * -ARTIST_ID : unique identifier of the artist
     * -ALBUM_ID : unique identifier of the album
     * -DURATION : the length in milliseconds of the song
     *
     * The column {@value android.provider.MediaStore.Audio.Media#DURATION}
     * was added back in API level 1, the columns so exists before Q, as shown
     * in the link below despite on what the warning it shows
     * @see "https://github.com/AndroidSDKSources/android-sdk-sources-for-api-level-1/blob/c77731af5068b85a350e768757d229cae00f8098/android/provider/MediaStore.java#L292"
     */
    private void initLibrary() {
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

        MediaLibraryLoader mediaLibraryLoader = new MediaLibraryLoader(context.getContentResolver(), projection, selection);

        /* Start the thread */
        mediaLibraryLoader.run();
    }

    /**
     * Loads the data asked from {@link MusicLibrary#initLibrary()} in background without blocking
     * the UI thread
     */
    private final class MediaLibraryLoader implements Runnable {

        private final ContentResolver contentResolver;
        private final String[] projection;
        private final String selection;

        private MediaLibraryLoader(ContentResolver contentResolver, String[] projection, String selection) {
            this.contentResolver = contentResolver;
            this.projection = projection;
            this.selection = selection;
        }

        @Override
        public void run() {
            try (Cursor cursor = contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    null,
                    null)) {

                /* Cache the column ids since they are always the same and used in every iteration */
                int idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
                int titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                int artistIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID);
                int artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                int albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
                int albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
                int durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);

                /* While a row is available */
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol);
                    String title = cursor.getString(titleCol);
                    String album = cursor.getString(albumCol);
                    String artist = cursor.getString(artistCol);
                    long artistId = cursor.getLong(artistIdCol);
                    long albumId = cursor.getLong(albumIdCol);
                    long duration = cursor.getLong(durationCol);

                    /* Build the songUri (the song itself to play) and the albumArtUri (the image of the album) */
                    Uri songUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                    Uri albumArtUri = ContentUris.withAppendedId(Uri.parse(ALBUM_ART_URI), albumId);

                    /*
                    If artistIds dont have this artistId add the artist data to a list of
                    ArtistItems that contains the artistId (unique) and the artist name.
                    The class (ArtistItem) also generates an IdString that is used to understand
                    what it is being asked from the MusicService (later in the class) and avoid
                    issued that could happen if we use as Uid just the Id or the title.
                     */
                    if (!artistIds.contains(artistId)) {
                        artists.add(
                                new ArtistItem(
                                        artistId,
                                        artist));

                        /* Add this artist id so we can know what items we already added */
                        artistIds.add(artistId);
                    }
                    if (!albumIds.contains(albumId)) {
                        albums.add(new AlbumItem(
                                albumId,
                                album));
                        albumIds.add(albumId);
                    }

                    /*
                    Add to the songs list a SongItem that holds all the useful
                    information about the song
                     */
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

                /* Sort the songs, albums and artists alphabetically using a comparator */
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


            } catch (NullPointerException e) {
                Log.e(TAG, "run: error while loading the data ", e);
            }
        }
    }

    /**
     * Returns a bitmap representation of the Uri given as parameter. The image is also resized to
     * be 320x320 to match Android Auto default size
     * @see "https://developer.android.com/guide/topics/media-apps/working-with-a-media-session#maintain-state"
     *
     * @param albumArtUri
     *                    The uri that points to the album art image in the storage
     * @return
     *              The bitmap created from the URI given, null if an IOException occurs
     */
    public Bitmap loadAlbumArt(Uri albumArtUri) {
        //If albumArtUri is null return the default album icon
        if (albumArtUri == null)
            return defaultBitmap;

        Bitmap bitmap = null;
        try {
            ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(albumArtUri, "r");
            if (pfd != null) {
                FileDescriptor fd = pfd.getFileDescriptor();
                BitmapFactory.Options options = new BitmapFactory.Options();
                //Reduce the sampling size to reduce memory consumption
                //read 1 pixel every options.inSampleSize pixel/pixels
                options.inSampleSize = 1;
                //Scale it down to be 320x320 since it will be anyway scaled down to that resolution
                bitmap = BitmapFactory.decodeFileDescriptor(fd, new Rect(0, 0, 320, 320), options);
                pfd.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return (bitmap != null) ? bitmap : defaultBitmap;
    }

    /**
     * Loads in a background thread the default drawable for media that has no album art
     */
    private void initDefaultBitmap(final Drawable drawable) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = Bitmap.createBitmap(
                        drawable.getIntrinsicWidth(),
                        drawable.getIntrinsicHeight(),
                        Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
                defaultBitmap = bitmap;
            }
        }).start();
    }


    /**
     * Creates the root elements using
     * {@link MusicLibrary#generateBrowsableOrPlaylistItem(String, String, String, Uri, int)}
     *
     * @return
     *              A list of mediaItems that holds the information of all the categories
     */
    public List<MediaBrowserCompat.MediaItem> getRootItems() {
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

        /*
        Create the main categories for the media: Albums, Artists, Songs.
        Create the categories ALUBUMS and ARTISTS as BROWSABLE and SONGS as a PLAYLIST
        */
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
     * since Songs is already a Playlist creates the PLAYLIST items for Albums and Artists from
     * the parentId given (Android Auto never gets in "case SONGS").
     * For default clients returns the {@value MusicLibrary#SONGS} MediaItems
     * (with the image Uri for the artist) and the flag set to {@value MusicLibrary#FLAG_PLAYABLE}
     *
     * @param parentId
     *                 The parent ID clicked to get in this category which can be ALBUMS or ARTISTS
     *                 or SONGS
     * @return
     *              The mediaItems as {@value MusicLibrary#FLAG_PLAYABLE}, {@link MusicLibrary#FLAG_PLAYLIST}
     *              or an empty list if the parentId does not exist or is unknown
     */
    public List<MediaBrowserCompat.MediaItem> getItemsFromParentId(String parentId) {
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        switch (parentId) {
            case ALBUMS:

                /* Create the children for ALBUMS, for every album in "albums" create the item as a PLAYLIST */
                for (AlbumItem album : albums) {
                    mediaItems.add(generateBrowsableOrPlaylistItem(
                            album.getIdString(),
                            album.getName(),
                            "",
                            null,
                            FLAG_PLAYLIST));
                }
                break;
            case ARTISTS:

                /* Create the children for ARTISTS, for every artist in "artists" create the item as a PLAYLIST */
                for (ArtistItem artist : artists) {
                    mediaItems.add(generateBrowsableOrPlaylistItem(
                            artist.getIdString(),
                            artist.getName(),
                            "",
                            null,
                            FLAG_PLAYLIST));
                }
                break;
            case SONGS:

                /* Called when a client subscribes to SONGS, it does not happen with Android Auto as client */
                for (SongItem song : songs) {
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
                /* This should not happen, if we get in here there is an error with the parentId value */
                Log.d(TAG, "getItemsFromParentId: DEFAULT ERROR" + parentId);
        }
        return mediaItems;
    }

    /**
     * Called when a specific album or artist is asked, build a playlist from {@param parentId}
     *
     * @param parentId
     *                 The parentId as a String, it is the unique identifier of the item
     * @return
     *              A list of MediaItems with the children of {@param parentId}
     */
    public List<MediaBrowserCompat.MediaItem> getAlbumArtistItemsFromParentId(String parentId) {
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

        /* The ids are in the form "album_id", "artist_id" */
        if (parentId.contains(ALBUM_)) {

            /* Get just the id value (just the number as string after "album_") */
            String string_id = parentId.substring(ALBUM_.length());

            /* The parent id is an album, we then send back the songs that share the same album */
            for (SongItem songItem : songs) {
                /* If the album ID (long) equals the ID at the end of the parentID "album_id" */
                if (String.valueOf(songItem.getAlbumId()).equals(string_id)) {

                    /* Generate the playable items */
                    mediaItems.add(generatePlayableItem(
                            songItem.getIdString(),
                            songItem.getTitle(),
                            songItem.getArtist(),
                            songItem.getAlbum(),
                            songItem.getAlbumArtUri(),
                            songItem.getSongUri()));
                }
            }
        } else if (parentId.contains(ARTIST_)) {
            String string_id = parentId.substring(ARTIST_.length());
            for (SongItem songItem : songs) {
                if (String.valueOf(songItem.getArtistId()).equals(string_id)) {
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
     * Creates a MediaItem with the flag {@value MusicLibrary#FLAG_PLAYLIST} from all the parameters given
     *
     * @param id
     *           The unique id of the playable item, since is a song is in the form <song_id> (es "song_1")
     * @param title
     *              The title of the song
     * @param artist
     *               The name of the artist
     * @param album
     *              The name of the album
     * @param albumUri
     *                 The Uri to the album image
     * @param mediaUri
     *                 The Uri to the song itself
     * @return
     *              Returns a MediaItem with all the data given with the flag
     *              {@value MusicLibrary#FLAG_PLAYLIST}
     */
    private MediaBrowserCompat.MediaItem generatePlayableItem(String id, String title, String artist, String album, Uri albumUri, Uri mediaUri) {
        MediaDescriptionCompat.Builder mediaDescriptionBuilder = new MediaDescriptionCompat.Builder();

        /*
        Dont set mediaDescriptionBuilder.setIconBitmap() to preserve resource consumption, it can
        loaded from the memory one at a time when needed, just set the uri to get the image
         */
        mediaDescriptionBuilder.setMediaId(id)
                /* Set the title of the song */
                .setTitle(title)
                /* Set the artist name */
                .setSubtitle(artist)
                /* Set the album name */
                .setDescription(album)
                /* Set the iconUri of the bitmap */
                .setIconUri(albumUri)
                /* Set the mediaUri of the song itself */
                .setMediaUri(mediaUri);
        /*
        Return a new MediaItem with the flag PLAYABLE indicating that the item can be playable
        so MediaBrowserServiceCompat can start the player screen on Android Auto
         */
        return new MediaBrowserCompat.MediaItem(mediaDescriptionBuilder.build(), FLAG_PLAYABLE);
    }

    /**
     * Creates the QueueItem for {@link MusicService} for all the songs
     *
     * @return
     *              A list of QueueItems that holds the information of all the songs
     */
    public List<MediaSessionCompat.QueueItem> getSongsQueue() {

        List<MediaSessionCompat.QueueItem> queueItems = new ArrayList<>();

        /*
        Assign a position to every QueueItem to know its position in the queue.
        This position value is also used as the id of the queue item from the MediaSessionCompat
        */
        int queuePosition = 0;
        for (SongItem songItem : songs) {

            /* Add extra data as DURATION and ALBUM_URI */
            Bundle extras = new Bundle();
            extras.putLong(DURATION_ARGS_EXTRA, songItem.getDuration());
            extras.putString(ALBUM_ART_URI_ARGS_EXTRA, songItem.getAlbumArtUri().toString());

            queueItems.add(
                    buildQueueItem(songItem.getIdString(),
                            songItem.getSongUri(),
                            songItem.getTitle(),
                            songItem.getArtist(),
                            songItem.getAlbum(),
                            extras,queuePosition));

            /* Increment the current item that is added */
            queuePosition++;
        }
        return queueItems;
    }

    /**
     * Helper method that builds a QueueItem
     * @param idString
     *                 The stringId of the song from {@link SongItem}
     * @param songUri
     *                The song uri
     * @param title
     *              The title of the song
     * @param artist
     *               The name of the artist
     * @param album
     *              The name of the album
     * @param extras
     *               A Bundle with more data to assign
     * @param queuePosition
     *                      The queue position to assign to the QueueItem
     * @return a
     *              The QueueItem
     */
    private MediaSessionCompat.QueueItem buildQueueItem(String idString, Uri songUri, String title, String artist, String album, Bundle extras, int queuePosition){
        return new MediaSessionCompat.QueueItem(
                new MediaDescriptionCompat.Builder().setMediaId(idString)
                .setMediaUri(songUri)
                .setTitle(title)
                .setSubtitle(artist)
                .setDescription(album)
                .setExtras(extras).build(),
                queuePosition
        );
    }

    /**
     * Creates the queue for a specific album
     *
     * @param albumId
     *                The albumId string as <album_id> (es "album_1")
     * @return
     *          The list of QueueItems with all the songs in the albumId album
     */
    /* Suppress because it is not a user visible string, no need to format to "DefaultLocale" */
    @SuppressLint("DefaultLocale")
    public List<MediaSessionCompat.QueueItem> getAlbumIdQueue(String albumId) {
        List<MediaSessionCompat.QueueItem> queueItems = new ArrayList<>();
        int queuePosition = 0;
        for (SongItem songItem : songs) {
            /*
            Check if the song album matches the same albumId building the albumId string from the
            songItem as "album_" + the albumId in the songItem
             */
            if (albumId.equals(String.format("%s%d", ALBUM_, songItem.getAlbumId()))) {
                Bundle extras = new Bundle();

                /* Add extra data as DURATION and ALBUM_URI */
                extras.putLong(DURATION_ARGS_EXTRA, songItem.getDuration());
                extras.putString(ALBUM_ART_URI_ARGS_EXTRA, songItem.getAlbumArtUri().toString());

                queueItems.add(
                        buildQueueItem(songItem.getIdString(),
                                songItem.getSongUri(),
                                songItem.getTitle(),
                                songItem.getArtist(),
                                songItem.getAlbum(),
                                extras,
                                queuePosition)
                );
                queuePosition++;
            }
        }
        return queueItems;
    }

    /**
     * Search in {@link MusicLibrary#albums} for {@link AlbumItem} name and builds a queue with the
     * songs in the album
     * @param query
     *              The query string parsed
     * @return
     *              The list of QueueItem with the songs in the album requested, null if the album
     *              does not exist
     */
    public List<MediaSessionCompat.QueueItem> getAlbumQueueFromQuery(String query) {
        String albumId = null;

        /* Search in albums */
        for (AlbumItem item : albums) {

            /* If the title of an album matches the query */
            if (query.equalsIgnoreCase(item.getName())) {

                /* Get the IdString of the album and stop the loop */
                albumId = item.getIdString();
                break;
            }
        }

        /* Delegate to getAlbumIdQueue building the queue, return null if no songs are available */
        return (albumId != null) ? getAlbumIdQueue(albumId) : null;
    }

    /**
     * Search in {@link MusicLibrary#artists} for {@link ArtistItem} name and builds a queue with
     * the songs of the same artist
     * @param query
     *              The query string parsed
     * @return
     *              The list of QueueItems with the songs that share the same artist, null if the
     *              artist does not exist
     */
    public List<MediaSessionCompat.QueueItem> getArtistQueueFromQuery(String query) {
        String artistId = null;

        /* Search in the artists */
        for (ArtistItem item : artists) {

            /* If the name of the artist matches query */
            if (query.equalsIgnoreCase(item.getName())) {

                /* Get the IdString of the artist and stop the loop */
                artistId = item.getIdString();
                break;
            }
        }

        /*
        Delegate to getArtistIdQueue building the queue, return null if no songs are available.
        Set the flag of the method to false because there is no need to edit the queue
        */
        return (artistId != null) ? getArtistIdQueue(artistId, null) : null;
    }

    /**
     * Search in {@link MusicLibrary#songs} for {@link SongItem} title and builds a queue with the
     * song queried in the first position and the songs of the same artist in the following positions
     * @param query t
     *              The query string parsed
     * @return
     *              The list of QueueItems with the song queried and the songs from the same artist,
     *              null if the song does not exist
     */
    @SuppressLint("DefaultLocale")
    public List<MediaSessionCompat.QueueItem> getSongsQueueFromQuery(String query) {

        SongItem songResult = null;

        /* Search in the songs */
        for (SongItem songItem : songs) {

            /* If the title matches the query */
            if (query.equalsIgnoreCase(songItem.getTitle())) {

                /* Get the SongItem that represent the song requested */
                songResult = songItem;
                break;
            }
        }

        /*
        Instead of returning a single item in the queue we add to the queue the songs of the same
        artist. The queue will have in first position the song asked followed by all the songs
        of the same artist, if any. The list must be created skipping the song queried to avoid
        issues in the queue screen of Android Auto when selecting a song to play and duplicates
        */
        if (songResult != null) {

            /* Get the queue for the artist setting the flag to skip the song we will add later */
            List<MediaSessionCompat.QueueItem> resultQueue =
                    getArtistIdQueue(String.format("%s%d", ARTIST_, songResult.getArtistId()), songResult.getIdString());

            /* Build the QueueItem for songResult */
            Bundle extras = new Bundle();
            extras.putLong(DURATION_ARGS_EXTRA, songResult.getDuration());
            extras.putString(ALBUM_ART_URI_ARGS_EXTRA, songResult.getAlbumArtUri().toString());

            /* Add the songItem in the first position of the list*/
            resultQueue.add(0,
                    buildQueueItem(songResult.getIdString(),
                            songResult.getSongUri(),
                            songResult.getTitle(),
                            songResult.getArtist(),
                            songResult.getAlbum(),
                            extras,
                            0
                    ));

            return resultQueue;
        }

        /* No matches available */
        return null;
    }

    /**
     * Creates a List containing the queue items that matches a common artist, it also used to create
     * a custom queue for a song query. If {@param songIdToSkip} is set to null the default behaviour
     * is applied and a queue with all the songs of an artist is created.
     * If is not null, set the initial position to be 1 because {@link MusicLibrary#getSongsQueueFromQuery(String)}
     * will add the search result in the first position, it is also necessary to skip
     * {@param songIdToSkip} to avoid setting the wrong position in the QueueItem and causing the queue
     * to have wrong ids and presenting unexpected behaviours
     *
     * @param artistId
     *                 The artistId string as <artist_id> (es "artist_1")
     * @param songIdToSkip
     *                    A string representing the song item to skip when building the queue.
     *                    If is null nothing is done. If is no null then the first item is assigned to
     *                    position 1 and in the loop when the current songItem matches {@param songIdToSkip}
     *                    is skipped and the current position not updated to avoid having duplicates
     *                    when later {@link MusicLibrary#getSongsQueueFromQuery(String)} will add
     *                    the query result to the first position
     * @return
     *              The list of QueueItems with all the songs with artistId as artist
     */
    /* Suppress because it is not a user visible string, no need to format to "DefaultLocale" */
    @SuppressLint("DefaultLocale")
    public List<MediaSessionCompat.QueueItem> getArtistIdQueue(String artistId, String songIdToSkip) {
        List<MediaSessionCompat.QueueItem> queueItems = new ArrayList<>();
        int queuePosition = 0;

        /* If not null start counting from 1 */
        if (songIdToSkip != null)
            queuePosition = 1;
        for (SongItem songItem : songs) {

            /* If the song item has the same stringId as the one asked to skip, continue */
            if (songItem.getIdString().equals(songIdToSkip))
                continue;
            /*
            Check if the song artist matches the same artistId building the artistId string from the
            songItem as "artist_" + artistId in the songItem
             */
            if (artistId.equals(String.format("%s%d", ARTIST_, songItem.getArtistId()))) {
                Bundle extras = new Bundle();

                /* Add extra data as DURATION and ALBUM_URI */
                extras.putLong(DURATION_ARGS_EXTRA, songItem.getDuration());
                extras.putString(ALBUM_ART_URI_ARGS_EXTRA, songItem.getAlbumArtUri().toString());

                queueItems.add(
                        buildQueueItem(songItem.getIdString(),
                                songItem.getSongUri(),
                                songItem.getTitle(),
                                songItem.getArtist(),
                                songItem.getAlbum(),
                                extras,
                                queuePosition)
                );
                queuePosition++;
            }
        }
        return queueItems;
    }

    /**
     * Creates a custom MediaItem. Builds a MediaItem that can be used by the session. This method
     * handles correctly only the items with {@value MusicLibrary#FLAG_BROWSABLE} or
     * {@value MusicLibrary#FLAG_PLAYLIST}
     *
     * @param id
     *           The id to assign to the item ( as seen before built as "<name>_id")
     * @param title
     *              The title to give to the item
     * @param subtitle
     *                 The text to show under the title, currently represent the number of items in
     *                 {@value MusicLibrary#ALBUMS}, {@value MusicLibrary#ARTISTS} and
     *                 {@value MusicLibrary#SONGS}
     * @param iconUri
     *                The icon Uri for the icon ( it is used for {@value MusicLibrary#ALBUMS},
     *                {@value MusicLibrary#ARTISTS} and {@value MusicLibrary#SONGS}
     * @param flag
     *             The flags to use to build the MediaItem as shown in the top of the class it is
     *             {@value MusicLibrary#FLAG_PLAYLIST} or {@value MusicLibrary#FLAG_BROWSABLE}
     * @return
     *              The MediaItem built from the data given
     */
    private MediaBrowserCompat.MediaItem generateBrowsableOrPlaylistItem(String id, String title, String subtitle, Uri iconUri, int flag) {

        if(flag != FLAG_PLAYLIST && flag != FLAG_BROWSABLE)
            return null;
        MediaDescriptionCompat.Builder mediaDescriptionBuilder = new MediaDescriptionCompat.Builder();

        /* The subtitle represents the number of item in a browsable or playlist item */
        mediaDescriptionBuilder.setMediaId(id)
                .setTitle(title)
                .setSubtitle(subtitle);

        /* If is not null assign the uri to the MediaItem */
        if (iconUri != null)
            mediaDescriptionBuilder.setIconUri(iconUri);

        return new MediaBrowserCompat.MediaItem(mediaDescriptionBuilder.build(), flag);
    }
}
