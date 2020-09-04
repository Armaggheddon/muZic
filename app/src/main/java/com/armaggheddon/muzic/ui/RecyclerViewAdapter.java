package com.armaggheddon.muzic.ui;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.armaggheddon.muzic.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;

/**
 * Custom adapter implementation for the items in the recyclerview hosted by {@link ListFragment}
 * and {@link QueueFragment}
 */

class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.RecyclerViewHolder> {

     /* ArrayList of {@link CustomListItem} that holds the information about the view */
    private ArrayList<CustomListItem> mCustomList;
    private OnItemClickListener mOnItemClickListener;
    private final RequestOptions defaultSizeOption = new RequestOptions().override(500, 500);

    /**
     * Interface to send click events received on the view
     */
    public interface OnItemClickListener{
        void onItemClick(int position);
        void onItemLongClick(int position);
    }

    /**
     * Get a reference to the listener
     * @param listener
     *                  The listener to be assigned to {@link OnItemClickListener}
     */
    public void setOnItemClickListener(OnItemClickListener listener){
        mOnItemClickListener = listener;
    }

    /**
     * Class containing the views inside {@link R.layout#media_list_item}
     */
    public static class RecyclerViewHolder extends RecyclerView.ViewHolder{

        public ImageView mImageView;
        public TextView mTitleTextView;
        public ImageView mArtImageView;

        public RecyclerViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);

            /* Initialize the views in the layout */
            mArtImageView = itemView.findViewById(R.id.art_image_view);
            mImageView = itemView.findViewById(R.id.now_playing_imageview);
            mTitleTextView = itemView.findViewById(R.id.now_playing_title);


            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(listener != null){
                        int position = getBindingAdapterPosition();
                        if(position != RecyclerView.NO_POSITION){
                            listener.onItemClick(position);
                        }
                    }
                }
            });
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int position = getBindingAdapterPosition();
                    if( position != RecyclerView.NO_POSITION){
                        listener.onItemLongClick(position);
                    }
                    return true;
                }
            });
        }
    }

    /**
     * Constructor of the class
     * @param list
     *              The ArrayList of {@link CustomListItem} with the data to assign to the views
     */
    public RecyclerViewAdapter(ArrayList<CustomListItem> list){
        mCustomList = list;
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.media_list_item, parent, false);

        /* Create a new RecyclerViewHolder to initialize the views and setting the lister */
        return new RecyclerViewHolder(v, mOnItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {

        /* Set the data in the view */
        CustomListItem currentItem = mCustomList.get(position);

        /*
        This method gets called when the views are created and when we sent a notifyItemChanged(...).
        The only change that is currently being pushed to the items in the recycler view is the
        drawable on mImageView being removed or added, where a resource id of 0 represents the
        resource to be removed
         */
        if(currentItem.getImageRes() == 0){
            holder.mImageView.setImageDrawable(null);
        }else {
            holder.mImageView.setImageResource(currentItem.getImageRes());
        }

        holder.mTitleTextView.setText(currentItem.getTitle());

        /* Load and display the album art for the item */
        Uri art = currentItem.getArt();
        Glide.with(holder.mArtImageView)
                    .load(art)
                    .apply( defaultSizeOption)
                    .error(R.drawable.ic_default_album_art_with_bg)
                    .into(holder.mArtImageView);
    }

    /**
     * Add {@param item} to {@link RecyclerViewAdapter#mCustomList}
     * @param item
     *          The item to be added
     */
    public void add(CustomListItem item){
        mCustomList.add(item);
    }

    /**
     * Return a {@link CustomListItem} given a position
     * @param position
     *          The position in the list that is being asked
     * @return
     *          The {@link CustomListItem} in the position {@param position}
     */
    public CustomListItem getItem(int position){
        return mCustomList.get(position);
    }

    @Override
    public int getItemCount() {
        return mCustomList.size();
    }
}
