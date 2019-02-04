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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.security.ProviderInstaller;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cz.maresmar.sfm.Assert;
import cz.maresmar.sfm.BuildConfig;
import cz.maresmar.sfm.R;
import timber.log.Timber;

/**
 * Application singleton that is used for logging using Timber
 *
 * @see Application
 */
public class SfmApp extends Application implements ProviderInstaller.ProviderInstallListener {

    File mTmpFile;
    PrintStream mPrintStream;

    /**
     * Shows about activity
     *
     * @param context Some valid context
     */
    public static void startAboutActivity(@NonNull Context context) {
        new LibsBuilder()
                .withActivityTitle(context.getString(R.string.drawer_about))
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
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
//                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
//                    .penaltyDeath()
                    .build());
        }
        super.onCreate();

        // Register logging method
        if (BuildConfig.DEBUG) {
            // Let's log to builtin Android logger
            Timber.plant(new Timber.DebugTree());
        }

        AsyncTask.execute(() -> {
            try {
                File logsDir = new File(getCacheDir(), "logs");
                if (!logsDir.exists()) {
                    boolean result = logsDir.mkdir();

                    if (BuildConfig.DEBUG) {
                        Assert.that(result, "Logs folder wasn't created");
                    }
                }
                mTmpFile = File.createTempFile("sfm", ".log", logsDir);
                mPrintStream = new PrintStream(new BufferedOutputStream(
                        new FileOutputStream(mTmpFile, true)));

                Timber.plant(new ReleaseTree(mPrintStream));
            } catch (IOException e) {
                e.printStackTrace();
            }

            Timber.i("Starting sfm %s", getString(R.string.app_version));
        });

        NotificationContract.initNotificationChannels(this);

        // Update the security provider
        ProviderInstaller.installIfNeededAsync(this, this);
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
     *
     * @return The log file
     */
    @NonNull
    public File getLogFile() {
        mPrintStream.flush();
        return mTmpFile;
    }

    /**
     * Opens email app with log file
     */
    public void sendFeedback(Context context) {
        // Shows subjects dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.feedback_subject_title)
                .setMessage(R.string.feedback_subject_msg)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null);

        // Set up the input
        final EditText input = new EditText(context);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        builder.setView(input);

        final AlertDialog dialog = builder.create();

        // Set OK action that doesn't need to dismiss dialog
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                if (TextUtils.isEmpty(input.getText())) {
                    input.setError(context.getString(R.string.feedback_subject_empty));
                } else {
                    dialog.dismiss();
                    sendFeedback(context, input.getText().toString());
                }
            });
        });
        dialog.show();
    }

    private void sendFeedback(Context context, String subject) {
        Timber.i("Device %s (%s) on SDK %d", Build.DEVICE, Build.MANUFACTURER,
                Build.VERSION.SDK_INT);

        File logFile = getLogFile();
        Uri logUri = FileProvider.getUriForFile(
                this,
                "cz.maresmar.sfm.FileProvider",
                logFile);

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"mmrmartin+dev" + '@' + "gmail.com"});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "[sfm] " + subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.feedback_mail_text));
        emailIntent.putExtra(Intent.EXTRA_STREAM, logUri);
        emailIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(emailIntent,
                PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            context.grantUriPermission(packageName, logUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        context.startActivity(Intent.createChooser(emailIntent, getString(R.string.feedback_choose_email_app_dialog)));
    }

    /**
     * This method is only called if the provider is successfully updated
     * (or is already up-to-date).
     */
    @Override
    public void onProviderInstalled() {
        Timber.i("Security provider is up-to-date");
    }

    /**
     * This method is called if updating fails; the error code indicates
     * whether the error is recoverable.
     */
    @Override
    public void onProviderInstallFailed(int errorCode, Intent intent) {
        GoogleApiAvailability availability = GoogleApiAvailability.getInstance();
        if (availability.isUserResolvableError(errorCode)) {
            // Recoverable error. Show a dialog prompting the user to
            // install/update/enable Google Play services.
            availability.showErrorNotification(getApplicationContext(), errorCode);
            Timber.w("Google play services needs install/update/enable");
        } else {
            // Google Play services is not available.
            Timber.e("Google play services not available");
        }
    }

    /**
     * A logging tree class which logs important information for crash reporting and feedback
     */
    private static class ReleaseTree extends Timber.DebugTree {

        PrintStream mLog;
        SimpleDateFormat mSdf;

        /**
         * Create Timber log tree that log to {@link PrintStream}
         *
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