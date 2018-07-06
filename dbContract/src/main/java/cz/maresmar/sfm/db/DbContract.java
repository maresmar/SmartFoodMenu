package cz.maresmar.sfm.db;

import android.provider.BaseColumns;

/**
 * API contract of table names and it's column names used in SQLite database
 */
public class DbContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private DbContract() {}

    /* Inner classes that defines the table contents */
    public static class Credential implements BaseColumns {
        public static final String TABLE_NAME = "Credential";
        public static final String COLUMN_NAME_CGID = "CGID";
        public static final String COLUMN_NAME_PGID = "PGID";
        public static final String COLUMN_NAME_UID = "UID";
        public static final String COLUMN_NAME_NAME = "CName";
        public static final String COLUMN_NAME_PASS = "CPass";
        public static final String COLUMN_NAME_CREDIT = "CCredit";
        public static final String COLUMN_NAME_CREDENTIAL_LOW_THRESHOLD = "CCreditLowThreshold";
        public static final String COLUMN_NAME_EXTRA = "CExtra";
        public static final String COLUMN_NAME_FLAGS = "CFlags";
    }

    public static class CredentialsGroup implements BaseColumns {
        public static final String TABLE_NAME = "CredentialsGroup";
        public static final String COLUMN_NAME_NAME = "CGName";
        public static final String COLUMN_NAME_DESCRIPTION = "CGDescription";
    }

    public static class Portal implements BaseColumns {
        public static final String TABLE_NAME = "Portal";
        public static final String COLUMN_NAME_PGID = "PGID";
        public static final String COLUMN_NAME_NAME = "PName";
        public static final String COLUMN_NAME_PLUGIN = "PPlug";
        public static final String COLUMN_NAME_REF = "PRef";
        public static final String COLUMN_NAME_LOC_N = "PLocN";
        public static final String COLUMN_NAME_LOC_E = "PLocE";
        public static final String COLUMN_NAME_EXTRA = "PExtra";
        public static final String COLUMN_NAME_SECURITY = "PSecurity";
        public static final String COLUMN_NAME_FEATURES = "PFeatures";
        public static final String COLUMN_NAME_FLAGS = "PFlags";
    }

    public static class MenuEntry implements BaseColumns {
        public static final String TABLE_NAME = "MenuEntry";
        public static final String COLUMN_NAME_PID = "PID";
        public static final String COLUMN_NAME_FID = "FID";
        public static final String COLUMN_NAME_MGID = "MGID";
        public static final String COLUMN_NAME_LABEL = "MELabel";
        public static final String COLUMN_NAME_DATE = "MEDate";
        public static final String COLUMN_NAME_REMAINING_TO_TAKE = "MERemainingTake";
        public static final String COLUMN_NAME_REMAINING_TO_ORDER = "MERemainingOrder";
        public static final String COLUMN_NAME_RELATIVE_ID = "MERelID";
        public static final String COLUMN_NAME_EXTRA = "MEExtra";
        // Aliases
        public static final String SYNCED_ACTION_TABLE_ALIAS = "SyncedAction";
        public static final String LOCAL_ACTION_TABLE_ALIAS = "LocalAction";
        public static final String EDIT_ACTION_TABLE_ALIAS = "EditAction";
    }

    public static class Food implements BaseColumns {
        public static final String TABLE_NAME = "Food";
        public static final String COLUMN_NAME_NAME = "FName";
    }

    public static class MenuGroup implements BaseColumns {
        public static final String TABLE_NAME = "MenuGroup";
        public static final String COLUMN_NAME_NAME = "MGName";
    }

    public static class GroupMenuEntry implements BaseColumns {
        public static final String TABLE_NAME = "GroupMenuEntry";
        public static final String COLUMN_NAME_CGID = "CGID";
        public static final String COLUMN_NAME_ME_RELATIVE_ID = MenuEntry.COLUMN_NAME_RELATIVE_ID;
        public static final String COLUMN_NAME_ME_PID = MenuEntry.COLUMN_NAME_PID;
        public static final String COLUMN_NAME_PRICE = "GMEPrice";
        public static final String COLUMN_NAME_STATUS = "GMEStatus";
    }

    public static class FoodAction implements BaseColumns {
        public static final String TABLE_NAME = "FoodAction";
        public static final String COLUMN_NAME_FA_RELATIVE_ID = "FARelID";
        public static final String COLUMN_NAME_CID = "CID";
        public static final String COLUMN_NAME_SYNC_STATUS = "FASyncStatus";
        public static final String COLUMN_NAME_ENTRY_TYPE = "FAEntryType";
        public static final String COLUMN_NAME_ME_RELATIVE_ID = MenuEntry.COLUMN_NAME_RELATIVE_ID;
        public static final String COLUMN_NAME_ME_PID = MenuEntry.COLUMN_NAME_PID;
        public static final String COLUMN_NAME_PRICE = "FAPrice";
        public static final String COLUMN_NAME_DESCRIPTION = "FADescription";
        public static final String COLUMN_NAME_RESERVED_AMOUNT = "FAResAmount";
        public static final String COLUMN_NAME_OFFERED_AMOUNT = "FAOffAmount";
        public static final String COLUMN_NAME_TAKEN_AMOUNT = "FATakenAmount";
        public static final String COLUMN_NAME_LAST_CHANGE = "FALastChange";
        // Aliases
        public static final String SYNCED_TABLE_ALIAS = "SyncedFoodAction";
    }

    public static class PortalGroup implements BaseColumns {
        public static final String TABLE_NAME = "PortalGroup";
        public static final String COLUMN_NAME_NAME = "PGName";
        public static final String COLUMN_NAME_DESCRIPTION = "PGDes";
    }

    public static class User implements BaseColumns {
        public static final String TABLE_NAME = "User";
        public static final String COLUMN_NAME_NAME = "UName";
        public static final String COLUMN_NAME_PICTURE = "UPicture";
    }
}
