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

/**
 * Interface that allows fragments to ask activity to show another fragments.
 */
public interface FragmentChangeRequestListener {
    /**
     * Shows {@link cz.maresmar.sfm.view.menu.MenuDetailsFragment} in activity
     *
     * @param menuRelativeId Relative ID of menu
     * @param portalId       Portal ID of menu
     */
    void showMenuDetail(long menuRelativeId, long portalId);
}
