/*
 * SmartFoodMenu - Android application for canteens extendable with plugins
 *
 * Copyright © 2016-2018  Martin Mareš <mmrmartin[at]gmail[dot]com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cz.maresmar.sfm.view.user;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.UUID;

import cz.maresmar.sfm.Assert;
import cz.maresmar.sfm.BuildConfig;
import cz.maresmar.sfm.R;
import cz.maresmar.sfm.provider.ProviderContract;
import cz.maresmar.sfm.view.DataForm;
import de.hdodenhof.circleimageview.CircleImageView;
import timber.log.Timber;

/**
 * Fragment used for crating and editing users
 */
public class UserDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, DataForm {

    private static final int USER_LOADER_ID = 1;

    private static final int PICK_PROFILE_PICTURE = 10000;
    private static final int CROP_PROFILE_PICTURE = 10001;

    static final String ARG_USER_URI = "userUri";
    private static final String ARG_USER_TEMP_URI = "userTempUri";
    private static final String ARG_PICTURE_URI = "pictureUri";
    private static final String ARG_PICTURE_BITMAP = "pictureBitmap";
    private static final String ARG_GENERATE_BITMAP = "generateBitmap";

    // UI elements
    CircleImageView mProfileImageButton;
    EditText mNameText;
    boolean mLoadDataFromDb = true;

    // Local state
    Uri mUserUri;
    Uri mUserTempUri;

    Uri mPictureUri;
    Bitmap mPictureBitmap;
    boolean mGenerateBitmap = true;

    /**
     * Creates new fragment empty fragment that can be used for creating of new user
     *
     * @return A new instance of this fragment
     */
    public static UserDetailFragment newEmptyInstance() {
        return newInstance(null);
    }

