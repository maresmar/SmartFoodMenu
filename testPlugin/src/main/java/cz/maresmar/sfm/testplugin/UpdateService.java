package cz.maresmar.sfm.testplugin;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import cz.maresmar.sfm.Assert;
import cz.maresmar.sfm.plugin.model.Action;
import cz.maresmar.sfm.plugin.model.GroupMenuEntry;
import cz.maresmar.sfm.plugin.model.LogData;
import cz.maresmar.sfm.plugin.model.MenuEntry;
import cz.maresmar.sfm.plugin.ActionContract;
import cz.maresmar.sfm.plugin.BroadcastContract;
import cz.maresmar.sfm.plugin.ExtraFormat;
import cz.maresmar.sfm.plugin.service.TaskGroup;
import cz.maresmar.sfm.plugin.service.TasksPluginService;
import cz.maresmar.sfm.plugin.service.WebPageFormatChangedException;
import cz.maresmar.sfm.provider.PublicProviderContract;
import cz.maresmar.sfm.testplugin.model.Credit;
import cz.maresmar.sfm.testplugin.model.Menu;
import cz.maresmar.sfm.testplugin.model.Order;

/**
 * Test plugin entry point
 */
public class UpdateService extends TasksPluginService {

    Stack<TaskGroup> mTaskGroups = new Stack<>();

    private static final String SERVER_URL = "http://www.mares.mzf.cz/sfm/testPortal";

    private static final String EXTRA_TEST_RESULT = "extraTestResult";
    private static final String EXTRA_TEST_RESULT_OK = "TEST_RESULT_OK";
    private static final String EXTRA_TEST_RESULT_INVALID_DATA = "TEST_RESULT_INVALID_DATA";

    @BroadcastContract.TestResult
    private int mPortalTestResult = 0;

    private int mExampleNumber = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        // Here you can register supported sync tasks

