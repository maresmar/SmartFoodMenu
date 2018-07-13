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

package cz.maresmar.sfm.view.menu;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import cz.maresmar.sfm.BuildConfig;
import cz.maresmar.sfm.R;
import cz.maresmar.sfm.view.DataForm;
import timber.log.Timber;

/**
 * Fragment that contains {@link ViewPager} with {@link CursorPagerAdapter}.
 * <p>
 * Activity that uses this fragment must implement {@link PagerPageChangedListener} interface to handle
 * interaction events.</p>
 *
 * @see cz.maresmar.sfm.view.menu.day.DayMenuPagerFragment
 * @see cz.maresmar.sfm.view.menu.portal.PortalMenuPagerFragment
 */
public class CursorPagerFragment extends Fragment implements ViewPager.OnPageChangeListener {

    private PagerPageChangedListener mListener;

    private ViewPager mViewPager;
    private CursorPagerAdapter mPagerAdapter;
    private View mEmptyView;

    private DataSetObserver mEmptyObserver = new EmptyDataSetObserver();

    // -------------------------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------------------------

    @Override
    @CallSuper
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_pager, container, false);

        mViewPager = view.findViewById(R.id.pager);
        // Set on portal change listener
        mViewPager.addOnPageChangeListener(this);

        // Prepare empty view
        mEmptyView = view.findViewById(R.id.empty_content);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof PagerPageChangedListener) {
            mListener = (PagerPageChangedListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement PagerPageChangedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    // -------------------------------------------------------------------------------------------
    // Public handling methods
    // -------------------------------------------------------------------------------------------

    /**
     * Sets new {@link CursorPagerAdapter}
     *
     * @param adapter New adapter
     */
    public void setPagerAdapter(CursorPagerAdapter adapter) {
        if (mPagerAdapter != null) {
            mPagerAdapter.unregisterDataSetObserver(mEmptyObserver);
        }
        mPagerAdapter = adapter;
        mPagerAdapter.registerDataSetObserver(mEmptyObserver);
        mViewPager.setAdapter(mPagerAdapter);
    }

    /**
     * Returns used {@link CursorPagerAdapter}
     *
     * @return Adapter or {@code null} if no adapter was set
     */
    public CursorPagerAdapter getPagerAdapter() {
        return mPagerAdapter;
    }

    /**
     * Shows first page with bigger or equal ID in {@link ViewPager}
     *
     * @param pageId ID of page (corresponding with {@link Cursor}'s row ID)
     */
    public void showFirstGePageId(long pageId) {
        int pagePosition = mPagerAdapter.getFirstGePosition(pageId);
        mViewPager.setCurrentItem(pagePosition, false);

        long shownPageId = mPagerAdapter.getId(pagePosition);
        mListener.onPageChanged(shownPageId);
    }

    /**
     * Shows page with specific ID in {@link ViewPager}
     *
     * @param pageId ID of page (corresponding with {@link Cursor}'s row ID)
     */
    public void showPageId(long pageId) {
        int pagePosition = mPagerAdapter.getPosition(pageId);
        if (pagePosition >= 0) {
            mViewPager.setCurrentItem(pagePosition, false);
            mListener.onPageChanged(pageId);
        }
    }

    /**
     * Returns ID of shown page in {@link ViewPager}
     *
     * @return page ID
     */
    public long getPageId() {
        int pagePosition = mViewPager.getCurrentItem();
        return mPagerAdapter.getId(pagePosition);
    }

    // -------------------------------------------------------------------------------------------
    // Page change listener
    // -------------------------------------------------------------------------------------------

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        if (mListener != null) {
            long portalId = mPagerAdapter.getId(position);
            mListener.onPageChanged(portalId);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if(getPagerAdapter().getCount() > 1) {
            // Fixing sensitivity of SwipeUpdateLayout
            // see https://stackoverflow.com/a/29946734/1392034 for more
            mListener.enableSwipeRefresh(state == ViewPager.SCROLL_STATE_IDLE);
        }
    }

    /**
     * Listens for page changes in {@link ViewPager}
     * <p>
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment.</p>
     */
    public interface PagerPageChangedListener {
        /**
         * Celled when {@link ViewPager} changes page
         *
         * @param pageId ID of page (corresponding with {@link Cursor}'s row ID)
         */
        void onPageChanged(long pageId);

        /**
         * Enables and disables activity's {@link android.support.v4.widget.SwipeRefreshLayout} because of
         * bug there (see <a href="https://stackoverflow.com/a/29946734/1392034">
         *     https://stackoverflow.com/a/29946734/1392034</a> for more).
         *
         * @param enabled {@code true} if swipe refresh should be enabled, {@code false} otherwise
         */
        void enableSwipeRefresh(boolean enabled);
    }

    /**
     * Keeps eye on data changes in Cursor and check if there is something to show
     */
    private class EmptyDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            super.onChanged();
            checkIfEmpty();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            checkIfEmpty();
        }

        private void checkIfEmpty() {
            if (getPagerAdapter().getCount() == 0) {
                mViewPager.setVisibility(View.GONE);
                mEmptyView.setVisibility(View.VISIBLE);
            } else {
                mEmptyView.setVisibility(View.GONE);
                mViewPager.setVisibility(View.VISIBLE);
            }
        }
    }
}
