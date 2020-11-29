/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 shibafu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package info.shibafu528.gallerymultipicker.internal;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.util.ArraySet;
import android.support.v4.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import info.shibafu528.gallerymultipicker.MultiPickerActivity;
import info.shibafu528.gallerymultipicker.R;

public class AlbumFragment extends ListFragment {
    private static final String[] SELECT_BUCKET = {
            MediaStore.Images.ImageColumns._ID,
            MediaStore.Images.ImageColumns.BUCKET_ID,
            MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Images.ImageColumns.DATA,
            MediaStore.Images.ImageColumns.ORIENTATION,
            "COUNT(*)"
    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ContentResolver resolver = getActivity().getContentResolver();

        Cursor cursor = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null,
                MediaStore.Images.Media.DATE_MODIFIED + " DESC"
        );
        View view = getActivity().getLayoutInflater().inflate(R.layout.info_shibafu528_gallerymultipicker_row_album, null);
        ViewHolder vh = new ViewHolder(view);
        if (cursor.moveToFirst()) {
            long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID));
            int orientation = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.ORIENTATION));
            vh.imageView.setImageResource(android.R.drawable.ic_popup_sync);
            ThumbnailAsyncTask.execute(vh.imageView, String.valueOf(id),
                    ((MultiPickerActivity) getActivity()).getThumbnailCache(),
                    new ThumbnailAsyncTask.ThumbParam(resolver, id, orientation));
        } else {
            vh.imageView.setImageResource(android.R.drawable.gallery_thumb);
        }
        vh.title.setText(R.string.info_shibafu528_gallerymultipicker_all_images);
        vh.count.setText(String.valueOf(cursor.getCount()));
        cursor.close();
        getListView().addHeaderView(view);

        ListAdapter adapter;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            Cursor c = resolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    SELECT_BUCKET,
                    "1) GROUP BY (2",
                    null,
                    "MAX(datetaken) DESC");
            adapter = new AlbumAdapter(getActivity(), c, resolver);
        } else {
            adapter = LawfulAlbumAdapter.newInstance(getActivity(), resolver, ((MultiPickerActivity) getActivity()).getThumbnailCache());
        }
        setListAdapter(adapter);
        getListView().setFastScrollEnabled(true);
    }

    @Override
    public void onDestroyView() {
        setListAdapter(null);
        super.onDestroyView();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        String bucketId = null;
        if (position-- > 0) {
            bucketId = ((BucketIdAdapter) getListAdapter()).getBucketId(position);
        }
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        transaction
                .setCustomAnimations(
                        R.anim.info_shibafu528_gallerymultipicker_open_enter,
                        R.anim.info_shibafu528_gallerymultipicker_open_exit,
                        R.anim.info_shibafu528_gallerymultipicker_close_enter,
                        R.anim.info_shibafu528_gallerymultipicker_close_exit)
                .replace(R.id.info_shibafu528_gallerymultipicker_container, GridFragment.newInstance(bucketId))
                .addToBackStack(null)
                .commit();
    }

    private interface BucketIdAdapter {
        String getBucketId(int position);
    }

    private class AlbumAdapter extends CursorAdapter implements BucketIdAdapter {
        private ContentResolver resolver;
        private LayoutInflater inflater;

        public AlbumAdapter(Context context, Cursor c, ContentResolver cr) {
            super(context, c, false);
            this.resolver = cr;
            this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View v = inflater.inflate(R.layout.info_shibafu528_gallerymultipicker_row_album, null);
            ViewHolder vh = new ViewHolder(v);
            bindExistView(vh, context, cursor);
            return v;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            bindExistView((ViewHolder) view.getTag(), context, cursor);
        }

        public void bindExistView(ViewHolder vh, Context context, Cursor cursor) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns._ID));
            int orientation = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.ORIENTATION));
            vh.imageView.setImageResource(android.R.drawable.ic_popup_sync);
            ThumbnailAsyncTask.execute(vh.imageView, String.valueOf(id),
                    ((MultiPickerActivity) getActivity()).getThumbnailCache(),
                    new ThumbnailAsyncTask.ThumbParam(resolver, id, orientation));
            vh.title.setText(cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME)));
            vh.count.setText(cursor.getString(cursor.getColumnIndex("COUNT(*)")));
        }

        @Override
        public String getBucketId(int position) {
            Cursor c = (Cursor) getListAdapter().getItem(position);
            return c.getString(c.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_ID));
        }
    }

    private static class LawfulAlbumAdapter extends ArrayAdapter<AlbumSummary> implements BucketIdAdapter {
        public static final String[] SELECT_IMAGE_AND_BUCKET = {
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.ORIENTATION,
                MediaStore.Images.ImageColumns.BUCKET_ID,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Images.ImageColumns.DATE_TAKEN
        };
        public static final String[] SELECT_ONLY_ID = {MediaStore.Images.ImageColumns._ID};

        @NonNull
        private final ContentResolver resolver;
        @NonNull
        private final LayoutInflater inflater;
        @NonNull
        private final LruCache<Long, Bitmap> thumbnailCache;

        public static LawfulAlbumAdapter newInstance(@NonNull Context context, @NonNull ContentResolver cr, @NonNull LruCache<Long, Bitmap> thumbnailCache) {
            ArrayList<AlbumSummary> summaries = new ArrayList<>();
            ArraySet<String> summarizedBucketIds = new ArraySet<>();

            Cursor cursor = cr.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    SELECT_IMAGE_AND_BUCKET,
                    null,
                    null,
                    MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");
            if (cursor != null && cursor.moveToFirst()) {
                try {
                    int columnId = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns._ID);
                    int columnOrientation = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.ORIENTATION);
                    int columnBucketId = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.BUCKET_ID);
                    int columnBucketName = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME);

                    do {
                        String bucketId = cursor.getString(columnBucketId);
                        String bucketName = cursor.getString(columnBucketName);

                        if (!summarizedBucketIds.contains(bucketId)) {
                            long count = countInBucket(cr, bucketId);
                            long latestImageId = cursor.getLong(columnId);
                            int latestImageOrientation = cursor.getInt(columnOrientation);

                            summaries.add(new AlbumSummary(bucketId, bucketName, count, latestImageId, latestImageOrientation));
                            summarizedBucketIds.add(bucketId);
                        }
                    } while (cursor.moveToNext());
                } finally {
                    cursor.close();
                }
            }

            return new LawfulAlbumAdapter(context, cr, thumbnailCache, summaries);
        }

        private static long countInBucket(@NonNull ContentResolver cr, @NonNull String bucketId) {
            Cursor cursor = cr.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    SELECT_ONLY_ID,
                    MediaStore.Images.ImageColumns.BUCKET_ID + "=?",
                    new String[]{bucketId},
                    null);

            if (cursor != null && cursor.moveToFirst()) {
                try {
                    return cursor.getCount();
                } finally {
                    cursor.close();
                }
            }

            return 0;
        }

        public LawfulAlbumAdapter(@NonNull Context context, @NonNull ContentResolver cr, @NonNull LruCache<Long, Bitmap> thumbnailCache, @NonNull List<AlbumSummary> objects) {
            super(context, 0, objects);
            this.resolver = cr;
            this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.thumbnailCache = thumbnailCache;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View v = convertView;
            ViewHolder vh;

            if (v == null) {
                v = inflater.inflate(R.layout.info_shibafu528_gallerymultipicker_row_album, parent, false);
                vh = new ViewHolder(v);
                v.setTag(vh);
            } else {
                vh = (ViewHolder) v.getTag();
            }

            AlbumSummary summary = getItem(position);
            if (summary != null) {
                vh.imageView.setImageResource(android.R.drawable.ic_popup_sync);
                ThumbnailAsyncTask.execute(vh.imageView, String.valueOf(summary.latestImageId), thumbnailCache,
                        new ThumbnailAsyncTask.ThumbParam(resolver, summary.latestImageId, summary.latestImageOrientation));
                vh.title.setText(summary.displayName);
                vh.count.setText(summary.countString);
            }

            return v;
        }

        @Override
        public String getBucketId(int position) {
            return getItem(position).bucketId;
        }
    }

    private static class AlbumSummary {
        final String bucketId;
        final String displayName;
        final long count;
        final String countString;
        final long latestImageId;
        final int latestImageOrientation;

        private AlbumSummary(String bucketId, String displayName, long count, long latestImageId, int latestImageOrientation) {
            this.bucketId = bucketId;
            this.displayName = displayName;
            this.count = count;
            this.countString = String.valueOf(count);
            this.latestImageId = latestImageId;
            this.latestImageOrientation = latestImageOrientation;
        }
    }

    private static class ViewHolder {
        ImageView imageView;
        TextView title;
        TextView count;

        public ViewHolder(View v) {
            imageView = v.findViewById(android.R.id.icon);
            title = v.findViewById(android.R.id.text1);
            count = v.findViewById(android.R.id.text2);
            v.setTag(this);
        }
    }
}
