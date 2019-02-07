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
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;

import cz.maresmar.sfm.db.DbContract.PortalGroup;

/**
 * Controller around PortalGroup table
 */
public class PortalGroupController extends SimpleController {

    /**
     * Create new controller
     */
    public PortalGroupController() {
        super(PortalGroup.TABLE_NAME);
    }

    @Override
    public long insert(@NonNull SQLiteDatabase db, ContentValues newValues) {
        if(!newValues.containsKey(PortalGroup._ID)) {
            long newId = findNextCustomId(db);
            newValues.put(PortalGroup._ID, newId);
        }

        return insertOrUpdate(db, mTableName, newValues, new String[0]);
    }
}
