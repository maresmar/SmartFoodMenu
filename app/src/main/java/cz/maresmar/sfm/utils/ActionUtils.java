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

package cz.maresmar.sfm.utils;

import android.app.PendingIntent;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.ArrayList;

import cz.maresmar.sfm.Assert;
import cz.maresmar.sfm.BuildConfig;
import cz.maresmar.sfm.R;
import cz.maresmar.sfm.app.NotificationContract;
import cz.maresmar.sfm.db.DbContract;
import cz.maresmar.sfm.provider.ProviderContract;
import cz.maresmar.sfm.service.plugin.sync.SyncHandler;
import cz.maresmar.sfm.view.MainActivity;
import timber.log.Timber;

/**
 * Utils for Actions. Helps check actions for sync results and provides methods to make changes in them.
 */
public class ActionUtils {

    public static final String BROADCAST_ACTION_EVENT = "cz.maresmar.sfm.broadcast.action-event";

    public static final String ARG_EVENT = "event";
    public static final String EVENT_SYNC_FAILED = "actionSyncFailed";

    // To prevent someone from accidentally instantiating the utils class,
    // make the constructor private.
    private ActionUtils() {
    }

    /**
     * Make edits in Actions for corresponding Menu entry. These action are saved as
     * {@link ProviderContract#ACTION_SYNC_STATUS_LOCAL} then.
     * <p>
     * Handles menu group restrictions (like one order per group), in such cases creates
     * {@link ProviderContract#ACTION_ENTRY_TYPE_VIRTUAL} actions to override the
     * {@link ProviderContract#ACTION_SYNC_STATUS_SYNCED} ones. If an action reserves nothing,
     * the action is removed.
     * </p>
     *
     * @param context    Some valid context
     * @param userUri    User Uri prefix
     * @param relativeId Relative ID of corresponding Menu entry
     * @param portalId   Portal ID of corresponding Menu entry
     * @param reserved   New amount of reserved food
     * @param offered    New amount of offered food
     */
    @WorkerThread
    public static void makeEdit(@NonNull Context context, @NonNull Uri userUri, long relativeId,
                                long portalId, int reserved, int offered) {
        // Load the corresponding menu entry
        @ProviderContract.PortalFeatures
        int portalFeatures;
        long groupId;
        int price;
        long date;
        int syncedReserved, syncedOffered, syncedTaken;
        boolean hasLocal;
        int localReserved, localOffered;

        Uri menuUri = Uri.withAppendedPath(userUri, ProviderContract.MENU_ENTRY_PATH);
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        try (Cursor menuCursor = context.getContentResolver().query(
                menuUri,
                new String[]{
                        ProviderContract.MenuEntry.PORTAL_FEATURES,
                        ProviderContract.MenuEntry.GROUP_ID,
                        ProviderContract.MenuEntry.PRICE,
                        ProviderContract.MenuEntry.DATE,
                        ProviderContract.MenuEntry.SYNCED_RESERVED_AMOUNT,
                        ProviderContract.MenuEntry.SYNCED_OFFERED_AMOUNT,
                        ProviderContract.MenuEntry.SYNCED_TAKEN_AMOUNT,
                        ProviderContract.MenuEntry.LOCAL_RESERVED_AMOUNT,
                        ProviderContract.MenuEntry.LOCAL_OFFERED_AMOUNT
                },
                ProviderContract.MenuEntry.ME_RELATIVE_ID + " = " + relativeId + " AND "
                        + ProviderContract.MenuEntry.PORTAL_ID + " = " + portalId,
                null,
                null)) {

            if (BuildConfig.DEBUG) {
                Assert.isOne(menuCursor.getCount());
            }

            menuCursor.moveToFirst();

            // Info
            portalFeatures = menuCursor.getInt(0);
            groupId = menuCursor.getLong(1);
            price = menuCursor.getInt(2);
            date = menuCursor.getLong(3);

            // Synced
            syncedReserved = menuCursor.getInt(4);
            syncedOffered = menuCursor.getInt(5);
            syncedTaken = menuCursor.getInt(6);

            // Local
            hasLocal = !menuCursor.isNull(7);
            localReserved = menuCursor.getInt(7);
            localOffered = menuCursor.getInt(8);

            // Insert changes
            Uri actionUri = Uri.withAppendedPath(userUri, ProviderContract.ACTION_PATH);
            // Insert virtual group changes
            boolean restrictToOneOrderPerGroup = (portalFeatures & ProviderContract.FEATURE_RESTRICT_TO_ONE_ORDER_PER_GROUP) ==
                    ProviderContract.FEATURE_RESTRICT_TO_ONE_ORDER_PER_GROUP;

            // Delete old edits as I want something new
            if (!restrictToOneOrderPerGroup) {
                // Delete action for this menu entry
                ops.add((ContentProviderOperation.newDelete(actionUri)
                        .withSelection(ProviderContract.Action.ME_RELATIVE_ID + " = " + relativeId + " AND " +
                                ProviderContract.Action.ME_PORTAL_ID + " = " + portalId + " AND " +
                                ProviderContract.Action.SYNC_STATUS + " = " + ProviderContract.ACTION_SYNC_STATUS_EDIT, null)
                        .build()));
            } else {
                // Delete actions for whole menu entry group
                ops.add(ContentProviderOperation.newDelete(actionUri)
                        .withSelection(ProviderContract.Action.ME_PORTAL_ID + " = " + portalId + " AND " +
                                ProviderContract.Action.SYNC_STATUS + " = " + ProviderContract.ACTION_SYNC_STATUS_EDIT + " AND " +
                                "EXISTS ( SELECT * FROM " + DbContract.MenuEntry.TABLE_NAME + " WHERE " +
                                DbContract.MenuEntry.COLUMN_NAME_PID + " == " + ProviderContract.Action.ME_PORTAL_ID + " AND " +
                                DbContract.MenuEntry.COLUMN_NAME_RELATIVE_ID + " == " + ProviderContract.Action.ME_RELATIVE_ID + " AND " +
                                DbContract.MenuEntry.COLUMN_NAME_DATE + " == " + date + " AND " +
                                DbContract.MenuEntry.COLUMN_NAME_MGID + " == " + groupId + " )", null)
                        .build());
            }

            // Insert new edits
            if ((hasLocal && !(reserved == localReserved && offered == localOffered)) ||
                    (!hasLocal && !(reserved == syncedReserved && offered == syncedOffered))) {
                if (restrictToOneOrderPerGroup) {
                    // Sets other actions in group to zeros
                    try (Cursor groupCursor = context.getContentResolver().query(
                            menuUri,
                            new String[]{
                                    ProviderContract.MenuEntry.ME_RELATIVE_ID,
                                    ProviderContract.MenuEntry.PRICE,
                                    ProviderContract.MenuEntry.STATUS,
                                    ProviderContract.MenuEntry.SYNCED_RESERVED_AMOUNT,
                                    ProviderContract.MenuEntry.SYNCED_TAKEN_AMOUNT
                            },
                            ProviderContract.MenuEntry.PORTAL_ID + " = " + portalId + " AND " +
                                    ProviderContract.MenuEntry.DATE + " = " + date + " AND " +
                                    ProviderContract.MenuEntry.GROUP_ID + " = " + groupId + " AND (" +
                                    "(IFNULL(" + ProviderContract.MenuEntry.SYNCED_RESERVED_AMOUNT + ", 0)" +
                                    " - IFNULL(" + ProviderContract.MenuEntry.SYNCED_TAKEN_AMOUNT + ", 0)) > 0 OR " +
                                    "(IFNULL(" + ProviderContract.MenuEntry.LOCAL_RESERVED_AMOUNT + ", 0)" +
                                    " - IFNULL(" + ProviderContract.MenuEntry.SYNCED_TAKEN_AMOUNT + ", 0)) > 0)",
                            null,
                            null)) {
                        if (groupCursor != null) {
                            while (groupCursor.moveToNext()) {
                                // Skip main changed row
                                if (groupCursor.getLong(0) == relativeId) {
                                    continue;
                                }

                                @ProviderContract.MenuStatus int status = groupCursor.getInt(2);
                                boolean canCancel = (status & ProviderContract.MENU_STATUS_CANCELABLE) == ProviderContract.MENU_STATUS_CANCELABLE;
                                boolean canUseStock = (status & ProviderContract.FEATURE_FOOD_STOCK) == ProviderContract.FEATURE_FOOD_STOCK;

                                // Insert virtual actions
                                ContentValues newAction = new ContentValues();
                                newAction.put(ProviderContract.Action.ME_RELATIVE_ID, groupCursor.getLong(0));
                                newAction.put(ProviderContract.Action.ME_PORTAL_ID, portalId);
                                newAction.put(ProviderContract.Action.SYNC_STATUS, ProviderContract.ACTION_SYNC_STATUS_EDIT);
                                newAction.put(ProviderContract.Action.ENTRY_TYPE, ProviderContract.ACTION_ENTRY_TYPE_VIRTUAL);
                                newAction.put(ProviderContract.Action.PRICE, groupCursor.getInt(1));
                                if (canCancel) {
                                    newAction.put(ProviderContract.Action.RESERVED_AMOUNT, 0);
                                    newAction.put(ProviderContract.Action.OFFERED_AMOUNT, 0);
                                } else {
                                    newAction.put(ProviderContract.Action.RESERVED_AMOUNT, groupCursor.getInt(3));
                                    newAction.put(ProviderContract.Action.OFFERED_AMOUNT, groupCursor.getInt(3));

                                    Toast.makeText(context, R.string.actions_food_stock_on_restricted_to_one,
                                            Toast.LENGTH_LONG)
                                            .show();
                                }
                                newAction.put(ProviderContract.Action.TAKEN_AMOUNT, groupCursor.getInt(4));

                                ops.add(ContentProviderOperation.newInsert(actionUri)
                                        .withValues(newAction)
                                        .build());
                            }
                        }
                    }
                }

                // Insert main edit
                ContentValues newAction = new ContentValues();
                newAction.put(ProviderContract.Action.ME_RELATIVE_ID, relativeId);
                newAction.put(ProviderContract.Action.ME_PORTAL_ID, portalId);
                newAction.put(ProviderContract.Action.SYNC_STATUS, ProviderContract.ACTION_SYNC_STATUS_EDIT);
                newAction.put(ProviderContract.Action.ENTRY_TYPE, ProviderContract.ACTION_ENTRY_TYPE_STANDARD);
                newAction.put(ProviderContract.Action.PRICE, price);
                newAction.put(ProviderContract.Action.RESERVED_AMOUNT, reserved);
                newAction.put(ProviderContract.Action.OFFERED_AMOUNT, offered);
                newAction.put(ProviderContract.Action.TAKEN_AMOUNT, syncedTaken);

                ops.add(ContentProviderOperation.newInsert(actionUri)
                        .withValues(newAction)
                        .build());
            }

            // Apply changes at once (it boost the performance)
            try {
                context.getContentResolver().applyBatch(ProviderContract.AUTHORITY, ops);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (OperationApplicationException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Changes {@link ProviderContract#ACTION_SYNC_STATUS_EDIT} actions to {@link ProviderContract#ACTION_SYNC_STATUS_LOCAL}
     * and starts changes sync.
     *
     * @param context Some valid context
     * @param userUri User Uri prefix
     */
    @WorkerThread
    public static void saveEdits(@NonNull Context context, @NonNull Uri userUri) {
        Uri actionUri = Uri.withAppendedPath(userUri, ProviderContract.ACTION_PATH);
        long userId = ContentUris.parseId(userUri);

        // Delete conflict local rows
        int conflictRows = context.getContentResolver().delete(
                actionUri,
                ProviderContract.Action.SYNC_STATUS + " == " + ProviderContract.ACTION_SYNC_STATUS_LOCAL + " AND " +
                        "EXISTS ( SELECT * FROM " + DbContract.FoodAction.TABLE_NAME + " AS EditAct WHERE " +
                        "EditAct." + DbContract.FoodAction.COLUMN_NAME_CID + " == " + ProviderContract.Action.CREDENTIAL_ID + " AND " +
                        "EditAct." + DbContract.FoodAction.COLUMN_NAME_ME_PID + " == " + ProviderContract.Action.ME_PORTAL_ID + " AND " +
                        "EditAct." + DbContract.FoodAction.COLUMN_NAME_ME_RELATIVE_ID + " == " + ProviderContract.Action.ME_RELATIVE_ID + " AND " +
                        "EditAct." + DbContract.FoodAction.COLUMN_NAME_SYNC_STATUS + " == " + ProviderContract.ACTION_SYNC_STATUS_EDIT + " )",
                null
        );
        Timber.d("Overeaten %d rows with conflict local values", conflictRows);

        // Save rows that has different values then the synced ones
        ContentValues newValues = new ContentValues();
        newValues.put(ProviderContract.Action.SYNC_STATUS, ProviderContract.ACTION_SYNC_STATUS_LOCAL);
        newValues.put(ProviderContract.Action.LAST_CHANGE, System.currentTimeMillis());

        int updatedRows = context.getContentResolver().update(
                actionUri,
                newValues,
                ProviderContract.Action.SYNC_STATUS + " == " + ProviderContract.ACTION_SYNC_STATUS_EDIT + " AND (" +
                        "NOT EXISTS (SELECT * FROM " + DbContract.FoodAction.TABLE_NAME + " AS SyncAct WHERE " +
                        "SyncAct." + DbContract.FoodAction.COLUMN_NAME_CID +
                            " IN (SELECT " + DbContract.Credential._ID + " FROM " + DbContract.Credential.TABLE_NAME +
                                " WHERE " + DbContract.Credential.COLUMN_NAME_UID + " == " + userId + ") AND " +
                        "SyncAct." + DbContract.FoodAction.COLUMN_NAME_ME_PID + " == " + ProviderContract.Action.ME_PORTAL_ID + " AND " +
                        "SyncAct." + DbContract.FoodAction.COLUMN_NAME_ME_RELATIVE_ID + " == " + ProviderContract.Action.ME_RELATIVE_ID + " AND " +
                        "SyncAct." + DbContract.FoodAction.COLUMN_NAME_SYNC_STATUS + " == " + ProviderContract.ACTION_SYNC_STATUS_SYNCED + " ) " +
                        "AND (" + ProviderContract.Action.RESERVED_AMOUNT + " > 0 OR " + ProviderContract.Action.OFFERED_AMOUNT + " > 0 ) " +
                        "OR EXISTS (SELECT * FROM " + DbContract.FoodAction.TABLE_NAME + " AS SyncAct WHERE " +
                        "SyncAct." + DbContract.FoodAction.COLUMN_NAME_CID +
                            " IN (SELECT " + DbContract.Credential._ID + " FROM " + DbContract.Credential.TABLE_NAME +
                                " WHERE " + DbContract.Credential.COLUMN_NAME_UID + " == " + userId + ") AND " +
                        "SyncAct." + DbContract.FoodAction.COLUMN_NAME_ME_PID + " == " + ProviderContract.Action.ME_PORTAL_ID + " AND " +
                        "SyncAct." + DbContract.FoodAction.COLUMN_NAME_ME_RELATIVE_ID + " == " + ProviderContract.Action.ME_RELATIVE_ID + " AND " +
                        "SyncAct." + DbContract.FoodAction.COLUMN_NAME_SYNC_STATUS + " == " + ProviderContract.ACTION_SYNC_STATUS_SYNCED + " AND (" +
                        "SyncAct." + DbContract.FoodAction.COLUMN_NAME_RESERVED_AMOUNT + " != " + ProviderContract.Action.RESERVED_AMOUNT + " OR " +
                        "SyncAct." + DbContract.FoodAction.COLUMN_NAME_OFFERED_AMOUNT + " != " + ProviderContract.Action.OFFERED_AMOUNT + " ) ) )",
                null
        );

        // Delete rest
        int unnecessaryRows = context.getContentResolver().delete(
                actionUri,
                ProviderContract.Action.SYNC_STATUS + " == " + ProviderContract.ACTION_SYNC_STATUS_EDIT,
                null
        );

        // Check invariants
        if (BuildConfig.DEBUG) {
            Assert.that(updatedRows + unnecessaryRows > 0, "Should change at least one rows");
            Assert.that(conflictRows <= updatedRows + unnecessaryRows, "Conflict rows overflow");
        }

        SyncHandler.planChangesSync(context);
    }

    /**
     * Deletes all {@link ProviderContract#ACTION_SYNC_STATUS_EDIT} actions
     *
     * @param context Some valid context
     * @param userUri User Uri prefix
     */
    @WorkerThread
    public static void discardEdits(@NonNull Context context, @NonNull Uri userUri) {
        Uri actionUri = Uri.withAppendedPath(userUri, ProviderContract.ACTION_PATH);
        int affectedRows = context.getContentResolver().delete(
                actionUri,
                ProviderContract.Action.SYNC_STATUS + " == " + ProviderContract.ACTION_SYNC_STATUS_EDIT,
                null
        );

        if (BuildConfig.DEBUG && affectedRows == 0) {
            Timber.w("Should delete at least one edit");
        }
    }

    /**
     * Check if all {@link ProviderContract#ACTION_SYNC_STATUS_LOCAL} actions were synced. If some
     * action fails the notification is shown.
     *
     * @param context Some valid context
     */
    @WorkerThread
    public static void checkActionResults(@NonNull Context context) {

        ContentValues newValues = new ContentValues();
        newValues.put(ProviderContract.Action.SYNC_STATUS, ProviderContract.ACTION_SYNC_STATUS_FAILED);
        newValues.put(ProviderContract.Action.LAST_CHANGE, System.currentTimeMillis());

        // Mark not synced actions as failed
        int failedActions = context.getContentResolver().update(
                ProviderContract.Action.getUri(),
                newValues,
                ProviderContract.Action.SYNC_STATUS + " == " + ProviderContract.ACTION_SYNC_STATUS_LOCAL + " AND " +
                        "(" +
                        "(EXISTS ( SELECT * FROM " + DbContract.FoodAction.TABLE_NAME + " AS SyncedAct WHERE " +
                        "SyncedAct." + DbContract.FoodAction.COLUMN_NAME_CID + " == " + ProviderContract.Action.CREDENTIAL_ID + " AND " +
                        "SyncedAct." + DbContract.FoodAction.COLUMN_NAME_ME_PID + " == " + ProviderContract.Action.ME_PORTAL_ID + " AND " +
                        "SyncedAct." + DbContract.FoodAction.COLUMN_NAME_ME_RELATIVE_ID + " == " + ProviderContract.Action.ME_RELATIVE_ID + " AND " +
                        "SyncedAct." + DbContract.FoodAction.COLUMN_NAME_SYNC_STATUS + " == " + ProviderContract.ACTION_SYNC_STATUS_SYNCED + " AND " +
                        "(SyncedAct." + DbContract.FoodAction.COLUMN_NAME_RESERVED_AMOUNT + " != " + ProviderContract.Action.RESERVED_AMOUNT + " OR " +
                        "SyncedAct." + DbContract.FoodAction.COLUMN_NAME_OFFERED_AMOUNT + " != " + ProviderContract.Action.OFFERED_AMOUNT + "))" +
                        ") OR (" +
                        "NOT EXISTS ( SELECT * FROM " + DbContract.FoodAction.TABLE_NAME + " AS SyncedAct WHERE " +
                        "SyncedAct." + DbContract.FoodAction.COLUMN_NAME_CID + " == " + ProviderContract.Action.CREDENTIAL_ID + " AND " +
                        "SyncedAct." + DbContract.FoodAction.COLUMN_NAME_ME_PID + " == " + ProviderContract.Action.ME_PORTAL_ID + " AND " +
                        "SyncedAct." + DbContract.FoodAction.COLUMN_NAME_ME_RELATIVE_ID + " == " + ProviderContract.Action.ME_RELATIVE_ID + " AND " +
                        "SyncedAct." + DbContract.FoodAction.COLUMN_NAME_SYNC_STATUS + " == " + ProviderContract.ACTION_SYNC_STATUS_SYNCED + " AND " +
                        "SyncedAct." + DbContract.FoodAction.COLUMN_NAME_RESERVED_AMOUNT + " == " + ProviderContract.Action.RESERVED_AMOUNT + " AND " +
                        "SyncedAct." + DbContract.FoodAction.COLUMN_NAME_OFFERED_AMOUNT + " == " + ProviderContract.Action.OFFERED_AMOUNT + ") AND " +
                        "(" + ProviderContract.Action.RESERVED_AMOUNT + " != 0 OR " +
                        ProviderContract.Action.OFFERED_AMOUNT + " != 0))" +
                        ")",
                null
        );

        if (failedActions > 0) {
            Timber.e("%d action failed to sync", failedActions);

            // Create an explicit intent for an Activity in your app
            PendingIntent pendingIntent = MainActivity.getShowOrdersIntent(context);

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, NotificationContract.FAILED_ACTIONS_CHANNEL_ID)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                    .setContentTitle(context.getString(R.string.notification_order_failed_title))
                    .setContentText(context.getString(R.string.notification_order_failed_text, failedActions))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    // Set the intent that will fire when the user taps the notification
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            // notificationId is a unique int for each notification that you must define
            notificationManager.notify(NotificationContract.FAILED_ACTION_ID, mBuilder.build());

            Intent intent = new Intent(BROADCAST_ACTION_EVENT);
            intent.putExtra(ARG_EVENT, EVENT_SYNC_FAILED);

            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
            localBroadcastManager.sendBroadcast(intent);
        }

        // Delete fully synced actions
        int doneOrders = context.getContentResolver().delete(
                ProviderContract.Action.getUri(),
                ProviderContract.Action.SYNC_STATUS + " == " + ProviderContract.ACTION_SYNC_STATUS_LOCAL + " AND " +
                        "(" +
                        "(EXISTS ( SELECT * FROM " + DbContract.FoodAction.TABLE_NAME + " AS SyncedAct WHERE " +
                        "SyncedAct." + DbContract.FoodAction.COLUMN_NAME_CID + " == " + ProviderContract.Action.CREDENTIAL_ID + " AND " +
                        "SyncedAct." + DbContract.FoodAction.COLUMN_NAME_ME_PID + " == " + ProviderContract.Action.ME_PORTAL_ID + " AND " +
                        "SyncedAct." + DbContract.FoodAction.COLUMN_NAME_ME_RELATIVE_ID + " == " + ProviderContract.Action.ME_RELATIVE_ID + " AND " +
                        "SyncedAct." + DbContract.FoodAction.COLUMN_NAME_SYNC_STATUS + " == " + ProviderContract.ACTION_SYNC_STATUS_SYNCED + " AND " +
                        "SyncedAct." + DbContract.FoodAction.COLUMN_NAME_RESERVED_AMOUNT + " == " + ProviderContract.Action.RESERVED_AMOUNT + " AND " +
                        "SyncedAct." + DbContract.FoodAction.COLUMN_NAME_OFFERED_AMOUNT + " == " + ProviderContract.Action.OFFERED_AMOUNT + ") " +
                        ") OR (" +
                        "NOT EXISTS ( SELECT * FROM " + DbContract.FoodAction.TABLE_NAME + " AS SyncedAct WHERE " +
                        "SyncedAct." + DbContract.FoodAction.COLUMN_NAME_CID + " == " + ProviderContract.Action.CREDENTIAL_ID + " AND " +
                        "SyncedAct." + DbContract.FoodAction.COLUMN_NAME_ME_PID + " == " + ProviderContract.Action.ME_PORTAL_ID + " AND " +
                        "SyncedAct." + DbContract.FoodAction.COLUMN_NAME_ME_RELATIVE_ID + " == " + ProviderContract.Action.ME_RELATIVE_ID + " AND " +
                        "SyncedAct." + DbContract.FoodAction.COLUMN_NAME_SYNC_STATUS + " == " + ProviderContract.ACTION_SYNC_STATUS_SYNCED + " AND " +
                        "(SyncedAct." + DbContract.FoodAction.COLUMN_NAME_RESERVED_AMOUNT + " != " + ProviderContract.Action.RESERVED_AMOUNT + " OR " +
                        "SyncedAct." + DbContract.FoodAction.COLUMN_NAME_OFFERED_AMOUNT + " != " + ProviderContract.Action.OFFERED_AMOUNT + ")) AND " +
                        ProviderContract.Action.RESERVED_AMOUNT + " == 0 AND " +
                        ProviderContract.Action.OFFERED_AMOUNT + " == 0)" +
                        ")",
                null
        );

        // Log Firebase event
        Bundle syncParams = new Bundle();
        syncParams.putInt("failed_actions", failedActions);
        syncParams.putInt("synced_actions", doneOrders);
        // Send to
        FirebaseAnalytics.getInstance(context).logEvent("actions_sync", syncParams);

        Timber.i("%d actions synced", doneOrders);
    }

    /**
     * Deletes old failed actions that are in conflict with local actions that will be synced
     *
     * @param context Some valid context
     */
    @WorkerThread
    public static void deleteConflictFailedActions(@NonNull Context context) {
        int affectedRows = context.getContentResolver().delete(
                ProviderContract.Action.getUri(),
                ProviderContract.Action.SYNC_STATUS + " == " + ProviderContract.ACTION_SYNC_STATUS_FAILED + " AND " +
                        "EXISTS ( SELECT * FROM " + DbContract.FoodAction.TABLE_NAME + " AS LocAct WHERE " +
                        "LocAct." + DbContract.FoodAction.COLUMN_NAME_CID + " == " + ProviderContract.Action.CREDENTIAL_ID + " AND " +
                        "LocAct." + DbContract.FoodAction.COLUMN_NAME_ME_PID + " == " + ProviderContract.Action.ME_PORTAL_ID + " AND " +
                        "LocAct." + DbContract.FoodAction.COLUMN_NAME_ME_RELATIVE_ID + " == " + ProviderContract.Action.ME_RELATIVE_ID + " AND " +
                        "LocAct." + DbContract.FoodAction.COLUMN_NAME_SYNC_STATUS + " == " + ProviderContract.ACTION_SYNC_STATUS_LOCAL + ")",
                null);
        if (affectedRows > 0) {
            Timber.w("%d conflict failed actions deleted", affectedRows);
        }
    }
}
