package com.armaggheddon.muzic.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.armaggheddon.muzic.R;
import com.armaggheddon.muzic.library.AlbumItem;
import com.armaggheddon.muzic.library.ArtistItem;
import com.armaggheddon.muzic.library.MusicLibrary;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment implementation that displays a RecyclerView using {@link RecyclerViewAdapter} and using
 * the layout described in {@link R.layout#fragment_list_layout}
 */

public class ListFragment extends Fragment{

    /* The TAGS assigned to FragmentManager to search for specific fragments */
    public static final String ALBUM_FRAGMENT_TAG = "album_fragment";
    public static final String ARTIST_FRAGMENT_TAG = "artist_fragment";
    public static final String SONG_FRAGMENT_TAG = "song_fragment";

    /* Extra arguments string key */
    private static final String SUBSCRIPTION_ARGS_EXTRA = "sub_extra";

    /* The listener to allow the caller to handle onClick events on the fragment layout */
    private FragmentListListener mFragmentListener;

    /*
    Represent the mediaId to subscribe to using the MediaBrowser instance given from MainActivity.
    Can be one of the following:
    -Albums both generic or a specific album
    -Artists both generic or a specific artist
    */
    private String subscribeTo;

    private RecyclerViewAdapter recyclerViewAdapter;

    /*
    Extending MediaBrowserProvider allows to ask for a MediaBrowser object to the class that
    implements it (MainActivity in this case)
    */
    public interface FragmentListListener extends MediaBrowserProvider {

        /**
         * Used when a browsable item is clicked in RecyclerView,
         * for example when an album that has children to display is clicked
         * @param mediaId
         *          The parentId that has been clicked representing a stringId as
         *          described in {@link ArtistItem} for artists and {@link AlbumItem} for albums
         */
        void onBrowsableItemClicked(String mediaId);

        /**
         * Used when a playable item is clicked in the RecyclerView,
         * a song
         * @param parentId
         *          The parentId string representing the parent of the items being currently
         *          displayed. For example for an Album A that has Song1 and Song2 the parentId
         *          is "Album A"
         * @param positionInQueue
         *          The position in the queue to skip to play the item clicked.
         *          Since the items showed are mapped 1:1 to the queue being created this value
         *          can be used to select the item clicked in the queue
         */
        void onPlayableItemClicked(String parentId, long positionInQueue);

        /**
         * Allows to get the title of the item clicked
         * @param title
         *          The title to set in the toolbar in {@link MainActivity} for navigation purposes
         */
        void setToolbarTitle(String title);
    }

    /**
     * Factory method to get a {@link ListFragment} given a mediaId
     * @param mediaId
     *          The string representing what the fragment should subscribe to in
     *          {@link ListFragment#mSubscriptionCallback} and what data will be shown to the user
     * @return
     *          Returns a fragment with the arguments set
     */
    public static ListFragment newInstance(String mediaId){
        ListFragment fragment = new ListFragment();
        Bundle args = new Bundle();
        args.putString(SUBSCRIPTION_ARGS_EXTRA, mediaId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_list_layout, container, false);

        RecyclerView mRecyclerView = view.findViewById(R.id.recyclerView);

        /* Setting hasFixedSize improves performance on the rendering of the view */
        mRecyclerView.setHasFixedSize(true);
        recyclerViewAdapter = new RecyclerViewAdapter(new ArrayList<CustomListItem>());
        recyclerViewAdapter.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(recyclerViewAdapter);

        recyclerViewAdapter.setOnItemClickListener(new RecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                String mediaId = recyclerViewAdapter.getItem(position).getId();
                /* If the mediaId of the item has "song_" than is a playable item */
                if(mediaId.contains(MusicLibrary.SONG_)){
                    mFragmentListener.onPlayableItemClicked(subscribeTo, position);
                }
                /* Else the item has "album_" or "artist_" so is a browsable item */
                else {
                    mFragmentListener.onBrowsableItemClicked(mediaId);
                    mFragmentListener.setToolbarTitle(recyclerViewAdapter.getItem(position).getTitle());
                }
            }
        });

        return view;
    }


    @Override
    public void onStart() {
        super.onStart();

        /* Get the arguments set on fragment creation */
        if(getArguments() != null){
            subscribeTo = getArguments().getString(SUBSCRIPTION_ARGS_EXTRA);
        }

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

            mFragmentListener.getMediaBrowser().subscribe(subscribeTo, mSubscriptionCallback);

    }


    private final MediaBrowserCompat.SubscriptionCallback mSubscriptionCallback = new MediaBrowserCompat.SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children) {
            super.onChildrenLoaded(parentId, children);

            /* For every item in children create a new CustomList instance*/
            for (MediaBrowserCompat.MediaItem item : children) {
                recyclerViewAdapter.add(new CustomListItem(
                        item.getMediaId(),
                        item.getDescription().getTitle().toString()));
            }

            /* When all the data is loaded notify the adapter about the changes */
            recyclerViewAdapter.notifyDataSetChanged();

            /* We can now unsubscribe to receive future updates since we just need to load the data once */
            mFragmentListener.getMediaBrowser().unsubscribe(subscribeTo, mSubscriptionCallback);
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
        if(mediaBrowser != null && mediaBrowser.isConnected()){
            mediaBrowser.unsubscribe(subscribeTo);
        }
    }
}
