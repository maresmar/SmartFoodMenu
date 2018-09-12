package cz.maresmar.sfm.provider;

import android.content.ComponentName;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import cz.maresmar.sfm.db.DbContract;

/**
 * API contract used in plugins (and in app) for access to access main {@link android.content.ContentProvider}.
 *
 * <p>Contains views column names and column values constants</p>
 */
@SuppressWarnings("WeakerAccess")
public class PublicProviderContract {

    // Main Uri authority
    public static final String AUTHORITY = "cz.maresmar.sfm.provider";

    public static final String PORTAL_PATH = "portal";
    public static final String CREDENTIALS_PATH = "credentials";
    public static final String LOGIN_DATA_PATH = "log-data";
    public static final String MENU_ENTRY_PATH = "menu";
    public static final String GROUP_MENU_ENTRY_PATH = "group-menu";
    public static final String ACTION_PATH = "action";

    // Static constants
    public static final int NO_INFO = -1;
    public static final long PORTAL_ONLY_CREDENTIAL_ID = -1;
    public static final long CUSTOM_DATA_OFFSET = 1_000_000;

    /**
     * Separator used in PLUGIN field, eg. package + {@code PLUGIN_DATA_SEPARATOR} + service
     */
    public static final char PLUGIN_DATA_SEPARATOR = '/';

    public static final int CREDENTIAL_GROUP_ID_CHILD = 0;
    public static final int CREDENTIAL_GROUP_ID_STUDENT = 1;
    public static final int CREDENTIAL_GROUP_ID_ADULT = 2;
    public static final int CREDENTIAL_GROUP_ID_EXTERNAL_ADULT = 3;

