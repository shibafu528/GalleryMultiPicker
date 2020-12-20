/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 shibafu
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

import android.os.Parcel;
import android.os.Parcelable;

class AlbumSummary implements Parcelable {
    final String bucketId;
    final String displayName;
    final long count;
    final String countString;
    final long latestImageId;
    final int latestImageOrientation;

    AlbumSummary(String bucketId, String displayName, long count, long latestImageId, int latestImageOrientation) {
        this.bucketId = bucketId;
        this.displayName = displayName;
        this.count = count;
        this.countString = String.valueOf(count);
        this.latestImageId = latestImageId;
        this.latestImageOrientation = latestImageOrientation;
    }

    protected AlbumSummary(Parcel in) {
        bucketId = in.readString();
        displayName = in.readString();
        count = in.readLong();
        countString = String.valueOf(count);
        latestImageId = in.readLong();
        latestImageOrientation = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(bucketId);
        dest.writeString(displayName);
        dest.writeLong(count);
        dest.writeLong(latestImageId);
        dest.writeInt(latestImageOrientation);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<AlbumSummary> CREATOR = new Creator<AlbumSummary>() {
        @Override
        public AlbumSummary createFromParcel(Parcel in) {
            return new AlbumSummary(in);
        }

        @Override
        public AlbumSummary[] newArray(int size) {
            return new AlbumSummary[size];
        }
    };
}
