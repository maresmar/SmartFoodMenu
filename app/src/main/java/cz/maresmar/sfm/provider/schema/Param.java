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

package cz.maresmar.sfm.provider.schema;

/**
 * Data model for parameters parsed from {@link android.net.Uri} and optimally {@link android.content.ContentValues}
 */
public class Param {

    /**
     * Name of parameter (column)
     */
    public final String paramName;

    /**
     * Value of parameter
     */
    public final long paramValue;

    /**
     * Create new params with given name and value
     * @param paramName Name of param
     * @param paramValue Value of param
     */
    public Param(String paramName, long paramValue) {
        this.paramName = paramName;
        this.paramValue = paramValue;
    }
}
