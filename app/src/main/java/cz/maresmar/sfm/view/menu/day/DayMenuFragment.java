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

package cz.maresmar.sfm.view.menu.day;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import cz.maresmar.sfm.R;
import cz.maresmar.sfm.provider.ProviderContract;
import cz.maresmar.sfm.utils.MenuUtils;
import cz.maresmar.sfm.view.CursorRecyclerViewAdapter;
import cz.maresmar.sfm.view.FragmentChangeRequestListener;
import cz.maresmar.sfm.view.menu.CursorPagerAdapter;
import cz.maresmar.sfm.view.menu.MenuViewAdapter;
import timber.log.Timber;

/**
 * Fragment that shows menu in one day grouped by portal
 *
 * @see cz.maresmar.sfm.view.menu.portal.PortalMenuFragment
 */
public class DayMenuFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, CursorPagerAdapter.Page {

    // the fragment initialization parameters
    private static final String ARG_USER_URI = "userUri";
    private static final String ARG_DATE = "date";
    private static final String ARG_AVAILABLE_MENU_ONLY = "availableMenuOnly";

    private static final int PORTAL_LOADER_ID = 1;
    private static final int MENU_LOADER_ID = 2;

    private Uri mUserUri;
    private long mDate;
    private boolean mAvailableMenuOnly;

    private RecyclerView mRecyclerView;
    private View mEmptyView;

    private MenuGroupViewAdapter mMenuGroupAdapter;
    private FragmentChangeRequestListener mFragmentRequestListener;

