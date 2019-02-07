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
import androidx.annotation.NonNull;

import cz.maresmar.sfm.provider.ProviderContract;
import timber.log.Timber;

import static cz.maresmar.sfm.db.DbContract.Food;
import static cz.maresmar.sfm.db.DbContract.MenuEntry;
import static cz.maresmar.sfm.db.DbContract.MenuGroup;

/**
 * Controller around Portal table
 */
public class SimpleMenuEntryController extends SimpleController {

    static final String mPortalQueryTables = MenuEntry.TABLE_NAME +
            // Food table
            " LEFT OUTER JOIN " + Food.TABLE_NAME +
            " ON (" + Food.TABLE_NAME + "." + Food._ID + " = " +
            MenuEntry.COLUMN_NAME_FID + ")" +
            // Menu group table
            " LEFT OUTER JOIN " + MenuGroup.TABLE_NAME +
            " ON (" + MenuGroup.TABLE_NAME + "." + MenuGroup._ID + " = " +
            MenuEntry.COLUMN_NAME_MGID + ")";

    /**
     * Create new controller
     */
    public SimpleMenuEntryController() {
        super(MenuEntry.TABLE_NAME);
    }

    // -------------------------------------------------------------------------------------------
    // Abstract class methods
    // -------------------------------------------------------------------------------------------

    @Override
    public long insert(@NonNull SQLiteDatabase db, ContentValues newValues) {
        db.beginTransaction();
        try {
            insertJoinedColumns(db, newValues);

            long newId = insertOrUpdate(db, mTableName, newValues, new String[]{
                    MenuEntry.COLUMN_NAME_RELATIVE_ID, MenuEntry.COLUMN_NAME_PID});
            db.setTransactionSuccessful();

            return newId;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public Cursor query(@NonNull SQLiteDatabase db, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Using SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        queryBuilder.setTables(mPortalQueryTables);

        fixIdColumnProjection(projection);

        return queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder);
    }

    // -------------------------------------------------------------------------------------------
    // Handling of "virtual" (in select joined columns)
    // -------------------------------------------------------------------------------------------

    private static void insertJoinedColumns(@NonNull SQLiteDatabase db, ContentValues values) {
        // Provides insert to FoodEntry
        String foodName = values.getAsString(ProviderContract.MenuEntry.TEXT);
        if (foodName != null) {
            values.remove(ProviderContract.MenuEntry.TEXT);
            long fid = findOrInsertFood(db, foodName);
            values.put(MenuEntry.COLUMN_NAME_FID, fid);
        }

        // Provides insert to GroupMenuEntry
        String groupName = values.getAsString(ProviderContract.MenuEntry.GROUP);
        if (groupName != null) {
            values.remove(ProviderContract.MenuEntry.GROUP);
            long mgid = findOrInsertMenuGroup(db, groupName);
            values.put(MenuEntry.COLUMN_NAME_MGID, mgid);
        }
    }

    /**
     * Finds or inserts Food with specific name
     *
     * @param db       Database to work with
     * @param foodName Name to be found or inserted
     */
    static long findOrInsertFood(SQLiteDatabase db, String foodName) {
        return findOrInsertToOneColumnTable(db, Food.TABLE_NAME,
                Food.COLUMN_NAME_NAME, foodName);
    }

    /**
     * Finds or inserts MenuGroup with specific name
     *
     * @param db        Database to work with
     * @param groupName Name of group to be found or inserted
     */
    static long findOrInsertMenuGroup(SQLiteDatabase db, String groupName) {
        return findOrInsertToOneColumnTable(db, MenuGroup.TABLE_NAME,
                MenuGroup.COLUMN_NAME_NAME, groupName);
    }

    static private long findOrInsertToOneColumnTable(SQLiteDatabase db, String tableName,
                                                     String colName, String colValue) {
        Cursor cursor = db.query(
                tableName, // Table
                new String[]{MenuGroup._ID}, // Selection
                colName + "=?", // Where
                new String[]{colValue}, // Where args
                null, // Group by
                null, // Having
                null // Sort by
        );
        if (cursor.getCount() > 0) {
            if (cursor.getCount() != 1) {
                Timber.wtf("Found %i entries with same name (%s) in %s",
                        cursor.getCount(), colValue, tableName);
            }
            cursor.moveToFirst();
            final long id = cursor.getLong(0);
            cursor.close();
            return id;
        } else {
            cursor.close();
            ContentValues values = new ContentValues();
            values.put(colName, colValue);
            return db.insert(tableName, null, values);
        }
    }
}
