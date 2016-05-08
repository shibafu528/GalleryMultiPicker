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
import android.graphics.Matrix;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

class ThumbnailAsyncTask extends ParallelAsyncTask<ThumbnailAsyncTask.ThumbParam, Void, Bitmap> {
    public static class ThumbParam {
        private ContentResolver resolver;
        private long id;
        private int orientation;

        public ThumbParam(ContentResolver resolver, long id, int orientation) {
            this.resolver = resolver;
            this.id = id;
            this.orientation = orientation;
        }
    }

    private static LruCache<Long, Bitmap> cache = new LruCache<Long, Bitmap>((Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)? 4*1024*1024 : 16*1024*1024) {
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

    ThumbnailAsyncTask(ImageView imageView, String tag) {
        this.imageView = new WeakReference<>(imageView);
        this.tag = tag;
        imageView.setTag(this);
    }

    @Override
    protected Bitmap doInBackground(ThumbParam... params) {
        Bitmap bitmap = cache.get(params[0].id);
        if (bitmap == null) {
            bitmap = MediaStore.Images.Thumbnails.getThumbnail(params[0].resolver, params[0].id, MediaStore.Images.Thumbnails.MINI_KIND, options);
            if (bitmap != null) {
                Matrix matrix = new Matrix();
                matrix.setRotate(params[0].orientation);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                cache.put(params[0].id, bitmap);
            }
        }
        return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        ImageView imageView = this.imageView != null ? this.imageView.get() : null;
        if (imageView != null && imageView.getTag() == this) {
            imageView.setImageBitmap(bitmap);
        }
    }

    static void execute(@NonNull ImageView imageView, String tag, ThumbParam param) {
        if (imageView.getTag() != null) {
            ThumbnailAsyncTask asyncTask = (ThumbnailAsyncTask) imageView.getTag();
            if (tag.equals(asyncTask.tag)) {
                // 同じタスクなら何もしない
                return;
            }
            // 以前のタスクをキャンセル
            asyncTask.cancel(true);
        }
        // 新しいタスクを実行
        new ThumbnailAsyncTask(imageView, tag).executeParallel(param);
    }
}