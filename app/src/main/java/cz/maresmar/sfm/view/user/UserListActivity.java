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

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import java.io.File;

import cz.maresmar.sfm.BuildConfig;
import cz.maresmar.sfm.view.CursorRecyclerViewAdapter;
import cz.maresmar.sfm.Assert;
import cz.maresmar.sfm.R;
import cz.maresmar.sfm.provider.ProviderContract;
import de.hdodenhof.circleimageview.CircleImageView;
import timber.log.Timber;

/**
 * An activity representing a list of Users.
 * <p>
 *     This activity has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link UserDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.</p>
 */
public class UserListActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int USERS_LOADER_ID = 1;

    public static String ARG_USER_URI = UserDetailFragment.ARG_USER_URI;

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    private UserRecyclerViewAdapter mAdapter;

    // -------------------------------------------------------------------------------------------
    // Lifecycle events
    // -------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        if (findViewById(R.id.user_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        // Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Fab
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.main_discard_fab);
        if (mTwoPane) {
            AppCompatActivity sourceActivity = this;
            fab.setOnClickListener(view -> {
                UserDetailFragment userDetailFragment = (UserDetailFragment) getSupportFragmentManager().
                        findFragmentById(R.id.user_detail_container);
                if (userDetailFragment.hasValidData()) {
                    userDetailFragment.saveData();
                }
            });
        } else {
            fab.setVisibility(View.GONE);
        }

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        View recyclerView = findViewById(R.id.user_list);
        setupRecyclerView((RecyclerView) recyclerView);

        if (Intent.ACTION_EDIT.equals(getIntent().getAction())) {
            Uri userUri = getIntent().getData();
            showUserDetail(userUri);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Timber.d("User list loader started");
        getSupportLoaderManager().initLoader(USERS_LOADER_ID, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_user_list, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_add:
                showUserDetail(null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.user_list) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.toolbar_user_list_popup, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.user_edit:
                long itemId = (long) info.id;
                Uri userUri = ContentUris.withAppendedId(ProviderContract.User.getUri(), itemId);
                return true;
            case R.id.user_delete:
                // remove stuff here
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        mAdapter = new UserRecyclerViewAdapter(this, null);
        recyclerView.setAdapter(mAdapter);
    }

    // -------------------------------------------------------------------------------------------
    // Callbacks
    // -------------------------------------------------------------------------------------------

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case USERS_LOADER_ID:
                return new CursorLoader(
                        this,
                        ProviderContract.User.getUri(),
                        new String[]{
                                ProviderContract.User._ID,
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
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case USERS_LOADER_ID:
                Timber.d("User list loader finished");
                mAdapter.swapCursor(data);
                break;
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + loader.getId());
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        switch (loader.getId()) {
            case USERS_LOADER_ID:
                mAdapter.swapCursor(null);
                break;
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + loader.getId());
        }
    }


    private void showUserDetail(@Nullable Uri userUri) {
        if (mTwoPane) {
            UserDetailFragment fragment = UserDetailFragment.newInstance(userUri);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.user_detail_container, fragment)
                    .commit();
        } else {
            Intent intent = new Intent(this, UserDetailActivity.class);
            intent.setData(userUri);
            startActivity(intent);
        }
    }

    private void deleteUser(@NonNull Uri userUri) {
        AsyncTask.execute(() -> {
            // Delete image
            try (Cursor cursor = getContentResolver().query(userUri, new String[]{ProviderContract.User.PICTURE}, null, null, null)) {
                int foundRows = cursor.getColumnCount();
                if (BuildConfig.DEBUG) {
                    Assert.isOne(foundRows);
                }

                cursor.moveToNext();
                Uri imageUri = Uri.parse(cursor.getString(0));
                File imageFile = new File(imageUri.getPath());
                boolean delResult = imageFile.delete();
                if (BuildConfig.DEBUG) {
                    Assert.that(delResult, "Deleting wasn't successful");
                }
            }
            // Delete entry from db
            int deletedRows = getContentResolver().delete(userUri, null, null);
            if (BuildConfig.DEBUG) {
                Assert.isOne(deletedRows);
            }
        });
    }

    // -------------------------------------------------------------------------------------------
    // Recycler View
    // -------------------------------------------------------------------------------------------

    /**
     * {@link RecyclerView} adapter that provides users
     */
    public static class UserRecyclerViewAdapter
            extends CursorRecyclerViewAdapter<UserRecyclerViewAdapter.ViewHolder> {

        private final UserListActivity mParentActivity;
        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long itemId = (long) view.getTag();
                Uri userUri = ContentUris.withAppendedId(ProviderContract.User.getUri(), itemId);

                mParentActivity.showUserDetail(userUri);
            }
        };
        private final View.OnLongClickListener mOnLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                // Prepare data
                long itemId = (long) view.getTag();
                Uri userUri = ContentUris.withAppendedId(ProviderContract.User.getUri(), itemId);
                // creating a popup menu
                PopupMenu popup = new PopupMenu(mParentActivity, view);
                // inflating menu from xml resource
                popup.inflate(R.menu.toolbar_user_list_popup);
                // adding click listener
                popup.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case R.id.user_edit:
                            mParentActivity.showUserDetail(userUri);
                            return true;
                        case R.id.user_delete:
                            mParentActivity.deleteUser(userUri);
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

        /**
         * Creates new adapter
         * @param parent Parent activity
         * @param cursor Cursor that contains users data
         */
        UserRecyclerViewAdapter(UserListActivity parent, Cursor cursor) {
            super(cursor);
            mParentActivity = parent;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_user, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor) {
            viewHolder.itemView.setTag(viewHolder.getItemId());
            viewHolder.mUserName.setText(cursor.getString(1));
            viewHolder.mUserImage.setImageURI(Uri.parse(cursor.getString(2)));

            viewHolder.itemView.setOnClickListener(mOnClickListener);
            viewHolder.itemView.setOnLongClickListener(mOnLongClickListener);
        }

        /**
         * Standard {@link RecyclerView.ViewHolder} that stores one user entry in UI
         */
        class ViewHolder extends RecyclerView.ViewHolder {
            final CircleImageView mUserImage;
            final TextView mUserName;

            ViewHolder(View view) {
                super(view);
                mUserImage = (CircleImageView) view.findViewById(R.id.user_image);
                mUserName = (TextView) view.findViewById(R.id.user_name);
            }
        }
    }
}
