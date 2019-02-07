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

package cz.maresmar.sfm.service.plugin.sync;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.collection.LongSparseArray;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.CountDownLatch;

import cz.maresmar.sfm.BuildConfig;
import cz.maresmar.sfm.app.SettingsContract;
import cz.maresmar.sfm.db.DbContract;
import cz.maresmar.sfm.plugin.ActionContract;
import cz.maresmar.sfm.plugin.ActionContract.SyncTask;
import cz.maresmar.sfm.plugin.BroadcastContract;
import cz.maresmar.sfm.provider.ProviderContract;
import cz.maresmar.sfm.service.plugin.PluginUtils;
import cz.maresmar.sfm.service.web.PortalsUpdateService;
import cz.maresmar.sfm.utils.ActionUtils;
import cz.maresmar.sfm.utils.MenuUtils;
import timber.log.Timber;

import static cz.maresmar.sfm.plugin.BroadcastContract.EXTRA_CREDENTIAL_ID;
import static cz.maresmar.sfm.plugin.BroadcastContract.EXTRA_ERROR_MESSAGE;
import static cz.maresmar.sfm.plugin.BroadcastContract.EXTRA_PORTAL_ID;
import static cz.maresmar.sfm.plugin.BroadcastContract.EXTRA_TASKS_LIST;
import static cz.maresmar.sfm.plugin.BroadcastContract.EXTRA_TASKS_RESULTS;
import static cz.maresmar.sfm.plugin.BroadcastContract.EXTRA_WORST_RESULT;
import static cz.maresmar.sfm.plugin.BroadcastContract.RESULT_NOT_SUPPORTED;
import static cz.maresmar.sfm.plugin.BroadcastContract.RESULT_PLUGIN_TIMEOUT;
import static cz.maresmar.sfm.plugin.BroadcastContract.SyncResult;
import static cz.maresmar.sfm.provider.PublicProviderContract.LogData;
import static cz.maresmar.sfm.provider.PublicProviderContract.PortalFeatures;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;

/**
 * This class performs food data updates in background. It selects plugins and sends them an actions
 * to performs.
 * <p>
 * The class gives plugins some time to do what they want and then checks {@code FoodActions} results and shows
 * notification about credit and menu changes.
 * </p>
 *
 * @see ActionContract
 * @see BroadcastContract
 */
public class SyncHandler {

    // Action supported by service
    static final String ACTION = "cz.maresmar.sfm.action";

    static final int ACTION_FULL_SYNC = 0;
    static final int ACTION_CHANGES_SYNC = 1;
    static final int ACTION_REMAINING_SYNC = 2;

