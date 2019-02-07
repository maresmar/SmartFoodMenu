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

package cz.maresmar.sfm.view.portal;

import androidx.loader.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.List;

import cz.maresmar.sfm.provider.PublicProviderContract;

/**
 * A custom {@link AsyncTaskLoader} that loads all of the installed plugins.
 */
public class PluginListLoader extends AsyncTaskLoader<List<PluginInfo>> {

    final PackageManager mPm;

    List<PluginInfo> mPlugins;

    /**
     * Creates new loader
     * @param context Some valid context
     */
    public PluginListLoader(Context context) {
        super(context);

        // Retrieve the package manager for later use; note we don't
        // use 'context' directly but instead the save global application
        // context returned by getContext().
        mPm = getContext().getPackageManager();
    }

    /**
     * This is where the bulk of our work is done.  This function is
     * called in a background thread and should generate a new set of
     * data to be published by the loader.
     */
    @Override
    public List<PluginInfo> loadInBackground() {
        // Retrieve all known plugins
        Intent intent = new Intent(Intent.ACTION_SYNC);
        intent.addCategory("cz.maresmar.sfm.plugin");
        final List<ResolveInfo> list = mPm.queryIntentServices(intent, 0);
        final List<PluginInfo> packages = new ArrayList<>();
        if (list != null) {
            for (ResolveInfo resolveInfo : list) {
                String pluginLabel = resolveInfo.serviceInfo.loadLabel(mPm).toString();
                String pluginId = resolveInfo.serviceInfo.packageName+ PublicProviderContract.PLUGIN_DATA_SEPARATOR +
                        resolveInfo.serviceInfo.name;
                packages.add(new PluginInfo(pluginLabel, pluginId));
            }
        }

        // Done!
        return packages;
    }

    /**
     * Called when there is new data to deliver to the client.  The
     * super class will take care of delivering it; the implementation
     * here just adds a little more logic.
     */
    @Override
    public void deliverResult(List<PluginInfo> plugins) {
        if (isReset()) {
            // An async query came in while the loader is stopped.  We
            // don't need the result.
            if (plugins != null) {
                onReleaseResources(plugins);
            }
        }
        List<PluginInfo> oldPlugins = mPlugins;
        mPlugins = plugins;

        if (isStarted()) {
            // If the Loader is currently started, we can immediately
            // deliver its results.
            super.deliverResult(plugins);
        }

        // At this point we can release the resources associated with
        // 'oldPlugins' if needed; now that the new result is delivered we
        // know that it is no longer in use.
        if (oldPlugins != null) {
            onReleaseResources(oldPlugins);
        }
    }

    /**
     * Handles a request to start the Loader.
     */
    @Override
    protected void onStartLoading() {
        if (mPlugins != null) {
            // If we currently have a result available, deliver it
            // immediately.
            deliverResult(mPlugins);
        }

        if (takeContentChanged() || mPlugins == null) {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            forceLoad();
        }
    }

    /**
     * Handles a request to stop the Loader.
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    /**
     * Handles a request to cancel a load.
     */
    @Override
    public void onCanceled(List<PluginInfo> plugins) {
        super.onCanceled(plugins);

        // At this point we can release the resources associated with 'apps'
        // if needed.
        onReleaseResources(plugins);
    }

    /**
     * Handles a request to completely reset the Loader.
     */
    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        // At this point we can release the resources associated with 'apps'
        // if needed.
        if (mPlugins != null) {
            onReleaseResources(mPlugins);
            mPlugins = null;
        }
    }

    /**
     * Helper function to take care of releasing resources associated
     * with an actively loaded data set.
     */
    protected void onReleaseResources(List<PluginInfo> plugins) {
        // For a simple List<> there is nothing to do.  For something
        // like a Cursor, we would close it here.
    }

}