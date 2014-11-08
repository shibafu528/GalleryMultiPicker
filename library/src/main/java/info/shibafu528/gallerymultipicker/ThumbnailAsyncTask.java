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

package info.shibafu528.gallerymultipicker;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.support.v4.util.LruCache;
import android.util.Pair;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

class ThumbnailAsyncTask extends ParallelAsyncTask<Pair<ContentResolver, Long>, Void, Bitmap> {
    private static LruCache<Long, Bitmap> cache = new LruCache<Long, Bitmap>(16*1024*1024) {
        @Override
        protected int sizeOf(Long key, Bitmap value) {
            return value.getRowBytes() * value.getHeight();
        }
    };
    private static BitmapFactory.Options options = new BitmapFactory.Options();

    private WeakReference<ImageView> imageView;
    private String tag;

    static {
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inPurgeable = true;
    }

    @Override
    protected void finalize() throws Throwable {
        cache.evictAll();
        super.finalize();
    }

    ThumbnailAsyncTask(ImageView imageView) {
        this.imageView = new WeakReference<>(imageView);
        this.tag = (String) imageView.getTag();
    }

    @Override
    protected Bitmap doInBackground(Pair<ContentResolver, Long>... params) {
        Bitmap bitmap = cache.get(params[0].second);
        if (bitmap == null) {
            bitmap = MediaStore.Images.Thumbnails.getThumbnail(params[0].first, params[0].second, MediaStore.Images.Thumbnails.MINI_KIND, options);
            if (bitmap != null) {
                cache.put(params[0].second, bitmap);
            }
        }
        return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        ImageView imageView = this.imageView != null ? this.imageView.get() : null;
        if (imageView != null && tag.equals(imageView.getTag())) {
            imageView.setImageBitmap(bitmap);
        }
    }
}