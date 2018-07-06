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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.support.annotation.NonNull;

import cz.maresmar.sfm.db.DbContract;
import cz.maresmar.sfm.provider.PublicProviderContract;

/**
 * Controller around MenuEntry table (with user id)
 */
public class MenuEntryController extends SimpleMenuEntryController {

    @Override
    public Cursor query(@NonNull SQLiteDatabase db, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        // Using SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        long userId = getUserId(selection);
        String userQueryTables = mPortalQueryTables +
                // Portal table
                " INNER JOIN " + DbContract.Portal.TABLE_NAME +
                " ON (" + DbContract.Portal.TABLE_NAME + "." + DbContract.Portal._ID + " = " +
                DbContract.MenuEntry.TABLE_NAME + "." + DbContract.MenuEntry.COLUMN_NAME_PID + ")" +
                // Credential table
                " INNER JOIN " + DbContract.Credential.TABLE_NAME +
                " ON (" + DbContract.Credential.TABLE_NAME + "." + DbContract.Credential.COLUMN_NAME_PGID + " = " +
                DbContract.Portal.TABLE_NAME + "." + DbContract.Portal.COLUMN_NAME_PGID + " AND " +
                DbContract.Credential.TABLE_NAME + "." + DbContract.Credential.COLUMN_NAME_UID + " = " +
                userId + ")" +
                // Group menu entry
                " LEFT OUTER JOIN " + DbContract.GroupMenuEntry.TABLE_NAME +
                " ON (" + DbContract.GroupMenuEntry.TABLE_NAME + "." + DbContract.GroupMenuEntry.COLUMN_NAME_CGID + " = " +
                DbContract.Credential.TABLE_NAME + "." + DbContract.Credential.COLUMN_NAME_CGID + " AND " +
                DbContract.GroupMenuEntry.TABLE_NAME + "." + DbContract.GroupMenuEntry.COLUMN_NAME_ME_PID + " = " +
                DbContract.MenuEntry.TABLE_NAME + "." + DbContract.MenuEntry.COLUMN_NAME_PID + " AND " +
                DbContract.GroupMenuEntry.TABLE_NAME + "." + DbContract.GroupMenuEntry.COLUMN_NAME_ME_RELATIVE_ID + " = " +
                DbContract.MenuEntry.TABLE_NAME + "." + DbContract.MenuEntry.COLUMN_NAME_RELATIVE_ID + ")" +
                // Synced Food action
                " LEFT OUTER JOIN " + DbContract.FoodAction.TABLE_NAME + " AS " + DbContract.MenuEntry.SYNCED_ACTION_TABLE_ALIAS +
                " ON (" + DbContract.Credential.TABLE_NAME + "." + DbContract.Credential._ID + " = " +
                DbContract.MenuEntry.SYNCED_ACTION_TABLE_ALIAS + "." + DbContract.FoodAction.COLUMN_NAME_CID + " AND " +
                DbContract.MenuEntry.TABLE_NAME + "." + DbContract.MenuEntry.COLUMN_NAME_PID + " = " +
                DbContract.MenuEntry.SYNCED_ACTION_TABLE_ALIAS + "." + DbContract.FoodAction.COLUMN_NAME_ME_PID + " AND " +
                DbContract.MenuEntry.TABLE_NAME + "." + DbContract.MenuEntry.COLUMN_NAME_RELATIVE_ID + " = " +
                DbContract.MenuEntry.SYNCED_ACTION_TABLE_ALIAS + "." + DbContract.FoodAction.COLUMN_NAME_ME_RELATIVE_ID + " AND " +
                DbContract.MenuEntry.SYNCED_ACTION_TABLE_ALIAS + "." + DbContract.FoodAction.COLUMN_NAME_SYNC_STATUS + " = " +
                PublicProviderContract.ACTION_SYNC_STATUS_SYNCED + ")" +
                // Local Food action
                " LEFT OUTER JOIN " + DbContract.FoodAction.TABLE_NAME + " AS " + DbContract.MenuEntry.LOCAL_ACTION_TABLE_ALIAS +
                " ON (" + DbContract.Credential.TABLE_NAME + "." + DbContract.Credential._ID + " = " +
                DbContract.MenuEntry.LOCAL_ACTION_TABLE_ALIAS + "." + DbContract.FoodAction.COLUMN_NAME_CID + " AND " +
                DbContract.MenuEntry.TABLE_NAME + "." + DbContract.MenuEntry.COLUMN_NAME_PID + " = " +
                DbContract.MenuEntry.LOCAL_ACTION_TABLE_ALIAS + "." + DbContract.FoodAction.COLUMN_NAME_ME_PID + " AND " +
                DbContract.MenuEntry.TABLE_NAME + "." + DbContract.MenuEntry.COLUMN_NAME_RELATIVE_ID + " = " +
                DbContract.MenuEntry.LOCAL_ACTION_TABLE_ALIAS + "." + DbContract.FoodAction.COLUMN_NAME_ME_RELATIVE_ID + " AND " +
                DbContract.MenuEntry.LOCAL_ACTION_TABLE_ALIAS + "." + DbContract.FoodAction.COLUMN_NAME_SYNC_STATUS + " = " +
                PublicProviderContract.ACTION_SYNC_STATUS_LOCAL + ")" +
                // Edit Food action
                " LEFT OUTER JOIN " + DbContract.FoodAction.TABLE_NAME + " AS " + DbContract.MenuEntry.EDIT_ACTION_TABLE_ALIAS +
                " ON (" + DbContract.Credential.TABLE_NAME + "." + DbContract.Credential._ID + " = " +
                DbContract.MenuEntry.EDIT_ACTION_TABLE_ALIAS + "." + DbContract.FoodAction.COLUMN_NAME_CID + " AND " +
                DbContract.MenuEntry.TABLE_NAME + "." + DbContract.MenuEntry.COLUMN_NAME_PID + " = " +
                DbContract.MenuEntry.EDIT_ACTION_TABLE_ALIAS + "." + DbContract.FoodAction.COLUMN_NAME_ME_PID + " AND " +
                DbContract.MenuEntry.TABLE_NAME + "." + DbContract.MenuEntry.COLUMN_NAME_RELATIVE_ID + " = " +
                DbContract.MenuEntry.EDIT_ACTION_TABLE_ALIAS + "." + DbContract.FoodAction.COLUMN_NAME_ME_RELATIVE_ID + " AND " +
                DbContract.MenuEntry.EDIT_ACTION_TABLE_ALIAS + "." + DbContract.FoodAction.COLUMN_NAME_SYNC_STATUS + " = " +
                PublicProviderContract.ACTION_SYNC_STATUS_EDIT + ")";

        queryBuilder.setTables(userQueryTables);

        fixIdColumnProjection(projection);

        return queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder);
    }

    private long getUserId(String selection) {
        return getIdFromSelection(selection, DbContract.Credential.COLUMN_NAME_UID);
    }
}
