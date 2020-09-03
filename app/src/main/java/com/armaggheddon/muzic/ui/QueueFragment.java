package com.armaggheddon.muzic.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.armaggheddon.muzic.R;
import com.armaggheddon.muzic.library.MusicLibrary;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    /* Set as a global variable so when the data is loaded is possible to scroll to the right position */
    private RecyclerView mRecyclerView;

    private MaterialButton mRandomQueueButton;

    /* The layout to show if no queue items are available, es empty queue*/
    private ConstraintLayout noQueueLayout;

    /* Allows to know which was the previously changed item in the RecyclerView to update the items */
    private int previousItem = 0;

    private QueueFragmentListener mQueueFragmentListener;

    /*
    Extending MediaBrowserProvider allows to ask for a MediaBrowser object to the class that
    implements it (MainActivity and FullPlayerActivity in this case)
    */
    public interface QueueFragmentListener extends MediaBrowserProvider {
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

        mRecyclerView = view.findViewById(R.id.recyclerView);

        /*
        Setting hasFixedSize improves performance on the rendering of the view.
        Even on FullPlayerActivity where the RecyclerView is being animated, its size does not
        changes
        */
        mRecyclerView.setHasFixedSize(true);
        recyclerViewAdapter = new RecyclerViewAdapter( new ArrayList<CustomListItem>());

        /* Restore the recycler view position only when the data has been loaded */
        recyclerViewAdapter.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY);

        mRecyclerView.setLayoutManager( new LinearLayoutManager(getContext()));

        mRecyclerView.addItemDecoration( new MarginItemDecorator((int) getResources().getDimension( R.dimen.text_margin)));
        mRecyclerView.addItemDecoration( new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        mRecyclerView.setAdapter(recyclerViewAdapter);

        recyclerViewAdapter.setOnItemClickListener(new RecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                mQueueFragmentListener.onQueueItemClicked(position);
            }

            @Override
            public void onItemLongClick(int position) {
                /*
                Not used for queue since all items are only playable items and no extra data is
                currently available
                */
            }
        });

        noQueueLayout = view.findViewById(R.id.empty_queue);

        mRandomQueueButton = view.findViewById(R.id.button_queue_random);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        /* Get the arguments set on fragment creation */
        if(getArguments() != null){
            is_main_activity = getArguments().getBoolean(IS_MAIN_ACTIVITY_ARGS_EXTRA);
        }

        MediaBrowserCompat mediaBrowser;
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

                String artUri = queueItem.getDescription().getExtras().getString(MusicLibrary.ALBUM_ART_URI_ARGS_EXTRA);

                /* Change the title adding the item position as "1   Song" (3 spaces) */
                recyclerViewAdapter.add(
                        new CustomListItem(
                                queueItem.getDescription().getMediaId(),
                                String.format("%d   %s",
                                        queueItem.getQueueId()+1,
                                        queueItem.getDescription().getTitle().toString()), Uri.parse(artUri)));
            }
            /* When all the data is loaded notify the adapter about the changes */
            recyclerViewAdapter.notifyDataSetChanged();

            mRandomQueueButton.setVisibility(View.VISIBLE);
            mRandomQueueButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int size = recyclerViewAdapter.getItemCount();
                    MediaControllerCompat.getMediaController(getActivity())
                            .getTransportControls().skipToQueueItem( new Random().nextInt(size));
                }
            });

            /* Load the metadata of the current item being played */
            //MediaMetadataCompat metadata = MediaControllerCompat.getMediaController(getActivity()).getMetadata();

            /* Get the active item position in the queue */
            previousItem = (int) MediaControllerCompat.getMediaController(getActivity()).getPlaybackState().getActiveQueueItemId();

            /* Add a small icon telling the item being currently played */
            recyclerViewAdapter.getItem( previousItem).changeImage(R.drawable.ic_audiotrack);

            /* Update only the view changed */
            recyclerViewAdapter.notifyItemChanged( previousItem);

            /*
            If the items in the recycler view are more than 5, we scroll the recycler view to the
            position of the item that is currently being played so the user doesn't have to search
            for the active item in the queue.
            This behaviour is applied only when the fragment is opened for the first time.
            It is possible to implement a smooth scroll behaviour but is only for aesthetics purposes
            */
            if( recyclerViewAdapter.getItemCount() > 5)
                mRecyclerView.scrollToPosition(previousItem);

            /* Register a callback to know when the song being currently played changes */
            MediaControllerCompat.getMediaController(getActivity()).registerCallback(mControllerCallback);
        }
    }

    MediaControllerCompat.Callback mControllerCallback = new MediaControllerCompat.Callback() {

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);

            /* Get the active item in the queue */
            int currentItem = (int) state.getActiveQueueItemId();

            updateRecyclerViewPosition(currentItem);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);

            /* Get the active item in the queue */
            int currentItem = (int) MediaControllerCompat.getMediaController(getActivity()).getPlaybackState().getActiveQueueItemId();

            updateRecyclerViewPosition(currentItem);

        }
    };

    /**
     * Updates the recycler view icon {@link R.drawable#ic_audiotrack} showing the icon on the
     * current active item, if the position passed is the same as {@link QueueFragment#previousItem}
     * does nothing
     * @param currentItemPosition
     *                            The current item in the queue that is in the play state
     */
    private void updateRecyclerViewPosition( int currentItemPosition){

        /* If the current item is different from the previous one*/
        if (currentItemPosition != previousItem) {

            /* Remove the icon on the previous item view */
            recyclerViewAdapter.getItem(previousItem).changeImage(0);

            /* Add the icon on the item that being played */
            recyclerViewAdapter.getItem(currentItemPosition).changeImage(R.drawable.ic_audiotrack);

            /* Notify the adapter to update the views */
            recyclerViewAdapter.notifyItemChanged(previousItem);
            recyclerViewAdapter.notifyItemChanged(currentItemPosition);

            /* Update the previousItem value*/
            previousItem = currentItemPosition;
        }
    }

    private boolean scroll_to_position = false;

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

        /* Remove the listener from the fragment */
        if(mQueueFragmentListener != null)
            mQueueFragmentListener = null;

        /* Unregister the callback to stop receiving updates from MediaControllerCompat */
        MediaControllerCompat.getMediaController(getActivity()).unregisterCallback(mControllerCallback);
    }
}
