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
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.text.format.DateFormat;

import cz.maresmar.sfm.Assert;
import cz.maresmar.sfm.BuildConfig;
import cz.maresmar.sfm.R;
import cz.maresmar.sfm.app.NotificationContract;
import cz.maresmar.sfm.plugin.BroadcastContract;
import cz.maresmar.sfm.provider.ProviderContract;
import cz.maresmar.sfm.view.MainActivity;

import static cz.maresmar.sfm.provider.ProviderContract.CREDENTIAL_FLAG_DISABLE_CREDIT_INCREASE_NOTIFICATION;
import static cz.maresmar.sfm.provider.ProviderContract.CREDENTIAL_FLAG_DISABLE_LOW_CREDIT_NOTIFICATION;
import static cz.maresmar.sfm.provider.ProviderContract.PORTAL_FLAG_DISABLE_NEW_MENU_NOTIFICATION;

/**
 * Utils for menu. Helps check menu for changes and shows more friendly data representations of data
 * columns used in menus.
 */
public class MenuUtils {

    // To prevent someone from accidentally instantiating the utils class,
    // make the constructor private.
    private MenuUtils() {
    }

    /**
     * Gets today date rounded to days
     *
     * @return Today's midnight in millis
     * @see System#currentTimeMillis()
     */
    public static long getTodayDate() {
        long actTime = System.currentTimeMillis();
        return actTime - (actTime % (1000 * 60 * 60 * 24));
    }

    /**
     * Translate date to user readable String using current locale
     *
     * @param context Valid context used to get locale
     * @param date    Date to be translated
     * @return String with date (without time)
     */
    @NonNull
    public static String getDateStr(Context context, long date) {
        return DateFormat.format("E, ", date) +
                DateFormat.getDateFormat(context).format(date);
    }

    /**
     * Translate date to user readable String using current locale
     *
     * @param context Valid context used to get locale
     * @param date    Date to be translated
     * @return String with date with time
     */
    @NonNull
    public static String getDateTimeStr(Context context, long date) {
        return getDateStr(context, date) + " " + DateFormat.getTimeFormat(context).format(date);
    }

    /**
     * Translate price to user readable String witch currency from current locale
     *
     * @param context  Valid context used to get locale
     * @param rawPrice Price to be translated (in farthing eg. {@code 5 CZK} is {@code 500})
     * @return String with price and currency
     */
    @NonNull
    public static String getPriceStr(@NonNull Context context, int rawPrice) {
        if (rawPrice != ProviderContract.NO_INFO) {
            float credit = ((float) rawPrice) / 100;
            return context.getString(R.string.credit_text, credit);
        } else {
            return context.getString(R.string.credit_no_info);
        }
    }

    /**
     * Returns error message if menu sync failed
     *
     * @param syncResult Worst sync result
     * @return String resource with error message
     * @see cz.maresmar.sfm.service.plugin.sync.SyncHandler.SyncResultListener#onSyncFinished(int)
     */
    @StringRes
    public static int getSyncErrorMessage(@BroadcastContract.SyncResult int syncResult) {
        switch (syncResult) {
            case BroadcastContract.RESULT_OK:
                throw new IllegalArgumentException("Cannot return error message when result is OK");
            case BroadcastContract.RESULT_WRONG_CREDENTIALS:
                return R.string.sync_result_wrong_credentials;
            case BroadcastContract.RESULT_UNKNOWN_PORTAL_FORMAT:
                return R.string.sync_result_unknown_portal_format;
            case BroadcastContract.RESULT_PORTAL_TEMPORALLY_INACCESSIBLE:
                return R.string.sync_result_temporally_inaccessible;
            case BroadcastContract.RESULT_NOT_SUPPORTED:
                return R.string.sync_result_not_supported;
            case BroadcastContract.RESULT_IO_EXCEPTION:
                return R.string.sync_result_io_exception;
            case BroadcastContract.RESULT_PLUGIN_TIMEOUT:
                return R.string.sync_result_plugin_timeout;
            default:
                throw new UnsupportedOperationException("Unknown result " + syncResult);
        }
    }

