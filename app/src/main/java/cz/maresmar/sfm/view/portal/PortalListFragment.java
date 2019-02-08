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

package cz.maresmar.sfm.view.portal;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import cz.maresmar.sfm.Assert;
import cz.maresmar.sfm.BuildConfig;
import cz.maresmar.sfm.R;
import cz.maresmar.sfm.app.SettingsContract;
import cz.maresmar.sfm.provider.ProviderContract;
import cz.maresmar.sfm.service.web.PortalsUpdateService;
import cz.maresmar.sfm.view.CursorRecyclerViewAdapter;
import timber.log.Timber;

/**
 * A fragment that shows all supported portals and place where an user could add new portal
 * <p>
 * Activity that uses this fragment must implement {@link OnPortalSelectedListener} interface to handle
 * interaction events.</p>
 */
public class PortalListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int PORTALS_LOADER_ID = 0;

    // This is the Adapter being used to display the list's data.
    private PortalListRecyclerViewAdapter mAdapter;

    // UI elements
    private SwipeRefreshLayout mSwipeRefreshLayout;

    // Internal state
    private boolean mPortalUpdatingState = false;
    private OnPortalSelectedListener mListener;
    private SharedPreferences mPrefs;

    /**
     * Listen to SyncHandler for results
     */
    private BroadcastReceiver mUpdateResultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            @PortalsUpdateService.UpdateResult
            int result = intent.getIntExtra(PortalsUpdateService.EXTRA_UPDATE_RESULT,
                    PortalsUpdateService.UPDATE_RESULT_IO_ERROR);
            // Disable updating
            mPortalUpdatingState = false;
            mSwipeRefreshLayout.setRefreshing(false);
            // Sends result to user
            switch (result) {
                case PortalsUpdateService.UPDATE_RESULT_OK:
                    Timber.i("Portals data update successfully finished");
                    break;
                case PortalsUpdateService.UPDATE_RESULT_IO_ERROR:
                    Timber.w("Portals data update failed because of IO ERROR");
                    Toast.makeText(getContext(), R.string.sync_result_io_exception, Toast.LENGTH_LONG)
                            .show();
                    break;
                case PortalsUpdateService.UPDATE_RESULT_OUTDATED_API:
                    Timber.e("Portals data update failed because of OUTDATED API");
                    // TODO make update dialog and points to google play
                    break;
            }
        }
    };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PortalListFragment() {
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment
     *
     * @return A new instance of fragment PortalListFragment.
     */
    public static PortalListFragment newInstance() {
        return new PortalListFragment();
    }

    // -------------------------------------------------------------------------------------------
    // Lifecycle events
    // -------------------------------------------------------------------------------------------

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnPortalSelectedListener) {
            mListener = (OnPortalSelectedListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnPortalSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        setHasOptionsMenu(true);

        // Init adapter
        mAdapter = new PortalListRecyclerViewAdapter();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_portal_list, container, false);
        Context context = view.getContext();

        // Update layout
        mSwipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setRefreshing(mPortalUpdatingState);
        mSwipeRefreshLayout.setOnRefreshListener(this::updatePortalsData);
        mSwipeRefreshLayout.setColorSchemeColors(
                getResources().getIntArray(R.array.swipeRefreshColors)
        );

        // RecyclerView
        RecyclerView mRecyclerView = view.findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        mRecyclerView.setAdapter(mAdapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Timber.d("Portal loader inited");
        getLoaderManager().initLoader(PORTALS_LOADER_ID, getArguments(), this);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Downloads fresh portals data from server
        // Register result receiver
        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(mUpdateResultReceiver,
                        new IntentFilter(PortalsUpdateService.BROADCAST_PORTALS_UPDATE_FINISHED));

        if(mPrefs.getBoolean(SettingsContract.UPDATE_PORTALS_AUTOMATICALLY,
                SettingsContract.UPDATE_PORTALS_AUTOMATICALLY_DEFAULT)) {
            updatePortalsData();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister result listener where there is no change of showing UI again
        LocalBroadcastManager.getInstance(getActivity())
                .unregisterReceiver(mUpdateResultReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar_portals_list, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.portals_list_refresh:
                updatePortalsData();
                return true;
            case R.id.portal_list_add:
                mListener.onPortalSelected(null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // -------------------------------------------------------------------------------------------
    // Callbacks
    // -------------------------------------------------------------------------------------------

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case PORTALS_LOADER_ID:
                return new CursorLoader(
                        getContext(),
                        ProviderContract.Portal.getUri(),
                        new String[]{
                                ProviderContract.Portal._ID,
                                ProviderContract.Portal.NAME
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
            case PORTALS_LOADER_ID:
                mAdapter.swapCursor(data);
                break;
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + loader.getId());
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        switch (loader.getId()) {
            case PORTALS_LOADER_ID:
                mAdapter.swapCursor(null);
                break;
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + loader.getId());
        }
    }

    // -------------------------------------------------------------------------------------------
    // Helping methods
    // -------------------------------------------------------------------------------------------

    private void updatePortalsData() {
        Timber.i("Update portals data request");

        PortalsUpdateService.startUpdate(getContext());
        // In this place the UI is not yet created so I save the state for later
        mPortalUpdatingState = true;
        if (mSwipeRefreshLayout != null)
            mSwipeRefreshLayout.setRefreshing(true);
    }

    private void selectPortal(@Nullable Uri portalUri) {
        if (mListener != null) {
            mListener.onPortalSelected(portalUri);
        }
    }

    private void deletePortal(@NonNull Uri portalUri) {
        AsyncTask.execute(() -> {
            int deletedRows = getContext().getContentResolver().delete(portalUri, null, null);
            if (BuildConfig.DEBUG) {
                Assert.isOne(deletedRows);
            }
            // TODO propagate to activity (the view state could be invalid)
        });
    }

    /**
     * Listen for {@link PortalListFragment} {@code onClick} events
     */
    public interface OnPortalSelectedListener {
        /**
         * Fired when portal is selected in {@link PortalListFragment}
         *
         * @param portalUri Uri to selected row or null if new custom portal
         *                  will be created
         */
        void onPortalSelected(@Nullable Uri portalUri);
    }

    // -------------------------------------------------------------------------------------------
    // Recycler view
    // -------------------------------------------------------------------------------------------

    /**
     * {@link RecyclerView} adapter that shows portals
     */
    class PortalListRecyclerViewAdapter extends CursorRecyclerViewAdapter<PortalListRecyclerViewAdapter.ViewHolder> {

        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long itemId = (long)view.getTag();
                Timber.i("Portal id %d selected", itemId);

                Uri portalUri = ContentUris.withAppendedId(ProviderContract.Portal.getUri(), itemId);
                selectPortal(portalUri);
            }
        };

        private final View.OnLongClickListener mOnLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                // Prepare data
                long itemId = (long)view.getTag();
                Uri portalUri = ContentUris.withAppendedId(ProviderContract.Portal.getUri(), itemId);
                // creating a popup menu
                PopupMenu popup = new PopupMenu(view.getContext(), view);
                // inflating menu from xml resource
                popup.inflate(R.menu.popup_portals_list);
                // adding click listener
                popup.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case R.id.portal_delete:
                            deletePortal(portalUri);
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
         */
        public PortalListRecyclerViewAdapter() {
            // Init adapter with empty cursor
            super(null);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_portal, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, Cursor cursor) {
            long portalId = cursor.getLong(0);
            if(portalId < ProviderContract.CUSTOM_DATA_OFFSET) {
                holder.mTypeText.setText("\uD83C\uDF10");
            } else {
                holder.mTypeText.setText("\uD83D\uDCCC");
            }
            holder.mNameText.setText(cursor.getString(1));

            holder.itemView.setTag(holder.getItemId());
            holder.itemView.setOnClickListener(mOnClickListener);
            holder.itemView.setOnLongClickListener(mOnLongClickListener);
        }

        /**
         * Standard {@link RecyclerView.ViewHolder} that stores one portal in UI
         */
        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView mTypeText;
            final TextView mNameText;

            ViewHolder(View view) {
                super(view);
                mTypeText = view.findViewById(R.id.type);
                mNameText = view.findViewById(R.id.name);
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mNameText.getText() + "'";
            }
        }
    }
}


