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

package cz.maresmar.sfm.view;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;

/**
 * Interface that allows fragments work as data form
 * <p>
 * Meaning the form can be saved, validated and discarded
 * </p>
 */
public interface DataForm {

    /**
     * Inserts fragment content to database or updates existing data if fragment was loaded with
     * previous data
     *
     * @return Uri that points to saved data
     */
    @Nullable
    default Uri saveData() {
        return null;
    }

    /**
     * Removes temp data from database and file system (only if fragment was started without any
     * input data - meaning for creating of new entry)
     *
     * @param context Some valid context
     */
    default void discardTempData(@NonNull Context context) {
    }

    /**
     * Tests if fragment contains valid data, so the data could be potentially saved
     *
     * @return {@code true} if data are valid, {@code false} otherwise
     */
    @UiThread
    boolean hasValidData();

    /**
     * Interface that allows host activity to watch for data validity changes
     */
    interface DataValidityListener {
        /**
         * Called when data validity changes
         *
         * @param source   Source fragment
         * @param newState New validity state ({@code true} if data are valid, {@code false} otherwise)
         */
        void onDataValidityChanged(@NonNull Fragment source, boolean newState);
    }
}
