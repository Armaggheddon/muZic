package com.armaggheddon.muzic.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.armaggheddon.muzic.R;
import com.armaggheddon.muzic.library.MusicLibrary;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Fragment implementation that displays a RecyclerView using {@link RecyclerViewAdapter} and using
 * the layout described in {@link R.layout#search_fragment_layout}
 */

public class SearchFragment extends Fragment{

    /* Extra arguments string key */
    public static final String SEARCH_FRAGMENT_TAG = "search_fragment";

    /* The listener to allow the caller to handle onClick events on the fragment layout */
    private FragmentListListener mFragmentListener;

    /*
    Adapters for the recycler views for Albums, Artists and Songs
    */

    private SearchAdapter albumAdapter;
    private SearchAdapter artistAdapter;
    private SearchAdapter songAdapter;

    /* Search bar edit text */
    private TextInputEditText searchBar;

    /* Enum to randomly pick a search suggestion from Album, Artists or Songs */
    private enum HintFrom {
        Album,
        Artist,
        Song
    }
    private HintFrom hintFrom;

    /*
    Extending MediaBrowserProvider allows to ask for a MediaBrowser object to the class that
    implements it (MainActivity in this case)
    */
    public interface FragmentListListener extends MediaBrowserProvider {

        /**
         * Called when the user clicks on an item in the search view, used for Songs
         * @param mediaId
         *          The mediaId string representing unique id of the item clicked.
         * @param position
         *          The absolute position of the item clicked to allow the correct playback.
         */
        void onSongSearchItemClicked(String mediaId, long position);

        /**
         * Called when an Album or an Artist is clicked
         * @param mediaId
         *          The mediaId string representing the unique id of the item clicked.
         */
        void onAlbumArtistSongItemClicked(String mediaId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.search_fragment_layout, container, false);

        /* Recycler view adapters for Albums, Artists, Songs*/
        final RecyclerView albumRecyclerView = view.findViewById(R.id.album_search);
        final RecyclerView artistRecyclerView = view.findViewById(R.id.artist_search);
        final RecyclerView songRecyclerView = view.findViewById(R.id.song_search);

        /* Assign the adapters*/
        albumAdapter = new SearchAdapter(new ArrayList<CustomSearchItem>());
        artistAdapter = new SearchAdapter( new ArrayList<CustomSearchItem>());
        songAdapter = new SearchAdapter( new ArrayList<CustomSearchItem>());

        /* Create the Layout manager in horizontal */
        LinearLayoutManager albumLm = new LinearLayoutManager(getContext());
        LinearLayoutManager artistLm = new LinearLayoutManager(getContext());
        LinearLayoutManager songLm = new LinearLayoutManager(getContext());
        albumLm.setOrientation(LinearLayoutManager.HORIZONTAL);
        artistLm.setOrientation(LinearLayoutManager.HORIZONTAL);
        songLm.setOrientation(LinearLayoutManager.HORIZONTAL);
        albumRecyclerView.setLayoutManager( albumLm);
        artistRecyclerView.setLayoutManager( artistLm);
        songRecyclerView.setLayoutManager( songLm);

        /* Add the decorator to space the items */
        albumRecyclerView.addItemDecoration( new MarginItemDecorator((int) getResources().getDimension(R.dimen.text_margin)));
        artistRecyclerView.addItemDecoration( new MarginItemDecorator((int) getResources().getDimension(R.dimen.text_margin)));
        songRecyclerView.addItemDecoration( new MarginItemDecorator((int) getResources().getDimension(R.dimen.text_margin)));

        /* Assign the adapters */
        albumRecyclerView.setAdapter(albumAdapter);
        artistRecyclerView.setAdapter(artistAdapter);
        songRecyclerView.setAdapter(songAdapter);

        /* Set the onClickListeners for the items */
        albumAdapter.setOnItemClickListener(new SearchAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                mFragmentListener.onAlbumArtistSongItemClicked(albumAdapter.getItem(position).getId());
            }
        });

        artistAdapter.setOnItemClickListener(new SearchAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                mFragmentListener.onAlbumArtistSongItemClicked(artistAdapter.getItem(position).getId());
            }
        });

        songAdapter.setOnItemClickListener(new SearchAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                mFragmentListener.onSongSearchItemClicked(
                        songAdapter.getItem(position).getId(),
                        songAdapter.getItem(position).getElementPosition());
            }
        });


        /* Get a reference to the search edit text */
        searchBar = view.findViewById(R.id.search_input);

        /* When the text changes */
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                /* Filter the recycler views based on what the user has typed */
                albumAdapter.getFilter().filter(s);
                artistAdapter.getFilter().filter(s);
                songAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        return view;
    }


    @Override
    public void onStart() {
        super.onStart();

        MediaBrowserCompat mediaBrowser = ((MainActivity)getActivity()).getMediaBrowser();

        /*
        If mediaBrowser is not null and is connected.
        Check for null before because calling isConnected on a null reference causes the
        application to crash
        */
        if(mediaBrowser.isConnected()){
            onConnected();
        }
    }


    /**
     * Subscribes to "subscribeTo" and sets the callback for the items loaded
     */
    public void onConnected(){

        /* Chose from what pick the search hint randomly */
        switch ( new Random().nextInt(3)){
            case 0:
                hintFrom = HintFrom.Album;
                break;
            case 1:
                hintFrom = HintFrom.Artist;
                break;
            case 2:
                hintFrom = HintFrom.Song;
                break;
        }

        /* Subscribe to the corresponding media */
        mFragmentListener.getMediaBrowser().subscribe(MusicLibrary.ALBUMS, mAlbumsSubscriptionCallback);
        mFragmentListener.getMediaBrowser().subscribe(MusicLibrary.ARTISTS, mArtistsSubscriptionCallback);
        mFragmentListener.getMediaBrowser().subscribe(MusicLibrary.SONGS, mSongsSubscriptionCallback);

    }


    private final MediaBrowserCompat.SubscriptionCallback mAlbumsSubscriptionCallback = new MediaBrowserCompat.SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children) {
            super.onChildrenLoaded(parentId, children);

            /* Get a random album title from the elements and set the hint text accordingly */
            if(hintFrom == HintFrom.Album){
                searchBar.setHint("Try searching for \"" +
                        (children.get(new Random().nextInt(children.size()))).getDescription().getTitle().toString()
                        + "\"");
            }

            /* For every item in children create a new CustomList instance*/
            for (MediaBrowserCompat.MediaItem item : children) {

                Uri image = item.getDescription().getIconUri();
                albumAdapter.add(new CustomSearchItem(
                        item.getMediaId(),
                        item.getDescription().getTitle().toString(),
                        image,
                        false));
            }

            /* When all the data is loaded notify the adapter about the changes */
            albumAdapter.notifyDataSetChanged();

            /* We can now unsubscribe to receive future updates since we just need to load the data once */
            mFragmentListener.getMediaBrowser().unsubscribe(MusicLibrary.ALBUMS, mAlbumsSubscriptionCallback);
        }
    };

    private final MediaBrowserCompat.SubscriptionCallback mArtistsSubscriptionCallback = new MediaBrowserCompat.SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children) {
            super.onChildrenLoaded(parentId, children);

            if(hintFrom == HintFrom.Artist){
                searchBar.setHint("Try searching for \"" +
                        (children.get(new Random().nextInt(children.size()))).getDescription().getTitle().toString()
                        + "\"");
            }

            /* For every item in children create a new CustomList instance*/
            for (MediaBrowserCompat.MediaItem item : children) {

                Uri image = item.getDescription().getIconUri();
                artistAdapter.add(new CustomSearchItem(
                        item.getMediaId(),
                        item.getDescription().getTitle().toString(),
                        image,
                        false));
            }

            /* When all the data is loaded notify the adapter about the changes */
            artistAdapter.notifyDataSetChanged();

            /* We can now unsubscribe to receive future updates since we just need to load the data once */
            mFragmentListener.getMediaBrowser().unsubscribe(MusicLibrary.ARTISTS, mArtistsSubscriptionCallback);
        }
    };

    private final MediaBrowserCompat.SubscriptionCallback mSongsSubscriptionCallback = new MediaBrowserCompat.SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children) {
            super.onChildrenLoaded(parentId, children);

            if(hintFrom == HintFrom.Song){
                searchBar.setHint("Try searching for \"" +
                        (children.get(new Random().nextInt(children.size()))).getDescription().getTitle().toString()
                + "\"");

            }

            /* For every item in children create a new CustomList instance*/
            for (MediaBrowserCompat.MediaItem item : children) {

                Uri image = item.getDescription().getIconUri();
                songAdapter.add(new CustomSearchItem(
                        item.getMediaId(),
                        item.getDescription().getTitle().toString(),
                        image,
                        true));
            }

            /* When all the data is loaded notify the adapter about the changes */
            songAdapter.notifyDataSetChanged();

            /* We can now unsubscribe to receive future updates since we just need to load the data once */
            mFragmentListener.getMediaBrowser().unsubscribe(MusicLibrary.SONGS, mSongsSubscriptionCallback);
        }
    };

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        /* If the Activity using ListFragment does not implement the interface throw an error */
        if(context instanceof FragmentListListener){
            mFragmentListener = (FragmentListListener) context;
        }else {
            throw new RuntimeException(context.toString() + " must implement FragmentListListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        /* Remove the listener, no need to listen for updates the fragment has been removed from the parent */
        mFragmentListener = null;
    }

    @Override
    public void onStop() {
        super.onStop();
        /*
        If the call to unsubscribe in the callback is interrupted for any reason
        unsubscribe here to stop receiving updates
        */

        MediaBrowserCompat mediaBrowser = mFragmentListener.getMediaBrowser();
        if (mediaBrowser != null && mediaBrowser.isConnected()) {
            mediaBrowser.unsubscribe(MusicLibrary.ARTISTS);
            mediaBrowser.unsubscribe(MusicLibrary.ALBUMS);
            mediaBrowser.unsubscribe(MusicLibrary.SONGS);
        }
    }
}
