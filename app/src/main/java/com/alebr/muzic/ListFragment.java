package com.alebr.muzic;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class ListFragment extends Fragment{

    private static final String SUBSCRIPTION_ARGS_EXTRA = "sub_extra";

    private ListView mListView;
    private FragmentListListener mListener;
    private BrowserAdapter mAdapter;
    private String subscribeTo;

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
        mListView = view.findViewById(R.id.listView);

        mAdapter = new BrowserAdapter(getActivity());

        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String stringId = mAdapter.getItem(position).getId();
                if(stringId.contains(MusicLibrary.SONG_)){
                    mListener.onPlayableItemClicked(subscribeTo, stringId, id);
                }else {
                    mListener.onBrowsableItemClicked(subscribeTo, mAdapter.getItem(position).getId(), id);
                    mListener.setToolbarTitle(mAdapter.getItem(position).getTitle());
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

            mAdapter.clear();
            for(MediaBrowserCompat.MediaItem item : children){
                mAdapter.add(new CustomListItem(
                        item.getMediaId(),
                        item.getDescription().getTitle().toString()
                ));
            }
            mAdapter.notifyDataSetChanged();
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
    }

    private static class BrowserAdapter extends ArrayAdapter<CustomListItem>{

        public BrowserAdapter(Activity context){
            super(context, android.R.layout.simple_list_item_1, new ArrayList<CustomListItem>());
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            //MediaBrowserCompat.MediaItem item = getItem(position);
            View v = super.getView(position, convertView, parent);
            String title = getItem(position).getTitle();
            TextView tv = v.findViewById(android.R.id.text1);
            tv.setText(title);
            return v;
        }

    }
}
