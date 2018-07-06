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

package cz.maresmar.sfm.view.menu.portal;


import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import cz.maresmar.sfm.view.CursorRecyclerViewAdapter;
import cz.maresmar.sfm.R;
import cz.maresmar.sfm.provider.ProviderContract;
import cz.maresmar.sfm.utils.MenuUtils;
import cz.maresmar.sfm.view.FragmentChangeRequestListener;
import cz.maresmar.sfm.view.init.WelcomeActivity;
import cz.maresmar.sfm.view.menu.CursorPagerAdapter;
import cz.maresmar.sfm.view.menu.MenuViewAdapter;
import timber.log.Timber;

/**
 * Fragment that shows menu in one portal grouped by day
 *
 * @see cz.maresmar.sfm.view.menu.day.DayMenuFragment
 */
public class PortalMenuFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, CursorPagerAdapter.Page {

    // the fragment initialization parameters
    private static final String ARG_USER_URI = "userUri";
    private static final String ARG_PORTAL_ID = "portalId";

    private static final int DAY_LOADER_ID = 1;
    private static final int MENU_LOADER_ID = 2;

    private Uri mUserUri;
    private long mPortalId;

    private MenuGroupViewAdapter mMenuGroupAdapter;
    private FragmentChangeRequestListener mFragmentRequestListener;

    private RecyclerView mRecyclerView;
    private View mEmptyView;

    /**
     * Creates new instance of fragment with specific portal
     *
     * @param userUri  User Uri prefix
     * @param portalId ID of selected portal
     * @return A new instance of fragment
     */
    public static PortalMenuFragment newInstance(Uri userUri, long portalId) {
        PortalMenuFragment fragment = new PortalMenuFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_USER_URI, userUri);
        args.putLong(ARG_PORTAL_ID, portalId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mUserUri = getArguments().getParcelable(ARG_USER_URI);
            mPortalId = getArguments().getLong(ARG_PORTAL_ID);
        }

        getLoaderManager().initLoader(DAY_LOADER_ID, null, this);
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
        return mPortalId;
    }

    // -------------------------------------------------------------------------------------------
    // Loader callbacks
    // -------------------------------------------------------------------------------------------

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        switch (id) {
            case DAY_LOADER_ID: {
                Uri daysUri = Uri.withAppendedPath(mUserUri, ProviderContract.DAY_PATH);
                return new CursorLoader(
                        getContext(),
                        daysUri,
                        new String[]{
                                ProviderContract.Day._ID,
                                ProviderContract.Day.DATE
                        },
                        ProviderContract.Day.DATE + " >= " + MenuUtils.getTodayDate(),
                        null,
                        null
                );
            }
            case MENU_LOADER_ID: {
                Uri menuUri = Uri.withAppendedPath(mUserUri, ProviderContract.MENU_ENTRY_PATH);
                return new CursorLoader(
                        getContext(),
                        menuUri,
                        MenuViewAdapter.PROJECTION,
                        ProviderContract.MenuEntry.PORTAL_ID + " = " + mPortalId,
                        null,
                        ProviderContract.MenuEntry.DATE + " ASC"
                );
            }
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + id);
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case DAY_LOADER_ID: {
                Timber.d("Day data loaded");

                // Empty state
                if(cursor.getCount() > 0) {
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
            case DAY_LOADER_ID:
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

    // -------------------------------------------------------------------------------------------
    // Recycler View
    // -------------------------------------------------------------------------------------------

    /**
     * {@link RecyclerView} adapter that shows menu grouped by days
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
            if(mMenuItemsCursor != newCursor) {
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
            long date = cursor.getLong(1);
            viewHolder.mTitle.setText(MenuUtils.getDateStr(getContext(), date));
            // Menu items
            viewHolder.mMenuViewAdapter.swapData(mUserUri, mMenuItemsCursor, date);
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

                mMenuViewAdapter = new MenuViewAdapter(mFragmentRequestListener, MenuViewAdapter.DATE_COLUMN_INDEX, mEmptyView);
                mMenuRecycleView.setAdapter(mMenuViewAdapter);
            }
        }
    }
}
