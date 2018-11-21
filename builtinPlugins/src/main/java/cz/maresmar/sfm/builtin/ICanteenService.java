package cz.maresmar.sfm.builtin;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLHandshakeException;

import cz.maresmar.sfm.plugin.model.Action;
import cz.maresmar.sfm.plugin.model.LogData;
import cz.maresmar.sfm.plugin.ActionContract;
import cz.maresmar.sfm.plugin.BroadcastContract;
import cz.maresmar.sfm.plugin.ExtraFormat;
import cz.maresmar.sfm.plugin.service.TaskGroup;
import cz.maresmar.sfm.plugin.service.TasksPluginService;
import cz.maresmar.sfm.plugin.service.WebPageFormatChangedException;
import cz.maresmar.sfm.plugin.service.WrongPassException;
import cz.maresmar.sfm.provider.PublicProviderContract;

/**
 * ICanteen plugin entry point
 */
public class ICanteenService extends TasksPluginService {

    // Static constants
    private static final String URL_MAIN = "/faces/login.jsp";
    private static final String URL_LOGIN = "/j_spring_security_check";
    private static final String URL_LOGOUT = "/j_spring_security_logout";
    private static final String URL_MONTH = "/faces/secured/month.jsp?terminal=false&keyboard=false&printer=false";
    private static final String URL_ORDER = "/faces/secured/";

    protected static final String EXTRA_PORTAL_VERSION = "portalVersion";
    protected static final String EXTRA_PORTAL_VERSION_AUTO_UPDATE = "portalAutoUpdate";
    protected static final String EXTRA_PORTAL_ID = "portalId";
    protected static final String EXTRA_ALLERGENS_PATTERN = "allergensPattern";

    private static final String YES = "✔️";
    private static final String NO = "✖️";

    private static final String VERSION_214 = "2.14";
    private static final String VERSION_213 = "2.13";
    private static final String VERSION_210 = "2.10";
    private static final String VERSION_207 = "2.7 - 2.8";
    private static final String VERSION_206 = "2.6";
    private static final String VERSION_205 = "2.5";

    private static final Pattern VERSION_PATTERN = Pattern.compile(".*iCanteen (verze|version) ([1-9])\\.([0-9]+)\\.([0-9]+).*");

    // LogData related variables
    int mPortalVersion = 213;
    boolean mPluginDataAutoUpdate = true;
    String mAllergensPattern = null;
    Integer mPortalId = null;

    @Override
    public void onCreate() {
        super.onCreate();

        registerTaskGroup(new MenuSyncTask());
        registerTaskGroup(new ChangesSyncTask());
    }

    @Override
    protected @BroadcastContract.TestResult
    int testPortalData(@NonNull LogData logData) {
        logData.portalFeatures = PublicProviderContract.FEATURE_RESTRICT_TO_ONE_ORDER_PER_GROUP;

        String webPage = logData.portalReference.trim();
        // Test if its valid web address
        if (!webPage.startsWith("http")) {
            webPage = "https://" + webPage;
        }

        if (webPage.startsWith("http:/")) {
            logData.portalSecurity = PublicProviderContract.SECURITY_TYPE_NOT_ENCRYPTED;
        }

        try {
            URL url = new URL(webPage);
        } catch (MalformedURLException e) {
            mErrorMessage = getString(R.string.malformed_url_test_msg);
            return BroadcastContract.TEST_RESULT_INVALID_DATA;
        }
        int endingIndex = webPage.indexOf("/faces/login.jsp");
        if (endingIndex != -1) {
            webPage = webPage.substring(0, endingIndex);
        }

        //Delete ending slash
        if (webPage.endsWith("/")) {
            webPage = webPage.substring(0, webPage.length() - 1);
        }

        logData.portalReference = webPage;

        updateProvidedLogData();

        // Test if its correct web page
        try {
            URL url = new URL(logData.portalReference + URL_MAIN);
            HttpURLConnection urlConnection = openUrl(url);
            Scanner sc = new Scanner(urlConnection.getInputStream());
            while (sc.hasNextLine()) {
                if (sc.nextLine().contains("iCanteen")) {
                    return BroadcastContract.TEST_RESULT_OK;
                }
            }
            sc.close();
            urlConnection.disconnect();

            mErrorMessage = getString(R.string.server_not_recognized_test_msg);
            return BroadcastContract.TEST_RESULT_INVALID_DATA;
        } catch (SSLHandshakeException e) {
            mErrorMessage = getString(R.string.ssl_exception_test_msg);
            e.printStackTrace();
            return BroadcastContract.TEST_RESULT_INVALID_DATA;
        } catch (FileNotFoundException e) {
            mErrorMessage = getString(R.string.illegal_web_page_test_msg);
            e.printStackTrace();
            return BroadcastContract.TEST_RESULT_INVALID_DATA;
        } catch (IOException e) {
            mErrorMessage = getString(R.string.io_exception_test_msg);
            e.printStackTrace();
            return BroadcastContract.TEST_RESULT_INVALID_DATA;
        }
    }

