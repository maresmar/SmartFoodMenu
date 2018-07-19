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

package cz.maresmar.sfm.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

import cz.maresmar.sfm.provider.ProviderContract;
import cz.maresmar.sfm.provider.PublicProviderContract;

import cz.maresmar.sfm.R;

import static cz.maresmar.sfm.db.DbContract.*;

/**
 * SQLite database helper. Helps create, update or downgrade SQLite database.
 */
public class DbHelper extends SQLiteOpenHelper {

    private static final String TEXT_TYPE = " TEXT";
    private static final String REAL_TYPE = " REAL";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String NOT_NULL = " NOT NULL";
    private static final String UNIQUE = " UNIQUE";
    private static final String COMMA_SEP = ",";

    private static final String SQL_CREATE_USER_ENTRIES =
            "CREATE TABLE " + User.TABLE_NAME + " (" +
                    User._ID + " INTEGER PRIMARY KEY," +
                    User.COLUMN_NAME_NAME + TEXT_TYPE + NOT_NULL + COMMA_SEP +
                    User.COLUMN_NAME_PICTURE + TEXT_TYPE + NOT_NULL +
                    " )";

    private static final String SQL_CREATE_CREDENTIALS_ENTRIES =
            "CREATE TABLE " + Credential.TABLE_NAME + " (" +
                    Credential._ID + " INTEGER PRIMARY KEY," +
                    Credential.COLUMN_NAME_CGID + INTEGER_TYPE + NOT_NULL + COMMA_SEP +
                    Credential.COLUMN_NAME_UID + INTEGER_TYPE + NOT_NULL + COMMA_SEP +
                    Credential.COLUMN_NAME_PGID + INTEGER_TYPE + NOT_NULL + COMMA_SEP +
                    Credential.COLUMN_NAME_NAME + TEXT_TYPE + COMMA_SEP +
                    Credential.COLUMN_NAME_PASS + TEXT_TYPE + COMMA_SEP +
                    Credential.COLUMN_NAME_CREDIT + INTEGER_TYPE + COMMA_SEP +
                    Credential.COLUMN_NAME_CREDENTIAL_LOW_THRESHOLD + INTEGER_TYPE + COMMA_SEP +
                    Credential.COLUMN_NAME_EXTRA + TEXT_TYPE + COMMA_SEP +
                    Credential.COLUMN_NAME_FLAGS + INTEGER_TYPE + NOT_NULL +
                    " DEFAULT 0" + COMMA_SEP +
                    "FOREIGN KEY(" + Credential.COLUMN_NAME_UID + ") REFERENCES " +
                        User.TABLE_NAME + "(" + User._ID + ") ON DELETE CASCADE" + COMMA_SEP +
                    "FOREIGN KEY(" + Credential.COLUMN_NAME_CGID + ") REFERENCES " +
                        CredentialsGroup.TABLE_NAME + "(" + CredentialsGroup._ID + ") ON DELETE CASCADE" + COMMA_SEP +
                    "FOREIGN KEY(" + Credential.COLUMN_NAME_PGID + ") REFERENCES " +
                        PortalGroup.TABLE_NAME + "(" + PortalGroup._ID + ") ON DELETE CASCADE" + COMMA_SEP +
                    "UNIQUE ("+ Credential.COLUMN_NAME_UID + COMMA_SEP + Credential.COLUMN_NAME_PGID +")" +
                    " )";

    private static final String SQL_CREATE_CREDENTIALS_GROUP_ENTRIES =
            "CREATE TABLE " + CredentialsGroup.TABLE_NAME + " (" +
                    CredentialsGroup._ID + " INTEGER PRIMARY KEY," +
                    CredentialsGroup.COLUMN_NAME_NAME + TEXT_TYPE + NOT_NULL + UNIQUE + COMMA_SEP +
                    CredentialsGroup.COLUMN_NAME_DESCRIPTION + TEXT_TYPE +
                    " )";

