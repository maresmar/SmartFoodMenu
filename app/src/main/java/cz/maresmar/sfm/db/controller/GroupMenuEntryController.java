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
import android.support.annotation.NonNull;

import cz.maresmar.sfm.db.DbContract.GroupMenuEntry;

/**
 * Controller around GroupMenuEntry table
 */
public class GroupMenuEntryController extends SimpleController {

    /**
     * Creates new controller
     */
    public GroupMenuEntryController()  {
        super(GroupMenuEntry.TABLE_NAME);
    }

    @Override
    public long insert(@NonNull SQLiteDatabase db, ContentValues newValues) {
        return insertOrUpdate(db, mTableName, newValues, new String[] { GroupMenuEntry.COLUMN_NAME_CGID,
                GroupMenuEntry.COLUMN_NAME_ME_RELATIVE_ID, GroupMenuEntry.COLUMN_NAME_ME_PID});
    }
}
