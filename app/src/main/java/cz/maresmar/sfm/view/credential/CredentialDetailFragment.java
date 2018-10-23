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

package cz.maresmar.sfm.view.credential;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

import java.util.concurrent.CountDownLatch;

import cz.maresmar.sfm.Assert;
import cz.maresmar.sfm.BuildConfig;
import cz.maresmar.sfm.R;
import cz.maresmar.sfm.plugin.ActionContract;
import cz.maresmar.sfm.provider.ProviderContract;
import cz.maresmar.sfm.view.DataForm;
import cz.maresmar.sfm.view.WithExtraFragment;
import timber.log.Timber;

/**
 * Fragment that shows one credential and allows to edit it.
 */
public class CredentialDetailFragment extends WithExtraFragment implements LoaderManager.LoaderCallbacks<Cursor>, DataForm {

    private static final int CREDENTIAL_LOADER_ID = 1;
    private static final int CREDENTIAL_GROUPS_LOADER_ID = 2;
    private static final int PORTAL_LOADER_ID = 3;

    private static final String ARG_START_WITH_EMPTY = "startWithEmpty";
    private static final String ARG_PORTAL_URI = "portalUri";
    private static final String ARG_CREDENTIAL_URI = "credentialUri";
    private static final String ARG_CREDENTIAL_TEMP_URI = "credentialTempUri";
    private static final String ARG_USER_PREFIX_URI = "userPrefixUri";
    private static final String ARG_PORTAL_GROUP_ID = "portalGroupId";
    private static final String ARG_LAST_PLUGIN = "lastPlugin";
    private static final String ARG_QUERY_URI = "queryUri";
    private static final String ARG_QUERY_SELECTION = "querySelection";

    // Local variables
    private Uri mCredentialUri;
    private Uri mCredentialTempUri;
    private Uri mUserPrefixUri;
    private Uri mPortalUri;
    private long mPortalGroupId;
    private String mLastPlugin = null;
    private boolean mLoadDataFromDb = true;

    // UI elements
    private EditText mNameText;
    private EditText mPasswordText;
    // Credential group
    private SimpleCursorAdapter mCredentialGroupAdapter;
    private Spinner mCredentialGroupSpinner;
    // Credit change notification
    private AppCompatCheckBox mCreditIncreaseNotificationCheckBox;
    private AppCompatCheckBox mLowCreditNotificationCheckBox;
    private EditText mLowCreditText;

    private CountDownLatch blockingLoaders = new CountDownLatch(1);

    /**
     * Create new fragment
     */
    public CredentialDetailFragment() {
        super(R.id.extras, ActionContract.FORMAT_TYPE_CREDENTIAL);
        // Required empty public constructor
    }