    private static final String SQL_CREATE_PORTAL_ENTRIES =
            "CREATE TABLE " + Portal.TABLE_NAME + " (" +
                    Portal._ID + " INTEGER PRIMARY KEY," +
                    Portal.COLUMN_NAME_PGID + INTEGER_TYPE + NOT_NULL + COMMA_SEP +
                    Portal.COLUMN_NAME_NAME + TEXT_TYPE + NOT_NULL + UNIQUE + COMMA_SEP +
                    Portal.COLUMN_NAME_REF + TEXT_TYPE + COMMA_SEP +
                    Portal.COLUMN_NAME_EXTRA + TEXT_TYPE + COMMA_SEP +
                    Portal.COLUMN_NAME_PLUGIN + TEXT_TYPE + NOT_NULL + COMMA_SEP +
                    Portal.COLUMN_NAME_LOC_N + REAL_TYPE +
                        " CHECK (" + Portal.COLUMN_NAME_LOC_N + " <= 90" +
                            " AND " + Portal.COLUMN_NAME_LOC_N + " > -90 )" + COMMA_SEP +
                    Portal.COLUMN_NAME_LOC_E + REAL_TYPE +
                        " CHECK (" + Portal.COLUMN_NAME_LOC_E + " <= 180" +
                            " AND " + Portal.COLUMN_NAME_LOC_E + " > -180 )" + COMMA_SEP +
                    Portal.COLUMN_NAME_SECURITY + INTEGER_TYPE + NOT_NULL +
                        " DEFAULT " + ProviderContract.SECURITY_TYPE_TRUST_TRUSTED + " " + COMMA_SEP +
                    Portal.COLUMN_NAME_FEATURES + INTEGER_TYPE + NOT_NULL +
                        " DEFAULT 0" + COMMA_SEP +
                    Portal.COLUMN_NAME_FLAGS + INTEGER_TYPE + NOT_NULL +
                    " DEFAULT 0" + COMMA_SEP +
                    "FOREIGN KEY(" + Portal.COLUMN_NAME_PGID + ") REFERENCES " +
                        PortalGroup.TABLE_NAME + "(" + PortalGroup._ID + ") ON DELETE CASCADE" +
                    " )";

    private static final String SQL_CREATE_MENU_ENTRY_ENTRIES =
            "CREATE TABLE " + MenuEntry.TABLE_NAME + " (" +
                    MenuEntry._ID + " INTEGER PRIMARY KEY," +
                    MenuEntry.COLUMN_NAME_RELATIVE_ID + INTEGER_TYPE + COMMA_SEP +
                    MenuEntry.COLUMN_NAME_PID + INTEGER_TYPE + NOT_NULL + COMMA_SEP +
                    MenuEntry.COLUMN_NAME_FID + INTEGER_TYPE + NOT_NULL + COMMA_SEP +
                    MenuEntry.COLUMN_NAME_MGID + INTEGER_TYPE + COMMA_SEP +
                    MenuEntry.COLUMN_NAME_LABEL + TEXT_TYPE + NOT_NULL + COMMA_SEP +
                    MenuEntry.COLUMN_NAME_DATE + INTEGER_TYPE +
                        " CHECK (" + MenuEntry.COLUMN_NAME_DATE + " > 0 )" + COMMA_SEP +
                    MenuEntry.COLUMN_NAME_REMAINING_TO_TAKE + INTEGER_TYPE + NOT_NULL + " DEFAULT -1 " +
                        " CHECK (" + MenuEntry.COLUMN_NAME_REMAINING_TO_TAKE + " >= -1 )" + COMMA_SEP +
                    MenuEntry.COLUMN_NAME_REMAINING_TO_ORDER + INTEGER_TYPE + NOT_NULL + " DEFAULT -1 " +
                        " CHECK (" + MenuEntry.COLUMN_NAME_REMAINING_TO_ORDER + " >= -1 )" + COMMA_SEP +
                    MenuEntry.COLUMN_NAME_EXTRA + TEXT_TYPE + COMMA_SEP +
                    "FOREIGN KEY(" + MenuEntry.COLUMN_NAME_PID + ") REFERENCES "
                        + Portal.TABLE_NAME + "(" + Portal._ID + ") ON DELETE CASCADE" + COMMA_SEP +
                    "FOREIGN KEY(" + MenuEntry.COLUMN_NAME_FID + ") REFERENCES "
                        + Food.TABLE_NAME + "(" + Food._ID + ") ON DELETE CASCADE" + COMMA_SEP +
                    "FOREIGN KEY(" + MenuEntry.COLUMN_NAME_MGID + ") REFERENCES " +
                        MenuGroup.TABLE_NAME + "(" + MenuGroup._ID + ") ON DELETE SET NULL" + COMMA_SEP +
                    "UNIQUE ("+ MenuEntry.COLUMN_NAME_PID + COMMA_SEP +
                        MenuEntry.COLUMN_NAME_RELATIVE_ID +")" +
                    " )";

