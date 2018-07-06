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

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cz.maresmar.sfm.Assert;
import cz.maresmar.sfm.BuildConfig;
import cz.maresmar.sfm.R;
import timber.log.Timber;

/**
 * Application singleton that is used for logging using Timber
 * @see Application
 */
public class SfmApp extends Application {

    File mTmpFile;
    PrintStream mPrintStream;

    /**
     * Shows about activity
     * @param context Some valid context
     */
    public static void startAboutActivity(@NonNull Context context) {
        new LibsBuilder()
                .withActivityTitle("About")
                //provide a style (optional) (LIGHT, DARK, LIGHT_DARK_TOOLBAR)
                .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                .withAboutAppName(context.getString(R.string.app_name))
                .withLicenseShown(true)
                .withLicenseDialog(true)
                .withVersionShown(false)
                //start the activity
                .start(context);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Register logging method
        if (BuildConfig.DEBUG) {
            // Let's log to builtin Android logger
            Timber.plant(new Timber.DebugTree());
        }

        try {
            File logsDir = new File(getCacheDir(), "logs");
            if (!logsDir.exists()) {
                boolean result = logsDir.mkdir();

                if (BuildConfig.DEBUG) {
                    Assert.that(result, "Logs folder wasn't created");
                }
            }
            mTmpFile = File.createTempFile("sfm", ".log", logsDir);
            mPrintStream = new PrintStream(new FileOutputStream(mTmpFile, true));

            Timber.plant(new ReleaseTree(mPrintStream));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Timber.i("Starting sfm %s", getString(R.string.app_version));

        NotificationContract.initNotificationChannels(this);
    }

    @Override
    public void onTerminate() {
        mPrintStream.close();
        boolean successful = mTmpFile.delete();

        if (BuildConfig.DEBUG) {
            Assert.that(successful, "Deleting log file failed");
        }

        super.onTerminate();
    }

    /**
     * Returns the file where the logs are written
     * @return The log file
     */
    @NonNull
    public File getLogFile() {
        mPrintStream.flush();
        return mTmpFile;
    }

    /**
     * A logging tree class which logs important information for crash reporting and feedback
     */
    private static class ReleaseTree extends Timber.DebugTree {

        PrintStream mLog;
        SimpleDateFormat mSdf;

        /**
         * Create Timber log tree that log to {@link PrintStream}
         * @param log Stream to log to
         */
        ReleaseTree(@NonNull PrintStream log) {
            mLog = log;
            mSdf = new SimpleDateFormat("dd/MM HH:mm:ss:SSS ",
                    Locale.getDefault());
        }


        @Override
        protected boolean isLoggable(String tag, int priority) {
            return priority != Log.VERBOSE && priority != Log.DEBUG;
        }

        @Override
        protected void log(int priority, String tag, @NonNull String message, Throwable t) {
            StringBuilder sb = new StringBuilder();

            sb.append(mSdf.format(new Date()));
            switch (priority) {
                case Log.ERROR:
                    sb.append("E/");
                    break;
                case Log.WARN:
                    sb.append("W/");
                    break;
                case Log.INFO:
                    sb.append("I/");
                    break;
                case Log.ASSERT:
                    sb.append("A/");
                    break;
            }
            sb.append(tag);
            sb.append(": ");
            sb.append(message);
            sb.append('\n');

            mLog.append(sb.toString());
            if (t != null) {
                t.printStackTrace(mLog);
            }

            /* TODO send anonymous error info to some analytics service
            FakeCrashLibrary.log(priority, tag, message);

            if (t != null) {
                if (priority == Log.ERROR) {
                    FakeCrashLibrary.logError(t);
                } else if (priority == Log.WARN) {
                    FakeCrashLibrary.logWarning(t);
                }
            }
            */
        }
    }

    /**
     * Check Google Play services and shows error dialog to user if they are not installed
     *
     * @param activity Activity to show dialog withing and context source
     * @return true if services are NOW accessible, false otherwise
     */
    public static boolean checkPlayServices(@NonNull Activity activity) {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(activity);
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(activity, result,
                        9000).show();
            }

            return false;
        }
        return true;
    }
}