    /**
     * Create new empty credential fragment with default data
     * @param userPrefixUri User prefix Uri
     * @param portalUri Uri of one portal used with this credentials
     * @return New empty fragment
     */
    public static CredentialDetailFragment newEmptyInstance(@Nullable Uri userPrefixUri, @Nullable Uri portalUri) {
        CredentialDetailFragment fragment = new CredentialDetailFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_START_WITH_EMPTY, true);
        args.putParcelable(ARG_PORTAL_URI, portalUri);
        args.putParcelable(ARG_USER_PREFIX_URI, userPrefixUri);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Create credential from saved data
     * @param credentialUri Uri of saved credential
     * @return New fragment with saved data
     */
    public static CredentialDetailFragment newInstance(@NonNull Uri credentialUri) {
        CredentialDetailFragment fragment = new CredentialDetailFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_START_WITH_EMPTY, false);
        args.putParcelable(ARG_CREDENTIAL_URI, credentialUri);
        fragment.setArguments(args);
        return fragment;
    }

    // -------------------------------------------------------------------------------------------
    // Lifecycle events
    // -------------------------------------------------------------------------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            if (getArguments().getBoolean(ARG_START_WITH_EMPTY)) {
                mUserPrefixUri = getArguments().getParcelable(ARG_USER_PREFIX_URI);
                mPortalUri = getArguments().getParcelable(ARG_PORTAL_URI);
            } else {
                mCredentialUri = getArguments().getParcelable(ARG_CREDENTIAL_URI);

                getLoaderManager().initLoader(CREDENTIAL_LOADER_ID, null, this);
            }
        }

        if (savedInstanceState != null) {
            mCredentialUri = savedInstanceState.getParcelable(ARG_CREDENTIAL_URI);
            mCredentialTempUri = savedInstanceState.getParcelable(ARG_CREDENTIAL_TEMP_URI);
            mPortalUri = savedInstanceState.getParcelable(ARG_PORTAL_URI);
            mUserPrefixUri = savedInstanceState.getParcelable(ARG_USER_PREFIX_URI);
            mPortalGroupId = savedInstanceState.getLong(ARG_PORTAL_GROUP_ID);
            mLastPlugin = savedInstanceState.getString(ARG_LAST_PLUGIN);
            mLoadDataFromDb = false;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_credential_detail, container, false);

        mNameText = view.findViewById(R.id.nameText);
        mPasswordText = view.findViewById(R.id.passwordText);
        mCredentialGroupSpinner = view.findViewById(R.id.credentialGroupSpinner);

        mCreditIncreaseNotificationCheckBox = view.findViewById(R.id.increaseCheckBox);
        mLowCreditNotificationCheckBox = view.findViewById(R.id.lowCreditCheckBox);
        mLowCreditText = view.findViewById(R.id.lowCreditText);
        mLowCreditNotificationCheckBox.setOnCheckedChangeListener((buttonView, checked) ->
                mLowCreditText.setEnabled(checked)
        );

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mPortalUri != null) {
            Bundle args = new Bundle();
            args.putParcelable(ARG_QUERY_URI, mPortalUri);
            args.putString(ARG_QUERY_SELECTION, null);

            getLoaderManager().initLoader(PORTAL_LOADER_ID, args, this);
        }

        getLoaderManager().initLoader(CREDENTIAL_GROUPS_LOADER_ID, null, this);
    }

    // -------------------------------------------------------------------------------------------
    // UI save and restore
    // -------------------------------------------------------------------------------------------

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ARG_CREDENTIAL_URI, mCredentialUri);
        outState.putParcelable(ARG_CREDENTIAL_TEMP_URI, mCredentialTempUri);
        outState.putParcelable(ARG_PORTAL_URI, mPortalUri);
        outState.putParcelable(ARG_USER_PREFIX_URI, mUserPrefixUri);
        outState.putLong(ARG_PORTAL_GROUP_ID, mPortalGroupId);
        outState.putString(ARG_LAST_PLUGIN, mLastPlugin);
    }

    // -------------------------------------------------------------------------------------------
    // Data form manipulating methods
    // -------------------------------------------------------------------------------------------

    /**
     * Resets fragment to its default values
     * @param userPrefixUri User prefix uri
     * @param portalUri Uri of one portal used with this credentials
     */
    public void reset(@Nullable Uri userPrefixUri, @Nullable Uri portalUri) {
        // Delete old temp data
        discardTempData(getContext());

        // Loads new data
        mUserPrefixUri = userPrefixUri;
        mPortalUri = portalUri;
        if (mPortalUri != null) {
            Bundle args = new Bundle();
            args.putParcelable(ARG_QUERY_URI, mPortalUri);
            args.putString(ARG_QUERY_SELECTION, null);

            getLoaderManager().restartLoader(PORTAL_LOADER_ID, args, this);
        } else {
            mNameText.setText("");
            mPasswordText.setText("");
            setExtraData(null);
        }
    }

    // -------------------------------------------------------------------------------------------
    // Callbacks
    // -------------------------------------------------------------------------------------------

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        //noinspection ConstantConditions
        @NonNull
        Context context = getContext();
        switch (id) {
            case CREDENTIAL_LOADER_ID:
                return new CursorLoader(
                        context,
                        mCredentialUri,
                        new String[]{
                                ProviderContract.Credentials.USER_NAME,
                                ProviderContract.Credentials.USER_PASS,
                                ProviderContract.Credentials.CREDENTIALS_GROUP_ID,
                                ProviderContract.Credentials.EXTRA,
                                ProviderContract.Credentials.PORTAL_GROUP_ID,
                                ProviderContract.Credentials.FLAGS,
                                ProviderContract.Credentials.LOW_CREDIT_THRESHOLD
                        },
                        null,
                        null,
                        null
                );
            case CREDENTIAL_GROUPS_LOADER_ID:
                return new CursorLoader(
                        context,
                        ProviderContract.CredentialsGroup.getUri(),
                        new String[]{
                                ProviderContract.CredentialsGroup._ID,
                                ProviderContract.CredentialsGroup.NAME
                        },
                        null,
                        null,
                        null
                );
            case PORTAL_LOADER_ID:
                return new CursorLoader(
                        context,
                        args.getParcelable(ARG_QUERY_URI),
                        new String[]{
                                ProviderContract.Portal.PLUGIN,
                                ProviderContract.Portal.PORTAL_GROUP_ID,
                        },
                        args.getString(ARG_QUERY_SELECTION),
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
            case CREDENTIAL_LOADER_ID:
                Timber.d("Credential data loaded");

                cursor.moveToFirst();
                if (BuildConfig.DEBUG) {
                    Assert.isOne(cursor.getCount());
                }

                if (mPortalUri == null) {
                    mPortalGroupId = cursor.getLong(4);

                    String selection = ProviderContract.Portal.PORTAL_GROUP_ID + " = " + mPortalGroupId;
                    Uri uri = ProviderContract.Portal.getUri();

                    Bundle args = new Bundle();
                    args.putParcelable(ARG_QUERY_URI, uri);
                    args.putString(ARG_QUERY_SELECTION, selection);

                    getLoaderManager().initLoader(PORTAL_LOADER_ID, args, this);
                }

                if (mLoadDataFromDb) {
                    // Do not override changes on screen rotation
                    mNameText.setText(cursor.getString(0));
                    mPasswordText.setText(cursor.getString(1));
                    long credentialGroupId = cursor.getLong(2);
                    setExtraData(cursor.getString(3));

                    @ProviderContract.CredentialFlags int flags = cursor.getInt(5);
                    boolean increaseNotification =
                            !((flags & ProviderContract.CREDENTIAL_FLAG_DISABLE_CREDIT_INCREASE_NOTIFICATION)
                                    == ProviderContract.CREDENTIAL_FLAG_DISABLE_CREDIT_INCREASE_NOTIFICATION);
                    mCreditIncreaseNotificationCheckBox.setChecked(increaseNotification);
                    boolean lowCreditNotification =
                            !((flags & ProviderContract.CREDENTIAL_FLAG_DISABLE_LOW_CREDIT_NOTIFICATION)
                                    == ProviderContract.CREDENTIAL_FLAG_DISABLE_LOW_CREDIT_NOTIFICATION);
                    mLowCreditNotificationCheckBox.setChecked(lowCreditNotification);
                    mLowCreditText.setText(String.valueOf(cursor.getInt(6)));

                    // Set credential group Id
                    AsyncTask.execute(() -> {
                        try {
                            blockingLoaders.await();
                        } catch (InterruptedException e) {
                            Timber.e(e);
                        }
                        getActivity().runOnUiThread(() -> setCredentialGroupId(credentialGroupId));
                    });
                    mLoadDataFromDb = false;
                }
                break;
            case CREDENTIAL_GROUPS_LOADER_ID:
                Timber.d("Credential group data loaded");

                mCredentialGroupAdapter = new SimpleCursorAdapter(getContext(), R.layout.support_simple_spinner_dropdown_item, cursor,
                        new String[]{ProviderContract.CredentialsGroup.NAME}, new int[]{android.R.id.text1}, 0);
                mCredentialGroupSpinner.setAdapter(mCredentialGroupAdapter);
                blockingLoaders.countDown();
                break;
            case PORTAL_LOADER_ID:
                Timber.d("Portal data loaded");

                cursor.moveToFirst();
                if (BuildConfig.DEBUG) {
                    Assert.isOne(cursor.getCount());
                }

                String pluginId = cursor.getString(0);
                requestExtraFormat(pluginId);
                if(mLastPlugin != null && !mLastPlugin.equals(pluginId)) {
                    setExtraData(null);
                }
                mLastPlugin = pluginId;

                mPortalGroupId = cursor.getLong(1);
                break;
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + loader.getId());
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        switch (loader.getId()) {
            case CREDENTIAL_LOADER_ID:
                Timber.e("Credential data %s is no longer valid", mCredentialUri);
                // Let's tread current user data as new entry
                reset(null, null);
                break;
            case CREDENTIAL_GROUPS_LOADER_ID:
                Timber.w("Credentials group loader reset called");
                mCredentialGroupAdapter.swapCursor(null);
                break;
            case PORTAL_LOADER_ID:
                Timber.w("Portal loader reset called");
                break;
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + loader.getId());
        }
    }

    // -------------------------------------------------------------------------------------------
    // UI handling
    // -------------------------------------------------------------------------------------------

    private void setCredentialGroupId(long credentialGroupId) {
        // Parse selected id
        int position = -1;
        // Find position for id
        // There are only few entries in adapter so its ok to do it with for cycle
        for (int i = 0; i < mCredentialGroupAdapter.getCount(); i++) {
            if (mCredentialGroupAdapter.getItemId(i) == credentialGroupId) {
                position = i;
            }
        }
        if (BuildConfig.DEBUG) {
            Assert.that(position >= 0, "Illegal credential group id value %d", credentialGroupId);
        }
        // Set it in UI
        mCredentialGroupSpinner.setSelection(position);
    }

    // -------------------------------------------------------------------------------------------
    // Data form events
    // -------------------------------------------------------------------------------------------

    @Nullable
    @Override
    public Uri saveData() {
        Timber.i("Saving credential data");

        // Defines an object to contain the new values to insert
        ContentValues values = new ContentValues();

        /*
         * Sets the values of each column and inserts the word. The arguments to the "put"
         * method are "column name" and "value"
         */
        values.put(ProviderContract.Credentials.CREDENTIALS_GROUP_ID, mCredentialGroupSpinner.getSelectedItemId());
        values.put(ProviderContract.Credentials.PORTAL_GROUP_ID, mPortalGroupId);
        // User will be added from user prefix
        values.put(ProviderContract.Credentials.USER_NAME, mNameText.getText().toString());
        values.put(ProviderContract.Credentials.USER_PASS, mPasswordText.getText().toString());
        values.put(ProviderContract.Credentials.EXTRA, getExtraData());
        // Notifications
        @ProviderContract.CredentialFlags int flags = 0;
        if (!mCreditIncreaseNotificationCheckBox.isChecked()) {
            flags |= ProviderContract.CREDENTIAL_FLAG_DISABLE_CREDIT_INCREASE_NOTIFICATION;
        }
        if (!mLowCreditNotificationCheckBox.isChecked()) {
            flags |= ProviderContract.CREDENTIAL_FLAG_DISABLE_LOW_CREDIT_NOTIFICATION;
        }
        values.put(ProviderContract.Credentials.FLAGS, flags);
        values.put(ProviderContract.Credentials.LOW_CREDIT_THRESHOLD, mLowCreditText.getText().toString());

        //noinspection ConstantConditions
        ContentResolver contentResolver = getContext().getContentResolver();
        if (mCredentialUri == null) {
            Uri credentialUri = Uri.withAppendedPath(mUserPrefixUri, ProviderContract.CREDENTIALS_PATH);
            mCredentialTempUri = contentResolver.insert(credentialUri, values);
            mCredentialUri = mCredentialTempUri;
        } else {
            int updatedRows = contentResolver.update(mCredentialUri, values, null, null);
            if (BuildConfig.DEBUG) {
                Assert.isOne(updatedRows);
            }
        }
        return mCredentialUri;
    }

    @WorkerThread
    @Override
    public void discardTempData(@NonNull Context context) {
        Timber.i("Discarding credential data");

        if (mCredentialTempUri != null) {
            // Disable using of temp data
            mCredentialUri = null;

            // Delete credential temp data
            int affectedRows = context.getContentResolver().
                    delete(mCredentialTempUri, null, null);
            if (BuildConfig.DEBUG) {
                Assert.isOne(affectedRows);
            }
            mCredentialTempUri = null;
        }
    }

    @Override
    public boolean hasValidData() {
        boolean isValid = true;

        if (!hasValidExtraData()) {
            isValid = false;
        }

        if (!isValid) {
            //noinspection ConstantConditions
            Snackbar.make(getView(), R.string.extra_invalid_input_error, Snackbar.LENGTH_LONG)
                    .setAction(android.R.string.ok, view -> {
                        // Only dismiss message
                    })
                    .show();
        }

        return isValid;
    }
}