    private static final String SQL_CREATE_FOOD_ENTRIES =
            "CREATE TABLE " + Food.TABLE_NAME + " (" +
                    Food._ID + " INTEGER PRIMARY KEY," +
                    Food.COLUMN_NAME_NAME + TEXT_TYPE + NOT_NULL + UNIQUE +
                    " )";


    private static final String SQL_CREATE_MENU_GROUP_ENTRIES =
            "CREATE TABLE " + MenuGroup.TABLE_NAME + " (" +
                    MenuGroup._ID + " INTEGER PRIMARY KEY," +
                    MenuGroup.COLUMN_NAME_NAME + TEXT_TYPE + NOT_NULL + UNIQUE +
                    " )";

    private static final String SQL_CREATE_PORTAL_GROUP_ENTRIES =
            "CREATE TABLE " + PortalGroup.TABLE_NAME + " (" +
                    PortalGroup._ID + " INTEGER PRIMARY KEY," +
                    PortalGroup.COLUMN_NAME_NAME + TEXT_TYPE + UNIQUE + COMMA_SEP +
                    PortalGroup.COLUMN_NAME_DESCRIPTION + TEXT_TYPE +
                    " )";


    private static final String SQL_CREATE_GROUP_MENU_ENTRY_ENTRIES =
            "CREATE TABLE " + GroupMenuEntry.TABLE_NAME + " (" +
                    GroupMenuEntry._ID + " INTEGER PRIMARY KEY," +
                    GroupMenuEntry.COLUMN_NAME_CGID + INTEGER_TYPE + NOT_NULL + COMMA_SEP +
                    GroupMenuEntry.COLUMN_NAME_ME_RELATIVE_ID + INTEGER_TYPE + NOT_NULL + COMMA_SEP +
                    GroupMenuEntry.COLUMN_NAME_ME_PID + INTEGER_TYPE + NOT_NULL + COMMA_SEP +
                    GroupMenuEntry.COLUMN_NAME_PRICE + INTEGER_TYPE + NOT_NULL +
                        " CHECK (" + GroupMenuEntry.COLUMN_NAME_PRICE + " >= 0 )" + COMMA_SEP +
                    GroupMenuEntry.COLUMN_NAME_STATUS + INTEGER_TYPE + NOT_NULL + COMMA_SEP +
                    "FOREIGN KEY(" + GroupMenuEntry.COLUMN_NAME_CGID + ") REFERENCES " +
                        CredentialsGroup.TABLE_NAME + "(" + CredentialsGroup._ID + ") ON DELETE CASCADE" + COMMA_SEP +
                    "FOREIGN KEY("+ GroupMenuEntry.COLUMN_NAME_ME_RELATIVE_ID + COMMA_SEP +
                            GroupMenuEntry.COLUMN_NAME_ME_PID + ") REFERENCES " +
                        MenuEntry.TABLE_NAME + "(" + MenuEntry.COLUMN_NAME_RELATIVE_ID + COMMA_SEP +
                            MenuEntry.COLUMN_NAME_PID+ ") ON DELETE CASCADE" + COMMA_SEP +
                    "UNIQUE ("+ GroupMenuEntry.COLUMN_NAME_CGID + COMMA_SEP +
                        GroupMenuEntry.COLUMN_NAME_ME_RELATIVE_ID + COMMA_SEP +
                        GroupMenuEntry.COLUMN_NAME_ME_PID + ")" +
                    " )";

