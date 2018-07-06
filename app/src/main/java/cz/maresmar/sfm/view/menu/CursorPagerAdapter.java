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

import android.database.Cursor;
import android.database.DataSetObserver;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import cz.maresmar.sfm.Assert;
import cz.maresmar.sfm.BuildConfig;

/**
 * {@link FragmentStatePagerAdapter} that is based on {@link Cursor}. The {@link Cursor} must contains
 * column with {@link BaseColumns#_ID} which work as connection between {@link Cursor}'s row and
 * {@link android.support.v4.view.ViewPager} page's ID.
 */
public abstract class CursorPagerAdapter extends FragmentStatePagerAdapter {


    private Cursor mCursor;
    private DataSetObserver mDataSetObserver;

    private int mIdColumnIndex = -1;

    /**
     * Creates new adapter
     *
     * @param fm Valid {@link FragmentManager} used in root view
     */
    public CursorPagerAdapter(FragmentManager fm) {
        super(fm);

        mDataSetObserver = new NotifyingDataSetObserver();
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        // Causes adapter to reload some Fragments when
        // notifyDataSetChanged is called
        if (mCursor != null) {
            if (!mCursor.isClosed()) {
                Page page = (Page) object;
                return getPosition(page.getItemId());
            } else {
                return POSITION_UNCHANGED;
                /* The POSITION_NONE causes empty fragment, this is valid behaviour
                 * but it will be solved in getPosition(int) method (with valid cursor) **/
            }
        } else {
            // If null cursor the pager should be empty
            return POSITION_NONE;
        }
    }

    /**
     * Replaces old {@link Cursor} with new one.
     *
     * @param newCursor New cursor to replace
     * @return Old cursor
     */
    public Cursor swapCursor(@Nullable Cursor newCursor) {
        // Ignore same cursor
        if (newCursor == mCursor) {
            return null;
        }

        // Release old cursor
        final Cursor oldCursor = mCursor;
        if (oldCursor != null && mDataSetObserver != null) {
            oldCursor.unregisterDataSetObserver(mDataSetObserver);
        }

        // Prepare new cursor
        mCursor = newCursor;
        if (mCursor != null) {
            mCursor.registerDataSetObserver(mDataSetObserver);
            mIdColumnIndex = mCursor.getColumnIndexOrThrow(BaseColumns._ID);
        } else {
            mIdColumnIndex = -1;
        }

        // Update results in UI
        notifyDataSetChanged();
        return oldCursor;
    }

    @Override
    public Fragment getItem(int position) {
        long itemId = getId(position);
        return getItemId(itemId);
    }

    /**
     * Returns fragment that correspond with {@link Cursor} row
     * <p>
     * Fragment must implement {@link Page} interface to survive possible {@link Cursor} changes.</p>
     *
     * @param pageId ID of row from {@link Cursor}
     * @return New fragment for selected ID
     */
    public abstract Fragment getItemId(long pageId);

    @Override
    public int getCount() {
        if (mCursor != null) {
            return mCursor.getCount();
        } else
            return 0;
    }

    /**
     * Returns first page position (in {@link android.support.v4.view.ViewPager}) that has bigger or
     * equal ID than selected ID
     *
     * @param pageId ID of page
     * @return Position in pager or {@code -1} if not found
     */
    public int getFirstGePosition(long pageId) {
        if (mCursor == null)
            throw new IllegalStateException("Not have valid cursor");

        long lastPageId = Long.MIN_VALUE;

        if (mCursor.moveToFirst()) {
            do {
                long foundPortalId = mCursor.getLong(mIdColumnIndex);

                if (BuildConfig.DEBUG) {
                    Assert.that(lastPageId < foundPortalId,
                            "Expected ascending order of IDs, but found !(%d < %d)", lastPageId, foundPortalId);
                }

                if (foundPortalId >= pageId) {
                    return mCursor.getPosition();
                }
                lastPageId = foundPortalId;
            } while (mCursor.moveToNext());
        }
        return -1;
    }

    /**
     * Returns page position (in {@link android.support.v4.view.ViewPager}) that has same ID as
     * selected ID
     *
     * @param pageId ID of page
     * @return Position in pager or {@link #POSITION_NONE} if not found
     */
    public int getPosition(long pageId) {
        if (mCursor == null) {
            throw new IllegalStateException("Not have valid cursor");
        }

        if (mCursor.moveToFirst()) {
            do {
                long foundPortalId = mCursor.getLong(mIdColumnIndex);
                if (foundPortalId == pageId)
                    return mCursor.getPosition();
            } while (mCursor.moveToNext());
        }
        return POSITION_NONE;
    }

    /**
     * Returns page ID of selected position
     * @param position Position in {@link android.support.v4.view.ViewPager}
     * @return ID corresponding with {@link Cursor}'s row ID
     */
    public long getId(int position) {
        if (mCursor == null)
            throw new IllegalStateException("Not have valid cursor");

        if (mCursor.moveToPosition(position)) {
            return mCursor.getLong(mIdColumnIndex);
        } else {
            return -1;
        }
    }

    /**
     * Keeps eye on data changes in Cursor
     */
    private class NotifyingDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            super.onChanged();
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            notifyDataSetChanged();
        }
    }

    /**
     * Interface used in {@link CursorPagerAdapter} fragments
     */
    public interface Page {
        /**
         * Returns ID of fragment (corresponding with {@link Cursor}'s row ID)
         */
        long getItemId();
    }
}