    @IntDef(value = {
            ACTION_FULL_SYNC,
            ACTION_CHANGES_SYNC,
            ACTION_REMAINING_SYNC
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface Action {
    }

    // Sync event broadcast
    public static final String BROADCAST_SYNC_EVENT = "cz.maresmar.sfm.broadcast.sync-result";

    private static final String SYNC_EVENT = "syncEvent";

    private static final int EVENT_STARTED = 0;
    private static final int EVENT_FINISHED = 1;

    @IntDef(value = {
            EVENT_STARTED,
            EVENT_FINISHED
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SyncEvent {
    }

    // JobDispatcher tags
    private static final String TAG_CHANGES_SYNC = "tag-activity-sync";
    private static final String TAG_PLANED_SYNC = "tag-planed-sync";

    // Action params (intended for starting this service)
    private static final String EXTRA_USER_ID = "userId";

    // Action param's constants
    private static final int UNKNOWN_ID = ActionContract.UNKNOWN_ID;

    // Filters for some actions
    @SyncTask
    private static final int TASKS_FOR_CHANGES_SYNC = ActionContract.TASK_ACTION_PRESENT_SYNC |
            ActionContract.TASK_CREDIT_SYNC;
    @SyncTask
    private static final int TASKS_FOR_REMAINING_SYNC = ActionContract.TASK_REMAINING_TO_ORDER_SYNC |
            ActionContract.TASK_REMAINING_TO_TAKE_SYNC | ActionContract.TASK_CREDIT_SYNC;
    @SuppressLint("WrongConstant")
    @SyncTask
    private static final int ALL_TASKS = (1 << ActionContract.PLUGIN_TASKS_LENGTH) - 1;

    // -------------------------------------------------------------------------------------------
    // Variables
    // -------------------------------------------------------------------------------------------

    // Update listener
    private BroadcastReceiver mSyncResultsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final long portalId = intent.getLongExtra(EXTRA_PORTAL_ID, UNKNOWN_ID);
            final long credentialsID = intent.getLongExtra(EXTRA_CREDENTIAL_ID, UNKNOWN_ID);
            final int[] doneTasks = intent.getIntArrayExtra(EXTRA_TASKS_LIST);
            @SyncResult final int[] results = intent.getIntArrayExtra(EXTRA_TASKS_RESULTS);
            @SyncResult final int worstResult = intent.getIntExtra(EXTRA_WORST_RESULT, RESULT_NOT_SUPPORTED);
            final String errorMsg = intent.getStringExtra(EXTRA_ERROR_MESSAGE);

            onPluginResultReceived(portalId, credentialsID, doneTasks, results, worstResult, errorMsg);
        }
    };

    private Context mContext;
    private CountDownLatch mRemainingResultsLatch;
    @SyncResult
    private int mWorstResult;
    private LongSparseArray<Integer> mPreviousCredit;
    private LongSparseArray<Long> mPreviousLastMenuDate;
    private SharedPreferences mPrefs;

    // -------------------------------------------------------------------------------------------
    // External starting methods
    // -------------------------------------------------------------------------------------------

    /**
     * Starts full menu sync as {@link IntentService}
     *
     * @param context Some valid context
     */
    public static void startFullSync(@NonNull Context context) {
        Intent intent = new Intent(context, SyncHandleIntentService.class);
        intent.putExtra(ACTION, ACTION_FULL_SYNC);
        context.startService(intent);
    }

    /**
     * Starts sync of remaining food in menu for specific user as {@link IntentService}
     * <p>
     * This could optimally save some mobile data.
     * </p>
     *
     * @param userId  ID of user that will be synced
     * @param context Some valid context
     */
    public static void startRemainingSync(@NonNull Context context, long userId) {
        Intent intent = new Intent(context, SyncHandleIntentService.class);
        intent.putExtra(ACTION, ACTION_REMAINING_SYNC);
        intent.putExtra(EXTRA_USER_ID, userId);
        context.startService(intent);
    }

    /**
     * Plan sync of changed actions (orders) using {@link com.firebase.jobdispatcher.JobService}
     *
     * @param context Some valid context
     */
    public static void planChangesSync(@NonNull Context context) {
        Bundle extras = new Bundle();
        extras.putInt(ACTION, ACTION_CHANGES_SYNC);

        // Create a new dispatcher using the Google Play driver. Create a new dispatc
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));

        Job job = dispatcher.newJobBuilder()
                .setService(SyncHandlerJob.class)
                .setTag(TAG_CHANGES_SYNC)
                .setRecurring(false)
                .setReplaceCurrent(true)
                .setTrigger(Trigger.executionWindow(0, 0))
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .setLifetime(Lifetime.FOREVER)
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .setExtras(extras)
                .build();

        dispatcher.mustSchedule(job);
    }

