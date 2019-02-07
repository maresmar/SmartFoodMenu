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

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import cz.maresmar.sfm.provider.ProviderContract;
import cz.maresmar.sfm.utils.MenuUtils;
import cz.maresmar.sfm.view.menu.CursorPagerAdapter;
import cz.maresmar.sfm.view.menu.CursorPagerFragment;
import timber.log.Timber;

/**
 * Fragment that shows {@link DayMenuFragment}s together so user can swipe to another day
 * <p>
 * Use the {@link DayMenuPagerFragment#newInstance} factory method to
 * create an instance of this fragment.</p>
 */
public class DayMenuPagerFragment extends CursorPagerFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String ARG_PAGER_PAGE_ID = "pagerPosition";

    // the fragment initialization parameters
    private static final String ARG_USER_URI = "userUri";

    private static final int DAY_LOADER = 1;

    private Uri mUserUri;
    private long mDay;

    /**
     * Creates new instance of this fragment for specific user
     *
     * @param userUri User Uri prefix
     * @return A new instance of fragment DayMenuPagerFragment
     */
    public static DayMenuPagerFragment newInstance(Uri userUri) {
        DayMenuPagerFragment fragment = new DayMenuPagerFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_USER_URI, userUri);
        fragment.setArguments(args);
        return fragment;
    }

    // -------------------------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mUserUri = getArguments().getParcelable(ARG_USER_URI);

            if (savedInstanceState == null) {
                mDay = MenuUtils.getTodayDate();
            } else {
                mDay = savedInstanceState.getLong(ARG_PAGER_PAGE_ID);
            }

            getLoaderManager().initLoader(DAY_LOADER, null, this);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        // Set correct adapter
        setPagerAdapter(new DayMenuPagerAdapter(getChildFragmentManager()));


        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putLong(ARG_PAGER_PAGE_ID, mDay);
    }

    @Override
    public void onPageSelected(int position) {
        super.onPageSelected(position);

        long dayId = getPagerAdapter().getId(position);
        mDay = dayId;
    }

    // -------------------------------------------------------------------------------------------
    // Loader callback
    // -------------------------------------------------------------------------------------------

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        switch (id) {
            case DAY_LOADER:
                Uri portalsUri = Uri.withAppendedPath(mUserUri, ProviderContract.DAY_PATH);
                return new CursorLoader(
                        getContext(),
                        portalsUri,
                        new String[]{
                                ProviderContract.Day._ID
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
            case DAY_LOADER:
                Timber.d("Day loader finished");
                // Set portal adapter
                getPagerAdapter().swapCursor(data);

                // Show correct day
                showFirstGePageId(mDay);
                break;
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + loader.getId());
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        switch (loader.getId()) {
            case DAY_LOADER:
                Timber.w("Day loader reset");
                getPagerAdapter().swapCursor(null);
                break;
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + loader.getId());
        }
    }

    // -------------------------------------------------------------------------------------------
    // Main activity connection points
    // -------------------------------------------------------------------------------------------

    /**
     * Changes selected user and restarts fragment
     *
     * @param userUri User uri prefix
     */
    public void reset(Uri userUri) {
        if (!mUserUri.equals(userUri)) {
            mUserUri = userUri;
            getPagerAdapter().swapCursor(null);
            getLoaderManager().restartLoader(DAY_LOADER, null, this);
        }
    }

    // -------------------------------------------------------------------------------------------
    // Fragment state pager adapter
    // -------------------------------------------------------------------------------------------

    /**
     * {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * selected day
     */
    class DayMenuPagerAdapter extends CursorPagerAdapter {

        /**
         * Creates new adapter
         *
         * @param fm Child {@link FragmentManager}
         */
        DayMenuPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItemId(long pageId) {
            // Item id is date actually
            return DayMenuFragment.newInstance(mUserUri, pageId);
        }
    }
}
