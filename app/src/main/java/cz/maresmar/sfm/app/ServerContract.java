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

package cz.maresmar.sfm.app;

/**
 * API contract that specifies server related constants
 */
public class ServerContract {

    private static final String SERVER_ADDRESS = "http://www.mares.mzf.cz/sfm";
    public static final String PORTALS_SERVER_ADDRESS = SERVER_ADDRESS + "/portals.php?format=json";
    public static final String PORTAL_GROUPS_SERVER_ADDRESS = SERVER_ADDRESS + "/portalGroups.php?format=json";

    public static final int PORTAL_JSON_API_VERSION = 1;
    public static final String REPLY_TYPE_PORTALS = "portals";
    public static final String REPLY_TYPE_PORTAL_GROUPS = "portalGroups";

    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private ServerContract() {}
}
