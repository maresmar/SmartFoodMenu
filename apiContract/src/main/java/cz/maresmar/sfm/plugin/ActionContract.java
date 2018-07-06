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

import android.content.Intent;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The API contract for plugin starting used in {@link Intent} and {@link android.content.Context#sendBroadcast(Intent)}
 */
public class ActionContract {

    // Intent params
    // Actions
    public static final String ACTION_SYNC = Intent.ACTION_SYNC;
    public static final String ACTION_PORTAL_TEST = "cz.maresmar.sfm.action.portalTest";
    public static final String ACTION_EXTRA_FORMAT = "cz.maresmar.sfm.action.extraFormat";
    // Extras
    public static final String EXTRA_PORTAL_ID = "cz.maresmar.sfm.extra.portalId";
    public static final String EXTRA_CREDENTIAL_ID = "cz.maresmar.sfm.extra.credentialId";
    public static final String EXTRA_TASKS = "cz.maresmar.sfm.extra.tasks";
    public static final String EXTRA_FORMAT_TYPE = "cz.maresmar.sfm.extra.formatType";
    public static final String EXTRA_PLUGIN = "cz.maresmar.sfm.extra.plugin";

    /**
     * Broadcast for {@link android.app.job.JobScheduler} to plan start of plugin as you cannot plan
     * running of external app
     */
    public static final String BROADCAST_PLAN_RUN = "cz.maresmar.sfm.broadcast.plan-run";
    public static final String EXTRA_JOB_ID = "cz.maresmar.sfm.extra.jobId";
    public static final String EXTRA_INTENT_TO_DO = "cz.maresmar.sfm.extra.intentToDo";

    // Static constants
    public static final int UNKNOWN_ID = -1;

    // Action tasks
    public static final int PLUGIN_TASKS_LENGTH = 7;
    // Menu
    public static final int TASK_MENU_SYNC = 1;
    public static final int TASK_GROUP_DATA_MENU_SYNC = 1 << 1;
    // Remaining
    public static final int TASK_REMAINING_TO_TAKE_SYNC = 1 << 2;
    public static final int TASK_REMAINING_TO_ORDER_SYNC = 1 << 3;
    // Action
    public static final int TASK_ACTION_PRESENT_SYNC = 1 << 4;
    public static final int TASK_ACTION_HISTORY_SYNC = 1 << 5;
    // Credit
    public static final int TASK_CREDIT_SYNC = 1 << (PLUGIN_TASKS_LENGTH - 1);
    // If you extend flags size you HAVE TO update PLUGIN_TASKS_LENGTH !!!

    @IntDef(flag = true, value = {
            TASK_MENU_SYNC,
            TASK_GROUP_DATA_MENU_SYNC,
            TASK_REMAINING_TO_TAKE_SYNC,
            TASK_REMAINING_TO_ORDER_SYNC,
            TASK_ACTION_PRESENT_SYNC,
            TASK_ACTION_HISTORY_SYNC,
            TASK_CREDIT_SYNC
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SyncTask {
    }

    public static final int FORMAT_TYPE_PORTAL = 0;
    public static final int FORMAT_TYPE_CREDENTIAL = 1;
    @IntDef(flag = true, value = {
            FORMAT_TYPE_PORTAL,
            FORMAT_TYPE_CREDENTIAL
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ExtraFormatType {
    }

    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private ActionContract() {}

    /**
     * Check if given number is valid plugin task
     * @param pluginTask number to check
     * @return true if is valid false otherwise
     */
    public static boolean isValidPluginTaskInt(int pluginTask) {
        return ((pluginTask >= 0) && (pluginTask < (1 << PLUGIN_TASKS_LENGTH)));
    }

}