    @NonNull
    @Override
    protected List<ExtraFormat> getPortalExtraFormat() {
        List<ExtraFormat> extraFormats = new ArrayList<>();

        String name;
        ExtraFormat extra;

        // Portal version
        name = getString(R.string.ui_portal_version_name);
        extra = new ExtraFormat(EXTRA_PORTAL_VERSION, name);
        extra.valuesList = new String[]{VERSION_214, VERSION_213, VERSION_210, VERSION_207, VERSION_206, VERSION_205};
        extraFormats.add(extra);

        // Plugin auto update
        name = getString(R.string.ui_portal_auto_update_name);
        extra = new ExtraFormat(EXTRA_PORTAL_VERSION_AUTO_UPDATE, name);
        extra.description = getString(R.string.ui_portal_auto_update_des);
        extra.valuesList = new String[]{YES, NO};
        extraFormats.add(extra);

        // Portal ID
        name = getString(R.string.ui_portal_id_name);
        extra = new ExtraFormat(EXTRA_PORTAL_ID, name);
        extra.pattern = "[0-9]*";
        extra.description = getString(R.string.ui_portal_id_des);
        extraFormats.add(extra);

        // Allergens pattern
        name = getString(R.string.ui_allergens_pattern_name);
        extra = new ExtraFormat(EXTRA_ALLERGENS_PATTERN, name);
        extra.description = getString(R.string.ui_allergens_pattern_des);
        extraFormats.add(extra);

        return extraFormats;
    }

    @Override
    protected void onExtraLoad(@NonNull LogData data) {
        try {
            JSONObject extraData = new JSONObject(data.portalExtra);

            // Portal version info
            switch (extraData.getString(EXTRA_PORTAL_VERSION)) {
                case VERSION_205:
                    mPortalVersion = 205;
                    break;
                case VERSION_206:
                    mPortalVersion = 206;
                    break;
                case VERSION_207:
                    mPortalVersion = 207;
                    break;
                case VERSION_210:
                    mPortalVersion = 210;
                    break;
                case VERSION_213:
                    mPortalVersion = 213;
                    break;
                case VERSION_214:
                    mPortalVersion = 214;
                    break;
                default:
                    throw new RuntimeException("Unknown portal version " + extraData.getString(EXTRA_PORTAL_VERSION));
            }

            // Auto update
            mPluginDataAutoUpdate = YES.equals(extraData.getString(EXTRA_PORTAL_VERSION_AUTO_UPDATE));

            // Portal ID
            String portalIdString = extraData.getString(EXTRA_PORTAL_ID);
            if (!TextUtils.isEmpty(portalIdString)) {
                mPortalId = Integer.valueOf(portalIdString);
            } else {
                mPortalId = null;
            }

            // Allergens pattern
            mAllergensPattern = extraData.getString(EXTRA_ALLERGENS_PATTERN);
        } catch (JSONException e) {
            Log.e("iCanPlugin", "Bad portal extra extra data", e);
        }
    }

    @Override
    protected void onExtraSave(@NonNull LogData data) {
        try {
            JSONObject extraData = new JSONObject();

            // Portal version info
            String portalVersionText;
            switch (mPortalVersion) {
                case 205:
                    portalVersionText = VERSION_205;
                    break;
                case 206:
                    portalVersionText = VERSION_206;
                    break;
                case 207:
                    portalVersionText = VERSION_207;
                    break;
                case 210:
                    portalVersionText = VERSION_210;
                    break;
                case 213:
                    portalVersionText = VERSION_213;
                    break;
                case 214:
                    portalVersionText = VERSION_214;
                    break;
                default:
                    throw new RuntimeException("Unknown portal version " + mPortalVersion);
            }
            extraData.put(EXTRA_PORTAL_VERSION, portalVersionText);

            // Auto update
            if (mPluginDataAutoUpdate) {
                extraData.put(EXTRA_PORTAL_VERSION_AUTO_UPDATE, YES);
            } else {
                extraData.put(EXTRA_PORTAL_VERSION_AUTO_UPDATE, NO);
            }

            // Portal ID
            if (mPortalId != null) {
                extraData.put(EXTRA_PORTAL_ID, "" + mPortalId);
            } else {
                extraData.put(EXTRA_PORTAL_ID, "");
            }

            // Allergens pattern
            extraData.put(EXTRA_ALLERGENS_PATTERN, mAllergensPattern);

            data.portalExtra = extraData.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onSyncRequestStarted(@NonNull LogData data) throws IOException {
        super.onSyncRequestStarted(data);

        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));

        // Login to portal
        doLogin(data);
        // Older version needs two sing-ins
        if (mPortalVersion == 205) {
            doLogin(data);
        }
    }

