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

import android.net.Uri;
import androidx.annotation.NonNull;

import cz.maresmar.sfm.db.controller.ViewController;

/**
 * This class connect {@link ViewController} with it's mine type
 *
 * @see android.content.ContentProvider#getType(Uri)
 */
public class ViewType {

    private String mMineType;
    private ViewController mController;

    /**
     * Creates new pair with specific mine type and controller
     * @param mineType View mine type
     * @param controller View controller
     */
    public ViewType(@NonNull String mineType, @NonNull ViewController controller) {
        mMineType = mineType;
        mController = controller;
    }

    /**
     * Gets view mine type
     * @return Type of table
     */
    @NonNull
    public String getMineType() {
        return mMineType;
    }

    /**
     * Gets view controller
     * @return View controller
     */
    @NonNull
    public ViewController getController() {
        return mController;
    }
}