    private static final String SQL_CREATE_FOOD_ACTION_ENTRIES =
            "CREATE TABLE " + FoodAction.TABLE_NAME + " (" +
                    FoodAction._ID + " INTEGER PRIMARY KEY, " +
                    FoodAction.COLUMN_NAME_FA_RELATIVE_ID + INTEGER_TYPE +
                    " CHECK (" + FoodAction.COLUMN_NAME_SYNC_STATUS + " != " + PublicProviderContract.ACTION_SYNC_STATUS_SYNCED +
                    " or " + FoodAction.COLUMN_NAME_FA_RELATIVE_ID + " is not null )" + COMMA_SEP +
                    FoodAction.COLUMN_NAME_CID + INTEGER_TYPE + NOT_NULL + COMMA_SEP +
                    FoodAction.COLUMN_NAME_SYNC_STATUS + INTEGER_TYPE + NOT_NULL +
                    " CHECK (" + FoodAction.COLUMN_NAME_SYNC_STATUS + " IN ( " +
                            PublicProviderContract.ACTION_SYNC_STATUS_EDIT + COMMA_SEP +
                            PublicProviderContract.ACTION_SYNC_STATUS_LOCAL + COMMA_SEP +
                            PublicProviderContract.ACTION_SYNC_STATUS_SYNCED + COMMA_SEP +
                            PublicProviderContract.ACTION_SYNC_STATUS_FAILED + "))" + COMMA_SEP +
                    FoodAction.COLUMN_NAME_ENTRY_TYPE + INTEGER_TYPE  + NOT_NULL + COMMA_SEP +
                    FoodAction.COLUMN_NAME_ME_RELATIVE_ID + INTEGER_TYPE  + COMMA_SEP +
                    FoodAction.COLUMN_NAME_ME_PID + INTEGER_TYPE  + COMMA_SEP +
                    FoodAction.COLUMN_NAME_PRICE + INTEGER_TYPE + NOT_NULL +
                    " CHECK (" + FoodAction.COLUMN_NAME_PRICE + " >= 0 )" + COMMA_SEP +
                    FoodAction.COLUMN_NAME_DESCRIPTION + TEXT_TYPE  + COMMA_SEP +
                    FoodAction.COLUMN_NAME_RESERVED_AMOUNT + INTEGER_TYPE + NOT_NULL +
                        " CHECK (" + FoodAction.COLUMN_NAME_RESERVED_AMOUNT + " >= 0 )" + COMMA_SEP +
                    FoodAction.COLUMN_NAME_OFFERED_AMOUNT + INTEGER_TYPE + NOT_NULL +
                    " CHECK (" + FoodAction.COLUMN_NAME_OFFERED_AMOUNT + " >= 0 )" + COMMA_SEP +
                    FoodAction.COLUMN_NAME_TAKEN_AMOUNT + INTEGER_TYPE +
                    " CHECK (" + FoodAction.COLUMN_NAME_TAKEN_AMOUNT + " >= 0 )" + COMMA_SEP +
                    FoodAction.COLUMN_NAME_LAST_CHANGE + INTEGER_TYPE + NOT_NULL +
                        " DEFAULT (STRFTIME('%s', 'now') * 1000)" +
                        " CHECK (" + FoodAction.COLUMN_NAME_LAST_CHANGE + " > 0 )" + COMMA_SEP +
                    "FOREIGN KEY(" + FoodAction.COLUMN_NAME_CID + ") REFERENCES " +
                        Credential.TABLE_NAME + "(" + Credential._ID + ") ON DELETE CASCADE" + COMMA_SEP +
                    "FOREIGN KEY("+ FoodAction.COLUMN_NAME_ME_RELATIVE_ID + COMMA_SEP +
                            FoodAction.COLUMN_NAME_ME_PID + ") REFERENCES " +
                        MenuEntry.TABLE_NAME + "(" + MenuEntry.COLUMN_NAME_RELATIVE_ID + COMMA_SEP +
                            MenuEntry.COLUMN_NAME_PID+ ") ON DELETE CASCADE" + COMMA_SEP +
                    " CHECK ((" + FoodAction.COLUMN_NAME_ME_RELATIVE_ID +" is not null and "+
                        FoodAction.COLUMN_NAME_ME_PID +" is not null)" +
                            " or "+
                        FoodAction.COLUMN_NAME_DESCRIPTION +" is not null)" + COMMA_SEP +
                    " CHECK ((" + FoodAction.COLUMN_NAME_ME_RELATIVE_ID +" is not null and "+
                        FoodAction.COLUMN_NAME_ME_PID +" is not null)" +
                            " or "+
                        FoodAction.COLUMN_NAME_ENTRY_TYPE +" == " + ProviderContract.ACTION_ENTRY_TYPE_PAYMENT +")" + COMMA_SEP +
                    " CHECK (" + FoodAction.COLUMN_NAME_RESERVED_AMOUNT + " >= " +
                            FoodAction.COLUMN_NAME_OFFERED_AMOUNT + " + " + FoodAction.COLUMN_NAME_TAKEN_AMOUNT +" )" + COMMA_SEP +
                    "UNIQUE ("+ FoodAction.COLUMN_NAME_CID + COMMA_SEP +
                            FoodAction.COLUMN_NAME_FA_RELATIVE_ID +")" + COMMA_SEP +
                    "UNIQUE ("+ FoodAction.COLUMN_NAME_CID + COMMA_SEP +
                    FoodAction.COLUMN_NAME_ME_RELATIVE_ID + COMMA_SEP +
                    FoodAction.COLUMN_NAME_ME_PID + COMMA_SEP +
                    FoodAction.COLUMN_NAME_SYNC_STATUS + ")" +
                    " )";

    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "FoodUser.db";
    private Context mContext;

