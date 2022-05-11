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
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import info.shibafu528.gallerymultipicker.MultiPickerActivity;
import info.shibafu528.gallerymultipicker.R;

public class GridFragment extends Fragment {
    private static final String ARGV_BUCKET_ID = "bucket_id";

    private GridView gridView;

    private ContentAdapter adapter;

    public static GridFragment newInstance(String bucketId) {
        GridFragment fragment = new GridFragment();
        Bundle args = new Bundle();
        args.putString(ARGV_BUCKET_ID, bucketId);
        fragment.setArguments(args);
        return fragment;
    }

    public GridFragment() {
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.info_shibafu528_gallerymultipicker_fragment_grid, container, false);
        gridView = v.findViewById(android.R.id.list);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ContentResolver resolver = getActivity().getContentResolver();
        String bucketId = getArguments().getString(ARGV_BUCKET_ID);
        Cursor cursor;
        if (TextUtils.isEmpty(bucketId)) {
            cursor = resolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null,
                    MediaStore.Images.Media.DATE_MODIFIED + " DESC"
            );
        } else {
            cursor = resolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null,
                    MediaStore.Images.Media.BUCKET_ID + "=?",
                    new String[]{bucketId},
                    MediaStore.Images.Media.DATE_MODIFIED + " DESC"
            );
        }
        adapter = new ContentAdapter(getActivity(), cursor, resolver);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ((MultiPickerActivity) getActivity()).toggleSelect(id);
                ViewHolder vh = (ViewHolder) view.getTag();
                if (vh != null) {
                    vh.maskView.setVisibility(((MultiPickerActivity) getActivity()).getSelectedIds().contains(id) ? View.VISIBLE : View.INVISIBLE);
                }
                ((MultiPickerActivity) getActivity()).updateLimitCount();
            }
        });
    }

    public void notifyDataSetChanged() {
        adapter.notifyDataSetChanged();
    }

    private class ContentAdapter extends CursorAdapter {
        private ContentResolver resolver;
        private LayoutInflater inflater;

        public ContentAdapter(Context context, Cursor c, ContentResolver cr) {
            super(context, c, false);
            this.resolver = cr;
            this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View v = inflater.inflate(R.layout.info_shibafu528_gallerymultipicker_row_picture, null);
            ViewHolder vh = new ViewHolder(v);
            {
                int columns = context.getResources().getInteger(R.integer.info_shibafu528_gallerymultipicker_grid_columns_num);
                WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                Display display = wm.getDefaultDisplay();
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(display.getWidth() / columns, display.getWidth() / columns);
                vh.imageView.setLayoutParams(params);
                vh.maskView.setLayoutParams(params);
            }
            bindExistView(vh, context, cursor);
            return v;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            bindExistView((ViewHolder) view.getTag(), context, cursor);
        }

        public void bindExistView(ViewHolder vh, Context context, Cursor cursor) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
            int orientation = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.ORIENTATION));
            vh.imageView.setImageResource(android.R.drawable.ic_popup_sync);
            ThumbnailAsyncTask.execute(vh.imageView, String.valueOf(id),
                    ((MultiPickerActivity) getActivity()).getThumbnailCache(),
                    new ThumbnailAsyncTask.ThumbParam(resolver, id, orientation));
            vh.maskView.setVisibility(((MultiPickerActivity) getActivity()).getSelectedIds().contains(id) ? View.VISIBLE : View.INVISIBLE);
        }


    }

    private static class ViewHolder {
        ImageView imageView;
        ImageView maskView;

        public ViewHolder(View v) {
            imageView = v.findViewById(android.R.id.icon1);
            maskView = v.findViewById(android.R.id.icon2);
            v.setTag(this);
        }
    }
}
