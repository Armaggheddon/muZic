package com.armaggheddon.muzic.ui;

import com.armaggheddon.muzic.R;
import com.armaggheddon.muzic.library.AlbumItem;
import com.armaggheddon.muzic.library.ArtistItem;
import com.armaggheddon.muzic.library.SongItem;

/**
 * Holds the information to show in the item in the RecyclerView in {@link ListFragment}
 * and {@link QueueFragment}
 */

class CustomListItem {

    private String id;
    private String title;
    private int imageRes;

    /**
     * Constructor of the item
     * @param id
     *          The id that represent the unique id build in {@link SongItem}
     * @param title
     *          The text to show in the view, it can be {@link SongItem} title, {@link ArtistItem}
     *          name or {@link AlbumItem} name
     */

    public CustomListItem(String id, String title) {
        this.id = id;
        this.title = title;
    }

    /**
     * Allows to change, and therefore set the image resource
     * @param imageRes
     *          The int resource of the image, as of this implementation is always
     *          {@value R.drawable#ic_audiotrack}
     */
    public void changeImage(int imageRes){
        this.imageRes = imageRes;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getImageRes(){return imageRes;}
}
