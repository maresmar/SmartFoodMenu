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

package cz.maresmar.sfm.db.controller;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;
import androidx.annotation.NonNull;

import cz.maresmar.sfm.provider.ProviderContract;

/**
 * Table controller around one table (without any joins)
 */
public class SimpleController extends ViewController {

    final String mTableName;

    /**
     * Crete new controller with specific table
     *
     * @param tableName Name of table
     */
    public SimpleController(String tableName) {
        mTableName = tableName;
    }

    // -------------------------------------------------------------------------------------------
    // Abstract class methods
    // -------------------------------------------------------------------------------------------

    @Override
    public long insert(@NonNull SQLiteDatabase db, ContentValues newValues) {
        return insertOrThrow(db, mTableName, newValues);
    }

    @Override
    public int update(@NonNull SQLiteDatabase db, ContentValues values, String selection, String[] selectionArgs) {
        return update(db, mTableName, values, selection, selectionArgs);
    }

    @Override
    public int delete(@NonNull SQLiteDatabase db, String selection, String[] selectionArgs) {
        return db.delete(mTableName, selection, selectionArgs);
    }

    @Override
    public Cursor query(@NonNull SQLiteDatabase db, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Using SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        queryBuilder.setTables(mTableName);

        fixIdColumnProjection(projection);

        return queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder);
    }

    // -------------------------------------------------------------------------------------------
    // Helping methods
    // -------------------------------------------------------------------------------------------

    /**
     * Adds to projection alias for {@link BaseColumns#_ID} column that is used in Android's UI methods.
     *
     * @param projection Projection to be fixed
     */
    void fixIdColumnProjection(String[] projection) {
        for (int i = 0; i < projection.length; i++) {
            if (projection[i].equals(BaseColumns._ID)) {
                projection[i] = mTableName + "." + BaseColumns._ID + " as " + BaseColumns._ID;
            }
        }
    }

    /**
     * Finds next non-conflict table ID, that could be used for custom entries (like custom portal)
     *
     * @param db Database to work with
     * @return Next recommended table ID
     */
    protected long findNextCustomId(@NonNull SQLiteDatabase db) {
        // Prepare query
        try (Cursor cursor = db.query(mTableName, new String[]{"MAX(" + BaseColumns._ID + ")"},
                null, null, null, null, null)) {
            // Get values
            cursor.moveToFirst();
            long maxId = cursor.getLong(0);

            // Count result
            if (maxId >= ProviderContract.CUSTOM_DATA_OFFSET) {
                return maxId + 1;
            } else {
                return ProviderContract.CUSTOM_DATA_OFFSET;
            }
        }
    }
}