    /**
     * Plan full menu sync using {@link com.firebase.jobdispatcher.JobService}
     *
     * @param context       Some valid context
     * @param frequencyDays How often (in days) the sync should be done
     * @param unmeteredOnly If it should start only on unmetered network
     * @param chargerOnly   If it should start only when phone is in charger
     */
    public static void planFullSync(@NonNull Context context, int frequencyDays, boolean unmeteredOnly, boolean chargerOnly) {
        // Create a new dispatcher using the Google Play driver.
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));

        if (frequencyDays > 0) {
            Bundle extras = new Bundle();
            extras.putInt(ACTION, ACTION_FULL_SYNC);

            int freqSec = (int) DAYS.toSeconds(frequencyDays);
            int thresholdSec = (int) HOURS.toSeconds(4);


            Job.Builder builder = dispatcher.newJobBuilder()
                    // the JobService that will be called
                    .setService(SyncHandlerJob.class)
                    // uniquely identifies the job
                    .setTag(TAG_PLANED_SYNC)
                    // one-off job
                    .setRecurring(true)
                    // don't persist past a device reboot
                    .setLifetime(Lifetime.FOREVER)
                    // start between 0 and 60 seconds from now
                    .setTrigger(Trigger.executionWindow(freqSec - thresholdSec, freqSec + thresholdSec))
                    // don't overwrite an existing job with the same tag
                    .setReplaceCurrent(true)
                    // retry with exponential backoff
                    .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                    .setExtras(extras);

            if (unmeteredOnly) {
                // only run on an unmetered network
                builder.addConstraint(Constraint.ON_UNMETERED_NETWORK);
            }

            if (chargerOnly) {
                // only run when the device is charging
                builder.addConstraint(Constraint.DEVICE_CHARGING);
            }

            Job myJob = builder.build();

            dispatcher.mustSchedule(myJob);
        } else {
            dispatcher.cancel(TAG_PLANED_SYNC);
        }
    }

    // -------------------------------------------------------------------------------------------
    // Service lifecycle
    // -------------------------------------------------------------------------------------------

    /**
     * {@link Service#onCreate()} method equivalent in this class
     *
     * @param context Some valid context
     */
    public void onCreate(@NonNull Context context) {
        mContext = context;

        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BroadcastContract.BROADCAST_PLUGIN_SYNC_RESULT);
        mContext.registerReceiver(mSyncResultsReceiver, filter);
    }

    /**
     * {@link Service#onDestroy()} method equivalent in this class
     */
    public void onDestroy() {
        mContext.unregisterReceiver(mSyncResultsReceiver);
    }

    // -------------------------------------------------------------------------------------------
    // Start sync
    // -------------------------------------------------------------------------------------------

    /**
     * Start the sync with given actions
     *
     * @param action Action to be done
     * @param extras Action's parameters
     * @return {@code true} if the action was successful, {@code false} otherwise
     */
    boolean startAction(@Action int action, @NonNull Bundle extras) {
        broadcastStart();

        boolean successful;
        try {
            switch (action) {
                case ACTION_FULL_SYNC:
                    startPlugins(ALL_TASKS, null, null);
                    break;
                case ACTION_CHANGES_SYNC:
                    startPlugins(TASKS_FOR_CHANGES_SYNC, null, null);
                    break;
                case ACTION_REMAINING_SYNC: {
                    final long userId = extras.getLong(EXTRA_USER_ID, UNKNOWN_ID);
                    startRemainingSync(userId);
                    break;
                }
                default: {
                    Timber.e("Unknown action %d", action);
                    // As there cold be more important don't lost other tasks in queue, crash only in debug
                    if (BuildConfig.DEBUG) {
                        throw new IllegalArgumentException("Unknown action " + action);
                    }
                }
            }

            // Give plugins some time to make it done
            try {
                int pluginsTimeout = Integer.valueOf(mPrefs.getString(
                        SettingsContract.PLUGINS_TIMEOUT,
                        SettingsContract.PLUGINS_TIMEOUT_DEFAULT)
                );
                boolean doneInTime = mRemainingResultsLatch.await(pluginsTimeout,
                        java.util.concurrent.TimeUnit.SECONDS);

                if (!doneInTime) {
                    mWorstResult = RESULT_PLUGIN_TIMEOUT;
                }
            } catch (InterruptedException e) {
                Timber.e("Waiting to plugins was interrupted");
            }

            // Updates portals for next start
            if (mPrefs.getBoolean(SettingsContract.UPDATE_PORTALS_AUTOMATICALLY,
                    SettingsContract.UPDATE_PORTALS_AUTOMATICALLY_DEFAULT)) {
                PortalsUpdateService.handleActionUpdate(mContext);
            }
        } finally {
            // Check actions only if I had an connection
            if (mWorstResult != BroadcastContract.RESULT_IO_EXCEPTION &&
                    mWorstResult != BroadcastContract.RESULT_PORTAL_TEMPORALLY_INACCESSIBLE) {

                // Check actions
                ActionUtils.deleteConflictFailedActions(mContext);
                ActionUtils.checkActionResults(mContext);

                // Save time to settings
                mPrefs.edit()
                        .putLong(SettingsContract.LAST_DONE_SYNC, System.currentTimeMillis())
                        .apply();

                successful = true;
            } else {
                successful = false;
            }

            // Send broadcast
            broadcastResults(mWorstResult);
        }

        return successful;
    }

    private void startPlugins(@SyncTask int taskFilter, String logDataSelection, String[] logDataArgs) {
        mPreviousCredit = new LongSparseArray<>();
        mPreviousLastMenuDate = new LongSparseArray<>();

        // Prepare query
        String[] projection = new String[]{
                LogData.PORTAL_ID,
                LogData.CREDENTIAL_ID,
                LogData.CREDENTIALS_GROUP_ID,
                LogData.PORTAL_PLUGIN,
                LogData.PORTAL_FEATURES,
                LogData.CREDIT
        };
        String sortOrder = LogData.CREDENTIALS_GROUP_ID + " ASC, " + LogData.PORTAL_ID + " ASC";

        // Finds data
        try (Cursor cursor = mContext.getContentResolver().query(LogData.getUri(), projection,
                logDataSelection, logDataArgs, sortOrder)) {
            long lastPortalId = UNKNOWN_ID;
            long lastCredentialId = UNKNOWN_ID;

            // Number of plugins to wait for
            mRemainingResultsLatch = new CountDownLatch(cursor.getCount());

            // For each log-data entry
            while (cursor.moveToNext()) {
                final long portalId = cursor.getLong(0);
                final long credentialId = cursor.getLong(1);
                final long credentialsGroupId = cursor.getLong(2);
                final String portalPlugin = cursor.getString(3);
                @PortalFeatures final int portalFeatures = cursor.getInt(4);
                int credit;
                if (!cursor.isNull(5)) {
                    credit = cursor.getInt(5);
                } else {
                    credit = Integer.MAX_VALUE;
                }

                if (mPreviousLastMenuDate.indexOfKey(portalId) < 0) {
                    long lastMenuDate = MenuUtils.getLastMenuDate(mContext, portalId);
                    mPreviousLastMenuDate.put(portalId, lastMenuDate);
                }

                @SyncTask int taskToDo = 0;

                // If first in group
                if (lastCredentialId != credentialId) {
                    lastCredentialId = credentialId;

                    taskToDo |= ActionContract.TASK_CREDIT_SYNC;
                }

                // If first in portal
                if (lastPortalId != portalId) {
                    lastPortalId = credentialId;

                    taskToDo |= ActionContract.TASK_MENU_SYNC |
                            ActionContract.TASK_GROUP_DATA_MENU_SYNC |
                            ActionContract.TASK_REMAINING_TO_TAKE_SYNC |
                            ActionContract.TASK_REMAINING_TO_ORDER_SYNC;
                } else if ((portalFeatures & ProviderContract.FEATURE_GROUP_FULL_SYNC) ==
                        ProviderContract.FEATURE_GROUP_FULL_SYNC) { // If not first in portal and portal needs it for everyone
                    taskToDo |= ActionContract.TASK_GROUP_DATA_MENU_SYNC;
                }

                // Everyone
                taskToDo |= ActionContract.TASK_ACTION_PRESENT_SYNC |
                        ActionContract.TASK_ACTION_HISTORY_SYNC;

                // Apply filter
                taskToDo &= taskFilter;

                // If I have what to do
                if (taskToDo != 0) {
                    if (mPreviousCredit.indexOfKey(credentialId) < 0) {
                        mPreviousCredit.put(credentialId, credit);
                    }

                    startPlugin(portalPlugin, portalId, credentialId, taskToDo);
                } else {
                    mRemainingResultsLatch.countDown();
                }
            }
        }
    }

    private void startRemainingSync(long userId) {
        String logDataSelection = LogData.CREDENTIAL_ID + " IN (" +
                "SELECT " + DbContract.Credential._ID + " " +
                "FROM " + DbContract.Credential.TABLE_NAME + " " +
                "WHERE " + DbContract.Credential.COLUMN_NAME_UID + " = " + userId + ")";

        startPlugins(TASKS_FOR_REMAINING_SYNC, logDataSelection, null);
    }

    // -------------------------------------------------------------------------------------------
    // Start plugin
    // -------------------------------------------------------------------------------------------

    private void startPlugin(@NonNull String plugin, long portalId, long credentialsID, @SyncTask int pluginTasks) {
        Intent intent = PluginUtils.buildPluginIntent(plugin);

        // Puts action details
        intent.setAction(ActionContract.ACTION_SYNC);
        intent.putExtra(ActionContract.EXTRA_PORTAL_ID, portalId);
        intent.putExtra(ActionContract.EXTRA_CREDENTIAL_ID, credentialsID);
        intent.putExtra(ActionContract.EXTRA_TASKS, pluginTasks);

        // Provides access to login data
        intent.setData(LogData.getUri(portalId, credentialsID));

        // Give permissions to access data
        int rwFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

        String packageName = intent.getComponent().getPackageName();
        mContext.grantUriPermission(packageName, intent.getData(), rwFlags);
        mContext.grantUriPermission(packageName, ProviderContract.Action.getCredentialUri(credentialsID), rwFlags);
        mContext.grantUriPermission(packageName, ProviderContract.GroupMenuEntry.getPortalUri(portalId), rwFlags);
        mContext.grantUriPermission(packageName, ProviderContract.MenuEntry.getPortalUri(portalId), rwFlags);

        Timber.i("Starting plugin \"%s\" on portal %d with credential %d", plugin, portalId, credentialsID);
        // Fire start
        PluginUtils.startPlugin(mContext, intent);
    }

    // -------------------------------------------------------------------------------------------
    // Sync results
    // -------------------------------------------------------------------------------------------

    private void onPluginResultReceived(long portalId, long credentialsId, int[] doneTasks,
                                        @SyncResult int[] results, @SyncResult int worstResult,
                                        String errorMsg) {
        // Credential change check
        int previousCredit = mPreviousCredit.get(credentialsId);
        if (previousCredit != Integer.MAX_VALUE) {
            boolean changed = MenuUtils.checkCreditChanges(mContext, portalId, credentialsId, previousCredit);
            if (changed) {
                mPreviousCredit.put(credentialsId, Integer.MAX_VALUE);
            }
        }

        // Long new menu check
        long previousLastMenuDate = mPreviousLastMenuDate.get(portalId);
        if (previousLastMenuDate != 0) {
            boolean changed = MenuUtils.checkMenuChanges(mContext, portalId, previousLastMenuDate);
            if (changed) {
                mPreviousLastMenuDate.put(portalId, 0L);
            }
        }

        switch (worstResult) {
            case BroadcastContract.RESULT_OK:
                // It is OK ;-)
                break;
            case BroadcastContract.RESULT_IO_EXCEPTION:
                Timber.w("Portal %d IO exception \n%s", portalId, errorMsg);
                break;
            case BroadcastContract.RESULT_PORTAL_TEMPORALLY_INACCESSIBLE:
                // Don't have to do anything special (will be solved on the end)
                break;
            case BroadcastContract.RESULT_UNKNOWN_PORTAL_FORMAT:
                Timber.e("Portal %d changed format \n%s", portalId, errorMsg);
                break;
            case BroadcastContract.RESULT_WRONG_CREDENTIALS:
                Timber.w("Portal %d with credentials %d has wrong credentials", portalId, credentialsId);
                break;
            case BroadcastContract.RESULT_NOT_SUPPORTED:
                Timber.w("Portal %d does not support anything", portalId);
                break;
            case BroadcastContract.RESULT_PLUGIN_TIMEOUT:
                throw new IllegalArgumentException("Cannot get RESULT_PLUGIN_TIMEOUT from plugin");
            default:
                throw new UnsupportedOperationException("Unknown sync result " + worstResult);
        }

        // Update results for UI
        mWorstResult = Math.max(mWorstResult, worstResult);
        mRemainingResultsLatch.countDown();
    }

    // -------------------------------------------------------------------------------------------
    // Broadcast results to UI
    // -------------------------------------------------------------------------------------------

    private void broadcastStart() {
        Timber.i("Sync started");

        Intent intent = new Intent(BROADCAST_SYNC_EVENT);
        intent.putExtra(SYNC_EVENT, EVENT_STARTED);

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        localBroadcastManager.sendBroadcast(intent);
    }

    private void broadcastResults(@SyncResult int worstResult) {
        Timber.i("Sync finished with %d", worstResult);

        Intent intent = new Intent(BROADCAST_SYNC_EVENT);
        intent.putExtra(SYNC_EVENT, EVENT_FINISHED);
        intent.putExtra(BroadcastContract.EXTRA_WORST_RESULT, worstResult);

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        localBroadcastManager.sendBroadcast(intent);
    }

    /**
     * {@link BroadcastReceiver} that binds sync results to {@link SyncResultListener}
     * <p>
     * This is more stable to API changes in future.
     * </p>
     */
    public static class SyncResultReceiver extends BroadcastReceiver {

        SyncResultListener mListener;

        /**
         * Create new receiver with specific listener
         *
         * @param listener Listener that will receive sync results
         */
        public SyncResultReceiver(@NonNull SyncResultListener listener) {
            mListener = listener;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            @SyncEvent final int event = intent.getIntExtra(SYNC_EVENT, -1);

            switch (event) {
                case EVENT_STARTED:
                    mListener.onSyncStarted();
                    break;
                case EVENT_FINISHED: {
                    @SyncResult final int worstResult = intent.getIntExtra(BroadcastContract.EXTRA_WORST_RESULT, BroadcastContract.RESULT_NOT_SUPPORTED);
                    mListener.onSyncFinished(worstResult);
                    break;
                }
                default:
                    throw new UnsupportedOperationException("Unknown event type " + event);
            }
        }
    }

    /**
     * Sync result listener
     */
    public interface SyncResultListener {
        /**
         * Called when sync starts
         */
        void onSyncStarted();

        /**
         * Called when sync is finished
         *
         * @param worstResult The worst plugin result in whole sync (the worst result should be shown
         *                    to user)
         */
        void onSyncFinished(@SyncResult int worstResult);
    }
}