    /**
     * Check if credit is different from previous credit. If so it shows notification (if enabled)
     *
     * @param context        Some valid context
     * @param portalId       ID of portal with corresponding credential
     * @param credentialId   ID of credential to be checked
     * @param previousCredit Previous credit (before some plugin operation like sync happen)
     * @return {@code true} if credit changed, {@code false} otherwise
     */
    public static boolean checkCreditChanges(@NonNull Context context, long portalId, long credentialId, int previousCredit) {
        try (Cursor cursor = context.getContentResolver().query(
                ProviderContract.LogData.getUri(portalId, credentialId),
                new String[]{
                        ProviderContract.LogData.CREDIT,
                        ProviderContract.LogData.CREDENTIAL_NAME,
                        ProviderContract.Credentials.FLAGS,
                        ProviderContract.Credentials.LOW_CREDIT_THRESHOLD
                },
                null,
                null,
                null)) {

            if (BuildConfig.DEBUG) {
                Assert.isOne(cursor.getCount());
            }

            cursor.moveToFirst();
            int actualCredit = cursor.getInt(0);
            String credentialName = cursor.getString(1);
            @ProviderContract.CredentialFlags int flags = cursor.getInt(2);
            int lowCreditThreshold = cursor.getInt(3) * 100;

            boolean increaseNotification = (flags & CREDENTIAL_FLAG_DISABLE_CREDIT_INCREASE_NOTIFICATION)
                    != CREDENTIAL_FLAG_DISABLE_CREDIT_INCREASE_NOTIFICATION;
            boolean lowCreditNotification = (flags & CREDENTIAL_FLAG_DISABLE_LOW_CREDIT_NOTIFICATION)
                    != CREDENTIAL_FLAG_DISABLE_LOW_CREDIT_NOTIFICATION;

            boolean changed = !(increaseNotification || lowCreditNotification);

            // Credit increased notification
            if (actualCredit > previousCredit && increaseNotification && actualCredit != ProviderContract.NO_INFO) {
                changed = true;

                // Prepare intent
                PendingIntent pendingIntent = MainActivity.getShowCreditIntent(context);

                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, NotificationContract.CREDIT_CHANGE_CHANNEL_ID)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                        .setContentTitle(context.getString(R.string.notification_credit_icrease_title))
                        .setContentText(context.getString(R.string.notification_credit_icrease_text,
                                getPriceStr(context, actualCredit), credentialName))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        // Set the intent that will fire when the user taps the notification
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                // notificationId is a unique int for each notification that you must define
                notificationManager.notify((int) credentialId * 10, mBuilder.build());
            }

            if (lowCreditThreshold > actualCredit && lowCreditNotification && actualCredit != ProviderContract.NO_INFO) {
                changed = true;

                // Prepare intent
                PendingIntent pendingIntent = MainActivity.getShowCreditIntent(context);

                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, NotificationContract.CREDIT_CHANGE_CHANNEL_ID)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                        .setContentTitle(context.getString(R.string.notification_low_credit_title))
                        .setContentText(context.getString(R.string.notification_low_credit_text,
                                getPriceStr(context, actualCredit), credentialName))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        // Set the intent that will fire when the user taps the notification
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                // notificationId is a unique int for each notification that you must define
                notificationManager.notify((int) credentialId * 10, mBuilder.build());
            }

            return changed;
        }
    }

    /**
     * Check if portal has new menu available. If so it shows notification (if enabled)
     *
     * @param context              Some valid context
     * @param portalId             ID of portal to be checked
     * @param previousLastMenuDate Previous last menu entry date (before some plugin operation like sync happen)
     * @return {@code true} if new menu found, {@code false} otherwise
     */
    public static boolean checkMenuChanges(@NonNull Context context, long portalId, long previousLastMenuDate) {
        long actualLastMenuDate = getLastMenuDate(context, portalId);

        if (actualLastMenuDate > previousLastMenuDate) {
            try (Cursor cursor = context.getContentResolver().query(
                    ContentUris.withAppendedId(ProviderContract.Portal.getUri(), portalId),
                    new String[]{
                            ProviderContract.Portal.NAME,
                            ProviderContract.Portal.FLAGS
                    },
                    null,
                    null,
                    null)) {
                if (BuildConfig.DEBUG) {
                    Assert.isOne(cursor.getCount());
                }

                cursor.moveToFirst();
                String portalName = cursor.getString(0);
                @ProviderContract.PortalFlags int flags = cursor.getInt(1);

                if ((flags & PORTAL_FLAG_DISABLE_NEW_MENU_NOTIFICATION) != PORTAL_FLAG_DISABLE_NEW_MENU_NOTIFICATION) {
                    // Prepare intent
                    PendingIntent pendingIntent = MainActivity.getDefaultIntent(context);

                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, NotificationContract.NEW_MENU_CHANNEL_ID)
                            .setSmallIcon(R.drawable.notification_icon)
                            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                            .setContentTitle(context.getString(R.string.notification_new_menu_title))
                            .setContentText(context.getString(R.string.notification_new_menu_text, portalName))
                            .setPriority(NotificationCompat.PRIORITY_LOW)
                            // Set the intent that will fire when the user taps the notification
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true);

                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                    // notificationId is a unique int for each notification that you must define
                    notificationManager.notify((int) portalId * 1000, mBuilder.build());
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns date of last menu entry in some portal
     *
     * @param context  Some valid context
     * @param portalId ID of portal
     * @return Date in millis or {@code 0} if portal hasn't any menu
     */
    public static long getLastMenuDate(@NonNull Context context, long portalId) {
        try (Cursor cursor = context.getContentResolver().query(
                ProviderContract.MenuEntry.getPortalUri(portalId),
                new String[]{
                        "MAX(" + ProviderContract.MenuEntry.DATE + ")"
                },
                null,
                null,
                null
        )) {
            if (BuildConfig.DEBUG) {
                Assert.isOne(cursor.getCount());
            }

            cursor.moveToFirst();

            if (cursor.isNull(0)) {
                return 0L;
            } else {
                return cursor.getLong(0);
            }
        }
    }
}
