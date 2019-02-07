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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cz.maresmar.sfm.plugin.ActionContract;
import cz.maresmar.sfm.plugin.BroadcastContract;
import cz.maresmar.sfm.plugin.ExtraFormat;
import cz.maresmar.sfm.provider.PublicProviderContract;
import timber.log.Timber;

/**
 * Handler that ask plugin to validate portal data. The validation could contains extras and portal
 * reference
 *
 * @see ExtraFormat
 */
public class PortalTestHandler {

    public static final String BROADCAST_PORTAL_TEST_RESULT = BroadcastContract.BROADCAST_PORTAL_TEST_RESULT;

    // To prevent someone from accidentally instantiating the handler class,
    // make the constructor private.
    private PortalTestHandler() {
    }

    /**
     * Starts plugin to validate portal
     *
     * @param context  Some valid context
     * @param plugin   Plugin name
     * @param portalId ID of portal to be tested
     */
    public static void requestTest(@NonNull Context context, @NonNull String plugin, final long portalId) {
        Timber.i("Test portal data request started");
        Intent intent = PluginUtils.buildPluginIntent(plugin);

        intent.setAction(ActionContract.ACTION_PORTAL_TEST);
        intent.setData(PublicProviderContract.LogData.getUri(portalId));

        context.grantUriPermission(intent.getComponent().getPackageName(), intent.getData(), Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        PluginUtils.startPlugin(context, intent);
    }

    /**
     * {@link BroadcastReceiver} that binds test results to {@link PortalTestResultListener}
     * <p>
     * This is more stable to API changes in future.
     * </p>
     */
    public static class PortalTestResultReceiver extends BroadcastReceiver {

        private PortalTestResultListener mListener;

        /**
         * Create new receiver with specific listener
         *
         * @param listener Listener that will receive test results
         */
        public PortalTestResultReceiver(@NonNull PortalTestResultListener listener) {
            mListener = listener;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final long resultPortalId = intent.getLongExtra(BroadcastContract.EXTRA_PORTAL_ID, BroadcastContract.UNKNOWN_ID);

            final @BroadcastContract.TestResult int testResult =
                    intent.getIntExtra(BroadcastContract.EXTRA_TEST_RESULT, BroadcastContract.TEST_RESULT_INVALID_DATA);
            final String errorMsg = intent.getStringExtra(BroadcastContract.EXTRA_ERROR_MESSAGE);
            Timber.i("Portal %d test result %d received", resultPortalId, testResult);

            mListener.onPortalTestResult(resultPortalId, testResult, errorMsg);
        }
    }

    /**
     * Test result listener
     */
    public interface PortalTestResultListener {
        /**
         * Called when plugin returns portal test result
         *
         * @param portalId ID of tested portal
         * @param result   Test result
         * @param errorMsg Error message if test fails
         */
        void onPortalTestResult(long portalId, @BroadcastContract.TestResult int result, @Nullable String errorMsg);
    }
}
