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
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import cz.maresmar.sfm.view.CursorRecyclerViewAdapter;

/**
 * {@link CursorRecyclerViewAdapter} that uses only part of cursor
 * <p>
 * This part is defined as rows that has in specific column equal values </p>
 * @param <T> Type of {@link RecyclerView.ViewHolder} used for showing
 */
public abstract class SectionCursorRecycleViewAdapter<T extends RecyclerView.ViewHolder> extends CursorRecyclerViewAdapter<T> {

    private int mFirstPosition = -1;
    private int mSize = 0;
    private int mColumnIndex;
    private long mColumnValue = -1;

    /**
     * Create new abstract adapter with specific cursor
     * @param columnIndex Column index that defines group, rows with equal values in that column determine one group
     */
    public SectionCursorRecycleViewAdapter(int columnIndex) {
        super(null);
        mColumnIndex = columnIndex;
    }

    /**
     * Replaces cursor with new one and changes column that defines group
     * @param cursor New cursor (the cursor have to be sorted {@code ASC} by column that defines group)
     * @param columnValue Column value that defines group, rows with that value determine one group
     */
    public void swapData(Cursor cursor, long columnValue) {
        if(mCursor == cursor && mColumnValue == columnValue) {
            return;
        }
        mColumnValue = columnValue;

        mFirstPosition = -1;
        mSize = 0;
        if (cursor != null && cursor.moveToFirst()) {
            do {
                if (mFirstPosition == -1) {
                    if (cursor.getLong(mColumnIndex) == columnValue) {
                        mFirstPosition = cursor.getPosition();
                        mSize = 1;
                    }
                } else {
                    if (cursor.getLong(mColumnIndex) == columnValue) {
                        mSize++;
                    } else {
                        break;
                    }
                }
            } while (cursor.moveToNext());
        }

        // Change cursor if needed
        if(mCursor != cursor) {
            swapCursor(cursor);
        } else {
            // The cursor is same but the columnValue changed
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemCount() {
        return mSize;
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position + mFirstPosition);
    }

    /**
     * Called by RecyclerView to display the data at specific position. This method should't be normally
     * overridden use {@link #onBindViewHolder(RecyclerView.ViewHolder, Cursor)} for UI bindings.
     */
    @Override
    public void onBindViewHolder(@NonNull T viewHolder, int position) {
        super.onBindViewHolder(viewHolder, position + mFirstPosition);
    }
}
