package com.armaggheddon.muzic.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.armaggheddon.muzic.R;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

//TODO: comment
public class PlayAlbumArtistBottomSheet extends BottomSheetDialogFragment {

    private static final String TITLE_ARGS_EXTRA = "title";
    private static final String MEDIA_ID_ARGS_EXTRA = "media_id";
    private static final String ART_URI_ARGS_EXTRA = "art_uri";

    public static PlayAlbumArtistBottomSheet PlayAlbumArtistBottomSheetFactory(String mediaId, String title, Uri art){
        PlayAlbumArtistBottomSheet bottomSheet = new PlayAlbumArtistBottomSheet();
        Bundle extra = new Bundle();
        extra.putString(MEDIA_ID_ARGS_EXTRA, mediaId);
        extra.putString(TITLE_ARGS_EXTRA, title);
        extra.putString(ART_URI_ARGS_EXTRA, art.toString());
        bottomSheet.setArguments(extra);
        return bottomSheet;
    }


    private BottomSheetListener mListener;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.play_album_artist_bottom_sheet, container, false);

        Bundle bundle = getArguments();
        assert bundle != null;
        final String mediaId = bundle.getString(MEDIA_ID_ARGS_EXTRA);
        String title_text = bundle.getString(TITLE_ARGS_EXTRA);
        String art = bundle.getString(ART_URI_ARGS_EXTRA);

        TextView title = v.findViewById(R.id.title_bs);
        title.setText(title_text);
        ImageView imageView = v.findViewById(R.id.album_art_bs);
        Glide.with(imageView)
                .load(Uri.parse(art))
                .placeholder(R.drawable.ic_default_album_art_with_bg)
                .error(R.drawable.ic_default_album_art_with_bg)
                .into(imageView);

        ExtendedFloatingActionButton playBtn = v.findViewById(R.id.play_button_bs);
        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mListener.onBottomSheetButtonClicked(mediaId);
                dismiss();
            }
        });
        return v;
    }
    public interface BottomSheetListener {
        void onBottomSheetButtonClicked(String mediaId);
    }
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (BottomSheetListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement BottomSheetListener");
        }
    }
}
