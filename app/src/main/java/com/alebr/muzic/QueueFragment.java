package com.alebr.muzic;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class QueueFragment extends Fragment{

    public static final String QUEUE_FRAGMENT_TAG = "queue_fragment";

    private static final String TAG = "QueueFragment";

    public static final String IS_MAIN_ACTIVITY_ARGS_EXTRA = "is_main_activity";

    private boolean is_main_activity = true;

    private ListView mListView;
    private QueueListener mQueueListener;
    private ConstraintLayout noQueueLayout;
    private BrowserAdapter mAdapter;

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
        mListView = view.findViewById(R.id.listView);
        noQueueLayout = view.findViewById(R.id.empty_queue);

        mAdapter = new BrowserAdapter(getActivity());

        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mQueueListener.onQueueItemClicked(mAdapter.getItem(position).getId(), id);
            }
        });

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
        if(mediaBrowser.isConnected()){
            onConnected();
        }
    }

    public void onConnected(){

        List<MediaSessionCompat.QueueItem> queueItems = MediaControllerCompat.getMediaController(getActivity()).getQueue();
        if(queueItems == null ||queueItems.size() == 0) {
            noQueueLayout.setVisibility(View.VISIBLE);
        }
        else {
            mAdapter.clear();
            for (MediaSessionCompat.QueueItem queueItem : queueItems) {
                mAdapter.add(
                        new CustomListItem(
                                queueItem.getDescription().getMediaId(),
                                String.format("%d   %s", queueItem.getQueueId()+1,queueItem.getDescription().getTitle().toString())));
            }
            mAdapter.notifyDataSetChanged();
        }
    }

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
