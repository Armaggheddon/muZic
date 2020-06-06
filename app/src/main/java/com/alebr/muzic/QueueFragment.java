package com.alebr.muzic;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment implementation that displays a RecyclerView using {@link RecyclerViewAdapter} and using
 * the layout described in {@link R.layout#fragment_list_layout}.
 * If the queue is empty and there are no items to display the layout
 * {@link R.layout#no_queue_items} is shown
 */

public class QueueFragment extends Fragment{

    /* The TAG assigned to FragmentManager to search for specific fragments */
    public static final String QUEUE_FRAGMENT_TAG = "queue_fragment";

    /* Extra arguments string key*/
    public static final String IS_MAIN_ACTIVITY_ARGS_EXTRA = "is_main_activity";

    /*
    This fragment is used by MainActivity and FullPlayerActivity, this flag allows the fragment
    to know which is the activity to ask the MediaBrowser instance.
    The default value is true because the QueueFragment is always displayed in MainActivity
    */
    private boolean is_main_activity = true;

    private RecyclerViewAdapter recyclerViewAdapter;

    private QueueFragmentListener mQueueFragmentListener;
     /* The layout to show if no queue items are available, es empty queue*/
    private ConstraintLayout noQueueLayout;

    /* Allows to know which was the previously changed item in the RecyclerView to update the items */
    private int previousItem = 0;

    /*
    Extending MediaBrowserProvider allows to ask for a MediaBrowser object to the class that
    implements it (MainActivity and FullPlayerActivity in this case)
    */
    public interface QueueFragmentListener extends MediaBrowserProvider{
        /**
         * Allows to know what is the position of the item clicked
         * @param positionInQueue
         *          The position of the item clicked. Since the items are shown in the same order as
         *          they are in the playback queue the position also represents the id of the queue
         *          item to play
         */
        void onQueueItemClicked( long positionInQueue);
    }

    /**
     * Factory method to get a {@link ListFragment} given a mediaId
     * @param is_main_activity
     *          Is set to true if the activity calling the fragment is {@link MainActivity},
     *          false if is {@link FullPlayerActivity}. The current implementation uses a boolean
     *          flag since only 2 activities are calling this fragment. To add more activities is
     *          necessary to change {@link QueueFragment#is_main_activity}
     * @return
     *          Returns a fragment with the arguments set
     */
    public static QueueFragment newInstance(boolean is_main_activity){
        QueueFragment fragment = new QueueFragment();
        Bundle args = new Bundle();
        args.putBoolean(IS_MAIN_ACTIVITY_ARGS_EXTRA, is_main_activity);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_layout, container, false);

        RecyclerView mRecyclerView = view.findViewById(R.id.recyclerView);

        /*
        Setting hasFixedSize improves performance on the rendering of the view.
        Even on FullPlayerActivity where the RecyclerView is being animated, its size does not
        changes
        */
        mRecyclerView.setHasFixedSize(true);
        recyclerViewAdapter = new RecyclerViewAdapter( new ArrayList<CustomListItem>());

        mRecyclerView.setLayoutManager( new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(recyclerViewAdapter);

        recyclerViewAdapter.setOnItemClickListener(new RecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                mQueueFragmentListener.onQueueItemClicked(position);
            }
        });

        noQueueLayout = view.findViewById(R.id.empty_queue);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        /* Get the arguments set on fragment creation */
        if(getArguments() != null){
            is_main_activity = getArguments().getBoolean(IS_MAIN_ACTIVITY_ARGS_EXTRA);
        }

        MediaBrowserCompat mediaBrowser = null;
        /* Get the MediaBrowser based on the activity that started the fragment */
        if(is_main_activity){
            mediaBrowser = ((MainActivity)getActivity()).getMediaBrowser();
        }else{
            mediaBrowser = ((FullPlayerActivity)getActivity()).getMediaBrowser();
        }

        /*
        If mediaBrowser is not null and is connected.
        Check for null before because calling isConnected on a null reference causes the
        application to crash
        */
        if(mediaBrowser != null && mediaBrowser.isConnected()){
            onConnected();
        }
    }

    /**
     * Initializes {@link QueueFragment#recyclerViewAdapter} with the items in the playback queue
     */
    /* Suppress because the text created does not depends on "DefaultLocale" */
    @SuppressLint("DefaultLocale")
    public void onConnected(){

        /* Get the items in the queue */
        List<MediaSessionCompat.QueueItem> queueItems =
                MediaControllerCompat.getMediaController(getActivity()).getQueue();

        /* If the queue has no items show the appropriate layout */
        if(queueItems == null ||queueItems.size() == 0) {
            noQueueLayout.setVisibility(View.VISIBLE);
        }
        else {

            /* For every QueueItem in queueItems add it to the adapter */
            for (MediaSessionCompat.QueueItem queueItem : queueItems) {

                /* Change the title adding the item position as "1   Song" (3 spaces) */
                recyclerViewAdapter.add(
                        new CustomListItem(
                                queueItem.getDescription().getMediaId(),
                                String.format("%d   %s",
                                        queueItem.getQueueId()+1,
                                        queueItem.getDescription().getTitle().toString())));
            }
            /* When all the data is loaded notify the adapter about the changes */
            recyclerViewAdapter.notifyDataSetChanged();

            /* Load the metadata of the current item being played */
            MediaMetadataCompat metadata = MediaControllerCompat.getMediaController(getActivity()).getMetadata();

            /* Get the item position in the queue */
            previousItem = (int) metadata.getBundle().getLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, 0);

            /* Add a small icon telling the item being currently played */
            recyclerViewAdapter.getItem( previousItem).changeImage(R.drawable.ic_audiotrack);

            /* Update only the view changed */
            recyclerViewAdapter.notifyItemChanged( previousItem);

            /* Register a callback to know when the song being currently played changes */
            MediaControllerCompat.getMediaController(getActivity()).registerCallback(mControllerCallback);
        }
    }

    MediaControllerCompat.Callback mControllerCallback = new MediaControllerCompat.Callback() {

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
            int currentItem = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS);

            /* If the current item is different from the previous one*/
            if(currentItem != previousItem){

                /* Remove the icon on the previous item view */
                recyclerViewAdapter.getItem( previousItem).changeImage(0);

                /* Add the icon on the item that being played */
                recyclerViewAdapter.getItem( currentItem).changeImage(R.drawable.ic_audiotrack);

                /* Notify the adapter to update the views */
                recyclerViewAdapter.notifyItemChanged(previousItem);
                recyclerViewAdapter.notifyItemChanged(currentItem);

                /* Update the previousItem value*/
                previousItem = currentItem;
            }
        }
    };

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        /* If the Activity using ListFragment does not implement the interface throw an error */
        if(context instanceof QueueFragmentListener){
            mQueueFragmentListener = (QueueFragmentListener) context;
        }else {
            throw new RuntimeException(context.toString() + " must implement QueueFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        /* If it is not yet removed remove the listener here as last resource */
        if(mQueueFragmentListener != null)
            mQueueFragmentListener = null;
    }

    @Override
    public void onStop() {
        super.onStop();

        /* Remove the listener, no need to listen for updates the fragment has been removed from the parent */
        mQueueFragmentListener = null;

        /* Unregister the callback to stop receiving updates from MediaControllerCompat */
        MediaControllerCompat.getMediaController(getActivity()).unregisterCallback(mControllerCallback);
    }
}
