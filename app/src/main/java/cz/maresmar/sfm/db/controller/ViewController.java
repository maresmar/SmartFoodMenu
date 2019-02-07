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
import android.provider.BaseColumns;
import androidx.annotation.NonNull;

import java.util.Map;

import cz.maresmar.sfm.Assert;
import cz.maresmar.sfm.BuildConfig;

/**
 * Abstract table view controller that handles SQL operations on one view.
 *
 * <p>Has some {@code static} methods to solve Android's SQLite library weird error handling.
 * One method could return {@link IllegalArgumentException} and {@code -1} there when some error happen,
 * so these methods returns only {@link IllegalArgumentException} or valid ID</p>
 */
public abstract class ViewController {

    // -------------------------------------------------------------------------------------------
    // Database handling
    // -------------------------------------------------------------------------------------------

    /**
     * Inserts new values to table associated with object
     *
     * @param db        Database to work with
     * @param newValues Values to be inserted
     * @return ID of new inserted row or throws {@link IllegalArgumentException} (if some table constraint is
     * violated)
     */
    public abstract long insert(@NonNull SQLiteDatabase db, ContentValues newValues);

    /**
     * Inserts new values to specific table
     *
     * @param db        Database to work with
     * @param tableName Name of table for inserting
     * @param newValues Values to be inserted (the table prefix will be cut off from column names)
     * @return ID of new inserted row or throws {@link IllegalArgumentException} (if some table constraint is
     * violated)
     */
    protected static long insertOrThrow(@NonNull SQLiteDatabase db, @NonNull String tableName, ContentValues newValues) {

        newValues = cutOffTablePrefix(tableName, newValues);

        return insertOrThrowStripped(db, tableName, newValues);
    }

    /**
     * Inserts new values to specific table (with columns without table prefix)
     *
     * @param db        Database to work with
     * @param tableName Name of table for inserting
     * @param newValues Values to be inserted (with columns without table prefix)
     * @return ID of new inserted row or throws {@link IllegalArgumentException} (if some table constraint is
     * violated)
     */
    private static long insertOrThrowStripped(@NonNull SQLiteDatabase db, @NonNull String tableName,
                                              ContentValues newValues) {
        long newEntryId = db.insert(tableName, null, newValues);

        if (newEntryId == -1) {
            throw new IllegalArgumentException("Cannot insert " + newValues + " to " + tableName);
        }

        return newEntryId;
    }

    /**
     * Inserts or update new values to specific table
     *
     * @param newValues     Values to be inserted or updated
     * @param tableName     Name of table for inserting or updating
     * @param uniqueColumns Columns that together making unique identification in table so we can
     *                      find corresponding _ID using them (and values from {@code newValues}
     * @return ID of new changed row or throws {@link IllegalArgumentException} (if some table constraint
     * violated)
     */
    protected static long insertOrUpdate(@NonNull SQLiteDatabase db, @NonNull String tableName,
                                         ContentValues newValues, String[] uniqueColumns) {
        newValues = cutOffTablePrefix(tableName, newValues);

        // Search arguments
        StringBuilder selection = new StringBuilder();
        String[] args;

        // Finds used _ID
        if (newValues.containsKey(BaseColumns._ID)) {
            // Prepare select from _ID column
            selection.append(BaseColumns._ID + " = ?");
            args = new String[1];
            args[0] = "" + newValues.getAsLong(BaseColumns._ID);
        } else {

            // Prepare select from uniqueColumns column
            args = new String[uniqueColumns.length];
            // Find values
            for (int i = 0; i < uniqueColumns.length; i++) {

                if (selection.length() != 0) {
                    selection.append(" AND ");
                }

                // Ads new key to selection
                if (BuildConfig.DEBUG) {
                    Assert.that(uniqueColumns[i].indexOf('.') == -1,
                            "Column %s cannot be prefixed", uniqueColumns[i]);
                }
                selection.append(uniqueColumns[i]).append(" = ?");

                // Finds value
                if (newValues.containsKey(uniqueColumns[i])) {
                    args[i] = "" + newValues.get(uniqueColumns[i]);
                } else {
                    // Don't have enough values to identify the row so I can only do insert
                    return insertOrThrowStripped(db, tableName, newValues);
                }
            }
        }

        // Do the query for original ID
        long originalRowId;
        try (Cursor cursor = db.query(tableName, new String[]{BaseColumns._ID}, selection.toString(),
                args, null, null, null)) {
            boolean hasEntryInTable = cursor.moveToFirst();

            if (hasEntryInTable) {
                // Update existing row in table
                originalRowId = cursor.getLong(0);
            } else {
                // Insert new row to table
                return insertOrThrowStripped(db, tableName, newValues);
            }
        }

        // Update the table with new values

        int affectedRows = db.update(tableName, newValues, BaseColumns._ID + " = ?",
                new String[]{"" + originalRowId});

        if (affectedRows != 1) {
            throw new IllegalArgumentException("Cannot update " + newValues.toString() + " in " + tableName);
        }

        return originalRowId;

    }

