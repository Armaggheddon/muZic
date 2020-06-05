package com.alebr.muzic;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

/**
 * Custom adapter implementation for the items in the recyclerview hosted by {@link ListFragment}
 * and {@link QueueFragment}
 */

class ExampleAdapter extends RecyclerView.Adapter<ExampleAdapter.ExampleViewHolder> {

    /**
     * ArrayList of {@link CustomListItem} that holds the information about the view
     */
    private ArrayList<CustomListItem> mCustomList;
    private OnItemClickListener mListener;

    /**
     * Interface to send click events received on the view
     */
    public interface OnItemClickListener{
        void onItemClick(int position);
    }

    /**
     * Get a reference to the listener
     * @param listener
     *          The listener to be assigned to {@link OnItemClickListener}
     */
    public void setOnItemClickListener(OnItemClickListener listener){
        mListener = listener;
    }

    /**
     * Class containing the views inside {@link R.layout#media_list_item}
     */
    public static class ExampleViewHolder extends RecyclerView.ViewHolder{

        public ImageView mImageView;
        public TextView mTitleTextView;

        public ExampleViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);

            /* Initialize the views in the layout */
            mImageView = itemView.findViewById(R.id.now_playing_imageview);
            mTitleTextView = itemView.findViewById(R.id.title_textview);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(listener != null){
                        int position = getAdapterPosition();
                        if(position != RecyclerView.NO_POSITION){
                            listener.onItemClick(position);
                        }
                    }
                }
            });
        }
    }

    /**
     * Constructor of the class
     * @param list
     *          The ArrayList of {@link CustomListItem} with the data to assign to the views
     */
    public ExampleAdapter(ArrayList<CustomListItem> list){
        mCustomList = list;
    }

    @NonNull
    @Override
    public ExampleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.media_list_item, parent, false);

        /* Create a new ExampleViewHolder to initialize the views and setting the lister */
        return new ExampleViewHolder(v, mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ExampleViewHolder holder, int position) {

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

    }

    /**
     * Utility method to add items to {@link ExampleAdapter#mCustomList}
     * @param item
     *          The item to be added
     */
    public void add(CustomListItem item){
        mCustomList.add(item);
    }

    /**
     * Utility method to return a {@link CustomListItem} given a position
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
