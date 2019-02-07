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

package cz.maresmar.sfm.service.plugin;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import java.util.HashMap;

import cz.maresmar.sfm.plugin.ActionContract;
import cz.maresmar.sfm.provider.ProviderContract;
import cz.maresmar.sfm.provider.PublicProviderContract;
import timber.log.Timber;

/**
 * Helping class that allows start of plugins on different API levels
 *
 * @see JobIntentService
 * @see <a href="https://developer.android.com/about/versions/oreo/background">Background Execution Limits</a>
 */
public class PluginUtils {

    // To prevent someone from accidentally instantiating the utils class,
    // make the constructor private.
    private PluginUtils() {
    }

    /**
     * The same {@link JobIntentService} has to be started with same ID. So it's stored here...
     */
    private static HashMap<String, Integer> mPluginsIds = new HashMap<>();

    /**
     * Starts plugin according to API level
     *
     * @param context Some valid context
     * @param intent  Intent of plugin to be started
     * @see JobIntentService#enqueueWork(Context, ComponentName, int, Intent)
     */
    public static void startPlugin(@NonNull Context context, @NonNull Intent intent) {
        // Test if the plugin exists
        PackageManager manager = context.getPackageManager();
        if (manager.queryIntentServices(intent, 0).size() != 1) {
            Timber.e("Plugin %s not found", intent.getComponent());
            throw new IllegalArgumentException("Plugin not found " + intent.getComponent());
        }

        // Finds jobId for selected plugin
        String pluginName = ProviderContract.buildString(intent.getComponent());
        int jobId;
        if (!mPluginsIds.containsKey(pluginName)) {
            jobId = mPluginsIds.size();
            mPluginsIds.put(pluginName, jobId);
        } else {
            jobId = mPluginsIds.get(pluginName);
        }

        // Starts plugin according to API level
        if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.O) ||
                (context.getPackageName().equals(intent.getComponent().getPackageName()))) {
            JobIntentService.enqueueWork(context, intent.getComponent(), jobId, intent);
        } else {
            // On Android >= O with external plugin use BroadcastContract.BROADCAST_PLAN_RUN to
            // start plugin, because planning of external APK's service is not allowed

            Intent plan = new Intent();
            // Explicitly select a package to communicate with
            plan.setPackage(intent.getComponent().getPackageName());
            plan.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

            plan.setAction(ActionContract.BROADCAST_PLAN_RUN);
            plan.putExtra(ActionContract.EXTRA_JOB_ID, jobId);
            plan.putExtra(ActionContract.EXTRA_INTENT_TO_DO, intent);
            context.sendBroadcast(plan);
        }
    }

    /**
     * Prepares plugin intent from plugin name
     * @param plugin Name of plugin
     * @return Intent that can start plugin
     * @see #startPlugin(Context, Intent)
     */
    @NonNull
    public static Intent buildPluginIntent(@NonNull String plugin) {
        // Creates the intent
        Intent intent = new Intent();

        ComponentName componentName = PublicProviderContract.parsePluginText(plugin);
        // Test result validity
        if (componentName == null) {
            Timber.e("Plugin name \"%s\" is badly formatted", plugin);
            throw new IllegalArgumentException("Plugin name is badly formatted " + plugin);
        }
        // Explicitly select service to communicate with
        intent.setComponent(componentName);
        return intent;
    }
}
