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

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import cz.maresmar.sfm.BuildConfig;
import cz.maresmar.sfm.view.CursorRecyclerViewAdapter;
import cz.maresmar.sfm.Assert;
import cz.maresmar.sfm.R;
import cz.maresmar.sfm.provider.ProviderContract;
import cz.maresmar.sfm.view.portal.PortalPickerActivity;
import timber.log.Timber;

/**
 * Activity where user could show existing credential, edit them or add new
 */
public class LoginListActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOG_DATA_LOADER_ID = 1;

    private static final int PORTAL_PICKER_REQUEST = 2;

    private CredentialRecyclerViewAdapter mAdapter;
    private Uri mUserUri;
    private Uri mPortalsUri;

    private RecyclerView mRecyclerView;
    private View mEmptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_list);

        mUserUri = getIntent().getData();
        mPortalsUri = Uri.withAppendedPath(mUserUri, ProviderContract.PORTAL_PATH);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mRecyclerView = findViewById(R.id.credential_list);
        setupRecyclerView(mRecyclerView);

        FloatingActionButton fab = findViewById(R.id.main_discard_fab);
        fab.setOnClickListener((View v) -> startPortalPickerActivity());

        // Prepare empty view
        mEmptyView = findViewById(R.id.empty_content);
        ((TextView) findViewById(R.id.empty_description_text)).setText(R.string.empty_try_to_add_credential);

        String intentAction = getIntent().getAction();
        if (intentAction == null) {
            intentAction = "null";
        }
        switch (intentAction) {
            case Intent.ACTION_VIEW:
                break;
            case Intent.ACTION_INSERT:
                startPortalPickerActivity();
                break;
            default:
                throw new UnsupportedOperationException("Unknown intent action " + intentAction);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Timber.d("Credential list loader started");
        getSupportLoaderManager().initLoader(LOG_DATA_LOADER_ID, null, this);
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        mAdapter = new CredentialRecyclerViewAdapter(this);
        recyclerView.setAdapter(mAdapter);
    }

    // -------------------------------------------------------------------------------------------
    // Callbacks
    // -------------------------------------------------------------------------------------------

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOG_DATA_LOADER_ID:
                return new CursorLoader(
                        this,
                        mPortalsUri,
                        new String[]{
                                ProviderContract.Portal._ID,
                                ProviderContract.Portal.CREDENTIAL_ID,
                                ProviderContract.Portal.NAME,
                                ProviderContract.Portal.CREDENTIAL_NAME
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
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case LOG_DATA_LOADER_ID:
                Timber.d("Credential list loader finished");

                // Empty state
                if (data.getCount() > 0) {
                    mEmptyView.setVisibility(View.GONE);
                    mRecyclerView.setVisibility(View.VISIBLE);
                } else {
                    mRecyclerView.setVisibility(View.GONE);
                    mEmptyView.setVisibility(View.VISIBLE);
                }

                mAdapter.swapCursor(data);
                break;
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + loader.getId());
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOG_DATA_LOADER_ID:
                mAdapter.swapCursor(null);
                break;
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + loader.getId());
        }
    }

    // -------------------------------------------------------------------------------------------
    // List events
    // -------------------------------------------------------------------------------------------

    private void editCredentials(Uri portalUri, Uri credentialUri) {
        Timber.i("Starting edit credentials LoginDetailActivity on portal %s " +
                "with credentials %s", portalUri, credentialUri);

        startCredentialDetailActivity(portalUri, credentialUri, LoginDetailActivity.CREDENTIAL_TAB);
    }

    private void editPortal(Uri portalUri, Uri credentialUri) {
        Timber.i("Starting edit portal LoginDetailActivity on portal %s " +
                "with credentials %s", portalUri, credentialUri);

        startCredentialDetailActivity(portalUri, credentialUri, LoginDetailActivity.PORTAL_TAB);
    }

    private void deleteCredentials(Uri portalUri, Uri credentialUri) {
        AsyncTask.execute(() -> {
            int deletedRows = getContentResolver().delete(credentialUri, null, null);
            if (BuildConfig.DEBUG) {
                Assert.isOne(deletedRows);
            }
        });
    }

    // -------------------------------------------------------------------------------------------
    // Activities events
    // -------------------------------------------------------------------------------------------

    private void startCredentialDetailActivity(Uri portalUri, Uri credentialUri, int tabId) {

        Intent intent = new Intent(this, LoginDetailActivity.class);
        intent.setData(mUserUri);
        intent.putExtra(LoginDetailActivity.PORTAL_URI, portalUri);
        intent.putExtra(LoginDetailActivity.CREDENTIAL_URI, credentialUri);
        intent.putExtra(LoginDetailActivity.TAB_ID, tabId);
        startActivity(intent);
    }

    private void startPortalPickerActivity() {
        Intent intent = new Intent(this, PortalPickerActivity.class);
        startActivityForResult(intent, PORTAL_PICKER_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PORTAL_PICKER_REQUEST && resultCode == Activity.RESULT_OK) {
            Uri portalUri = data.getData();

            if (portalUri != null) {
                long portalId = ContentUris.parseId(portalUri);

                // Try to find credentials with this portal
                try (Cursor cursor = getContentResolver().query(
                        mPortalsUri,
                        new String[]{ProviderContract.Portal.CREDENTIAL_ID},
                        ProviderContract.Portal.PORTAL_ID + " == " + portalId,
                        null,
                        null)) {

                    // Has already data
                    if (cursor != null && cursor.getCount() > 0) {
                        cursor.moveToFirst();

                        long credentialId = cursor.getLong(0);
                        Uri credentialUri = ContentUris.withAppendedId(
                                Uri.withAppendedPath(mUserUri, ProviderContract.CREDENTIALS_PATH),
                                credentialId
                        );
                        startCredentialDetailActivity(portalUri, credentialUri, LoginDetailActivity.CREDENTIAL_TAB);
                    } else {
                        startCredentialDetailActivity(portalUri, null, LoginDetailActivity.CREDENTIAL_TAB);
                    }
                }
            } else {
                // Create new portal
                startCredentialDetailActivity(portalUri, null, LoginDetailActivity.PORTAL_TAB);
            }
        }
    }

    // -------------------------------------------------------------------------------------------
    // Recycler View
    // -------------------------------------------------------------------------------------------

    private class CredentialRecyclerViewAdapter
            extends CursorRecyclerViewAdapter<CredentialRecyclerViewAdapter.ViewHolder> {

        private final LoginListActivity mParentActivity;

        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Prepare data
                ViewIds ids = (ViewIds) view.getTag();

                Uri portalUri = ContentUris.withAppendedId(ProviderContract.Portal.getUri(), ids.portalId);
                Uri credentialUri = Uri.withAppendedPath(mUserUri, ProviderContract.CREDENTIALS_PATH +
                        "/" + ids.credentialId);

                mParentActivity.editCredentials(portalUri, credentialUri);
            }
        };
        private final View.OnLongClickListener mOnLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                // Prepare data
                ViewIds ids = (ViewIds) view.getTag();

                Uri portalUri = ContentUris.withAppendedId(ProviderContract.Portal.getUri(), ids.portalId);
                Uri credentialUri = Uri.withAppendedPath(mUserUri, ProviderContract.CREDENTIALS_PATH +
                        "/" + ids.credentialId);

                // creating a popup menu
                PopupMenu popup = new PopupMenu(mParentActivity, view);
                // inflating menu from xml resource
                popup.inflate(R.menu.popup_credential_list);
                // adding click listener
                popup.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case R.id.credential_edit_credential:
                            mParentActivity.editCredentials(portalUri, credentialUri);
                            return true;
                        case R.id.credential_edit_portal:
                            mParentActivity.editPortal(portalUri, credentialUri);
                            return true;
                        case R.id.credential_delete:
                            mParentActivity.deleteCredentials(portalUri, credentialUri);
                            return true;
                        default:
                            return false;
                    }
                });
                //displaying the popup
                popup.show();
                return true;
            }
        };

        private CredentialRecyclerViewAdapter(LoginListActivity parent) {
            super(null);
            mParentActivity = parent;
        }

        @NonNull
        @Override
        public CredentialRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_credential, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor) {
            // IDs
            ViewIds ids = new ViewIds(cursor.getLong(0), cursor.getLong(1));
            viewHolder.itemView.setTag(ids);
            // Portal name
            viewHolder.mPortalGroupName.setText(cursor.getString(2));
            // User name
            String userName = cursor.getString(3);
            if (!TextUtils.isEmpty(userName)) {
                viewHolder.mUserName.setText(userName);
            } else {
                viewHolder.mUserName.setText(R.string.credential_list_without_credential);
            }

            viewHolder.itemView.setOnClickListener(mOnClickListener);
            viewHolder.itemView.setOnLongClickListener(mOnLongClickListener);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView mPortalGroupName;
            final TextView mUserName;

            ViewHolder(View view) {
                super(view);
                mPortalGroupName = view.findViewById(R.id.portal_group_name_text);
                mUserName = view.findViewById(R.id.user_name_text);
            }
        }

        class ViewIds {
            final long portalId;
            final long credentialId;

            public ViewIds(long portalId, long credentialId) {
                this.portalId = portalId;
                this.credentialId = credentialId;
            }
        }
    }
}
