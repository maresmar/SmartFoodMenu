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

import cz.maresmar.sfm.view.MainActivity;

/**
 * API contract that specifies settings related constants and default values
 */
public class SettingsContract {
    public static final String FIRST_RUN = "firstRun";
    public static final boolean FIRST_RUN_DEFAULT = true;

    public static final String LAST_USER = "lastUser";
    public static final long LAST_USER_UNKNOWN = -1;

    public static final String LAST_FRAGMENT = "lastFragment";
    public static final long LAST_FRAGMENT_UNKNOWN = MainActivity.DAY_PAGER_FRAGMENT_ID;

    public static final String PLUGINS_TIMEOUT = "pluginsTimeout";
    public static final String PLUGINS_TIMEOUT_DEFAULT = "60";

    public static final String SYNC_FREQUENCY = "syncFrequency";
    public static final String SYNC_FREQUENCY_DEFAULT = "7";

    public static final String SYNC_WHEN_APP_OPENS = "syncWhenAppOpens";
    public static final boolean SYNC_WHEN_APP_OPENS_DEFAULT = true;

    public static final String SYNC_UNMETERED_ONLY = "syncOnlyWhenUnmetered";
    public static final boolean SYNC_UNMETERED_ONLY_DEFAULT = true;

    public static final String SYNC_CHARGING_ONLY = "syncOnlyWhenCharging";
    public static final boolean SYNC_CHARGING_ONLY_DEFAULT = true;

    public static final String UPDATE_PORTALS_AUTOMATICALLY = "portalsAutoUpdate";
    public static final boolean UPDATE_PORTALS_AUTOMATICALLY_DEFAULT = true;

    public static final String LAST_DONE_SYNC = "lastSync";
    public static final long LAST_DONE_SYNC_DEFAULT = 0;

    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private SettingsContract() {}
}
