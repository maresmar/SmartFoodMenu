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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cz.maresmar.sfm.plugin.ActionContract;
import cz.maresmar.sfm.plugin.BroadcastContract;
import cz.maresmar.sfm.plugin.ExtraFormat;
import timber.log.Timber;

/**
 * Handler that asks plugin for it's extras that can be shown in UI. Extras specify value-pair settings
 * that are specific for each plugin.
 *
 * @see ExtraFormat
 */
public class ExtraFormatHandler {

    // To prevent someone from accidentally instantiating the handler class,
    // make the constructor private.
    private ExtraFormatHandler() {
    }

    public static final IntentFilter INTENT_FILTER = new IntentFilter(BroadcastContract.BROADCAST_EXTRA_FORMAT);

    /**
     * Starts plugin to get extra format
     *
     * @param context    Some valid context
     * @param plugin     Name of plugin
     * @param formatType Type of extras that are needed (portal specific or credential specific)
     */
    public static void requestExtraFormat(@NonNull Context context, @NonNull String plugin, @ActionContract.ExtraFormatType int formatType) {
        Timber.i("Extra format request received");
        Intent intent = PluginUtils.buildPluginIntent(plugin);

        intent.setAction(ActionContract.ACTION_EXTRA_FORMAT);
        intent.putExtra(ActionContract.EXTRA_PLUGIN, plugin);
        intent.putExtra(ActionContract.EXTRA_FORMAT_TYPE, formatType);

        PluginUtils.startPlugin(context, intent);
    }

    /**
     * {@link BroadcastReceiver} that binds extra results to {@link ResultListener}
     * <p>
     * This is more stable to API changes in future.
     * </p>
     */
    public static class ResultReceiver extends BroadcastReceiver {

        private ResultListener mListener;

        /**
         * Create new receiver with specific listener
         *
         * @param listener Listener that will receive extra results
         */
        public ResultReceiver(@NonNull ResultListener listener) {
            mListener = listener;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            // Parse format type
            int resFormatType = intent.getIntExtra(BroadcastContract.EXTRA_FORMAT_TYPE, -1);

            String resPlugin = intent.getStringExtra(BroadcastContract.EXTRA_PLUGIN);

            // Parse format data
            List<ExtraFormat> extraFormats = null;
            try {
                JSONArray extraFormatData = new JSONArray(intent.getStringExtra(BroadcastContract.EXTRA_FORMAT_DATA));
                extraFormats = new ArrayList<>(extraFormatData.length());
                for (int i = 0; i < extraFormatData.length(); i++) {
                    JSONObject jsonObject = extraFormatData.getJSONObject(i);
                    extraFormats.add(new ExtraFormat(jsonObject));
                }
            } catch (JSONException e) {
                if (extraFormats == null) {
                    Timber.e(e, "Cannot parse extras from JSON %s, malformed top level array", resPlugin);
                    extraFormats = new ArrayList<>();
                } else {
                    Timber.e(e, "Cannot parse extra %d from JSON %s", extraFormats.size(), resPlugin);
                }
            }
            Timber.i("Extra format received %d", resFormatType);

            mListener.onExtraFormatResult(resPlugin, resFormatType, extraFormats);
        }
    }

    /**
     * Extra results received listener
     */
    public interface ResultListener {
        /**
         * Called when plugin returns extras
         * @param plugin Plugin name
         * @param formatType Extra format type (portal specific or credential specific)
         * @param extraFormat Concrete extra format
         */
        void onExtraFormatResult(@NonNull String plugin, @ActionContract.ExtraFormatType int formatType,
                                 @NonNull List<ExtraFormat> extraFormat);
    }
}
