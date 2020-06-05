package com.alebr.muzic;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class QueueFragment extends Fragment{

    public static final String QUEUE_FRAGMENT_TAG = "queue_fragment";

    private static final String TAG = "QueueFragment";

    public static final String IS_MAIN_ACTIVITY_ARGS_EXTRA = "is_main_activity";

    private boolean is_main_activity = true;

    private RecyclerView mRecyclerView;
    private ExampleAdapter exampleAdapter;

    private QueueListener mQueueListener;
    private ConstraintLayout noQueueLayout;

    private int previousItem = 0;

    public interface QueueListener extends MediaBrowserProvider{
        void onQueueItemClicked(String stringId, long id);
    }

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
        mRecyclerView.setHasFixedSize(true);
        exampleAdapter = new ExampleAdapter( new ArrayList<CustomListItem>());

        mRecyclerView.setLayoutManager( new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(exampleAdapter);

        exampleAdapter.setOnItemClickListener(new ExampleAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                mQueueListener.onQueueItemClicked(exampleAdapter.getItem(position).getId(), position);
            }
        });

        noQueueLayout = view.findViewById(R.id.empty_queue);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        if(getArguments() != null){
            is_main_activity = getArguments().getBoolean(IS_MAIN_ACTIVITY_ARGS_EXTRA);
        }

        MediaBrowserCompat mediaBrowser = null;
        if(is_main_activity){
            mediaBrowser = ((MainActivity)getActivity()).getMediaBrowser();
        }else{
            mediaBrowser = ((FullPlayerActivity)getActivity()).getMediaBrowser();
        }
        if(mediaBrowser != null && mediaBrowser.isConnected()){
            onConnected();
        }
    }

    //Suppress because it is just a concatenation of the position of the item + its name
    @SuppressLint("DefaultLocale")
    public void onConnected(){

        List<MediaSessionCompat.QueueItem> queueItems = MediaControllerCompat.getMediaController(getActivity()).getQueue();
        if(queueItems == null ||queueItems.size() == 0) {
            noQueueLayout.setVisibility(View.VISIBLE);
        }
        else {
            for (MediaSessionCompat.QueueItem queueItem : queueItems) {
                exampleAdapter.add(
                        new CustomListItem(
                                queueItem.getDescription().getMediaId(),
                                String.format("%d   %s", queueItem.getQueueId()+1,queueItem.getDescription().getTitle().toString())));
            }
            exampleAdapter.notifyDataSetChanged();

            MediaMetadataCompat metadata = MediaControllerCompat.getMediaController(getActivity()).getMetadata();
            previousItem = (int) metadata.getBundle().getLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, 0);

            exampleAdapter.getItem( previousItem).changeImage(R.drawable.ic_audiotrack);
            exampleAdapter.notifyItemChanged( previousItem);
            MediaControllerCompat.getMediaController(getActivity()).registerCallback(mControllerCallback);
        }
    }

    MediaControllerCompat.Callback mControllerCallback = new MediaControllerCompat.Callback() {

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
            int currentItem = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS);
            if(currentItem != previousItem){

                //Remove the icon on the previous item
                exampleAdapter.getItem( previousItem).changeImage(0);

                //Add the icon on the current item
                exampleAdapter.getItem( currentItem).changeImage(R.drawable.ic_audiotrack);

                //Notify the adapter about the two views updated
                exampleAdapter.notifyItemChanged(previousItem);
                exampleAdapter.notifyItemChanged(currentItem);

                previousItem = currentItem;
            }
        }
    };

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof QueueListener){
            mQueueListener = (QueueListener) context;
        }else {
            throw new RuntimeException(context.toString() + " must implement QueueListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mQueueListener = null;
        MediaControllerCompat.getMediaController(getActivity()).unregisterCallback(mControllerCallback);
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}
