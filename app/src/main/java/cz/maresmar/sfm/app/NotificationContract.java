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

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;

import cz.maresmar.sfm.R;

/**
 * API contract that specifies notification related constants
 *
 * @see <a href="https://developer.android.com/training/notify-user/channels">Create and Manage Notification Channels</a>
 */
public class NotificationContract {

    public static final String FAILED_ACTIONS_CHANNEL_ID = "failedActionsChannel";
    public static final String NEW_MENU_CHANNEL_ID = "newMenuChannel";
    public static final String CREDIT_CHANGE_CHANNEL_ID = "creditChangeChannel";

    public static final int FAILED_ACTION_ID = 1;

    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private NotificationContract() {
    }

    /**
     * Create default app notification channels
     *
     * @param context Some context
     */
    static void initNotificationChannels(@NonNull Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

            // Action failed chanel
            CharSequence name = context.getString(R.string.notification_channel_failed_actions_name);
            String description = context.getString(R.string.notification_channel_failed_action_des);
            NotificationChannel channel = new NotificationChannel(FAILED_ACTIONS_CHANNEL_ID, name,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(channel);

            // New menu chanel
            name = context.getString(R.string.notification_channel_new_menu_name);
            description = context.getString(R.string.notification_channel_new_menu_des);
            channel = new NotificationChannel(NEW_MENU_CHANNEL_ID, name,
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(channel);

            // Credit change chanel
            name = context.getString(R.string.notification_chanel_credit_change_name);
            description = context.getString(R.string.notification_channel_credit_change_des);
            channel = new NotificationChannel(CREDIT_CHANGE_CHANNEL_ID, name,
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(channel);
        }
    }
}
