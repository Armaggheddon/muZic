package com.armaggheddon.muzic;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Hashtable;
import java.util.concurrent.ExecutionException;

/**
 * Content provider to expose the album art URI to Android Auto client and for internal use
 */
public class ArtProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        return true;
    }

    /* Keep a map to map content uris with the real uri*/
    private static Hashtable<Uri, Uri> uriMap = new Hashtable<>();

    public static Uri mapUri(Uri uri) {
        String path = "";
        try {
            path = uri.getEncodedPath().substring(1).replace("/", ":");
        }catch (NullPointerException e){
            return Uri.EMPTY;
        }
        Uri contentUri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority("com.armaggheddon.muzic")
                .path(uri.getEncodedPath().substring(1).replace("/", ":"))
                .build();
        uriMap.put(contentUri, uri);
        return contentUri;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return "image/jpeg";
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Nullable
    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {

        /* Return a ParcelFileDescriptor that allows the client to access the album art uri that resolves in a bitmap */
        Context context = this.getContext();
        Uri remoteUri = uriMap.get(uri);
        File file = new File(context.getCacheDir(), uri.getPath());

        if (!file.exists()) {
            try {
                File cacheFile = Glide.with(context).asFile().load(remoteUri).submit().get();
                cacheFile.renameTo(file);
                file = cacheFile;
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }
}
