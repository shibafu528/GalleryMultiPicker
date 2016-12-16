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
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
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

        setListAdapter(new AlbumAdapter(getActivity(),
                resolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        SELECT_BUCKET,
                        "1) GROUP BY (2",
                        null,
                        "MAX(datetaken) DESC"),
                resolver));
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
            Cursor c = (Cursor) getListAdapter().getItem(position);
            bucketId = c.getString(c.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_ID));
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

    private class AlbumAdapter extends CursorAdapter {
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
    }

    private static class ViewHolder {
        ImageView imageView;
        TextView title;
        TextView count;

        public ViewHolder(View v) {
            imageView = (ImageView) v.findViewById(android.R.id.icon);
            title = (TextView) v.findViewById(android.R.id.text1);
            count = (TextView) v.findViewById(android.R.id.text2);
            v.setTag(this);
        }
    }
}