    /**
     * Updates values in table associated with object
     *
     * @param db            Database to work with
     * @param values        Values to be updated
     * @param selection     Update selection
     * @param selectionArgs Update selection args
     * @return Number of changed rows
     */
    public abstract int update(@NonNull SQLiteDatabase db, ContentValues values, String selection,
                               String[] selectionArgs);

    /**
     * Updates values in specific table
     *
     * @param db            Database to work with
     * @param table         Table to be updated
     * @param values        Values to be updated (the table prefix will be cut off from column names)
     * @param selection     Update selection
     * @param selectionArgs Update selection args
     * @return Number of changed rows
     */
    protected static int update(@NonNull SQLiteDatabase db, String table, ContentValues values, String selection,
                                String[] selectionArgs) {
        values = cutOffTablePrefix(table, values);

        return db.update(table, values, selection, selectionArgs);
    }

    /**
     * Delete from table associated with object
     *
     * @param db            Database to work with
     * @param selection     Delete selection
     * @param selectionArgs Delete selection args
     * @return Number of deleted rows
     */
    public abstract int delete(@NonNull SQLiteDatabase db, String selection, String[] selectionArgs);

    /**
     * Query table associated with object
     *
     * @param db            Database to work with
     * @param projection    Select projection
     * @param selection     Select selection
     * @param selectionArgs Select selection args
     * @param sortOrder     Select sort order
     * @return Cursor with data or throws {@link IllegalArgumentException}
     */
    public abstract Cursor query(@NonNull SQLiteDatabase db, String[] projection, String selection,
                                 String[] selectionArgs, String sortOrder);

    // -------------------------------------------------------------------------------------------
    // Helping methods
    // -------------------------------------------------------------------------------------------

    /**
     * Parse ID value from selection String
     *
     * @param selection  Selection to search within
     * @param columnName Name of column to be found
     * @return Parsed value
     */
    protected long getIdFromSelection(String selection, String columnName) {
        return Long.parseLong(selection.replaceFirst(".*" + columnName + " *= *(-?\\d+).*",
                "$1"));
    }

    private static ContentValues cutOffTablePrefix(String tableName, ContentValues newValues) {
        ContentValues fixedValues = null;
        for (Map.Entry<String, Object> value : newValues.valueSet()) {
            String key = value.getKey();
            if (key.startsWith(tableName + ".")) {
                String newKey = key.replace(tableName + ".", "");

                if (fixedValues == null) {
                    fixedValues = new ContentValues();
                    fixedValues.putAll(newValues);
                }

                fixedValues.remove(key);
                if (value.getValue() != null) {
                    fixedValues.put(newKey, value.getValue().toString());
                } else {
                    fixedValues.putNull(newKey);
                }
            }
        }

        if (fixedValues != null) {
            return fixedValues;
        } else {
            return newValues;
        }
    }
}
