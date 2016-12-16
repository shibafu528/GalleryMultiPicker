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

import android.graphics.Bitmap;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.LruCache;

public class ThumbnailCacheFragment extends Fragment {
    public static final String TAG = "ThumbnailCache";

    public LruCache<Long, Bitmap> mThumbnailCache;

    public ThumbnailCacheFragment() {
        setRetainInstance(true);
    }

    public static ThumbnailCacheFragment findOrCreateFragment(FragmentManager manager) {
        ThumbnailCacheFragment fragment = (ThumbnailCacheFragment) manager.findFragmentByTag(TAG);
        if (fragment == null) {
            fragment = new ThumbnailCacheFragment();
            manager.beginTransaction().add(fragment, TAG).commit();
        }
        return fragment;
    }
}