    /**
     * Creates new instance of fragment with specific day
     *
     * @param userUri User Uri prefix
     * @param date    Date to be shown
     * @return A new instance of fragment
     */
    public static DayMenuFragment newInstance(Uri userUri, long date) {
        DayMenuFragment fragment = new DayMenuFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_USER_URI, userUri);
        args.putLong(ARG_DATE, date);
        args.putBoolean(ARG_AVAILABLE_MENU_ONLY, false);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Creates new instance of fragment that will show today
     * <p>
     * This fragment is a little bit specific, it shows only menu that can user still gain in some way
     * (like ordered or available to take)
     * </p>
     *
     * @param userUri User Uri prefix
     * @return A new instance of fragment
     */
    public static DayMenuFragment newTodayInstance(Uri userUri) {
        DayMenuFragment fragment = new DayMenuFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_USER_URI, userUri);
        args.putLong(ARG_DATE, MenuUtils.getTodayDate());
        args.putBoolean(ARG_AVAILABLE_MENU_ONLY, true);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mUserUri = getArguments().getParcelable(ARG_USER_URI);
            mDate = getArguments().getLong(ARG_DATE);
            mAvailableMenuOnly = getArguments().getBoolean(ARG_AVAILABLE_MENU_ONLY);
        }

        getLoaderManager().initLoader(PORTAL_LOADER_ID, null, this);
        getLoaderManager().initLoader(MENU_LOADER_ID, null, this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_menu_groups, container, false);

        mRecyclerView = view.findViewById(R.id.menu_groups_recycle_view);
        mMenuGroupAdapter = new MenuGroupViewAdapter();
        mRecyclerView.setAdapter(mMenuGroupAdapter);

        // Prepare empty view
        mEmptyView = view.findViewById(R.id.empty_content);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof FragmentChangeRequestListener) {
            mFragmentRequestListener = (FragmentChangeRequestListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement FragmentChangeRequestListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFragmentRequestListener = null;
    }

    @Override
    public long getItemId() {
        return mDate;
    }

    // -------------------------------------------------------------------------------------------
    // Loader callbacks
    // -------------------------------------------------------------------------------------------

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        switch (id) {
            case PORTAL_LOADER_ID: {
                Uri daysUri = Uri.withAppendedPath(mUserUri, ProviderContract.PORTAL_PATH);
                return new CursorLoader(
                        getContext(),
                        daysUri,
                        new String[]{
                                ProviderContract.Portal._ID,
                                ProviderContract.Portal.NAME
                        },
                        null,
                        null,
                        null
                );
            }
            case MENU_LOADER_ID: {
                Uri menuUri = Uri.withAppendedPath(mUserUri, ProviderContract.MENU_ENTRY_PATH);
                String selection = ProviderContract.MenuEntry.DATE + " = " + mDate;

                if (mAvailableMenuOnly) {
                    selection += " AND (" +
                            "(" + ProviderContract.MenuEntry.REMAINING_TO_TAKE + " > 0 ) OR " +
                            "(" + ProviderContract.MenuEntry.REMAINING_TO_ORDER + " > 0 ) OR " +
                            "(" + ProviderContract.MenuEntry.SYNCED_RESERVED_AMOUNT + " > 0 ) OR " +
                            "((" + ProviderContract.MenuEntry.STATUS + " & " + ProviderContract.MENU_STATUS_ORDERABLE + ") == " + ProviderContract.MENU_STATUS_ORDERABLE + ")" +
                            ")";
                }

                return new CursorLoader(
                        getContext(),
                        menuUri,
                        MenuViewAdapter.PROJECTION,
                        selection,
                        null,
                        ProviderContract.MenuEntry.PORTAL_ID + " ASC"
                );
            }
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + id);
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case PORTAL_LOADER_ID: {
                Timber.d("Day data loaded");

                // Empty state
                if (cursor.getCount() > 0) {
                    mEmptyView.setVisibility(View.GONE);
                    mRecyclerView.setVisibility(View.VISIBLE);
                } else {
                    mRecyclerView.setVisibility(View.GONE);
                    mEmptyView.setVisibility(View.VISIBLE);
                }

                // Swap the data cursor
                mMenuGroupAdapter.swapCursor(cursor);
                break;
            }
            case MENU_LOADER_ID: {
                Timber.d("Menu data loaded");

                mMenuGroupAdapter.setMenuItemsCursor(cursor);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + loader.getId());
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        switch (loader.getId()) {
            case PORTAL_LOADER_ID:
                Timber.e("Day data with user %s is no longer valid", mUserUri);
                mMenuGroupAdapter.swapCursor(null);
                break;
            case MENU_LOADER_ID:
                Timber.e("Menu data with user %s is no longer valid", mUserUri);
                mMenuGroupAdapter.setMenuItemsCursor(null);
                break;
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + loader.getId());
        }
    }

    /**
     * Restart the fragment with new user
     *
     * @param userUri User uri prefix
     */
    public void reset(Uri userUri) {
        if (!mUserUri.equals(userUri)) {
            mUserUri = userUri;
            getLoaderManager().restartLoader(PORTAL_LOADER_ID, null, this);
            getLoaderManager().restartLoader(MENU_LOADER_ID, null, this);
        }
    }

    // -------------------------------------------------------------------------------------------
    // Recycler View
    // -------------------------------------------------------------------------------------------

    /**
     * {@link RecyclerView} adapter that shows menu grouped by portals
     */
    class MenuGroupViewAdapter
            extends CursorRecyclerViewAdapter<MenuGroupViewAdapter.ViewHolder> {

        private Cursor mMenuItemsCursor;

        /**
         * Creates new adapter
         */
        MenuGroupViewAdapter() {
            super(null);
        }

        /**
         * Changes menu cursor to show new data
         *
         * @param newCursor New menu cursor
         */
        public void setMenuItemsCursor(Cursor newCursor) {
            if (mMenuItemsCursor != newCursor) {
                mMenuItemsCursor = newCursor;
                notifyDataSetChanged();
            }
        }

        @NonNull
        @Override
        public MenuGroupViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.card_menu_group, parent, false);
            return new MenuGroupViewAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(MenuGroupViewAdapter.ViewHolder viewHolder, Cursor cursor) {
            // Menu group title
            long portalId = cursor.getLong(0);
            String portalName = cursor.getString(1);

            viewHolder.mTitle.setText(portalName);
            // Menu items
            viewHolder.mMenuViewAdapter.swapData(mUserUri, mMenuItemsCursor, portalId);
        }

        /**
         * Standard {@link RecyclerView.ViewHolder} that stores menu in one portal for one day
         */
        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView mTitle;
            final RecyclerView mMenuRecycleView;
            final MenuViewAdapter mMenuViewAdapter;
            final View mEmptyView;

            ViewHolder(View view) {
                super(view);
                mTitle = view.findViewById(R.id.menu_group_title);
                mMenuRecycleView = view.findViewById(R.id.menu_recycle_view);
                mEmptyView = view.findViewById(R.id.menu_emty_view);

                mMenuViewAdapter = new MenuViewAdapter(mFragmentRequestListener, MenuViewAdapter.PORTAL_COLUMN_INDEX, mEmptyView);
                mMenuRecycleView.setAdapter(mMenuViewAdapter);
            }
        }
    }
}
