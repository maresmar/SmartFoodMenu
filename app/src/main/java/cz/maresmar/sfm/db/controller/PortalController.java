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

import cz.maresmar.sfm.db.DbContract;
import cz.maresmar.sfm.db.DbContract.Portal;
import cz.maresmar.sfm.db.DbContract.PortalGroup;

/**
 * Controller around Portal table
 */
public class PortalController extends SimpleController {

    private static final String mQueryTables = Portal.TABLE_NAME + " INNER JOIN " +
            PortalGroup.TABLE_NAME + " ON (" + PortalGroup.TABLE_NAME + "." + PortalGroup._ID + " = " +
            Portal.TABLE_NAME + "." + Portal.COLUMN_NAME_PGID + ")";

    /**
     * Creates new controller
     */
    public PortalController() {
        super(Portal.TABLE_NAME);
    }

    @Override
    public long insert(@NonNull SQLiteDatabase db, ContentValues newValues) {
        if(!newValues.containsKey(Portal._ID)) {
            long newId = findNextCustomId(db);
            newValues.put(Portal._ID, newId);
        }

        return insertOrUpdate(db, mTableName, newValues, new String[0]);
    }

    @Override
    public Cursor query(@NonNull SQLiteDatabase db, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Using SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        String tables;
        if(selection.contains(DbContract.Credential.COLUMN_NAME_UID)) {
            // Filter rows with correct userId
            long userId = getUserId(selection);
            tables = mQueryTables +
                    // Credential table
                    " INNER JOIN " + DbContract.Credential.TABLE_NAME +
                    " ON ((" + DbContract.Credential.TABLE_NAME + "." + DbContract.Credential.COLUMN_NAME_PGID + " = " +
                    DbContract.Portal.TABLE_NAME + "." + Portal.COLUMN_NAME_PGID + ") AND (" +
                    DbContract.Credential.TABLE_NAME + "." + DbContract.Credential.COLUMN_NAME_UID + " = " +
                    userId + "))";
        } else {
            // Takes all rows
            tables = mQueryTables;
        }

        queryBuilder.setTables(tables);

        fixIdColumnProjection(projection);

        return queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder);
    }

    private long getUserId(String selection) {
        return getIdFromSelection(selection, DbContract.Credential.COLUMN_NAME_UID);
    }
}
