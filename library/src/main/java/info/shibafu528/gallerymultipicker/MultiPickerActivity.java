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

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.AnimRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.util.LruCache;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import info.shibafu528.gallerymultipicker.internal.AlbumFragment;
import info.shibafu528.gallerymultipicker.internal.ThumbnailCacheFragment;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MultiPickerActivity extends AppCompatActivity {
    private static final int REQUEST_GALLERY    = 0;
    private static final int REQUEST_CAMERA     = 1;

    private static final String STATE_SELECTED_IDS = "mSelectedIds";
    private static final String STATE_CAMERA_TEMP = "mCameraTemp";

    public static final String EXTRA_PICK_LIMIT             = "max";
    public static final String EXTRA_CAMERA_DEST_DIR        = "camera_dest_dir";
    public static final String EXTRA_URIS                   = "uris";
    public static final String EXTRA_THEME                  = "theme";
    public static final String EXTRA_ICON_THEME             = "icon_theme";
    public static final String EXTRA_CLOSE_ENTER_ANIMATION  = "close_enter_anim";
    public static final String EXTRA_CLOSE_EXIT_ANIMATION   = "close_exit_anim";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({ICON_THEME_DARK, ICON_THEME_LIGHT})
    public @interface IconTheme{}
    public static final String ICON_THEME_DARK  = "dark";
    public static final String ICON_THEME_LIGHT = "light";

    public static final int PICK_LIMIT_INFINITY = -1;

    private FloatingActionButton mGalleryFab;
    private FloatingActionButton mCameraFab;
    private FloatingActionButton mDoneFab;

    private String mMenuIconTheme = null;
    private boolean mOverrideTransition;
    private int mCloseEnterAnimation;
    private int mCloseExitAnimation;

    private int mPickLimit = 1;
    private String mCameraDestDir;
    private Uri mCameraTemp;

    private LongArray mSelectedIds = new LongArray();

    private LruCache<Long, Bitmap> mThumbnailCache;

    public static Intent newIntent(Context packageContext,
                                   int pickLimit) {
        Intent intent = new Intent(packageContext, MultiPickerActivity.class);
        intent.putExtra(EXTRA_PICK_LIMIT, pickLimit);
        return intent;
    }

    public static Intent newIntent(Context packageContext,
                                   int pickLimit,
                                   @Nullable String cameraDestDir) {
        Intent intent = new Intent(packageContext, MultiPickerActivity.class);
        intent.putExtra(EXTRA_PICK_LIMIT, pickLimit);
        intent.putExtra(EXTRA_CAMERA_DEST_DIR, cameraDestDir);
        return intent;
    }

    public static Intent newIntent(Context packageContext,
                                   int pickLimit,
                                   @Nullable String cameraDestDir,
                                   @IconTheme String menuIconTheme) {
        Intent intent = new Intent(packageContext, MultiPickerActivity.class);
        intent.putExtra(EXTRA_PICK_LIMIT, pickLimit);
        intent.putExtra(EXTRA_CAMERA_DEST_DIR, cameraDestDir);
        intent.putExtra(EXTRA_ICON_THEME, menuIconTheme);
        return intent;
    }

    public static Intent newIntent(Context packageContext,
                                   int pickLimit,
                                   @Nullable String cameraDestDir,
                                   @IconTheme String menuIconTheme,
                                   @AnimRes int closeEnterAnimationRes,
                                   @AnimRes int closeExitAnimationRes) {
        Intent intent = new Intent(packageContext, MultiPickerActivity.class);
        intent.putExtra(EXTRA_PICK_LIMIT, pickLimit);
        intent.putExtra(EXTRA_CAMERA_DEST_DIR, cameraDestDir);
        intent.putExtra(EXTRA_ICON_THEME, menuIconTheme);
        intent.putExtra(EXTRA_CLOSE_ENTER_ANIMATION, closeEnterAnimationRes);
        intent.putExtra(EXTRA_CLOSE_EXIT_ANIMATION, closeExitAnimationRes);
        return intent;
    }

    public static Uri[] getPickedUris(Intent onActivityResultData) {
        Parcelable[] uris = onActivityResultData.getParcelableArrayExtra(EXTRA_URIS);
        Uri[] pickedUris = new Uri[uris.length];
        for (int i = 0; i < uris.length; i++) {
            pickedUris[i] = (Uri) uris[i];
        }
        return pickedUris;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        {
            Intent intent = getIntent();
            int themeResId = intent.getIntExtra(EXTRA_THEME, -1);
            if (themeResId > -1) {
                setTheme(themeResId);
            }
            mPickLimit = intent.getIntExtra(EXTRA_PICK_LIMIT, PICK_LIMIT_INFINITY);
            mCameraDestDir = intent.getStringExtra(EXTRA_CAMERA_DEST_DIR);
            mMenuIconTheme = intent.getStringExtra(EXTRA_ICON_THEME);
            mCloseEnterAnimation = intent.getIntExtra(EXTRA_CLOSE_ENTER_ANIMATION, 0);
            mCloseExitAnimation = intent.getIntExtra(EXTRA_CLOSE_EXIT_ANIMATION, 0);
            mOverrideTransition = intent.hasExtra(EXTRA_CLOSE_ENTER_ANIMATION) || intent.hasExtra(EXTRA_CLOSE_EXIT_ANIMATION);
            if (mMenuIconTheme == null) try {
                ActivityInfo info = getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);
                mMenuIconTheme = info.metaData != null ? info.metaData.getString("info.shibafu528.gallerymultipicker.ICON_THEME") : null;
            } catch (PackageManager.NameNotFoundException ignored) {}
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.info_shibafu528_gallerymultipicker_container);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setTitle(R.string.info_shibafu528_gallerymultipicker_title);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.info_shibafu528_gallerymultipicker_container, new AlbumFragment())
                    .commit();
        } else {
            mSelectedIds.addAll(savedInstanceState.getLongArray(STATE_SELECTED_IDS));
            mCameraTemp = savedInstanceState.getParcelable(STATE_CAMERA_TEMP);
        }

        mGalleryFab = (FloatingActionButton) findViewById(R.id.info_shibafu528_gallerymultipicker_action_gallery);
        mGalleryFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_GALLERY);
            }
        });
        mCameraFab = (FloatingActionButton) findViewById(R.id.info_shibafu528_gallerymultipicker_action_camera);
        mCameraFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean existExternal = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
                if (!existExternal) {
                    Toast.makeText(MultiPickerActivity.this, R.string.info_shibafu528_gallerymultipicker_storage_error, Toast.LENGTH_SHORT).show();
                } else {
                    File extDestDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                            mCameraDestDir != null ? mCameraDestDir : "Camera");
                    if (!extDestDir.exists()) {
                        extDestDir.mkdirs();
                    }
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
                    String fileName = sdf.format(new Date(System.currentTimeMillis()));
                    File destFile = new File(extDestDir.getPath(), fileName + ".jpg");
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.TITLE, fileName);
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.Images.Media.DATA, destFile.getPath());
                    mCameraTemp = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, mCameraTemp);
                    startActivityForResult(intent, REQUEST_CAMERA);
                }
            }
        });
        mDoneFab = (FloatingActionButton) findViewById(R.id.info_shibafu528_gallerymultipicker_action_done);
        mDoneFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                accept(getSelectedUris());
            }
        });

        updateLimitCount();

        {
            ThumbnailCacheFragment fragment = ThumbnailCacheFragment.findOrCreateFragment(getSupportFragmentManager());
            mThumbnailCache = fragment.mThumbnailCache;
            if (mThumbnailCache == null) {
                final int maxMemorySize;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                    maxMemorySize = 4;
                } else {
                    maxMemorySize = 16;
                }

                mThumbnailCache = new LruCache<Long, Bitmap>(maxMemorySize * 1024 * 1024) {
                    @Override
                    protected int sizeOf(Long key, Bitmap value) {
                        return value.getRowBytes() * value.getHeight();
                    }
                };
                fragment.mThumbnailCache = mThumbnailCache;
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLongArray(STATE_SELECTED_IDS, mSelectedIds.toPrimitive());
        outState.putParcelable(STATE_CAMERA_TEMP, mCameraTemp);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
            } else {
                setResult(RESULT_CANCELED);
                finish();
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GALLERY:
                if (resultCode == RESULT_OK) {
                    if (data.getData() != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                                && !data.getDataString().startsWith("file://") && !data.getDataString().startsWith("content://media/")) {
                            final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                            getContentResolver().takePersistableUriPermission(data.getData(), takeFlags);
                        }
                        accept(data.getData());
                    } else {
                        Toast.makeText(this, R.string.info_shibafu528_gallerymultipicker_gallery_error, Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case REQUEST_CAMERA: {
                if (resultCode == RESULT_OK) {
                    if (data != null && data.getData() != null) {
                        //getDataでUriが返ってくる端末用, フィールドは手に入ったUriで上書き
                        mCameraTemp = data.getData();
                    }
                    if (mCameraTemp == null) {
                        Toast.makeText(this, R.string.info_shibafu528_gallerymultipicker_camera_error, Toast.LENGTH_LONG).show();
                    } else {
                        accept(mCameraTemp);
                    }
                } else if (resultCode == RESULT_CANCELED) {
                    Cursor c = getContentResolver().query(mCameraTemp,
                            new String[]{MediaStore.Images.Media.DATA}, null, null, null);
                    if (c != null) {
                        if (c.moveToFirst()) {
                            getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    MediaStore.Images.Media.DATA + "=?",
                                    new String[]{c.getString(0)});
                        }
                        c.close();
                    }
                }
                break;
            }
        }
    }

    @Override
    public void finish() {
        super.finish();
        if (mOverrideTransition) {
            overridePendingTransition(mCloseEnterAnimation, mCloseExitAnimation);
        }
    }

    private void accept(Uri... uri) {
        Intent result = new Intent();
        result.putExtra(EXTRA_URIS, uri);
        setResult(RESULT_OK, result);
        finish();
    }

    private String getQuantityString(int pluralResId, int zeroStringResId, int quantity) {
        if (quantity == 0) {
            return getString(zeroStringResId);
        } else {
            return getResources().getQuantityString(pluralResId, quantity, quantity);
        }
    }

    public void updateLimitCount() {
        if (canInfinityPick()) {
            getSupportActionBar().setSubtitle(getQuantityString(
                    R.plurals.info_shibafu528_gallerymultipicker_subtitle_unlimited,
                    R.string.info_shibafu528_gallerymultipicker_subtitle_unlimited_zero,
                    getSelectedIds().size()
            ));
        } else {
            getSupportActionBar().setSubtitle(getQuantityString(
                    R.plurals.info_shibafu528_gallerymultipicker_subtitle_limited,
                    R.string.info_shibafu528_gallerymultipicker_subtitle_limited_zero,
                    getPickLimit() - getSelectedIds().size()
            ));
        }
        if (mSelectedIds.isEmpty()) {
            mDoneFab.hide(new FloatingActionButton.OnVisibilityChangedListener() {
                @Override
                public void onHidden(FloatingActionButton fab) {
                    mGalleryFab.show();
                    mCameraFab.show();
                }
            });
        } else {
            mGalleryFab.hide();
            mCameraFab.hide(new FloatingActionButton.OnVisibilityChangedListener() {
                @Override
                public void onHidden(FloatingActionButton fab) {
                    mDoneFab.show();
                }
            });
        }
    }

    public boolean canInfinityPick() {
        return getPickLimit() == PICK_LIMIT_INFINITY;
    }

    public int getPickLimit() {
        return mPickLimit;
    }

    public List<Long> getSelectedIds() {
        return mSelectedIds;
    }

    public Uri[] getSelectedUris() {
        Uri[] uris = new Uri[mSelectedIds.size()];
        for (int i = 0; i < mSelectedIds.size(); i++) {
            uris[i] = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(mSelectedIds.get(i)));
        }
        return uris;
    }

    public LruCache<Long, Bitmap> getThumbnailCache() {
        return mThumbnailCache;
    }

    public void toggleSelect(long id) {
        if (mSelectedIds.contains(id)) {
            mSelectedIds.remove(id);
        } else if (canInfinityPick() || getPickLimit() > mSelectedIds.size()){
            mSelectedIds.add(id);
        }
    }

}
