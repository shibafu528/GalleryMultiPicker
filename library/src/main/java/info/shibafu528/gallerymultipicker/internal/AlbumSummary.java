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
