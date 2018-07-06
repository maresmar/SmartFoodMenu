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

package cz.maresmar.sfm.testplugin.model;

import com.google.gson.annotations.SerializedName;

public class Menu {
    @SerializedName("relativeId")
    public long relativeId;

    @SerializedName("text")
    public String text;

    @SerializedName("group")
    public String group;

    @SerializedName("label")
    public String label;

    @SerializedName("date")
    public long date;

    @SerializedName("price")
    public int price;

    @SerializedName("features")
    public int features;

    @SerializedName("remainingToOrder")
    public int remainingToOrder;

    @SerializedName("remainingToTake")
    public int remainingToTake;
}

