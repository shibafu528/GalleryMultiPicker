/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 shibafu
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

package info.shibafu528.gallerymultipicker.sample;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import info.shibafu528.gallerymultipicker.MultiPickerActivity;

import java.io.FileNotFoundException;

public class SampleActivity extends AppCompatActivity {
    private static final int REQUEST_PICK = 1;

    private ListView mListView;
    private Uri[] mPickedUris;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = MultiPickerActivity.newIntent(getApplicationContext(), MultiPickerActivity.PICK_LIMIT_INFINITY);
                startActivityForResult(intent, REQUEST_PICK);
            }
        });
        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = MultiPickerActivity.newIntent(getApplicationContext(), 4);
                startActivityForResult(intent, REQUEST_PICK);
            }
        });

        mListView = (ListView) findViewById(R.id.listView);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK && resultCode == RESULT_OK) {
            mPickedUris = MultiPickerActivity.getPickedUris(data);
            mListView.setAdapter(new ImageAdapter(this, mPickedUris));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArray("mPickedUris", mPickedUris);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        Parcelable[] pickedUris = savedInstanceState.getParcelableArray("mPickedUris");
        if (pickedUris != null) {
            mPickedUris = new Uri[pickedUris.length];
            for (int i = 0; i < pickedUris.length; i++) {
                mPickedUris[i] = (Uri) pickedUris[i];
            }
            mListView.setAdapter(new ImageAdapter(this, mPickedUris));
        }
    }

    private static class ImageAdapter extends ArrayAdapter<Uri> {
        private LayoutInflater inflater;

        public ImageAdapter(Context context, Uri[] objects) {
            super(context, 0, objects);
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.row, null);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            Uri uri = getItem(position);
            try {
                holder.imageView.setImageBitmap(BitmapFactory.decodeStream(getContext().getContentResolver().openInputStream(uri)));
            } catch (FileNotFoundException e) {
                holder.imageView.setImageResource(android.R.drawable.ic_delete);
            }
            holder.textView.setText(uri.toString());
            return convertView;
        }

        private static class ViewHolder {
            ImageView imageView;
            TextView textView;

            ViewHolder(View v) {
                imageView = (ImageView) v.findViewById(R.id.imageView);
                textView = (TextView) v.findViewById(R.id.textView);
            }
        }
    }
}