    /**
     * Creates new fragment with selected user
     *
     * @param userUri User Uri or {@code null} if new user will be created
     * @return A new instance of this fragment
     */
    public static UserDetailFragment newInstance(@Nullable Uri userUri) {
        UserDetailFragment fragment = new UserDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_USER_URI, userUri);
        fragment.setArguments(args);
        return fragment;
    }

    // -------------------------------------------------------------------------------------------
    // Activity lifecycle
    // -------------------------------------------------------------------------------------------


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mUserUri = getArguments().getParcelable(ARG_USER_URI);
        }

        if (savedInstanceState != null) {
            mUserUri = savedInstanceState.getParcelable(ARG_USER_URI);
            mUserTempUri = savedInstanceState.getParcelable(ARG_USER_TEMP_URI);
            mPictureUri = savedInstanceState.getParcelable(ARG_PICTURE_URI);
            mPictureBitmap = savedInstanceState.getParcelable(ARG_PICTURE_BITMAP);
            mGenerateBitmap = savedInstanceState.getBoolean(ARG_GENERATE_BITMAP);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_user_detail, container, false);

        // Profile image
        mProfileImageButton = view.findViewById(R.id.profileImageButton);
        mProfileImageButton.setOnClickListener(view1 -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, getString(R.string.user_pick_picture_button)), PICK_PROFILE_PICTURE);
        });

        // User name
        mNameText = view.findViewById(R.id.nameText);
        mNameText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mGenerateBitmap) {
                    setDefaultProfilePicture();
                }
            }
        });

        setProfilePicture(mPictureBitmap);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Loads portal data from DB
        if (mUserUri != null) {
            getLoaderManager().initLoader(USER_LOADER_ID, null, this);
        }
    }

    // -------------------------------------------------------------------------------------------
    // UI save and restore
    // -------------------------------------------------------------------------------------------

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(ARG_USER_URI, mUserUri);
        outState.putParcelable(ARG_USER_TEMP_URI, mUserTempUri);
        outState.putParcelable(ARG_PICTURE_URI, mPictureUri);
        outState.putParcelable(ARG_PICTURE_BITMAP, mPictureBitmap);
        outState.putBoolean(ARG_GENERATE_BITMAP, mGenerateBitmap);
    }

    // -------------------------------------------------------------------------------------------
    // Callbacks
    // -------------------------------------------------------------------------------------------

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case PICK_PROFILE_PICTURE:
                if (resultCode == Activity.RESULT_OK) {
                    Timber.i("Profile picture selected");

                    // Try to crop image (not every Android support it)
                    try {
                        Intent intent = new Intent("com.android.camera.action.CROP");
                        intent.setDataAndType(data.getData(), data.getType()); // Push selected picture to crop
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.putExtra("crop", "true");
                        intent.putExtra("aspectX", 1);
                        intent.putExtra("aspectY", 1);
                        intent.putExtra("return-data", true);
                        intent.putExtra("finishActivityOnSaveCompleted", true);
                        startActivityForResult(Intent.createChooser(intent, getString(R.string.user_crop_picture_dialog)), CROP_PROFILE_PICTURE);
                    } catch (ActivityNotFoundException e) {
                        Timber.w(e, "No cropping activity found");
                    }

                    // Save selected image
                    new PictureLoaderAsyncTask(this).execute(data.getData());
                }
                break;
            case CROP_PROFILE_PICTURE:
                if (resultCode == Activity.RESULT_OK && data.getExtras() != null) {
                    Bitmap resBitmap = data.getExtras().getParcelable("data");
                    if (resBitmap != null) {
                        Timber.i("User picture cropped");
                        new PictureCropperAsyncTask(this).execute(resBitmap);
                        break;
                    }
                }
                Timber.w("Cropping wasn't successful (result code: %d)", requestCode);
                break;
            default:
                throw new UnsupportedOperationException("Unknown action request " + requestCode);
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case USER_LOADER_ID:
                //noinspection ConstantConditions
                return new CursorLoader(
                        getContext(),
                        mUserUri,
                        new String[]{
                                ProviderContract.User.NAME,
                                ProviderContract.User.PICTURE
                        },
                        null,
                        null,
                        null
                );
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + id);
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case USER_LOADER_ID:
                if(mLoadDataFromDb) {
                    Timber.d("User data loaded");

                    cursor.moveToFirst();
                    if (BuildConfig.DEBUG) {
                        Assert.isOne(cursor.getCount());
                    }

                    // User name
                    mNameText.setText(cursor.getString(0));
                    // Picture
                    mPictureUri = Uri.parse(cursor.getString(1));
                    new PictureLoaderAsyncTask(this).execute(mPictureUri);

                    mLoadDataFromDb = false;
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + loader.getId());
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        switch (loader.getId()) {
            case USER_LOADER_ID:
                Timber.e("User data %s is no longer valid", mUserUri);
                // Let's tread current user data as new entry
                reset(null);
                break;
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + loader.getId());
        }
    }

    // -------------------------------------------------------------------------------------------
    // UI events
    // -------------------------------------------------------------------------------------------

    @UiThread
    private void setProfilePicture(Bitmap bitmap) {
        mPictureBitmap = bitmap;
        if (bitmap != null) {
            mGenerateBitmap = false;
            mProfileImageButton.setImageBitmap(mPictureBitmap);
        } else {
            mGenerateBitmap = true;
            setDefaultProfilePicture();
        }
    }

    /**
     * Create new profile picture using first letter of name
     */
    private void setDefaultProfilePicture() {
        if (mNameText.getText().length() > 0) {
            Drawable drawable = generateDefaultPicture();
            mProfileImageButton.setImageDrawable(drawable);
            mPictureBitmap = getBitmap(drawable);
        }
    }

    private Drawable generateDefaultPicture() {
        ColorGenerator generator = ColorGenerator.MATERIAL;
        int color = generator.getColor(mNameText.getText());
        String firstLetter = "" + mNameText.getText().charAt(0);
        return TextDrawable.builder()
                .beginConfig()
                .width(getResources().getDimensionPixelSize(R.dimen.user_image_size))  // width in px
                .height(getResources().getDimensionPixelSize(R.dimen.user_image_size)) // height in px
                .endConfig()
                .buildRound(firstLetter, color);
    }

    // -------------------------------------------------------------------------------------------
    // Data form manipulating methods
    // -------------------------------------------------------------------------------------------

    /**
     * Changes user and restore fragments UI to default values
     * @param userUri User Uri or {@code null} if new user will be created
     */
    public void reset(@Nullable Uri userUri) {
        // Delete old temp data
        if (userUri != mUserUri) {
            discardTempData(getContext());
        }

        // Loads new data
        mUserUri = userUri;
        if (mUserUri != null) {
            getLoaderManager().restartLoader(USER_LOADER_ID, null, this);
        } else {
            setProfilePicture(null);
            mNameText.setText("");
        }
    }

    /**
     * Transform any Drawable to bitmap
     * <p>
     * taken from https://stackoverflow.com/a/35574775/1392034
     *
     * @param drawable Input Drawable
     * @return Transformed Bitmap
     */
    private Bitmap getBitmap(@NonNull Drawable drawable) {
        Canvas canvas = new Canvas();
        int imageSize = getResources().getDimensionPixelSize(R.dimen.user_image_size);
        Bitmap bitmap = Bitmap.createBitmap(imageSize, imageSize, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, imageSize, imageSize);
        drawable.draw(canvas);

        return bitmap;
    }

    // -------------------------------------------------------------------------------------------
    // Data form events
    // -------------------------------------------------------------------------------------------

    @Override
    public boolean hasValidData() {
        boolean valid = true;

        // Test user name
        if (TextUtils.isEmpty(mNameText.getText())) {
            mNameText.setError(getString(R.string.user_empty_name_error));
            Snackbar.make(getView(), R.string.user_empty_name_error, Snackbar.LENGTH_LONG)
                    .setAction(R.string.user_pick_random_name_action, view -> mNameText.setText(R.string.user_random_name_value))
                    .show();
            valid = false;
        } else {
            mNameText.setError(null);
        }

        return valid;
    }

    @Override
    public void discardTempData(@NonNull Context context) {
        Timber.i("Discarding user data");

        if (mUserTempUri != null) {
            int affectedRows = context.getContentResolver().
                    delete(mUserTempUri, null, null);
            if (BuildConfig.DEBUG) {
                Assert.isOne(affectedRows);
            }
            mUserTempUri = null;

            File file = new File(mPictureUri.getPath());
            boolean deleteResult = file.delete();
            if (BuildConfig.DEBUG) {
                Assert.that(deleteResult, "Delete of %s wasn't successful", mPictureUri);
            }
            mPictureUri = null;

            mUserUri = null;
        }
    }

    @Override
    public Uri saveData() {
        Timber.i("Saving user data");

        // Delete old image
        if (mPictureUri != null) {
            File file = new File(mPictureUri.getPath());
            boolean deleteResult = file.delete();
            if (BuildConfig.DEBUG) {
                Assert.that(deleteResult, "Delete of %s wasn't successful", mPictureUri);
            }
        }

        // Saves new one
        File outputDir = getContext().getFilesDir(); // context being the Activity pointer
        File outputFile = new File(outputDir, "profile_" + UUID.randomUUID().toString() + ".jpg");

        try (OutputStream outputStream = new FileOutputStream(outputFile)) {
            mPictureBitmap.compress(Bitmap.CompressFormat.PNG, 95, outputStream);
        } catch (IOException ex) {
            Timber.e(ex, "Profile picture cannot been saved");
            ex.printStackTrace();
        }

        mPictureUri = Uri.fromFile(outputFile);

        // Defines an object to contain the new values to insert
        ContentValues values = new ContentValues();
        values.put(ProviderContract.User.NAME, mNameText.getText().toString());
        values.put(ProviderContract.User.PICTURE, mPictureUri.toString());

        if (mUserUri == null) {
            mUserTempUri = getContext().getContentResolver().insert(ProviderContract.User.getUri(),
                    values);
            mUserUri = mUserTempUri;
        } else {
            getContext().getContentResolver().update(mUserUri, values, null, null);
        }
        return mUserUri;
    }

    // -------------------------------------------------------------------------------------------
    // Async tasks
    // -------------------------------------------------------------------------------------------

    /**
     * Loads user's picture from filesystem
     */
    private static class PictureLoaderAsyncTask extends AsyncTask<Uri, Void, Bitmap> {

        WeakReference<UserDetailFragment> mFragmentRef;

        private PictureLoaderAsyncTask(UserDetailFragment userDetailFragment) {
            mFragmentRef = new WeakReference<>(userDetailFragment);
        }

        @Override
        protected Bitmap doInBackground(Uri... uris) {
            Uri pictureUri = uris[0];
            Timber.i("Loading profile image %s", pictureUri);
            try {
                int bitmapMaxSize = mFragmentRef.get().getResources().getDimensionPixelSize(R.dimen.user_image_size);
                Bitmap orgBitmap = MediaStore.Images.Media.getBitmap(mFragmentRef.get().getContext().getContentResolver(),
                        pictureUri);
                if (orgBitmap.getWidth() > bitmapMaxSize || orgBitmap.getWidth() != orgBitmap.getHeight()) {
                    return ThumbnailUtils.extractThumbnail(orgBitmap, bitmapMaxSize, bitmapMaxSize, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
                } else {
                    return orgBitmap;
                }
            } catch (IOException e) {
                Timber.e(e, "Profile image cannot be loaded");
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            Timber.d("Profile image loaded");

            mFragmentRef.get().setProfilePicture(bitmap);
        }
    }

    /**
     * Crop {@link Bitmap} to maximal user image's size
     */
    private static class PictureCropperAsyncTask extends AsyncTask<Bitmap, Void, Bitmap> {

        WeakReference<UserDetailFragment> mFragmentRef;

        private PictureCropperAsyncTask(UserDetailFragment userDetailFragment) {
            mFragmentRef = new WeakReference<>(userDetailFragment);
        }

        @Override
        protected Bitmap doInBackground(Bitmap... bitmaps) {
            Timber.i("Cropping profile image");
            Bitmap orgBitmap = bitmaps[0];
            int bitmapMaxSize = mFragmentRef.get().getResources().getDimensionPixelSize(R.dimen.user_image_size);
            if (orgBitmap.getWidth() > bitmapMaxSize || orgBitmap.getWidth() != orgBitmap.getHeight()) {
                return ThumbnailUtils.extractThumbnail(orgBitmap, bitmapMaxSize, bitmapMaxSize, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
            } else {
                return orgBitmap;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            mFragmentRef.get().setProfilePicture(bitmap);
        }
    }
}
