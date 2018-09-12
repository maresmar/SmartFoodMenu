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
import android.support.annotation.NonNull;

import cz.maresmar.sfm.Assert;
import cz.maresmar.sfm.BuildConfig;
import cz.maresmar.sfm.db.DbContract;
import cz.maresmar.sfm.db.DbContract.FoodAction;
import cz.maresmar.sfm.provider.PublicProviderContract;

/**
 * Controller around Actions table
 */
public class ActionController extends SimpleController {

    private static final String mQueryTables = FoodAction.TABLE_NAME +
            // Synced action entry (self join)
            " LEFT OUTER JOIN " + FoodAction.TABLE_NAME + " AS " + FoodAction.SYNCED_TABLE_ALIAS +
            " ON (" + FoodAction.TABLE_NAME + "." + FoodAction.COLUMN_NAME_CID + " = " +
            FoodAction.SYNCED_TABLE_ALIAS + "." + FoodAction.COLUMN_NAME_CID + " AND " +
            FoodAction.TABLE_NAME + "." + FoodAction.COLUMN_NAME_ME_RELATIVE_ID + " = " +
            FoodAction.SYNCED_TABLE_ALIAS + "." + FoodAction.COLUMN_NAME_ME_RELATIVE_ID + " AND " +
            FoodAction.TABLE_NAME + "." + FoodAction.COLUMN_NAME_ME_PID + " = " +
            FoodAction.SYNCED_TABLE_ALIAS + "." + FoodAction.COLUMN_NAME_ME_PID + " AND " +
            FoodAction.SYNCED_TABLE_ALIAS + "." + FoodAction.COLUMN_NAME_SYNC_STATUS + " = " +
            PublicProviderContract.ACTION_SYNC_STATUS_SYNCED + ")" +
            // Menu entry table
            " LEFT OUTER JOIN " + DbContract.MenuEntry.TABLE_NAME +
            " ON (" + FoodAction.TABLE_NAME + "." + FoodAction.COLUMN_NAME_ME_PID + " = " +
            DbContract.MenuEntry.TABLE_NAME + "." + DbContract.MenuEntry.COLUMN_NAME_PID + " AND " +
            FoodAction.TABLE_NAME + "." + FoodAction.COLUMN_NAME_ME_RELATIVE_ID + " = " +
            DbContract.MenuEntry.TABLE_NAME + "." + DbContract.MenuEntry.COLUMN_NAME_RELATIVE_ID + ")" +
            // Food table
            " LEFT OUTER JOIN " + DbContract.Food.TABLE_NAME +
            " ON (" + DbContract.Food.TABLE_NAME + "." + DbContract.Food._ID + " = " +
            DbContract.MenuEntry.COLUMN_NAME_FID + ")" +
            // Menu group table
            " LEFT OUTER JOIN " + DbContract.MenuGroup.TABLE_NAME +
            " ON (" + DbContract.MenuGroup.TABLE_NAME + "." + DbContract.MenuGroup._ID + " = " +
            DbContract.MenuEntry.COLUMN_NAME_MGID + ")" +
            // Portal table
            " LEFT OUTER JOIN " + DbContract.Portal.TABLE_NAME +
            " ON (" + DbContract.Portal.TABLE_NAME + "." + DbContract.Portal._ID + " = " +
            FoodAction.TABLE_NAME + "." + FoodAction.COLUMN_NAME_ME_PID + ")";

    private static final String CIDS_LOADER_SELECT = "SELECT " + DbContract.Credential._ID +
            " FROM " + DbContract.Credential.TABLE_NAME +
            " WHERE " + DbContract.Credential.COLUMN_NAME_UID + " = ";

    /**
     * Creates new controller
     */
    public ActionController() {
        super(FoodAction.TABLE_NAME);
    }

    @Override
    public long insert(@NonNull SQLiteDatabase db, ContentValues newValues) {
        return insertOrUpdate(db, mTableName, newValues,
                new String[]{
                        FoodAction.COLUMN_NAME_FA_RELATIVE_ID,
                        FoodAction.COLUMN_NAME_CID,
                        FoodAction.COLUMN_NAME_SYNC_STATUS
                });
    }

    @Override
    public int update(@NonNull SQLiteDatabase db, ContentValues values, String selection, String[] selectionArgs) {
        if (selection != null && selection.contains(DbContract.Credential.COLUMN_NAME_UID)) {
            long userId = getUserId(selection);
            selection = selection.replaceAll("\\( *" + DbContract.Credential.COLUMN_NAME_UID + " *= *(-?\\d+) *\\)", "") +
                    FoodAction.COLUMN_NAME_CID + " IN (" + CIDS_LOADER_SELECT + userId + ")";
        }

        return super.update(db, values, selection, selectionArgs);
    }

    @Override
    public int delete(@NonNull SQLiteDatabase db, String selection, String[] selectionArgs) {
        if (selection != null && selection.contains(DbContract.Credential.COLUMN_NAME_UID)) {
            long userId = getUserId(selection);
            selection = selection.replaceAll("\\( *" + DbContract.Credential.COLUMN_NAME_UID + " *= *(-?\\d+) *\\)", "") +
                    FoodAction.COLUMN_NAME_CID + " IN (" + CIDS_LOADER_SELECT + userId + ")";
        }

        return super.delete(db, selection, selectionArgs);
    }

