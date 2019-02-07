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

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import cz.maresmar.sfm.plugin.ActionContract.ExtraFormatType;

/**
 * The API contract for plugins used in Broadcast. These broadcasts contains plugin results.
 */
public class BroadcastContract {
    // Intent params
    // Actions
    public static final String BROADCAST_PLUGIN_SYNC_RESULT = "cz.maresmar.sfm.broadcast.plugin-sync-result";
    public static final String BROADCAST_PORTAL_TEST_RESULT = "cz.maresmar.sfm.broadcast.portal-test-result";
    public static final String BROADCAST_EXTRA_FORMAT = "cz.maresmar.sfm.broadcast.extra-format";

    // Extras
    public static final String EXTRA_TASKS_LIST = "cz.maresmar.sfm.extra.doneTasks";
    public static final String EXTRA_PORTAL_ID = ActionContract.EXTRA_PORTAL_ID;
    public static final String EXTRA_CREDENTIAL_ID = ActionContract.EXTRA_CREDENTIAL_ID;
    public static final String EXTRA_TASKS_RESULTS = "cz.maresmar.sfm.extra.tasksResults";
    public static final String EXTRA_TEST_RESULT = "cz.maresmar.sfm.extra.testResult";
    public static final String EXTRA_WORST_RESULT = "cz.maresmar.sfm.extra.worstResult";
    public static final String EXTRA_FORMAT_DATA = "cz.maresmar.sfm.extra.formatData";
    public static final String EXTRA_PLUGIN = ActionContract.EXTRA_PLUGIN;
    public static final String EXTRA_FORMAT_TYPE = ActionContract.EXTRA_FORMAT_TYPE;
    public static final String EXTRA_ERROR_MESSAGE = "cz.maresmar.sfm.extra.errorMsg";

    // Test result
    public static final int TEST_RESULT_OK = 0;
    public static final int TEST_RESULT_INVALID_DATA = 1;

    @IntDef(flag = true, value = {
            TEST_RESULT_OK,
            TEST_RESULT_INVALID_DATA
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface TestResult {
    }

    /**
     * The sync task result. The worse result has bigger number.
     */

    public static final int RESULT_NOT_SUPPORTED = 100;
    public static final int RESULT_OK = 200;
    public static final int RESULT_PORTAL_TEMPORALLY_INACCESSIBLE = 300;
    public static final int RESULT_IO_EXCEPTION = 350;
    public static final int RESULT_WRONG_CREDENTIALS = 400;
    public static final int RESULT_UNKNOWN_PORTAL_FORMAT = 500;
    public static final int RESULT_PLUGIN_TIMEOUT = 600;

    @IntDef(value = {
            RESULT_NOT_SUPPORTED,
            RESULT_OK,
            RESULT_PORTAL_TEMPORALLY_INACCESSIBLE,
            RESULT_IO_EXCEPTION,
            RESULT_WRONG_CREDENTIALS,
            RESULT_UNKNOWN_PORTAL_FORMAT,
            RESULT_PLUGIN_TIMEOUT
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SyncResult {
    }

    // Static constants
    public static final int UNKNOWN_ID = ActionContract.UNKNOWN_ID;

    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private BroadcastContract() {
    }

    /**
     * Send broadcast to tell that update is done. Extra information contains results.
     * <p>
     * <p>This is result of {@link ActionContract#ACTION_SYNC}
     *
     * @param context        Some valid context
     * @param destinationPkg package name of application that will receive broadcast
     * @param portalId       ID of portal that was be synced
     * @param credentialsId  ID of credentials that was synced
     * @param tasks          Tasks that was done
     * @param results        Task's results
     * @param worstResult    the worst task result (the result with biggest number)
     * @param errorMsg       the worst task result's error message (used for debugging)
     * @see Intent#setPackage(String)
     */
    public static void broadcastSyncDone(@NonNull Context context, @Nullable String destinationPkg,
                                         long portalId, long credentialsId, @ActionContract.SyncTask int[] tasks,
                                         @SyncResult int[] results, @SyncResult int worstResult, String errorMsg) {
        Intent intent = new Intent();
        // Explicitly select a package to communicate with
        intent.setPackage(destinationPkg);
        intent.setAction(BROADCAST_PLUGIN_SYNC_RESULT);
        intent.putExtra(EXTRA_PORTAL_ID, portalId);
        intent.putExtra(EXTRA_CREDENTIAL_ID, credentialsId);
        intent.putExtra(EXTRA_TASKS_LIST, tasks);
        intent.putExtra(EXTRA_TASKS_RESULTS, results);
        intent.putExtra(EXTRA_WORST_RESULT, worstResult);
        intent.putExtra(EXTRA_ERROR_MESSAGE, errorMsg);
        context.sendBroadcast(intent);
    }

    /**
     * Send broadcast to tell that portal test is done. Extra information contains results.
     * <p>
     * <p>This is result of {@link ActionContract#ACTION_PORTAL_TEST}
     *
     * @param context        Some valid context
     * @param destinationPkg package name of application that will receive broadcast
     * @param portalId       ID of tested portal
     * @param testResult     Test result
     * @param errorMsg       Error message if test fails
     * @see IntentService
     * @see Intent#setPackage(String)
     */
    public static void broadcastTestDone(@NonNull Context context, @Nullable String destinationPkg,
                                         long portalId, @TestResult int testResult, @Nullable String errorMsg) {
        Intent intent = new Intent();
        // Explicitly select a package to communicate with
        intent.setPackage(destinationPkg);
        intent.setAction(BROADCAST_PORTAL_TEST_RESULT);
        intent.putExtra(EXTRA_PORTAL_ID, portalId);
        intent.putExtra(EXTRA_TEST_RESULT, testResult);
        intent.putExtra(EXTRA_ERROR_MESSAGE, errorMsg);
        context.sendBroadcast(intent);
    }

    /**
     * Send broadcast to tell that portal needs some extra values. These values are shown in UI
     * and user can change it. The format support some basic (regex) validation but the real validation
     * should be done in {@link ActionContract#ACTION_PORTAL_TEST}.
     * <p>
     * <p>This is result of {@link ActionContract#ACTION_EXTRA_FORMAT}
     *
     * @param context        Some valid context
     * @param destinationPkg Package name of application that will receive broadcast
     * @param sourcePlugin   Plugin that broadcast result
     * @param formatType     Extra format (portal or credential)
     * @param extraData      Extra format encoded as JSON
     * @see ExtraFormat
     * @see IntentService
     * @see Intent#setPackage(String)
     */
    public static void broadcastExtraFormat(@NonNull Context context, @Nullable String destinationPkg,
                                            @NonNull String sourcePlugin, @ExtraFormatType int formatType,
                                            @NonNull JSONArray extraData) {
        Intent intent = new Intent();
        // Explicitly select a package to communicate with
        intent.setPackage(destinationPkg);
        intent.setAction(BROADCAST_EXTRA_FORMAT);
        intent.putExtra(EXTRA_PLUGIN, sourcePlugin);
        intent.putExtra(EXTRA_FORMAT_TYPE, formatType);
        // Extra format
        intent.putExtra(EXTRA_FORMAT_DATA, extraData.toString());
        context.sendBroadcast(intent);
    }

}
