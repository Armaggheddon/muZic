package com.armaggheddon.muzic.ui;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

class MarginItemDecorator extends RecyclerView.ItemDecoration {

    private int spaceHeight = 0;

    MarginItemDecorator(int spaceHeight){
        this.spaceHeight = spaceHeight;
    }
    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        if (parent.getChildAdapterPosition(view) == 0){
            outRect.top = spaceHeight;
        }
        outRect.bottom = spaceHeight;
    }
}
