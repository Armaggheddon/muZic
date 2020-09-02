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
        outRect.top = spaceHeight/2;
        outRect.bottom = spaceHeight/2;
    }
}
