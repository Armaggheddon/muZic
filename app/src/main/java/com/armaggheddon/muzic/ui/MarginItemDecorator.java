package com.armaggheddon.muzic.ui;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Decorates the recycler view items by adding a margin to the top end the bottom
 */

class MarginItemDecorator extends RecyclerView.ItemDecoration {

    private int spaceHeight;

    /**
     * Default constructor
     * @param spaceHeight
     *          The value to set as margin. This value will be set as spaceHeight/2 for marginTop and
     *          spaceHeight/2 for marginBottom.
     */
    MarginItemDecorator(int spaceHeight){
        this.spaceHeight = spaceHeight;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        /*
         To operate only on the first item of the list use this code
         if (parent.getChildAdapterPosition(view) == 0){
                outRect.top = spaceHeight;
         }
         */
        /* If the layout is scrolling horizontally apply margin only on left - right */
        if( parent.getLayoutManager().canScrollHorizontally()){
            outRect.left = spaceHeight/2;
            outRect.right = spaceHeight/2;
        }
        /* Else the layout is scrolling vertically then apply margin on top and bottom */
        else {
            outRect.top = spaceHeight / 2;
            outRect.bottom = spaceHeight / 2;
        }
    }
}