        registerTaskGroup(new MenuSyncTask());
        registerTaskGroup(new RemainingSyncTask());
        registerTaskGroup(new OrdersSyncTask());
        registerTaskGroup(new HistorySyncTask());
        registerTaskGroup(new CreditSyncTask());
    }

    // -------------------------------------------------------------------------------------------
    // Extras
    // -------------------------------------------------------------------------------------------

    @NonNull
    @Override
    protected List<ExtraFormat> getPortalExtraFormat() {
        List<ExtraFormat> extraFormats = new ArrayList<>();

        String name;
        ExtraFormat extra;

        // Example list extra - portal test result
        name = getString(R.string.ui_portal_test_result);
        extra = new ExtraFormat(EXTRA_TEST_RESULT, name);
        extra.valuesList = new String[]{EXTRA_TEST_RESULT_OK, EXTRA_TEST_RESULT_INVALID_DATA};
        extraFormats.add(extra);

        // Example number extra
        extra = new ExtraFormat("exampleNumber", "Some number");
        extra.pattern = "[0-9]*";
        extra.description = "Add only numbers to pass";
        extraFormats.add(extra);

        return extraFormats;
    }

    @NonNull
    @Override
    protected List<ExtraFormat> getCredentialExtraFormat() {
        List<ExtraFormat> extraFormats = new ArrayList<>();

        String name;
        ExtraFormat extra;

        // Example text extra
        extra = new ExtraFormat("exampleText", "Some text");
        extraFormats.add(extra);

        return extraFormats;
    }

    @Override
    protected void onExtraLoad(@NonNull LogData data) {
        try {
            JSONObject extraData = new JSONObject(data.portalExtra);

            // Example list extra - portal test result
            switch (extraData.getString(EXTRA_TEST_RESULT)) {
                case EXTRA_TEST_RESULT_OK:
                    mPortalTestResult = BroadcastContract.TEST_RESULT_OK;
                    break;
                case EXTRA_TEST_RESULT_INVALID_DATA:
                    mPortalTestResult = BroadcastContract.TEST_RESULT_INVALID_DATA;
                    break;
                default:
                    throw new RuntimeException("Unknown test result " + extraData.getString(EXTRA_TEST_RESULT));
            }

            mExampleNumber = extraData.getInt("exampleNumber");

            // ...
        } catch (JSONException e) {
            Log.e("TestPlugin", "Bad portal extra data", e);
        }

        if (TextUtils.isEmpty(data.credentialName)) {
            data.credentialName = "1";
            updateProvidedLogData();
        }
    }

    @Override
    protected void onExtraSave(@NonNull LogData data) {
        try {
            JSONObject extraData = new JSONObject();

            // Example list extra - portal test result
            String portalTestResultText;
            switch (mPortalTestResult) {
                case BroadcastContract.TEST_RESULT_OK:
                    portalTestResultText = EXTRA_TEST_RESULT_OK;
                    break;
                case BroadcastContract.TEST_RESULT_INVALID_DATA:
                    portalTestResultText = EXTRA_TEST_RESULT_INVALID_DATA;
                    break;
                default:
                    throw new RuntimeException("Unknown test result " + mPortalTestResult);
            }
            extraData.put(EXTRA_TEST_RESULT, portalTestResultText);

            extraData.put("exampleNumber", mExampleNumber);

            data.portalExtra = extraData.toString();

            // ...
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------------------------
    // Portal test (if you add new portal)
    // -------------------------------------------------------------------------------------------

    @Override
    protected @BroadcastContract.TestResult
    int testPortalData(@NonNull LogData logData) {

        if (TextUtils.isEmpty(logData.portalReference)) {
            logData.portalReference = SERVER_URL;
            logData.portalSecurity = PublicProviderContract.SECURITY_TYPE_NOT_ENCRYPTED;
        }
        logData.portalFeatures = PublicProviderContract.FEATURE_FOOD_STOCK | PublicProviderContract.FEATURE_MULTIPLE_ORDERS |
                PublicProviderContract.FEATURE_REMAINING_FOOD | PublicProviderContract.FEATURE_RESTRICT_TO_ONE_ORDER_PER_GROUP;
        updateProvidedLogData();

        // Extra syntax is checked in app
        // Here you can check semantics for example...

        return mPortalTestResult;
    }

    // -------------------------------------------------------------------------------------------
    // Tasks
    // -------------------------------------------------------------------------------------------

    private void parseMenu(LogData data, URL url) throws IOException {
        HttpURLConnection urlConnection = openUrl(url);

        try (FirstLineInputStream is = new FirstLineInputStream(urlConnection.getInputStream())) {
            Reader reader = new InputStreamReader(is, "UTF-8");

            Type listType = new TypeToken<ArrayList<Menu>>() {
            }.getType();
            List<Menu> parsedEntries = new Gson().fromJson(reader, listType);

            List<MenuEntry> menuEntries = new ArrayList<>();
            List<GroupMenuEntry> groupMenuEntries = new ArrayList<>();
            for (Menu parsedEntry : parsedEntries) {
                MenuEntry menuEntry = new MenuEntry(parsedEntry.relativeId);
                menuEntry.text = parsedEntry.text;
                menuEntry.group = parsedEntry.group;
                menuEntry.label = parsedEntry.label;
                menuEntry.date = parsedEntry.date;
                menuEntry.remainingToOrder = parsedEntry.remainingToOrder;
                menuEntry.remainingToTake = parsedEntry.remainingToTake;
                menuEntries.add(menuEntry);

                GroupMenuEntry groupMenuEntry = new GroupMenuEntry(parsedEntry.relativeId, data.credentialGroupId);
                groupMenuEntry.price = parsedEntry.price * 100;
                groupMenuEntry.menuStatus = parsedEntry.features;
                groupMenuEntries.add(groupMenuEntry);
            }
            saveMenuEntries(menuEntries);
            saveGroupMenuEntries(groupMenuEntries);
        } finally {
            urlConnection.disconnect();
        }
    }

    private void parseOrders(LogData data, URL url, boolean history) throws IOException {
        HttpURLConnection urlConnection = openUrl(url);

        try (FirstLineInputStream is = new FirstLineInputStream(urlConnection.getInputStream())) {
            Reader reader = new InputStreamReader(is, "UTF-8");

            Type listType = new TypeToken<ArrayList<Order>>() {
            }.getType();
            List<Order> parsedEntries = new Gson().fromJson(reader, listType);

            List<Action.MenuEntryAction> menuActions = new ArrayList<>();
            List<Action.PaymentAction> paymentActions = new ArrayList<>();

            List<MenuEntry> firstEntries = loadMenuEntries(null, null, PublicProviderContract.MenuEntry.DATE + " ASC LIMIT 1");
            long firstMenuEntryDate;
            if (firstEntries.size() > 0) {
                firstMenuEntryDate = firstEntries.get(0).date;
            } else {
                firstMenuEntryDate = Long.MAX_VALUE;
            }

            for (Order parsedOrder : parsedEntries) {
                if ("standard".equals(parsedOrder.Type)) {
                    if (firstMenuEntryDate <= (parsedOrder.FoodId / 100 * 1000)) {
                        long id = Math.max(parsedOrder._ID, parsedOrder.FoodId);
                        Action.MenuEntryAction action = new Action.MenuEntryAction(id, parsedOrder.FoodId, data.portalId);
                        action.lastChange = parsedOrder.Date;
                        action.reservedAmount = parsedOrder.Reserved;
                        action.offeredAmount = parsedOrder.Offered;
                        action.takenAmount = parsedOrder.Taken;
                        action.description = parsedOrder.Description;
                        action.price = parsedOrder.Price * 100;
                        menuActions.add(action);
                    }
                } else {
                    Action.PaymentAction action = new Action.PaymentAction(parsedOrder._ID, parsedOrder.Description, parsedOrder.Price * 100);
                    action.lastChange = parsedOrder.Date;
                    action.reservedAmount = parsedOrder.Reserved;
                    action.offeredAmount = parsedOrder.Offered;
                    action.takenAmount = parsedOrder.Taken;
                    paymentActions.add(action);
                }
            }
            if (!history) {
                mergeActionEntries(menuActions, System.currentTimeMillis());

                if (BuildConfig.DEBUG) {
                    Assert.isZero(paymentActions.size());
                }
            } else {
                saveActions(menuActions);
                saveActions(paymentActions);
            }
        } finally {
            urlConnection.disconnect();
        }
    }

    class MenuSyncTask extends TaskGroup {

        private static final String MENU_PATH = "/menu.php?format=json";

        @Override
        public @ActionContract.SyncTask
        int provides() {
            return ActionContract.TASK_MENU_SYNC | ActionContract.TASK_GROUP_DATA_MENU_SYNC;
        }

        @Override
        public void run(@NonNull LogData data) throws IOException {
            URL menuUrl = new URL(data.portalReference + MENU_PATH + "&user=" + data.credentialName);
            parseMenu(data, menuUrl);
        }
    }

    class RemainingSyncTask extends TaskGroup {

        private static final String REMAINING_PATH = "/remaining.php?format=json";

        @Override
        public @ActionContract.SyncTask
        int provides() {
            return ActionContract.TASK_REMAINING_TO_ORDER_SYNC | ActionContract.TASK_REMAINING_TO_TAKE_SYNC;
        }

        @Override
        public void run(@NonNull LogData data) throws IOException {
            URL remainingUrl = new URL(data.portalReference + REMAINING_PATH + "&user=" + data.credentialName);
            parseMenu(data, remainingUrl);
        }
    }

    class OrdersSyncTask extends TaskGroup {

        private static final String ORDERS_PATH = "/orders.php?format=json";
        private static final String ORDER_PATH = "/order.php?format=json";

        @Override
        @ActionContract.SyncTask
        public int depends() {
            return ActionContract.TASK_MENU_SYNC;
        }

        @Override
        @ActionContract.SyncTask
        public int provides() {
            return ActionContract.TASK_ACTION_PRESENT_SYNC;
        }

        @Override
        public void run(@NonNull LogData data) throws IOException {
            URL ordersUrl = new URL(data.portalReference + ORDERS_PATH + "&user=" + data.credentialName);
            parseOrders(data, ordersUrl, false);

            String selection = PublicProviderContract.Action.SYNC_STATUS + " = " + PublicProviderContract.ACTION_SYNC_STATUS_LOCAL + " AND " +
                    PublicProviderContract.Action.ME_DATE + " >= " + System.currentTimeMillis()
                    / 1000 / 60 / 60 / 24 * 24 * 60 * 60 * 1000;
            List<Action.MenuEntryAction> actions = (List<Action.MenuEntryAction>) loadActions(selection, null, null);

            boolean doneSth = false;
            for (Action.MenuEntryAction action : actions) {
                // Skip task where is nothing to do
                if (action.reservedAmount == action.syncedReservedAmount && action.offeredAmount == action.syncedOfferedAmount)
                    continue;

                doneSth = true;

                String params = "&id=" + action.relativeMenuEntryId + "&user=" + data.credentialName +
                        "&format=json&reserved=" + action.reservedAmount + "&offered=" + action.offeredAmount +
                        "&taken=" + action.takenAmount;

                // Send the change
                URL changeUrl = new URL(data.portalReference + ORDER_PATH + params);
                HttpURLConnection urlConnection = openUrl(changeUrl);

                int responseCode = urlConnection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new WebPageFormatChangedException("Illegal server response " + responseCode);
                }

                urlConnection.disconnect();
            }

            if (doneSth) {
                parseOrders(data, ordersUrl, false);
            }
        }
    }

    class HistorySyncTask extends TaskGroup {

        private static final String HISTORY_PATH = "/history.php?format=json";

        @Override
        @ActionContract.SyncTask
        public int depends() {
            return ActionContract.TASK_MENU_SYNC;
        }

        @Override
        @ActionContract.SyncTask
        public int provides() {
            return ActionContract.TASK_ACTION_HISTORY_SYNC;
        }

        @Override
        public void run(@NonNull LogData data) throws IOException {
            URL historyUrl = new URL(data.portalReference + HISTORY_PATH + "&user=" + data.credentialName);
            parseOrders(data, historyUrl, true);
        }
    }

    class CreditSyncTask extends TaskGroup {

        private static final String CREDIT_PATH = "/credit.php?format=json";

        @Override
        public @ActionContract.SyncTask
        int provides() {
            return ActionContract.TASK_CREDIT_SYNC;
        }

        @Override
        public void run(@NonNull LogData data) throws IOException {
            URL creditUrl = new URL(data.portalReference + CREDIT_PATH + "&user=" + data.credentialName);
            HttpURLConnection urlConnection = openUrl(creditUrl);

            try (FirstLineInputStream is = new FirstLineInputStream(urlConnection.getInputStream())) {
                Reader reader = new InputStreamReader(is, "UTF-8");

                Credit result = new Gson().fromJson(reader, Credit.class);

                data.credit = result.credit * 100;
                updateProvidedLogData();
            } finally {
                urlConnection.disconnect();
            }
        }
    }
}

