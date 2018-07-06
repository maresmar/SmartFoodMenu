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

package cz.maresmar.sfm.provider.repository;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

/**
 * Listener that notifies {@link android.content.ContentResolver} about changes in data. Some views could use
 * multiple tables so update to one table could cause changes in multiple views.
 */
public class NotifyChangeListener {

    /**
     * Notify about changes in {@link Uri} everyone that needs to be. Default implementation notifies only self.
     *
     * @param context Some valid context
     * @param uri     Data that was changed
     */
    @CallSuper
    public void notifyChange(@NonNull Context context, @NonNull Uri uri) {
        context.getContentResolver().notifyChange(uri, null);
    }

}
