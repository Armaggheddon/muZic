package com.armaggheddon.muzic.ui;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.armaggheddon.muzic.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom adapter implementation for the items in the recyclerview hosted by {@link ListFragment}
 * and {@link QueueFragment}
 */

class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.RecyclerViewHolder> implements Filterable {

     /* ArrayList of {@link CustomSearchItem} that holds the information about the view */
    private ArrayList<CustomSearchItem> mCustomList;
    private ArrayList<CustomSearchItem> mCustomListFull;
    private OnItemClickListener mOnItemClickListener;

    /* Cache the request option since it will be used for every view */
    private final RequestOptions defaultSizeOption = new RequestOptions().override(500, 500);

    /**
     * Interface to send click events received on the view
     */
    public interface OnItemClickListener{
        void onItemClick(int position);
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
     * Class containing the views inside {@link R.layout#search_item}
     */
    public static class RecyclerViewHolder extends RecyclerView.ViewHolder{


        public TextView mTitleTextView;
        public ImageView mArtImageView;

        public RecyclerViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);

            /* Initialize the views in the layout */
            mArtImageView = itemView.findViewById(R.id.search_art_image);
            mTitleTextView = itemView.findViewById(R.id.search_song_title);


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
        }
    }

    /**
     * Constructor of the class
     * @param list
     *              The ArrayList of {@link CustomSearchItem} with the data to assign to the views
     */
    public SearchAdapter(ArrayList<CustomSearchItem> list){
        mCustomList = list;
        mCustomListFull = new ArrayList<>(mCustomList);
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.search_item, parent, false);

        /* Create a new RecyclerViewHolder to initialize the views and setting the lister */
        return new RecyclerViewHolder(v, mOnItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {

        /* Set the data in the view */
        CustomSearchItem currentItem = mCustomList.get(position);

        /*
        This method gets called when the views are created and when we sent a notifyItemChanged(...).
        The only change that is currently being pushed to the items in the recycler view is the
        drawable on mImageView being removed or added, where a resource id of 0 represents the
        resource to be removed
         */

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
     * Add {@param item} to {@link SearchAdapter#mCustomList} and also to {@link SearchAdapter#mCustomListFull}
     * @param item
     *          The item to be added
     */
    public void add(CustomSearchItem item){
        mCustomList.add(item);
        mCustomListFull.add(item);
    }

    /**
     * Return a {@link CustomSearchItem} given a position
     * @param position
     *          The position in the list that is being asked
     * @return
     *          The {@link CustomSearchItem} in the position {@param position}
     */
    public CustomSearchItem getItem(int position){
        return mCustomList.get(position);
    }

    @Override
    public int getItemCount() {
        return mCustomList.size();
    }

    @Override
    public Filter getFilter() {
        return searchFilter;
    }

    private Filter searchFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<CustomSearchItem> filteredList = new ArrayList<>();

            /* If the constraint is empty return all the elements */
            if( constraint == null || constraint.length() == 0){
                filteredList.addAll(mCustomListFull);
            }else {
                /* Else get get the elements that matches the constraint */
                /* Currently the filter removes all the spaces between the words */
                String filterPattern = constraint.toString().toLowerCase().trim().replace(" ", "");
                for(CustomSearchItem item : mCustomListFull){
                    if (item.getTitle().toLowerCase().trim().replace(" ", "").contains(filterPattern)){
                        filteredList.add(item);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {

            mCustomList.clear();
            mCustomList.addAll((ArrayList)results.values);

            notifyDataSetChanged();

        }
    };
}
