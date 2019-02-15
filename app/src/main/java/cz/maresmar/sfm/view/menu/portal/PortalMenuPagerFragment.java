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
import cz.maresmar.sfm.view.menu.CursorPagerAdapter;
import cz.maresmar.sfm.view.menu.CursorPagerFragment;
import timber.log.Timber;

/**
 * Fragment that shows {@link PortalMenuFragment}s together so user can swipe to another portal
 * <p>
 * Activities that contain this fragment must implement the
 * {@link cz.maresmar.sfm.view.menu.CursorPagerFragment.PagerPageChangedListener} interface
 * to handle interaction events.</p><p>
 * Use the {@link PortalMenuPagerFragment#newInstance} factory method to
 * create an instance of this fragment.</p>
 */
public class PortalMenuPagerFragment extends CursorPagerFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String ARG_PAGER_PAGE_ID = "pagerPosition";

    // the fragment initialization parameters
    private static final String ARG_USER_URI = "userUri";
    private static final String ARG_PORTAL_ID = "portalId";

    private static final int PORTAL_LOADER_ID = 1;

    private Uri mUserUri;
    private long mPortalId;

    /**
     * Creates new instance of this fragment for specific user and portal
     *
     * @param userUri  User Uri prefix
     * @param portalId Selected portal ID
     * @return A new instance of fragment
     */
    public static PortalMenuPagerFragment newInstance(Uri userUri, long portalId) {
        PortalMenuPagerFragment fragment = new PortalMenuPagerFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_USER_URI, userUri);
        args.putLong(ARG_PORTAL_ID, portalId);
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
                mPortalId = getArguments().getLong(ARG_PORTAL_ID);
            } else {
                mPortalId = savedInstanceState.getLong(ARG_PAGER_PAGE_ID);
            }

            getLoaderManager().initLoader(PORTAL_LOADER_ID, null, this);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        // Set correct adapter
        setPagerAdapter(new PortalMenuPagerAdapter(getChildFragmentManager()));


        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putLong(ARG_PAGER_PAGE_ID, mPortalId);
    }

    @Override
    public void onPageSelected(int position) {
        super.onPageSelected(position);

        long portalId = getPagerAdapter().getId(position);
        mPortalId = portalId;
    }

    // -------------------------------------------------------------------------------------------
    // Loader callback
    // -------------------------------------------------------------------------------------------

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        switch (id) {
            case PORTAL_LOADER_ID:
                Uri portalsUri = Uri.withAppendedPath(mUserUri, ProviderContract.PORTAL_PATH);
                return new CursorLoader(
                        getContext(),
                        portalsUri,
                        new String[]{
                                ProviderContract.Portal._ID
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
            case PORTAL_LOADER_ID:
                Timber.d("Portal loader finished");
                // Set portal adapter
                getPagerAdapter().swapCursor(data);

                // Show correct portal
                showPageId(mPortalId);
                break;
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + loader.getId());
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        switch (loader.getId()) {
            case PORTAL_LOADER_ID:
                Timber.w("Portal loader reset");
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
     * Changes selected user or portal and restarts fragment
     *
     * @param userUri  User Uri prefix
     * @param portalId Selected portal ID
     */
    public void reset(Uri userUri, long portalId) {
        if (!mUserUri.equals(userUri)) {
            // Update variables
            mUserUri = userUri;
            mPortalId = portalId;
            // Restart loader
            getPagerAdapter().swapCursor(null);
            getLoaderManager().restartLoader(PORTAL_LOADER_ID, null, this);
        } else {
            if(portalId != mPortalId) {
                mPortalId = portalId;
                showPageId(mPortalId);
            }
        }
    }

    // -------------------------------------------------------------------------------------------
    // Fragment state pager adapter
    // -------------------------------------------------------------------------------------------

    /**
     * {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * selected portal
     */
    class PortalMenuPagerAdapter extends CursorPagerAdapter {

        /**
         * Creates new adapter
         *
         * @param fm Child {@link FragmentManager}
         */
        PortalMenuPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItemId(long pageId) {
            return PortalMenuFragment.newInstance(mUserUri, pageId);
        }
    }
}
