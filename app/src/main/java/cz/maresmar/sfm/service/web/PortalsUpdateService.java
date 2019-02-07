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

package cz.maresmar.sfm.service.web;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import cz.maresmar.sfm.app.ServerContract;
import cz.maresmar.sfm.provider.ProviderContract;
import cz.maresmar.sfm.provider.PublicProviderContract;
import timber.log.Timber;

/**
 * Portals updates service that downloads portals data from web server. Existing data with same primary
 * key is replaced with new data from web server.
 * <p>
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.</p>
 *
 * @see ServerContract
 */
public class PortalsUpdateService extends IntentService {
    public static final String BROADCAST_PORTALS_UPDATE_FINISHED = "cz.maresmar.sfm.broadcast.PORTAL_UPDATE_FINISHED";
    public static final String EXTRA_UPDATE_RESULT = "updateResult";
    public static final String EXTRA_MESSAGE = "updateMsg";

    public static final int UPDATE_RESULT_OK = 0;
    public static final int UPDATE_RESULT_IO_ERROR = 1;
    public static final int UPDATE_RESULT_OUTDATED_API = 2;

    @IntDef(value = {
            UPDATE_RESULT_OK,
            UPDATE_RESULT_IO_ERROR,
            UPDATE_RESULT_OUTDATED_API
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface UpdateResult {
    }

    // IntentService can perform, e.g. UPDATE_PORTALS
    private static final String ACTION_UPDATE_PORTALS = "cz.maresmar.sfm.action.UPDATE_PORTALS";

    /**
     * Create new instance with default name for thread.
     */
    public PortalsUpdateService() {
        super("PortalsUpdateService");
    }

    /**
     * Starts this service to perform portals update. If
     * the service is already performing a task this action will be queued.
     *
     * @param context Application context
     * @see IntentService
     */
    public static void startUpdate(Context context) {
        Intent intent = new Intent(context, PortalsUpdateService.class);
        intent.setAction(ACTION_UPDATE_PORTALS);
        context.startService(intent);
    }

    /**
     * Sends update result to registered broadcast listeners.
     *
     * @param context Application context
     * @param result  Result of update
     * @param msg     Message from web server that specify some info (eg when API is updated, or
     *                server is under maintenance)
     */
    protected static void sendResult(Context context, @UpdateResult int result, String msg) {
        Intent intent = new Intent();
        intent.setAction(BROADCAST_PORTALS_UPDATE_FINISHED);
        intent.putExtra(EXTRA_UPDATE_RESULT, result);
        intent.putExtra(EXTRA_MESSAGE, msg);
        LocalBroadcastManager.getInstance(context)
                .sendBroadcast(intent);
    }

    /**
     * Receive incoming intent from query and call corresponding method.
     *
     * @param intent Incoming intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_UPDATE_PORTALS.equals(action)) {
                Timber.i("Portals update started");
                handleActionUpdate(this);
            } else {
                Timber.e("Unknown action fired %s", action);
            }
        }
    }

    /**
     * Does portals and portal groups update in the provided background thread
     * <p>
     * Can be called from another class because of background limit restrictions of services introduced
     * in Oreo</p>
     *
     * @param context Some valid context
     */
    @WorkerThread
    public static void handleActionUpdate(@NonNull Context context) {
        try {

            updatePortalGroups(context);
            updatePortals(context);

            Timber.i("Update completed");
            sendResult(context, UPDATE_RESULT_OK, null);
        } catch (IOException e) {
            Timber.w(e, "Network error during portal's update ");
            sendResult(context, UPDATE_RESULT_IO_ERROR, null);
        } catch (UnsupportedApiException e) {
            Timber.e(e, "Newer api received");
            sendResult(context, UPDATE_RESULT_OUTDATED_API, e.getServerMessage());
        }
    }

    private static void updatePortals(@NonNull Context context) throws IOException, UnsupportedApiException {
        Timber.i("Portal data sync started");

        URL url = new URL(ServerContract.PORTALS_SERVER_ADDRESS);

        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        try (FirstLineInputStream fis = new FirstLineInputStream(urlConnection.getInputStream())) {

            SfmJsonPortalResponse response = parseEntries(ServerContract.REPLY_TYPE_PORTALS,
                    SfmJsonPortalResponse.class, fis);

            ArrayList<ContentProviderOperation> operations = new ArrayList<>();
            Uri portalUri = ProviderContract.Portal.getUri();

            for (PortalEntry portalEntry : response.data) {
                operations.add(
                        ContentProviderOperation.newInsert(portalUri)
                                .withValue(ProviderContract.Portal._ID, portalEntry.id)
                                .withValue(ProviderContract.Portal.PORTAL_GROUP_ID, portalEntry.pgid)
                                .withValue(ProviderContract.Portal.NAME, portalEntry.name)
                                .withValue(ProviderContract.Portal.PLUGIN, portalEntry.plugin)
                                .withValue(ProviderContract.Portal.REFERENCE, portalEntry.reference)
                                .withValue(ProviderContract.Portal.LOC_N, portalEntry.locN)
                                .withValue(ProviderContract.Portal.LOC_E, portalEntry.locE)
                                .withValue(ProviderContract.Portal.EXTRA, portalEntry.extra)
                                .withValue(ProviderContract.Portal.SECURITY, portalEntry.security)
                                .withValue(ProviderContract.Portal.FEATURES, portalEntry.features)
                                .withYieldAllowed(true)
                                .build()
                );
            }

            // Apply them once for performance boost
            try {
                context.getContentResolver().applyBatch(ProviderContract.AUTHORITY, operations);
            } catch (OperationApplicationException e) {
                Timber.e(e, "Cannot save elements to db");
                throw new UnsupportedOperationException("Cannot save elements to db ", e);
            } catch (RemoteException e) {
                Timber.e(e, "Cannot connect to db");
                throw new RuntimeException("Cannot connect to db", e);
            }
        } finally {
            urlConnection.disconnect();
        }
    }

    private static void updatePortalGroups(@NonNull Context context) throws IOException, UnsupportedApiException {
        Timber.i("Portal group update started");

        URL url = new URL(ServerContract.PORTAL_GROUPS_SERVER_ADDRESS);

        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        try (FirstLineInputStream fis = new FirstLineInputStream(urlConnection.getInputStream())) {

            SfmJsonPortalGroupResponse response = parseEntries(ServerContract.REPLY_TYPE_PORTAL_GROUPS,
                    SfmJsonPortalGroupResponse.class, fis);

            ArrayList<ContentProviderOperation> operations = new ArrayList<>();
            Uri portalGroupUri = ProviderContract.PortalGroup.getUri();

            for (PortalGroupEntry portalGroupEntry : response.data) {
                operations.add(
                        ContentProviderOperation.newInsert(portalGroupUri)
                                .withValue(ProviderContract.PortalGroup._ID, portalGroupEntry.id)
                                .withValue(ProviderContract.PortalGroup.NAME, portalGroupEntry.name)
                                .withValue(ProviderContract.PortalGroup.DESCRIPTION, portalGroupEntry.description)
                                .withYieldAllowed(true)
                                .build()
                );
            }

            // Apply them once for performance boost
            try {
                context.getContentResolver().applyBatch(ProviderContract.AUTHORITY, operations);
            } catch (OperationApplicationException e) {
                Timber.e(e, "Cannot save elements to db");
                throw new UnsupportedOperationException("Cannot save elements to db ", e);
            } catch (RemoteException e) {
                Timber.e(e, "Cannot connect to db");
                throw new RuntimeException("Cannot connect to db", e);
            }
        } finally {
            urlConnection.disconnect();
        }
    }

    /**
     * Parse incoming stream containing server response in JSON. See docs for correct format.
     *
     * @param is            Input stream for server response
     * @param replyType     Response type returned from server
     * @param responseClass Wrapper class of server response format
     * @param <T>           Type of returned data
     * @return Parsed <code>T</code> list from results
     * @throws UnsupportedApiException Thrown when server uses unsupported API version (eg never)
     * @throws IOException             Thrown when IOException occurred or server respond in unsupported format
     *                                 (that could be caused by Wi-Fi captive portals)
     */
    static <T extends SfmJsonResponse> T parseEntries(@NonNull String replyType, @NonNull Class<T> responseClass,
                                                      @NonNull InputStream is) throws UnsupportedApiException, IOException {
        try {
            Reader reader = new InputStreamReader(is, "UTF-8");
            T response = new Gson().fromJson(reader, responseClass);

            if (response.apiVersion != ServerContract.PORTAL_JSON_API_VERSION) {
                Timber.e("Received result from new API %d, but %d excepted", response.apiVersion,
                        ServerContract.PORTAL_JSON_API_VERSION);
                Timber.i("Update message: %s", response.updateMessage);
                throw new UnsupportedApiException("Received API " + response.apiVersion + " but API " +
                        ServerContract.PORTAL_JSON_API_VERSION + " excepted", response.updateMessage);
            }
            if (!replyType.equals(response.replyType)) {
                Timber.e("Received result type '%s', but '%s' excepted", response.replyType,
                        replyType);
                throw new IOException("Web server returns unexpected result " + response.replyType +
                        ", but expected " + replyType);
            }

            return response;
        } catch (UnsupportedEncodingException e) {
            Timber.wtf(e, "UTF-8 encoding is not supported, poor me");
            throw new AssertionError(e);
        } catch (JsonSyntaxException e) {
            Timber.e(e, "Exception in reply syntax");
            throw new IOException("Web server provides invalid reply", e);
        }
    }
}

/**
 * JSON response helping class. See docs for formal description.
 */
class SfmJsonResponse {

    @SerializedName("apiVersion")
    public int apiVersion;

    @SerializedName("replyType")
    public String replyType;

    @SerializedName("updateMsg")
    public String updateMessage;
}

class SfmJsonPortalResponse extends SfmJsonResponse {
    @SerializedName("data")
    public List<PortalEntry> data;
}

class SfmJsonPortalGroupResponse extends SfmJsonResponse {
    @SerializedName("data")
    public List<PortalGroupEntry> data;
}


/**
 * Helping class for JSON parsing of food portals
 */
class PortalEntry {

    /**
     * Unique ID of entry in database (shared between web and application)
     */
    @SerializedName("ID")
    public int id;

    /**
     * Unique ID of group entry in database (shared between web and application)
     */
    @SerializedName("PGID")
    public int pgid;

    /**
     * Name of portal
     */
    @SerializedName("Name")
    public String name;

    /**
     * Plugin plugin in format "package;service"
     */
    @SerializedName("Plugin")
    public String plugin;

    /**
     * Reference to portal for internal usage of portal
     */
    @SerializedName("Ref")
    public String reference;

    /**
     * Location information
     */
    @SerializedName("LocN")
    public double locN;
    @SerializedName("LocE")
    public double locE;

    /**
     * Extra plugin data (specific for each plugin)
     */
    @SerializedName("Extra")
    public String extra;

    /**
     * Security restriction used for on-line communication
     */
    @SerializedName("Security")
    @PublicProviderContract.SecurityType
    public int security;

    /**
     * Supported portal features
     */
    @SerializedName("Features")
    @PublicProviderContract.PortalFeatures
    public int features;
}

/**
 * Helping class for JSON parsing of food portals
 */
class PortalGroupEntry {

    /**
     * Unique ID of entry in database (shared between web and application)
     */
    @SerializedName("ID")
    public int id;

    /**
     * Name of portal group
     */
    @SerializedName("Name")
    public String name;

    /**
     * Description of portal group
     */
    @SerializedName("Des")
    public String description;
}