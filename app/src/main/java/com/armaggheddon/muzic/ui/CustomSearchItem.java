package com.armaggheddon.muzic.ui;

import android.net.Uri;

import com.armaggheddon.muzic.library.AlbumItem;
import com.armaggheddon.muzic.library.ArtistItem;
import com.armaggheddon.muzic.library.SongItem;

/**
 * Holds the information to show in the item in the RecyclerView in {@link ListFragment}
 * and {@link QueueFragment}
 */

class CustomSearchItem {

    private String id;
    private String title;
    private Uri art;
    private int elementPosition;

    private static int position = 0;


    /**
     * Constructor of the item
     * @param id
     *          The id that represent the unique id build in {@link SongItem}
     * @param title
     *          The text to show in the view, it can be {@link SongItem} title, {@link ArtistItem}
     *          name or {@link AlbumItem} name
     */

    public CustomSearchItem(String id, String title, Uri art, boolean usePosition) {
        this.id = id;
        this.title = title;
        this.art = art;

        if(usePosition)
            elementPosition = position++;
    }


    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Uri getArt(){return art;}

    public int getElementPosition(){return elementPosition;}
}
