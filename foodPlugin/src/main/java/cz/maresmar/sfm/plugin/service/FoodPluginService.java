package cz.maresmar.sfm.plugin.service;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.CallSuper;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.JobIntentService;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import cz.maresmar.sfm.Assert;
import cz.maresmar.sfm.plugin.ActionContract;
import cz.maresmar.sfm.plugin.ActionContract.ExtraFormatType;
import cz.maresmar.sfm.plugin.ActionContract.SyncTask;
import cz.maresmar.sfm.plugin.BroadcastContract;
import cz.maresmar.sfm.plugin.BroadcastContract.SyncResult;
import cz.maresmar.sfm.plugin.BroadcastContract.TestResult;
import cz.maresmar.sfm.plugin.BuildConfig;
import cz.maresmar.sfm.plugin.ExtraFormat;
import cz.maresmar.sfm.plugin.controller.ObjectsController;
import cz.maresmar.sfm.plugin.model.Action;
import cz.maresmar.sfm.plugin.model.GroupMenuEntry;
import cz.maresmar.sfm.plugin.model.LogData;
import cz.maresmar.sfm.plugin.model.MenuEntry;
import cz.maresmar.sfm.provider.PublicProviderContract;

/**
 * Plugin entry point, this class is called by app when when app needs action from
 * plugin, supported actions are {@link ActionContract#ACTION_SYNC},
 * {@link ActionContract#ACTION_PORTAL_TEST} and {@link ActionContract#ACTION_EXTRA_FORMAT}.
 * Action and extra data is delivered with {@link Intent}. See docs for more.
 * <p>
 * An {@link JobIntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * </p>
 */
public abstract class FoodPluginService extends JobIntentService {

    private static final String TAG = "FoodPluginService";

    private Uri mLogDataUri;
    private LogData mLogData;
    private String mErrorMessage;

    /**
     * Sets LogData (used only for testing)
     *
     * @param logData Some valid log data
     */
    @VisibleForTesting
    public void setLogData(@NonNull LogData logData) {
        mLogData = logData;
    }

    // -------------------------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------------------------

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        String action = intent.getAction();
        if (action == null)
            action = "null";

