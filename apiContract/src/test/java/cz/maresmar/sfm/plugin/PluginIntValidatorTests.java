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

package cz.maresmar.sfm.plugin;

import org.junit.Test;

import cz.maresmar.sfm.plugin.ActionContract;

import static org.junit.Assert.*;

/**
 * Local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class PluginIntValidatorTests {
    @Test
    public void Constants() {
        assertTrue(ActionContract.isValidPluginTaskInt(ActionContract.TASK_MENU_SYNC));
        assertTrue(ActionContract.isValidPluginTaskInt(ActionContract.TASK_ACTION_HISTORY_SYNC));
        assertTrue(ActionContract.isValidPluginTaskInt(ActionContract.TASK_MENU_SYNC | ActionContract.TASK_ACTION_HISTORY_SYNC));
        assertTrue(ActionContract.isValidPluginTaskInt(ActionContract.TASK_MENU_SYNC | ActionContract.TASK_ACTION_HISTORY_SYNC |
            ActionContract.TASK_GROUP_DATA_MENU_SYNC));
    }

    @Test
    public void LowerLimit()  {
        assertTrue(ActionContract.isValidPluginTaskInt(0));
        assertFalse(ActionContract.isValidPluginTaskInt(-1));
    }

    @Test
    public void UpperLimit()  {
        assertTrue(ActionContract.isValidPluginTaskInt((1 << ActionContract.PLUGIN_TASKS_LENGTH)-1));
        assertFalse(ActionContract.isValidPluginTaskInt(1 << ActionContract.PLUGIN_TASKS_LENGTH));
    }

    @Test
    public void RandomBig() {
        assertFalse(ActionContract.isValidPluginTaskInt(Integer.MAX_VALUE));
        assertFalse(ActionContract.isValidPluginTaskInt(Integer.MIN_VALUE));
    }
}