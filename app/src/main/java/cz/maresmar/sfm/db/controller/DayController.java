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

/**
 * Controller around day view, this view extracts days existing in MenuEntry table
 */
public class DayController extends SimpleController {

    /**
     * Creates new controller
     */
    public DayController() {
        super(DbContract.MenuEntry.TABLE_NAME);
    }

    // -------------------------------------------------------------------------------------------
    // Abstract class methods
    // -------------------------------------------------------------------------------------------

    @Override
    public Cursor query(@NonNull SQLiteDatabase db, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        // Using SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        long userId = getUserId(selection);
        String userQueryTables = mTableName +
                // Portal table
                " INNER JOIN " + DbContract.Portal.TABLE_NAME +
                " ON (" + DbContract.Portal.TABLE_NAME + "." + DbContract.Portal._ID + " = " +
                DbContract.MenuEntry.COLUMN_NAME_PID + ")" +
                // Credential table
                " INNER JOIN " + DbContract.Credential.TABLE_NAME +
                " ON (" + DbContract.Credential.TABLE_NAME + "." + DbContract.Credential.COLUMN_NAME_PGID + " = " +
                DbContract.Portal.TABLE_NAME + "." + DbContract.Portal.COLUMN_NAME_PGID + " AND " +
                DbContract.Credential.TABLE_NAME + "." + DbContract.Credential.COLUMN_NAME_UID + " = " +
                userId + ")";

        queryBuilder.setTables(userQueryTables);

        fixIdColumnProjection(projection);

        return queryBuilder.query(db, projection, selection,
                selectionArgs, DbContract.MenuEntry.COLUMN_NAME_DATE, null, sortOrder);
    }

    // -------------------------------------------------------------------------------------------
    // Helping methods
    // -------------------------------------------------------------------------------------------

    private long getUserId(String selection) {
        return getIdFromSelection(selection, DbContract.Credential.COLUMN_NAME_UID);
    }
}