    @Override
    protected void onSyncRequestFinished(@NonNull LogData data) throws IOException {
        super.onSyncRequestFinished(data);

        // Logout from portal
        doLogout(data);
    }

    private String preLogin(LogData data) throws IOException {

        URL loginUrl = new URL(data.portalReference + URL_MAIN);

        HttpURLConnection urlConnection = openUrl(loginUrl);
        urlConnection.addRequestProperty("Content-Type", "text/xml; charset=utf-8");

        // Output check up
        try (Scanner sc = new Scanner(urlConnection.getInputStream())) {
            Pattern pattern = Pattern.compile(".*<input +type=\"hidden\" +name=\"_csrf\" +value=\"([^\"]+) *\"/>.*");

            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                Matcher matcher = pattern.matcher(line);

                if (matcher.matches()) {
                    return matcher.group(1);
                } else if (line.contains("iCanteen")) {
                    autoUpdatePortalVersion(line);
                }
            }

            int responseCode = urlConnection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK)
                throw new WebPageFormatChangedException("Illegal server response " + responseCode);
        } finally {
            urlConnection.disconnect();
        }

        if (mPortalVersion >= 214) {
            throw new WebPageFormatChangedException("No csrf found");
        } else {
            return null;
        }
    }

    private void doLogin(LogData data) throws IOException {

        String csrf = preLogin(data);

        URL loginUrl = new URL(data.portalReference + URL_LOGIN);

        HttpURLConnection urlConnection = openUrl(loginUrl);
        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        // POST params
        Uri.Builder postDataBuilder = new Uri.Builder()
                .appendQueryParameter("j_username", data.credentialName)
                .appendQueryParameter("j_password", data.credentialPassword)
                .appendQueryParameter("_spring_security_remember_me", "false")
                .appendQueryParameter("terminal", "false");
        if (csrf != null) {
            postDataBuilder.appendQueryParameter("_csrf", csrf);
        }
        postDataBuilder.appendQueryParameter("targetUrl", "/faces/secured/main.jsp?terminal=false&menuStatus=true&printer=false&keyboard=false");
        String postQuery = postDataBuilder.build().getEncodedQuery();

        // POST data
        OutputStream output = urlConnection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(output, "UTF-8"));
        writer.write(postQuery);
        writer.close();
        output.close();

        urlConnection.setInstanceFollowRedirects(true);

        // Output check up
        try (Scanner sc = new Scanner(urlConnection.getInputStream())) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (line.contains("Špatné přihlašovací údaje") | line.contains("Bad credentials")) {
                    throw new WrongPassException("Bad logging data");
                } else if (line.contains("iCanteen")) {
                    autoUpdatePortalVersion(line);
                }
            }

            int responseCode = urlConnection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK)
                throw new WebPageFormatChangedException("Illegal server response " + responseCode);
        } finally {
            urlConnection.disconnect();
        }
    }

    private void doLogout(LogData data) throws IOException {
        URL logoutUrl = new URL(data.portalReference + URL_LOGOUT);

        HttpURLConnection urlConnection = openUrl(logoutUrl);

        urlConnection.setInstanceFollowRedirects(true);

        int responseCode = urlConnection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK)
            throw new WebPageFormatChangedException("Illegal server response " + responseCode);

        urlConnection.disconnect();
    }

    @VisibleForTesting
    void autoUpdatePortalVersion(String line) {
        if (mPluginDataAutoUpdate) {
            Matcher matcher = VERSION_PATTERN.matcher(line);
            if (matcher.matches()) {
                mPortalVersion = Integer.parseInt(matcher.group(2)) * 100
                        + Integer.parseInt(matcher.group(3));
            }
        }
    }

    /**
     * Menu sync task
     */
    class MenuSyncTask extends TaskGroup {

        @Override
        public @ActionContract.SyncTask
        int provides() {
            return ActionContract.TASK_MENU_SYNC | ActionContract.TASK_GROUP_DATA_MENU_SYNC |
                    ActionContract.TASK_CREDIT_SYNC;
        }

        @Override
        public void run(@NonNull LogData data) throws IOException {
            String idParam = "";
            if (mPortalId != null) {
                idParam = "&vydejna=" + mPortalId;
            }

            URL monthUrl = new URL(data.portalReference + URL_MONTH + idParam);

            HttpURLConnection urlConnection = openUrl(monthUrl);
            urlConnection.addRequestProperty("Content-Type", "text/xml; charset=utf-8");


            try (InputStream is = urlConnection.getInputStream()) {
                // Skip first bites as there is some weird characters that crash parser
                int ch;
                //noinspection StatementWithEmptyBody
                while ((ch = is.read()) != '>' && ch != -1) {
                }

                // Parse data from web page
                ICanteenMenuParser fxp = new ICanteenMenuParser(
                        mPortalVersion,
                        mAllergensPattern,
                        mPluginDataAutoUpdate);
                fxp.parseData(is, data);

                // Save results
                // Menu entries
                mergeMenuEntries(fxp.getMenuEntries());
                // Group menu entries
                saveGroupMenuEntries(fxp.getGroupMenuEntries());
                // Actions
                long since;
                if (fxp.getMenuEntries().size() > 0) {
                    since = fxp.getMenuEntries().get(0).date;
                } else {
                    since = System.currentTimeMillis();
                }

                mergeActionEntries(fxp.getActionEntries(), since, data.portalId);
                // Credit
                data.credit = fxp.getCredit();
                // Extras
                mPortalVersion = fxp.getPortalVersion();
                updateProvidedLogData();

            } catch (XmlPullParserException e) {
                throw new WebPageFormatChangedException("New web server version....", e);
            } finally {
                urlConnection.disconnect();
            }
        }
    }

    /**
     * Changes sync task
     */
    class ChangesSyncTask extends TaskGroup {

        public long getTodayDate() {
            long actTime = System.currentTimeMillis();
            return actTime - (actTime % (1000 * 60 * 60 * 24));
        }

        @Override
        public @ActionContract.SyncTask
        int provides() {
            return ActionContract.TASK_ACTION_PRESENT_SYNC;
        }

        @Override
        public @ActionContract.SyncTask
        int depends() {
            return ActionContract.TASK_MENU_SYNC;
        }

        @Override
        public void run(@NonNull LogData data) throws IOException {
            String selection = PublicProviderContract.Action.SYNC_STATUS + " = " + PublicProviderContract.ACTION_SYNC_STATUS_LOCAL + " AND " +
                    PublicProviderContract.Action.ME_DATE + " >= " + getTodayDate() + " AND " +
                    PublicProviderContract.Action.ME_PORTAL_ID + " == " + data.portalId;
            List<Action.MenuEntryAction> actions = (List<Action.MenuEntryAction>) loadActions(selection, null, null);

            boolean doneSth = false;

            // For each change do following
            String lastUrlTime = null;
            for (Action.MenuEntryAction action : actions) {
                // Skip task where is nothing to do
                if (action.reservedAmount == action.syncedReservedAmount && action.offeredAmount == action.syncedOfferedAmount)
                    continue;

                // If new order is not possible
                if (action.reservedAmount > action.syncedReservedAmount &&
                        ((action.groupStatus & PublicProviderContract.MENU_STATUS_ORDERABLE) != PublicProviderContract.MENU_STATUS_ORDERABLE))
                    continue;

                // If order cancel is not possible
                if (action.reservedAmount < action.syncedReservedAmount &&
                        ((action.groupStatus & PublicProviderContract.MENU_STATUS_CANCELABLE) != PublicProviderContract.MENU_STATUS_CANCELABLE))
                    continue;

                doneSth = true;

                // Do the change
                String changeUrl = action.extra;
                // Fix time flag
                if (lastUrlTime != null) {
                    changeUrl = changeUrl.replaceFirst("time=\\d{13}", "time=" + lastUrlTime);
                }
                // Send the change
                URL url = new URL(data.portalReference + URL_ORDER + changeUrl);
                HttpURLConnection urlConnection = openUrl(url);
                urlConnection.addRequestProperty("Content-Type", "text/xml; charset=utf-8");
                // Get new time flag
                if (changeUrl.contains("time=")) {
                    Scanner sc = new Scanner(urlConnection.getInputStream());
                    switch (mPortalVersion) {
                        case 205:
                        case 206:
                            while (sc.hasNextLine()) {
                                String line = sc.nextLine();
                                int timePos = line.indexOf("time=");
                                if (timePos != -1) {
                                    lastUrlTime = line.substring(timePos + 5, line.indexOf("&", timePos + 5));
                                    break;
                                }
                            }
                            break;
                        default:
                            while (sc.hasNextLine()) {
                                String line = sc.nextLine();
                                int timePos = line.indexOf("id=\"time\"");
                                if (timePos != -1) {
                                    int beg = line.indexOf(">", timePos) + 1;
                                    int end = line.indexOf("<", beg);
                                    lastUrlTime = line.substring(beg, end);
                                    break;
                                }
                            }
                    }
                }

                // Check result in every case
                int responseCode = urlConnection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK)
                    throw new WebPageFormatChangedException("Illegal server response " + responseCode);
                urlConnection.disconnect();
            }

            if (doneSth) {
                // Do check menu sync
                new MenuSyncTask().run(data);
            }

            // The app check the results on the end, so it's done for now ;-)
        }
    }
}