        // Sync action
        switch (action) {
            case ActionContract.ACTION_PORTAL_TEST:
                handleActionPortalTest(intent);
                break;
            case ActionContract.ACTION_SYNC:
                handleActionSync(intent);
                break;
            case ActionContract.ACTION_EXTRA_FORMAT:
                handleActionExtraFormat(intent);
                break;
            default:
                throw new IllegalArgumentException("Unsupported plugin action " + action);
        }
    }

    /**
     * Test's portal data, this method loads portal data from {@link android.content.ContentProvider}
     * and calls {@link #testPortalData(LogData)} then broadcast test results back to the app
     *
     * @param intent Input intent with parameters
     */
    private void handleActionPortalTest(@NonNull Intent intent) {
        // Input data
        mLogDataUri = intent.getData();
        if (mLogDataUri == null) {
            throw new IllegalArgumentException("Action was started without log-data uri");
        }
        loadLogData(mLogDataUri);

        // Test data
        @TestResult int result = testPortalData(mLogData);
        long portalId = Long.parseLong(mLogDataUri.getPathSegments().get(mLogDataUri.getPathSegments().size() - 2));

        // Return result of task
        BroadcastContract.broadcastTestDone(this, intent.getPackage(), portalId, result);
    }

    /**
     * Starts menu and action sync, this method first of all loads {@link LogData} and then calls
     * {@link #handleActionSync(int)} then broadcast sync results back to the app
     *
     * @param intent Input intent with parameters
     */
    private void handleActionSync(@NonNull Intent intent) {
        // Parse intent data
        final int todo = intent.getIntExtra(ActionContract.EXTRA_TASKS, -1);
        final long portalId = intent.getLongExtra(ActionContract.EXTRA_PORTAL_ID, ActionContract.UNKNOWN_ID);
        final long credentialsID = intent.getLongExtra(ActionContract.EXTRA_CREDENTIAL_ID,
                ActionContract.UNKNOWN_ID);
        if (!ActionContract.isValidPluginTaskInt(todo)) {
            throw new IllegalArgumentException("Action was started with illegal tasks list");
        }

        // Data entry points
        mLogDataUri = intent.getData();
        if (mLogDataUri == null) {
            throw new IllegalArgumentException("Action was started without log-data uri");
        }
        loadLogData(mLogDataUri);

        // Run sync action
        //noinspection WrongConstant
        SparseIntArray results = handleActionSync(todo);

        // Send back results
        @SyncTask int[] task = new int[results.size()];
        @SyncResult int[] result = new int[results.size()];
        @SyncResult int worstResult = BroadcastContract.RESULT_NOT_SUPPORTED;
        for (int i = 0; i < results.size(); i++) {
            //noinspection WrongConstant
            task[i] = results.keyAt(i);
            //noinspection WrongConstant
            @SyncResult
            int taskResult = results.get(task[i]);
            result[i] = taskResult;
            // Worst result
            worstResult = Math.max(worstResult, taskResult);
        }
        BroadcastContract.broadcastSyncDone(this, intent.getPackage(), portalId, credentialsID,
                task, result, worstResult, mErrorMessage);
    }

    /**
     * Broadcast extra format back to the app
     *
     * @param intent Input intent with parameters
     */
    private void handleActionExtraFormat(@NonNull Intent intent) {
        // Parse arguments
        @ExtraFormatType int formatType = intent.getIntExtra(ActionContract.EXTRA_FORMAT_TYPE, -1);
        String plugin = intent.getStringExtra(ActionContract.EXTRA_PLUGIN);

        // Find results
        List<ExtraFormat> extraFormats;
        switch (formatType) {
            case ActionContract.FORMAT_TYPE_PORTAL:
                extraFormats = getPortalExtraFormat();
                break;
            case ActionContract.FORMAT_TYPE_CREDENTIAL:
                extraFormats = getCredentialExtraFormat();
                break;
            default:
                throw new IllegalArgumentException("Unknown format type " + formatType);
        }

        JSONArray jsonExtras = new JSONArray();
        for (ExtraFormat extra : extraFormats) {
            try {
                jsonExtras.put(extra.toJSONObject());
            } catch (JSONException e) {
                Log.e(TAG, "Cannot convert extra to JSON", e);
            }
        }

        // Send result
        BroadcastContract.broadcastExtraFormat(this, intent.getPackage(), plugin, formatType, jsonExtras);
    }

    /**
     * Prepares calls for {@link #handleSyncTasks(LogData, int)} and saves results
     *
     * @param todo Task to be done
     * @return For each task it returns it's result (result codes are in {@link BroadcastContract})
     */
    @SuppressWarnings("WrongConstant")
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public SparseIntArray handleActionSync(@SyncTask int todo) {
        mErrorMessage = null;

        // Request lifecycle
        // SparseIntArray is Android's HastTable<Integer, Integer> with better performance
        SparseIntArray results = new SparseIntArray();

        // Remove unsupported tasks
        @SyncTask int notSupported = todo ^ (todo & supportedSyncTasks());
        for (int i = 0; (notSupported >> i) != 0; i++) {
            @SyncTask final int task = (1 << i);
            if ((notSupported & task) != 0) { // If task is set in to do list
                results.put(task, BroadcastContract.RESULT_NOT_SUPPORTED);
            }
        }

        // Ads dependencies for the rest
        todo = todo & supportedSyncTasks();
        todo = addSyncTasksDependencies(todo);


        try {
            onSyncRequestStarted(mLogData);

            while (todo > 0) {
                // Do the job
                @SyncTask int done = handleSyncTasks(mLogData, todo);

                // Puts OK results for done tasks
                for (int i = 0; (done >> i) != 0; i++) {
                    @SyncTask final int task = (1 << i);
                    if ((done & task) != 0) { // If flag is set in done list
                        results.put(task, BroadcastContract.RESULT_OK);
                    }
                }

                // Remove done tasks from to do list
                todo = todo ^ (done & todo);
            }

            // Request lifecycle
            onSyncRequestFinished(mLogData);
        } catch (WrongPassException e) {
            e.printStackTrace();
            results.put(todo, BroadcastContract.RESULT_WRONG_CREDENTIALS);
        } catch (WebPageFormatChangedException e) {
            e.printStackTrace();

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            e.printStackTrace(printWriter);
            mErrorMessage = stringWriter.toString();

            results.put(todo, BroadcastContract.RESULT_UNKNOWN_PORTAL_FORMAT);
        } catch (ServerMaintainException e) {
            e.printStackTrace();

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            e.printStackTrace(printWriter);
            mErrorMessage = stringWriter.toString();

            results.put(todo, BroadcastContract.RESULT_PORTAL_TEMPORALLY_INACCESSIBLE);
        } catch (IOException e) {
            e.printStackTrace();

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            e.printStackTrace(printWriter);
            mErrorMessage = stringWriter.toString();

            results.put(todo, BroadcastContract.RESULT_IO_EXCEPTION);
        }

        return results;
    }

    // -------------------------------------------------------------------------------------------
    // Helping methods
    // -------------------------------------------------------------------------------------------

    /**
     * Get Actions Uri that is associated with current sync action
     *
     * @return Action Uri
     */
    @NonNull
    private Uri getActionsUri() {
        return PublicProviderContract.Action.getCredentialUri(mLogData.credentialId);
    }

    /**
     * Get MenuEntries Uri that is associated with current sync action
     *
     * @return MenuEntry Uri
     */
    @NonNull
    private Uri getMenuEntriesUri() {
        return PublicProviderContract.MenuEntry.getPortalUri(mLogData.portalId);
    }

    /**
     * Get GroupMenuActions Uri that is associated with current sync action
     *
     * @return MenuEntry Uri
     */
    @NonNull
    private Uri getGroupMenuEntriesUri() {
        return PublicProviderContract.GroupMenuEntry.getPortalUri(mLogData.portalId);
    }

    // -------------------------------------------------------------------------------------------
    // Data loading and saving
    // -------------------------------------------------------------------------------------------

    // LogData
    private void loadLogData(@NonNull Uri uri) {
        // Load data
        List<LogData> data = ObjectsController.loadElements(this, uri, new LogData.LogDataInitializer(),
                null, null, PublicProviderContract.LogData.CREDENTIAL_ID + " ASC LIMIT 1");

        // Check result
        if (BuildConfig.DEBUG) {
            Assert.isOne(data.size());
        }

        mLogData = data.get(0);
        // Handle lifecycle
        onExtraLoad(mLogData);
    }

    private void updateLogData(@NonNull LogData data) {
        ObjectsController.changeElement(this, mLogDataUri, data);
        // Handle lifecycle
        onExtraLoad(mLogData);
    }


    /**
     * Notifies service that LogData was changed so the provided LogData will be saved to
     * {@link android.content.ContentProvider}
     */
    protected void updateProvidedLogData() {
        // Handle lifecycle
        onExtraSave(mLogData);
        // Save data
        updateLogData(mLogData);
    }

    // Actions

    /**
     * Loads last (food) action associated with current sync action (meaning with biggest date)
     *
     * @return Last Action
     */
    @Nullable
    protected Action loadLastSyncedAction() {
        // Load data
        String sortOrder = "ORDER BY " + PublicProviderContract.Action._ID + " DESC LIMIT 1";
        String selection = PublicProviderContract.Action.SYNC_STATUS + " = " + PublicProviderContract.ACTION_SYNC_STATUS_SYNCED;
        List<? extends Action> entries = loadActions(selection, null, sortOrder);

        switch (entries.size()) {
            case 0:
                return null;
            case 1:
                return entries.get(0);
            default:
                throw new RuntimeException("More last data found " + entries.size() + "found");
        }
    }

    /**
     * Loads last actions associated with current sync action
     *
     * @param selection     {@link android.content.ContentProvider} select selection
     * @param selectionArgs {@link android.content.ContentProvider} select arguments
     * @param sortOrder     {@link android.content.ContentProvider} select sort order
     * @return List of loaded actions
     */
    @NonNull
    protected List<? extends Action> loadActions(String selection, String[] selectionArgs, String sortOrder) {
        // Not include PublicProviderContract.ACTION_ENTRY_TYPE_VIRTUAL in results
        final String filterVirtual = "(" + PublicProviderContract.Action.ENTRY_TYPE + " != " + PublicProviderContract.ACTION_ENTRY_TYPE_VIRTUAL + ")";
        if (TextUtils.isEmpty(selection)) {
            selection = filterVirtual;
        } else {
            selection = selection + " AND " + filterVirtual;
        }

        // Do the query
        return ObjectsController.loadElements(this, getActionsUri(),
                new Action.Initializer(), selection, selectionArgs, sortOrder);
    }

    /**
     * Saves actions using parameters from current sync action
     *
     * @param actions Actions to be saved
     */
    protected void saveActions(@NonNull List<? extends Action> actions) {
        ObjectsController.saveElements(this, getActionsUri(), actions);
    }

    /**
     * Deletes actions using parameters from current sync action
     *
     * @param actions Actions to be deleted
     */
    protected void deleteActions(@NonNull List<? extends Action> actions) {
        ObjectsController.deleteElements(this, getActionsUri(), actions);
    }

    // Menu entry

    /**
     * Loads last MenuEntry associated with current sync action (meaning with biggest date)
     *
     * @return Last MenuEntry
     */
    @Nullable
    protected MenuEntry loadLastMenuEntry() {
        // Load data
        String sortOrder = "ORDER BY " + PublicProviderContract.MenuEntry._ID + " DESC LIMIT 1";
        List<MenuEntry> entries = loadMenuEntries(null, null, sortOrder);

        // Check result
        switch (entries.size()) {
            case 0:
                return null;
            case 1:
                return entries.get(0);
            default:
                throw new RuntimeException("More last data found " + entries.size() + "found");
        }
    }

    /**
     * Saves MenuEntries using parameters from current sync action
     *
     * @param entries MenuEntries to be saved
     */
    protected void saveMenuEntries(@NonNull List<MenuEntry> entries) {
        ObjectsController.saveElements(this, getMenuEntriesUri(), entries);
    }

    /**
     * Loads MenuEntries associated with current sync action
     *
     * @param selection     {@link android.content.ContentProvider} select selection
     * @param selectionArgs {@link android.content.ContentProvider} select arguments
     * @param sortOrder     {@link android.content.ContentProvider} select sort order
     * @return List of loaded MenuEntries
     */
    @NonNull
    protected List<MenuEntry> loadMenuEntries(String selection, String[] selectionArgs, String sortOrder) {
        return ObjectsController.loadElements(this, getMenuEntriesUri(),
                new MenuEntry.Initializer(), selection, selectionArgs, sortOrder);
    }

    /**
     * Deletes MenuEntries using parameters from current sync action
     *
     * @param entries MenuEntries to be deleted
     */
    protected void deleteMenuEntries(@NonNull List<MenuEntry> entries) {
        ObjectsController.deleteElements(this, getMenuEntriesUri(), entries);
    }

    // Group menu entry

    /**
     * Loads GroupMenuEntries associated with current sync action
     *
     * @param selection     {@link android.content.ContentProvider} select selection
     * @param selectionArgs {@link android.content.ContentProvider} select arguments
     * @param sortOrder     {@link android.content.ContentProvider} select sort order
     * @return List of loaded GroupMenuEntries
     */
    @NonNull
    protected List<GroupMenuEntry> loadGroupMenuEntries(String selection, String[] selectionArgs, String sortOrder) {
        return ObjectsController.loadElements(this, getGroupMenuEntriesUri(),
                new GroupMenuEntry.Initializer(), selection, selectionArgs, sortOrder);
    }

    /**
     * Saves GroupMenuEntries using parameters from current sync action
     *
     * @param entries GroupMenuEntries to be saved
     */
    protected void saveGroupMenuEntries(@NonNull List<GroupMenuEntry> entries) {
        ObjectsController.saveElements(this, getGroupMenuEntriesUri(), entries);
    }

    /**
     * Deletes GroupMenuEntries using parameters from current sync action
     *
     * @param entries GroupMenuEntries to be deleted
     */
    protected void deleteGroupMenuEntries(@NonNull List<GroupMenuEntry> entries) {
        ObjectsController.deleteElements(this, getGroupMenuEntriesUri(), entries);
    }

    /**
     * Saves new MenuEntries to {@link android.content.ContentProvider} and removes MenuEntries that are not
     * included in {@code newEntries} from {@link android.content.ContentProvider}
     * <p>
     * The change uses parameters from current sync action
     * </p>
     *
     * @param newEntries Entries sorted by {@link MenuEntry#relativeId}
     */
    protected void mergeMenuEntries(@NonNull List<MenuEntry> newEntries) {
        if (newEntries.size() == 0)
            return;
        MenuEntry firstEntry = newEntries.get(0);

        // Load entries from db
        String dbSelection = PublicProviderContract.MenuEntry.DATE + " >= ?";
        String[] dbSelectionArgs = new String[]{"" + firstEntry.date};
        String dbSortOrder = PublicProviderContract.MenuEntry.ME_RELATIVE_ID + " ASC";
        List<MenuEntry> dbEntries = loadMenuEntries(dbSelection, dbSelectionArgs, dbSortOrder);

        // Decide witch entry delete or save
        List<MenuEntry> toDelete = new ArrayList<>();
        // Go thought entries
        mergeEntries(dbEntries, newEntries, toDelete, (entry) -> entry.relativeId);

        // Send result to db
        deleteMenuEntries(toDelete);
        saveMenuEntries(newEntries);
    }

    /**
     * Saves new actions to {@link android.content.ContentProvider} and removes actions that are not
     * included in {@code newEntries} from {@link android.content.ContentProvider}
     * <p>
     * The change uses parameters from current sync action
     * </p>
     *
     * @param newEntries Entries sorted by {@link cz.maresmar.sfm.plugin.model.Action.MenuEntryAction#relativeMenuEntryId}
     * @param sinceDate  Time in millis where the merge should start
     */
    protected void mergeActionEntries(@NonNull List<Action.MenuEntryAction> newEntries, long sinceDate) {
        // Load entries from db
        String dbSelection = PublicProviderContract.Action.ME_DATE + " >= ? AND " +
                PublicProviderContract.Action.SYNC_STATUS + " == ? AND " +
                PublicProviderContract.Action.ENTRY_TYPE + " == ?";
        String[] dbSelectionArgs = new String[]{"" + sinceDate,
                "" + PublicProviderContract.ACTION_SYNC_STATUS_SYNCED,
                "" + PublicProviderContract.ACTION_ENTRY_TYPE_STANDARD
        };
        String dbSortOrder = PublicProviderContract.Action.ME_RELATIVE_ID + " ASC";
        List<Action.MenuEntryAction> dbEntries =
                (List<Action.MenuEntryAction>) loadActions(dbSelection, dbSelectionArgs, dbSortOrder);

        // Decide witch entry delete or save
        List<Action.MenuEntryAction> toDelete = new ArrayList<>();

        mergeEntries(dbEntries, newEntries, toDelete, (action) -> action.relativeMenuEntryId);


        // Send result to db
        deleteActions(toDelete);
        saveActions(newEntries);
    }

    private <T> void mergeEntries(@NonNull List<T> dbEntries, @NonNull List<T> newEntries,
                                  @NonNull List<T> toDelete, @NonNull Function<T, Long> mapper) {
        // Go thought entries
        Iterator<T> dbEntryIt = dbEntries.iterator();
        Iterator<T> newEntryIt = newEntries.iterator();

        if (dbEntryIt.hasNext() && newEntryIt.hasNext()) {

            T dbEntry = dbEntryIt.next();
            T newEntry = newEntryIt.next();

            long dbRelId = Long.MIN_VALUE;
            long newRelId = Long.MIN_VALUE;

            while (true) {
                if (BuildConfig.DEBUG) {
                    if (dbRelId > mapper.apply(dbEntry) || newRelId > mapper.apply(newEntry))
                        throw new RuntimeException("Entries should be ordered by ID ascending");
                }

                dbRelId = mapper.apply(dbEntry);
                newRelId = mapper.apply(newEntry);

                if (dbRelId == newRelId) { // ==
                    if (!dbEntryIt.hasNext() || !newEntryIt.hasNext())
                        break;
                    dbEntry = dbEntryIt.next();
                    newEntry = newEntryIt.next();
                } else if (dbRelId < newRelId) { // <
                    toDelete.add(dbEntry);
                    if (!dbEntryIt.hasNext())
                        break;
                    dbEntry = dbEntryIt.next();
                } else { // >
                    if (!newEntryIt.hasNext())
                        break;
                    newEntry = newEntryIt.next();
                }
            }
        }
        // Rest should be deleted
        while (dbEntryIt.hasNext()) {
            toDelete.add(dbEntryIt.next());
        }
    }


    // -------------------------------------------------------------------------------------------
    // Methods to implement or override
    // -------------------------------------------------------------------------------------------

    // LogData extra events

    /**
     * {@link FoodPluginService} lifecycle method where plugin should save it's config from extras.
     * This method is called when plugin saves LogData
     *
     * @param data LogData that contains extras
     * @see org.json.JSONObject
     */
    protected void onExtraSave(@NonNull LogData data) {
    }

    /**
     * {@link FoodPluginService} lifecycle method where plugin should load it's config from extras.
     * This method is called when plugin loads LogData associated with sync event.
     *
     * @param data LogData that contains extras
     * @see org.json.JSONObject
     */
    protected void onExtraLoad(@NonNull LogData data) {
    }

    // Action portal test

    /**
     * Method called by {@link ActionContract#ACTION_PORTAL_TEST} testing if LogData contains valid
     * portal information
     *
     * @param logData LogData contains only portal part to be tested
     * @return Test result
     * @see BroadcastContract
     */
    @TestResult
    protected abstract int testPortalData(@NonNull LogData logData);

    // Extra format

    /**
     * Method called by {@link ActionContract#EXTRA_FORMAT_TYPE} returning extra format for portal
     *
     * @return List of extras
     * @see ExtraFormat
     */
    @NonNull
    protected List<ExtraFormat> getPortalExtraFormat() {
        return new ArrayList<>();
    }

    /**
     * Method called by {@link ActionContract#EXTRA_FORMAT_TYPE} returning extra format for credential
     *
     * @return List of extras
     * @see ExtraFormat
     */
    @NonNull
    protected List<ExtraFormat> getCredentialExtraFormat() {
        return new ArrayList<>();
    }

    // Action sync

    /**
     * Adds dependencies needed by tasks that need to be done
     *
     * @param todo Tasks to be done
     * @return Tasks to be done with it's dependencies
     */
    @CheckResult
    @SyncTask
    protected int addSyncTasksDependencies(@SyncTask int todo) {
        return todo;
    }

    /**
     * {@link FoodPluginService} lifecycle method that is called when new sync action starts. This is
     * place where plugin should proceed login
     *
     * @param data LogData for this sync event
     * @throws IOException Thrown when some network error occurs
     */
    @CallSuper
    protected void onSyncRequestStarted(@NonNull LogData data) throws IOException {
    }

    /**
     * {@link FoodPluginService} lifecycle method that is called when sync action ends. This is
     * place where plugin should proceed logout
     *
     * @param data LogData for this sync event
     * @throws IOException Thrown when some network error occurs
     */
    @CallSuper
    protected void onSyncRequestFinished(@NonNull LogData data) throws IOException {
    }

    /**
     * Returns supported sync tasks
     *
     * @return Sync task using flags
     * @see ActionContract
     */
    @SyncTask
    protected abstract int supportedSyncTasks();

    /**
     * Sync action main entry point. This method is called as long there is remaining action to do.
     * After each call the method should do some sync task and return what did. The task dependencies are
     * already included.
     *
     * @param data LogData valid for current sync actions
     * @param task Task to do (see {@link ActionContract} for more
     * @return Done tasks
     * @throws IOException Thrown when some network exception occurs
     */
    @SyncTask
    protected abstract int handleSyncTasks(@NonNull LogData data, @SyncTask int task) throws IOException;

    // -------------------------------------------------------------------------------------------
    // HTTP connection security classes
    // -------------------------------------------------------------------------------------------

    // Create a trust manager that does not validate certificate chains (BAD thing)
    private final static TrustManager[] TRUST_ALL_CERTS = new TrustManager[]{new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }

        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkClientTrusted(X509Certificate[] chain,
                                       String authType) {
        }

        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkServerTrusted(X509Certificate[] chain,
                                       String authType) {
        }
    }};

    /**
     * Converts {@link URL} to (optimally subclass of) {@link HttpURLConnection} with application of
     * {@link cz.maresmar.sfm.provider.PublicProviderContract.LogData#PORTAL_SECURITY}. This validates using of encryption and
     * approach to certificates.
     *
     * @param url Url to be converted
     * @return Valid connection object with selected encryption and certificate trusting
     * @throws IOException If an I/O exception occurs
     */
    @NonNull
    protected HttpURLConnection openUrl(@NonNull URL url) throws IOException {
        HttpURLConnection urlConnection;
        switch (mLogData.portalSecurity) {
            case PublicProviderContract.SECURITY_TYPE_TRUST_TRUSTED: {
                if (!url.getProtocol().equalsIgnoreCase("https")) {
                    throw new IOException("Cannot open http URL " + url.toString() +
                            " with SECURITY_TYPE_TRUST_TRUSTED");
                }
                urlConnection = (HttpsURLConnection) url.openConnection();
                break;
            }
            case PublicProviderContract.SECURITY_TYPE_TRUEST_ALL: {
                if (!url.getProtocol().equalsIgnoreCase("https")) {
                    throw new IOException("Cannot open http URL " + url.toString() +
                            " with SECURITY_TYPE_TRUEST_ALL");
                }
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                try {
                    SSLContext sc = SSLContext.getInstance("TLS");
                    sc.init(null, TRUST_ALL_CERTS, new java.security.SecureRandom());
                    connection.setSSLSocketFactory(sc.getSocketFactory());
                    // optimally I can disable checking for host validation, but even self signed
                    // certs should be OK with that
                    // see https://developer.android.com/training/articles/security-ssl.html#UnknownCa
                    // for more
                } catch (NoSuchAlgorithmException | KeyManagementException e) {
                    Log.e(TAG, "Cannot set SECURITY_TYPE_TRUEST_ALL", e);
                }
                urlConnection = connection;
                break;
            }
            case PublicProviderContract.SECURITY_TYPE_NOT_ENCRYPTED:
                urlConnection = (HttpURLConnection) url.openConnection();
                break;
            default:
                throw new UnsupportedOperationException("Unsupported portal security " + mLogData.portalSecurity);
        }
        urlConnection.setConnectTimeout(10000);
        return urlConnection;
    }

}

/**
 * {@link java.util.function.Function} support for API 24
 */
interface Function<T, Y> {
    /**
     * {@link java.util.function.Function#apply(Object)} support for API 24
     */
    Y apply(T element);
}
