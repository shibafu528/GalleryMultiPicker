package info.shibafu528.gallerymultipicker.internal;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.util.ArraySet;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class AlbumSummaryAsyncTask extends ParallelAsyncTask<Void, Void, List<AlbumSummary>> {
    private static final String[] SELECT_IMAGE_AND_BUCKET = {
            MediaStore.Images.ImageColumns._ID,
            MediaStore.Images.ImageColumns.ORIENTATION,
            MediaStore.Images.ImageColumns.BUCKET_ID,
            MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Images.ImageColumns.DATE_TAKEN
    };
    private static final String[] SELECT_ONLY_ID = {MediaStore.Images.ImageColumns._ID};

    @NonNull
    private final WeakReference<Context> context;
    @NonNull
    private final WeakReference<Callback> callback;

    AlbumSummaryAsyncTask(@NonNull Context context, @NonNull Callback callback) {
        this.context = new WeakReference<>(context);
        this.callback = new WeakReference<>(callback);
    }

    @Override
    protected List<AlbumSummary> doInBackground(Void... args) {
        Context context = this.context.get();
        if (context == null) {
            return Collections.emptyList();
        }

        ArrayList<AlbumSummary> summaries = new ArrayList<>();
        ArraySet<String> summarizedBucketIds = new ArraySet<>();

        ContentResolver cr = context.getContentResolver();
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

        return summaries;
    }

    @Override
    protected void onPostExecute(List<AlbumSummary> summaries) {
        super.onPostExecute(summaries);

        Callback callback = this.callback.get();
        if (callback != null) {
            callback.onAlbumSummariesLoaded(summaries);
        }
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

    interface Callback {
        void onAlbumSummariesLoaded(List<AlbumSummary> summaries);
    }
}