    /**
     * Create new DbHelper
     * @param context Some valid context (used for language settings)
     */
    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        mContext = context;
    }

    /**
     * This method is called only on Android 4.1 and enables foreign key support
     */
    @SuppressLint("NewApi")
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);

        db.setForeignKeyConstraintsEnabled(true);
    }

    /**
     * Enables foreign key support on Android 4.0 (for newer there is better API)
     * @param db Database where it will be enabled
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    private void enableForeignKeySupport(SQLiteDatabase db) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN && !db.isReadOnly()) {
            // Enable foreign key constraints
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        //noinspection deprecation
        enableForeignKeySupport(db);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //noinspection deprecation
        enableForeignKeySupport(db);
        // Without foreign key

        createTables(db);

        insertCredentialGroups(db);
    }

    private void createTables(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_USER_ENTRIES);
        db.execSQL(SQL_CREATE_CREDENTIALS_GROUP_ENTRIES);
        db.execSQL(SQL_CREATE_PORTAL_GROUP_ENTRIES);
        db.execSQL(SQL_CREATE_FOOD_ENTRIES);
        db.execSQL(SQL_CREATE_MENU_GROUP_ENTRIES);
        // Second group connected to above
        db.execSQL(SQL_CREATE_CREDENTIALS_ENTRIES);
        db.execSQL(SQL_CREATE_PORTAL_ENTRIES);
        // Third group
        db.execSQL(SQL_CREATE_MENU_ENTRY_ENTRIES);
        // Rest
        db.execSQL(SQL_CREATE_GROUP_MENU_ENTRY_ENTRIES);
        db.execSQL(SQL_CREATE_FOOD_ACTION_ENTRIES);
    }

    private void insertCredentialGroups(SQLiteDatabase db) {
        // Child
        ContentValues insertValues = new ContentValues();
        insertValues.put(CredentialsGroup._ID, PublicProviderContract.CREDENTIAL_GROUP_ID_CHILD);
        insertValues.put(CredentialsGroup.COLUMN_NAME_NAME, mContext.getString(R.string.credential_group_child));
        db.insert(CredentialsGroup.TABLE_NAME, null, insertValues);
        // Student
        insertValues = new ContentValues();
        insertValues.put(CredentialsGroup._ID, PublicProviderContract.CREDENTIAL_GROUP_ID_STUDENT);
        insertValues.put(CredentialsGroup.COLUMN_NAME_NAME, mContext.getString(R.string.credential_group_student));
        db.insert(CredentialsGroup.TABLE_NAME, null, insertValues);
        // Adult
        insertValues = new ContentValues();
        insertValues.put(CredentialsGroup._ID, PublicProviderContract.CREDENTIAL_GROUP_ID_ADULT);
        insertValues.put(CredentialsGroup.COLUMN_NAME_NAME, mContext.getString(R.string.credential_group_adult));
        db.insert(CredentialsGroup.TABLE_NAME, null, insertValues);
        // Adult from outside
        insertValues = new ContentValues();
        insertValues.put(CredentialsGroup._ID, PublicProviderContract.CREDENTIAL_GROUP_ID_EXTERNAL_ADULT);
        insertValues.put(CredentialsGroup.COLUMN_NAME_NAME, mContext.getString(R.string.credential_group_external_adult));
        db.insert(CredentialsGroup.TABLE_NAME, null, insertValues);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //noinspection deprecation
        enableForeignKeySupport(db);
        throw new UnsupportedOperationException("SqLiteDb don't have more versions");
        // you should call onCreate(db); on the end when it changed
    }
}