    @Override
    public Cursor query(@NonNull SQLiteDatabase db, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Using SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        String tables;
        if (selection.contains(DbContract.Credential.COLUMN_NAME_UID)) {
            // Filter rows with correct userId
            long userId = getUserId(selection);
            tables = mQueryTables +
                    // Credential table
                    " INNER JOIN " + DbContract.Credential.TABLE_NAME +
                    " ON (" + DbContract.Credential.TABLE_NAME + "." + DbContract.Credential._ID + " = " +
                    DbContract.FoodAction.TABLE_NAME + "." + FoodAction.COLUMN_NAME_CID + " AND " +
                    DbContract.Credential.TABLE_NAME + "." + DbContract.Credential.COLUMN_NAME_UID + " = " +
                    userId + ")" +
                    // Group menu entry
                    " LEFT OUTER JOIN " + DbContract.GroupMenuEntry.TABLE_NAME +
                    " ON (" + DbContract.GroupMenuEntry.TABLE_NAME + "." + DbContract.GroupMenuEntry.COLUMN_NAME_CGID + " = " +
                    DbContract.Credential.TABLE_NAME + "." + DbContract.Credential.COLUMN_NAME_CGID + " AND " +
                    DbContract.GroupMenuEntry.TABLE_NAME + "." + DbContract.GroupMenuEntry.COLUMN_NAME_ME_PID + " = " +
                    FoodAction.TABLE_NAME + "." + FoodAction.COLUMN_NAME_ME_PID + " AND " +
                    DbContract.GroupMenuEntry.TABLE_NAME + "." + DbContract.GroupMenuEntry.COLUMN_NAME_ME_RELATIVE_ID + " = " +
                    FoodAction.TABLE_NAME + "." + FoodAction.COLUMN_NAME_ME_RELATIVE_ID + ")";
        } else if (selection.contains(DbContract.FoodAction.COLUMN_NAME_CID)) {
            // Filter rows with correct userId
            long credentialId = getIdFromSelection(selection, DbContract.FoodAction.COLUMN_NAME_CID);
            tables = mQueryTables +
                    // Credential table
                    " LEFT OUTER JOIN " + DbContract.Credential.TABLE_NAME +
                    " ON (" + DbContract.Credential.TABLE_NAME + "." + DbContract.Credential._ID + " = " + credentialId + ")" +
                    // Group menu entry
                    " LEFT OUTER JOIN " + DbContract.GroupMenuEntry.TABLE_NAME +
                    " ON (" + DbContract.GroupMenuEntry.TABLE_NAME + "." + DbContract.GroupMenuEntry.COLUMN_NAME_CGID + " = " +
                    DbContract.Credential.TABLE_NAME + "." + DbContract.Credential.COLUMN_NAME_CGID + " AND " +
                    DbContract.GroupMenuEntry.TABLE_NAME + "." + DbContract.GroupMenuEntry.COLUMN_NAME_ME_PID + " = " +
                    FoodAction.TABLE_NAME + "." + FoodAction.COLUMN_NAME_ME_PID + " AND " +
                    DbContract.GroupMenuEntry.TABLE_NAME + "." + DbContract.GroupMenuEntry.COLUMN_NAME_ME_RELATIVE_ID + " = " +
                    FoodAction.TABLE_NAME + "." + FoodAction.COLUMN_NAME_ME_RELATIVE_ID + ")";
        } else {
            // Takes all rows
            tables = mQueryTables;
        }

        queryBuilder.setTables(tables);

        fixIdColumnProjection(projection);

        return queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder);

    }

    /**
     * Finds Credential ID from user and portal ID
     * @param db Database to work with
     * @param userId ID of user
     * @param portalId ID of portal
     * @return Found credentials ID
     */
    public static long findCredentialId(@NonNull SQLiteDatabase db, long userId, long portalId) {
        // Build query
        String table = DbContract.Portal.TABLE_NAME +
                // Credential table
                " INNER JOIN " + DbContract.Credential.TABLE_NAME +
                " ON (" + DbContract.Credential.TABLE_NAME + "." + DbContract.Credential.COLUMN_NAME_PGID + " = " +
                DbContract.Portal.TABLE_NAME + "." + DbContract.Portal.COLUMN_NAME_PGID + " AND " +
                DbContract.Credential.TABLE_NAME + "." + DbContract.Credential.COLUMN_NAME_UID + " = " +
                userId + ")";
        String[] projection = new String[]{DbContract.Credential.TABLE_NAME + "." + DbContract.Credential._ID};
        String selection = DbContract.Portal.TABLE_NAME + "." + DbContract.Portal._ID + " = " + portalId;

        // Ask for result
        try (Cursor cursor = db.query(table, projection, selection, null, null,
                null, null)) {
            // Get result
            cursor.moveToFirst();
            if (BuildConfig.DEBUG) {
                Assert.isOne(cursor.getCount());
            }
            return cursor.getLong(0);
        }
    }

    private long getUserId(String selection) {
        return getIdFromSelection(selection, DbContract.Credential.COLUMN_NAME_UID);
    }
}
