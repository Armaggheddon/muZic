package com.alebr.muzic;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ListFragment extends Fragment{

    public static final String ALBUM_FRAGMENT_TAG = "album_fragment";
    public static final String ARTIST_FRAGMENT_TAG = "artist_fragment";
    public static final String SONG_FRAGMENT_TAG = "song_fragment";

    private static final String SUBSCRIPTION_ARGS_EXTRA = "sub_extra";

    private FragmentListListener mListener;
    private String subscribeTo;

    private RecyclerView mRecyclerView;
    private ExampleAdapter exampleAdapter;


    public interface FragmentListListener extends MediaBrowserProvider{
        void onBrowsableItemClicked(String caller, String mediaId, long id);
        void onPlayableItemClicked(String caller, String mediaId, long id);
        void setToolbarTitle(String title);
        //void getMediaBrowser();
    }

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

        mRecyclerView = view.findViewById(R.id.recyclerView);
        mRecyclerView.setHasFixedSize(true);
        exampleAdapter = new ExampleAdapter(new ArrayList<CustomListItem>());

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(exampleAdapter);

        exampleAdapter.setOnItemClickListener(new ExampleAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                String mediaId = exampleAdapter.getItem(position).getId();
                if(mediaId.contains(MusicLibrary.SONG_)){
                    mListener.onPlayableItemClicked(subscribeTo, mediaId, position);
                }else {
                    mListener.onBrowsableItemClicked(subscribeTo, mediaId, position);
                    mListener.setToolbarTitle(exampleAdapter.getItem(position).getTitle());
                }
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        if(getArguments() != null){
            subscribeTo = getArguments().getString(SUBSCRIPTION_ARGS_EXTRA);
        }

        MediaBrowserCompat mediaBrowser = ((MainActivity)getActivity()).getMediaBrowser();
        if(mediaBrowser.isConnected()){
            onConnected();
        }
    }

    public void onConnected(){

        mListener.getMediaBrowser().subscribe(subscribeTo, mSubscriptionCallback);

    }

    private final MediaBrowserCompat.SubscriptionCallback mSubscriptionCallback = new MediaBrowserCompat.SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children) {
            super.onChildrenLoaded(parentId, children);

            for(MediaBrowserCompat.MediaItem item : children){
                exampleAdapter.add(new CustomListItem(
                        item.getMediaId(),
                        item.getDescription().getTitle().toString()));
            }
            exampleAdapter.notifyDataSetChanged();

            mListener.getMediaBrowser().unsubscribe(subscribeTo, mSubscriptionCallback);
        }
    };

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof FragmentListListener){
            mListener = (FragmentListListener) context;
        }else {
            throw new RuntimeException(context.toString() + " must implement FragmentListListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onStop() {
        super.onStop();

        MediaBrowserCompat mediaBrowser = mListener.getMediaBrowser();
        if(mediaBrowser != null && mediaBrowser.isConnected()){
            mediaBrowser.unsubscribe(subscribeTo);
        }
    }
}