/**
 * Filters standard input stream. The FirstLineInputStream provides access only to first line
 * of source input stream (ended with `\n`), the rest is is treated as end of the stream.
 */
class FirstLineInputStream extends InputStream {
    boolean mFirstLine = true;
    InputStream mInputStream;

    /**
     * Create new FirstLineInputStream from internal InputStream
     *
     * @param is internal valid InputStream
     */
    public FirstLineInputStream(InputStream is) {
        mInputStream = is;
    }

    /**
     * Reads the next byte of data from the input stream. The value byte is
     * returned as an <code>int</code> in the range <code>0</code> to
     * <code>255</code>. If no byte is available because the end of the stream or
     * end of the line has been reached, the value <code>-1</code> is returned.
     * This method blocks until input data is available, the end of the stream is detected,
     * or an exception is thrown.
     * <p>
     * <p> A subclass must provide an implementation of this method.
     *
     * @return the next byte of data, or <code>-1</code> if the end of the
     * stream  or end of fist line is reached.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public int read() throws IOException {
        if (mFirstLine) {
            int ch = mInputStream.read();
            if (ch != '\n')
                return ch;
            else
                mFirstLine = false;
        }
        return -1;
    }

    /**
     * Closes the internal input stream.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        // According to docs close method of InputStream did nothing
        super.close();
        mInputStream.close();
    }
}