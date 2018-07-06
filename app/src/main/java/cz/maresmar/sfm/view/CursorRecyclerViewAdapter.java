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

package cz.maresmar.sfm.view;

import android.database.Cursor;
import android.database.DataSetObserver;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

/**
 * {@link RecyclerView.Adapter} that is based on {@link Cursor}. The {@link Cursor} must contains
 * column with {@link BaseColumns#_ID} which work as connection between {@link Cursor}'s row and
 * {@link RecyclerView.ViewHolder} item's ID.
 */

public abstract class CursorRecyclerViewAdapter<T extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<T> {
    /**
     * The source cursor to display data from
     */
    protected Cursor mCursor;

    /**
     * Index of cursor's column containing IDs of rows
     * Contains index of column or -1 if there no valid Cursor set {@link BaseColumns#_ID}
     */
    private int mIdColumnIndex;

    /**
     * Keeps eye on data changes in cursor and provides feedback
     */
    private DataSetObserver mDataSetObserver;

    /**
     * Create new abstract adapter with specific cursor
     */
    public CursorRecyclerViewAdapter(Cursor cursor) {
        mDataSetObserver = new NotifyingDataSetObserver();
        setHasStableIds(true);

        mCursor = cursor;
        mIdColumnIndex = mCursor != null ? mCursor.getColumnIndexOrThrow(BaseColumns._ID) : -1;
        mDataSetObserver = new NotifyingDataSetObserver();
        if (mCursor != null) {
            mCursor.registerDataSetObserver(mDataSetObserver);
        }
    }

    /**
     * Gets internal {@link Cursor}
     *
     * @return Internal {@link Cursor} or {@code null} if there is no valid cursor
     */
    public Cursor getCursor() {
        return mCursor;
    }

    @Override
    public int getItemCount() {
        if (mCursor != null) {
            return mCursor.getCount();
        }
        return 0;
    }

    @Override
    public long getItemId(int position) {
        if (mCursor != null && mCursor.moveToPosition(position)) {
            return mCursor.getLong(mIdColumnIndex);
        }
        return 0;
    }

    /**
     * Called by CursorRecyclerViewAdapter to display the data from specific cursor row. This method should
     * update the contents of the itemView to reflect the item at the given row.
     *
     * @param viewHolder The ViewHolder which should be updated to represent the contents of the
     *                   item at the given cursor row in the data set.
     * @param cursor     The cursor moved to specific position (you don't need to change it's position)
     */
    public abstract void onBindViewHolder(T viewHolder, Cursor cursor);

    /**
     * Called by RecyclerView to display the data at specific position. This method should't be normally
     * overridden use {@link #onBindViewHolder(RecyclerView.ViewHolder, Cursor)} for UI bindings.
     */
    @Override
    public void onBindViewHolder(@NonNull T viewHolder, int position) {
        if (mCursor == null) {
            throw new IllegalStateException("This should only be called when the cursor is valid");
        }
        if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("Couldn't move cursor to position " + position);
        }
        onBindViewHolder(viewHolder, mCursor);
    }

    /**
     * Change the underlying cursor to a new cursor. If there is an existing cursor it will be
     * closed.
     *
     * @param cursor New cursor
     */
    public void replaceCursor(Cursor cursor) {
        Cursor old = swapCursor(cursor);
        if (old != null) {
            old.close();
        }
    }

    /**
     * Replaces old cursor with new one
     *
     * @param newCursor New cursor
     * @return Old cursor
     */
    public Cursor swapCursor(Cursor newCursor) {
        if (newCursor == mCursor) {
            return null;
        }
        final Cursor oldCursor = mCursor;
        if (oldCursor != null && mDataSetObserver != null) {
            oldCursor.unregisterDataSetObserver(mDataSetObserver);
        }
        mCursor = newCursor;
        if (mCursor != null) {
            mCursor.registerDataSetObserver(mDataSetObserver);
            mIdColumnIndex = mCursor.getColumnIndexOrThrow(BaseColumns._ID);

            notifyDataSetChanged();
        } else {
            mIdColumnIndex = -1;
            notifyDataSetChanged();
            //There is no notifyDataSetInvalidated() method in RecyclerView.Adapter
        }
        return oldCursor;
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
            //There is no notifyDataSetInvalidated() method in RecyclerView.Adapter
        }
    }
}