    @IntDef(value = {
            CREDENTIAL_GROUP_ID_CHILD,
            CREDENTIAL_GROUP_ID_STUDENT,
            CREDENTIAL_GROUP_ID_ADULT,
            CREDENTIAL_GROUP_ID_EXTERNAL_ADULT
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface BuiltinCredentialGroupIds {
    }

    // Security type
    public static final int SECURITY_TYPE_TRUST_TRUSTED = 100;
    public static final int SECURITY_TYPE_TRUEST_ALL = 101;
    public static final int SECURITY_TYPE_NOT_ENCRYPTED = 102;

    @IntDef(value = {
            SECURITY_TYPE_TRUST_TRUSTED,
            SECURITY_TYPE_TRUEST_ALL,
            SECURITY_TYPE_NOT_ENCRYPTED
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SecurityType {
    }

    // Action sync status
    public static final int ACTION_SYNC_STATUS_EDIT = 200;
    public static final int ACTION_SYNC_STATUS_LOCAL = 201;
    public static final int ACTION_SYNC_STATUS_SYNCED = 202;
    public static final int ACTION_SYNC_STATUS_FAILED = 203;

    @IntDef(value = {
            ACTION_SYNC_STATUS_EDIT,
            ACTION_SYNC_STATUS_LOCAL,
            ACTION_SYNC_STATUS_SYNCED,
            ACTION_SYNC_STATUS_FAILED
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ActionSyncStatus {
    }

    // Action type
    public static final int ACTION_ENTRY_TYPE_STANDARD = 300;
    public static final int ACTION_ENTRY_TYPE_PAYMENT = 301;
    public static final int ACTION_ENTRY_TYPE_VIRTUAL = 302;

    @IntDef(value = {
            ACTION_ENTRY_TYPE_STANDARD,
            ACTION_ENTRY_TYPE_PAYMENT,
            ACTION_ENTRY_TYPE_VIRTUAL,

    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ActionEntryType {
    }

    public static final int FEATURE_GROUP_FULL_SYNC = 1;
    public static final int FEATURE_FOOD_STOCK = 1 << 2;
    public static final int FEATURE_REMAINING_FOOD = 1 << 3;
    public static final int FEATURE_MULTIPLE_ORDERS = 1 << 4;
    public static final int FEATURE_RESTRICT_TO_ONE_ORDER_PER_GROUP = 1 << 5;

    @IntDef(flag = true, value = {
            FEATURE_GROUP_FULL_SYNC,
            FEATURE_FOOD_STOCK,
            FEATURE_REMAINING_FOOD,
            FEATURE_MULTIPLE_ORDERS,
            FEATURE_RESTRICT_TO_ONE_ORDER_PER_GROUP
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface PortalFeatures {
    }

    public static final int MENU_STATUS_ORDERABLE = 1;
    public static final int MENU_STATUS_CANCELABLE = 1 << 1;
    public static final int MENU_STATUS_COULD_USE_STOCK = 1 << 2;

    @IntDef(flag = true, value = {
            MENU_STATUS_ORDERABLE,
            MENU_STATUS_CANCELABLE,
            MENU_STATUS_COULD_USE_STOCK
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface MenuStatus {
    }

    static final String DOT = ".";


    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor package private.
    PublicProviderContract() {
    }

    /**
     * Parse database String to ComponentName that could be used in plugin
     *
     * @param plugin input text
     * @return ComponentName or null if input text is corrupted
     */
    @Nullable
    public static ComponentName parsePluginText(@NonNull String plugin) {
        // Fins the separator
        int slashIndex = plugin.indexOf(PLUGIN_DATA_SEPARATOR);
        // Check correct format
        if (slashIndex == -1) {
            return null;
        }

        String pluginPackage = plugin.substring(0, slashIndex);
        String pluginService = plugin.substring(slashIndex + 1, plugin.length());

        return new ComponentName(pluginPackage, pluginService);
    }

    /**
     * Creates new String from ComponentName for {@link LogData#PORTAL_PLUGIN} field
     *
     * @param component Input component name
     * @return Created field String
     */
    @NonNull
    public static String buildString(@NonNull ComponentName component) {
        return component.getPackageName() +
                PLUGIN_DATA_SEPARATOR +
                component.getClassName();
    }

    public static class LogData {
        public static final String PORTAL_ID = DbContract.Portal.TABLE_NAME + DOT + DbContract.Portal._ID;
        public static final String CREDENTIAL_ID = DbContract.Credential.TABLE_NAME + DOT + DbContract.Credential._ID;
        public static final String CREDENTIALS_GROUP_ID = DbContract.Credential.TABLE_NAME + DOT + DbContract.Credential.COLUMN_NAME_CGID;
        // Credential
        public static final String CREDENTIAL_NAME = DbContract.Credential.COLUMN_NAME_NAME;
        public static final String CREDENTIAL_PASS = DbContract.Credential.COLUMN_NAME_PASS;
        public static final String CREDIT = DbContract.Credential.COLUMN_NAME_CREDIT;
        public static final String CREDENTIAL_EXTRA = DbContract.Credential.COLUMN_NAME_EXTRA;
        // Portal
        public static final String PORTAL_NAME = DbContract.Portal.COLUMN_NAME_NAME;
        public static final String PORTAL_PLUGIN = DbContract.Portal.COLUMN_NAME_PLUGIN;
        public static final String PORTAL_REFERENCE = DbContract.Portal.COLUMN_NAME_REF;
        public static final String PORTAL_EXTRA = DbContract.Portal.COLUMN_NAME_EXTRA;
        public static final String PORTAL_SECURITY = DbContract.Portal.COLUMN_NAME_SECURITY;
        public static final String PORTAL_FEATURES = DbContract.Portal.COLUMN_NAME_FEATURES;

        @NonNull
        public static Uri getUri() {
            return Uri.parse("content://" + AUTHORITY + "/" + LOGIN_DATA_PATH);
        }

        @NonNull
        public static Uri getUri(long portalId, long credentialId) {
            return Uri.parse("content://" + AUTHORITY + "/" +
                    LOGIN_DATA_PATH + "/" + portalId + "/" + credentialId);
        }

        @NonNull
        public static Uri getUri(long portalId) {
            return Uri.parse("content://" + AUTHORITY + "/" +
                    LOGIN_DATA_PATH + "/" + portalId + "/" + PORTAL_ONLY_CREDENTIAL_ID);
        }
    }

    public static class MenuEntry {
        public static final String _ID = DbContract.MenuEntry.TABLE_NAME + DOT + DbContract.MenuEntry._ID;
        // RW
        public static final String ME_RELATIVE_ID = DbContract.MenuEntry.TABLE_NAME + DOT + DbContract.MenuEntry.COLUMN_NAME_RELATIVE_ID;
        public static final String PORTAL_ID = DbContract.MenuEntry.TABLE_NAME + DOT + DbContract.MenuEntry.COLUMN_NAME_PID;
        public static final String TEXT = DbContract.Food.COLUMN_NAME_NAME;
        public static final String GROUP = DbContract.MenuGroup.COLUMN_NAME_NAME;
        public static final String LABEL = DbContract.MenuEntry.COLUMN_NAME_LABEL;
        public static final String DATE = DbContract.MenuEntry.COLUMN_NAME_DATE;
        public static final String REMAINING_TO_TAKE = DbContract.MenuEntry.COLUMN_NAME_REMAINING_TO_TAKE;
        public static final String REMAINING_TO_ORDER = DbContract.MenuEntry.COLUMN_NAME_REMAINING_TO_ORDER;
        public static final String EXTRA = DbContract.MenuEntry.COLUMN_NAME_EXTRA;
        // RO
        public static final String PRICE = DbContract.GroupMenuEntry.COLUMN_NAME_PRICE;
        public static final String STATUS = DbContract.GroupMenuEntry.COLUMN_NAME_STATUS;
        public static final String SYNCED_RESERVED_AMOUNT = DbContract.MenuEntry.SYNCED_ACTION_TABLE_ALIAS + DOT + DbContract.FoodAction.COLUMN_NAME_RESERVED_AMOUNT;
        public static final String SYNCED_OFFERED_AMOUNT = DbContract.MenuEntry.SYNCED_ACTION_TABLE_ALIAS + DOT + DbContract.FoodAction.COLUMN_NAME_OFFERED_AMOUNT;
        public static final String SYNCED_TAKEN_AMOUNT = DbContract.MenuEntry.SYNCED_ACTION_TABLE_ALIAS + DOT + DbContract.FoodAction.COLUMN_NAME_TAKEN_AMOUNT;
        public static final String LOCAL_RESERVED_AMOUNT = DbContract.MenuEntry.LOCAL_ACTION_TABLE_ALIAS + DOT + DbContract.FoodAction.COLUMN_NAME_RESERVED_AMOUNT;
        public static final String LOCAL_OFFERED_AMOUNT = DbContract.MenuEntry.LOCAL_ACTION_TABLE_ALIAS + DOT + DbContract.FoodAction.COLUMN_NAME_OFFERED_AMOUNT;
        public static final String EDIT_RESERVED_AMOUNT = DbContract.MenuEntry.EDIT_ACTION_TABLE_ALIAS + DOT + DbContract.FoodAction.COLUMN_NAME_RESERVED_AMOUNT;
        public static final String EDIT_OFFERED_AMOUNT = DbContract.MenuEntry.EDIT_ACTION_TABLE_ALIAS + DOT + DbContract.FoodAction.COLUMN_NAME_OFFERED_AMOUNT;
        // Helping columns
        public static final String PORTAL_FEATURES = DbContract.Portal.TABLE_NAME + DOT + DbContract.Portal.COLUMN_NAME_FEATURES;
        public static final String PORTAL_NAME = DbContract.Portal.TABLE_NAME + DOT + DbContract.Portal.COLUMN_NAME_NAME;
        public static final String GROUP_ID = DbContract.MenuEntry.COLUMN_NAME_MGID;
        public static final String LAST_ACTION_CHANGE = "MAX(IFNULL(" + DbContract.MenuEntry.EDIT_ACTION_TABLE_ALIAS + DOT + DbContract.FoodAction.COLUMN_NAME_LAST_CHANGE + ", " + NO_INFO + "), " +
                "IFNULL(" + DbContract.MenuEntry.LOCAL_ACTION_TABLE_ALIAS + DOT + DbContract.FoodAction.COLUMN_NAME_LAST_CHANGE + ", " + NO_INFO + "), " +
                "IFNULL(" + DbContract.MenuEntry.SYNCED_ACTION_TABLE_ALIAS + DOT + DbContract.FoodAction.COLUMN_NAME_LAST_CHANGE + ", " + NO_INFO + ") )";

        @NonNull
        public static Uri getPortalUri(long portalId) {
            return Uri.parse("content://" + AUTHORITY + "/" +
                    PORTAL_PATH + "/" + portalId + "/" + MENU_ENTRY_PATH);
        }
    }

    public static class GroupMenuEntry implements BaseColumns {
        public static final String GROUP_ID = DbContract.GroupMenuEntry.COLUMN_NAME_CGID;
        public static final String ME_RELATIVE_ID = DbContract.GroupMenuEntry.COLUMN_NAME_ME_RELATIVE_ID;
        public static final String ME_PORTAL_ID = DbContract.GroupMenuEntry.COLUMN_NAME_ME_PID;
        public static final String PRICE = DbContract.GroupMenuEntry.COLUMN_NAME_PRICE;
        public static final String STATUS = DbContract.GroupMenuEntry.COLUMN_NAME_STATUS;

        @NonNull
        public static Uri getPortalUri(long portalId) {
            return Uri.parse("content://" + AUTHORITY + "/" +
                    PORTAL_PATH + "/" + portalId + "/" + GROUP_MENU_ENTRY_PATH);
        }
    }

    public static class Action {
        public static final String _ID = DbContract.FoodAction.TABLE_NAME + DOT + DbContract.FoodAction._ID;
        // RW
        public static final String ACTION_RELATIVE_ID = DbContract.FoodAction.TABLE_NAME + DOT + DbContract.FoodAction.COLUMN_NAME_FA_RELATIVE_ID;
        public static final String CREDENTIAL_ID = DbContract.FoodAction.TABLE_NAME + DOT + DbContract.FoodAction.COLUMN_NAME_CID;
        public static final String SYNC_STATUS = DbContract.FoodAction.TABLE_NAME + DOT + DbContract.FoodAction.COLUMN_NAME_SYNC_STATUS;
        public static final String ENTRY_TYPE = DbContract.FoodAction.TABLE_NAME + DOT + DbContract.FoodAction.COLUMN_NAME_ENTRY_TYPE;
        public static final String ME_RELATIVE_ID = DbContract.FoodAction.TABLE_NAME + DOT + DbContract.FoodAction.COLUMN_NAME_ME_RELATIVE_ID;
        public static final String ME_PORTAL_ID = DbContract.FoodAction.TABLE_NAME + DOT + DbContract.FoodAction.COLUMN_NAME_ME_PID;
        public static final String PRICE = DbContract.FoodAction.TABLE_NAME + DOT + DbContract.FoodAction.COLUMN_NAME_PRICE;
        public static final String DESCRIPTION = DbContract.FoodAction.TABLE_NAME + DOT + DbContract.FoodAction.COLUMN_NAME_DESCRIPTION;
        public static final String RESERVED_AMOUNT = DbContract.FoodAction.TABLE_NAME + DOT + DbContract.FoodAction.COLUMN_NAME_RESERVED_AMOUNT;
        public static final String OFFERED_AMOUNT = DbContract.FoodAction.TABLE_NAME + DOT + DbContract.FoodAction.COLUMN_NAME_OFFERED_AMOUNT;
        public static final String TAKEN_AMOUNT = DbContract.FoodAction.TABLE_NAME + DOT + DbContract.FoodAction.COLUMN_NAME_TAKEN_AMOUNT;
        public static final String LAST_CHANGE = DbContract.FoodAction.TABLE_NAME + DOT + DbContract.FoodAction.COLUMN_NAME_LAST_CHANGE;
        // RO - self join
        public static final String SYNCED_RESERVED_AMOUNT = DbContract.FoodAction.SYNCED_TABLE_ALIAS + DOT + DbContract.FoodAction.COLUMN_NAME_RESERVED_AMOUNT;
        public static final String SYNCED_OFFERED_AMOUNT = DbContract.FoodAction.SYNCED_TABLE_ALIAS + DOT + DbContract.FoodAction.COLUMN_NAME_OFFERED_AMOUNT;
        // RO - other tables join
        public static final String ME_DATE = DbContract.MenuEntry.COLUMN_NAME_DATE;
        public static final String ME_LABEL = DbContract.MenuEntry.COLUMN_NAME_LABEL;
        public static final String ME_GROUP_STATUS = DbContract.GroupMenuEntry.COLUMN_NAME_STATUS;
        public static final String ME_FOOD_NAME = DbContract.Food.COLUMN_NAME_NAME;
        public static final String ME_GROUP_NAME = DbContract.MenuGroup.COLUMN_NAME_NAME;
        public static final String ME_EXTRA = DbContract.MenuEntry.COLUMN_NAME_EXTRA;
        public static final String ME_PORTAL_NAME = DbContract.Portal.TABLE_NAME + DOT + DbContract.Portal.COLUMN_NAME_NAME;

        @NonNull
        public static Uri getCredentialUri(long credentialId) {
            return Uri.parse("content://" + AUTHORITY + "/" +
                    CREDENTIALS_PATH + "/" + credentialId + "/" + ACTION_PATH);
        }

        @NonNull
        public static Uri getUri() {
            return Uri.parse("content://" + AUTHORITY + "/" + ACTION_PATH);
        }
    }
}
