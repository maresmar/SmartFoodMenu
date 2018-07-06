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

package cz.maresmar.sfm.db.controller;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cz.maresmar.sfm.db.DbContract.Credential;
import cz.maresmar.sfm.db.DbContract.Portal;
import cz.maresmar.sfm.provider.ProviderContract;

import static cz.maresmar.sfm.provider.ProviderContract.LogData;
import static cz.maresmar.sfm.provider.PublicProviderContract.PORTAL_ONLY_CREDENTIAL_ID;

/**
 * Controller around LogData view (join of Portal and Credential table, the Credential table is
 * optimal).
 */
public class LogDataController extends ViewController {

    private static final String mPortalOnlyQueryTables = Portal.TABLE_NAME + " LEFT OUTER JOIN " +
            Credential.TABLE_NAME + " ON (" + Credential.TABLE_NAME + "." + Credential.COLUMN_NAME_PGID + " = " +
            Portal.TABLE_NAME + "." + Portal.COLUMN_NAME_PGID  + ")";
    private static final String mStandardQueryTables = Portal.TABLE_NAME + " INNER JOIN " +
            Credential.TABLE_NAME + " ON (" + Credential.TABLE_NAME + "." + Credential.COLUMN_NAME_PGID + " = " +
            Portal.TABLE_NAME + "." + Portal.COLUMN_NAME_PGID  + ")";


    private static Set<String> mCredentialsColumns = new HashSet<>();
    static {
        mCredentialsColumns.add(LogData.CREDENTIAL_ID);
        mCredentialsColumns.add(LogData.CREDENTIAL_NAME);
        mCredentialsColumns.add(LogData.CREDENTIAL_PASS);
        mCredentialsColumns.add(LogData.CREDIT);
        mCredentialsColumns.add(LogData.CREDENTIAL_EXTRA);
    }

    // -------------------------------------------------------------------------------------------
    // Abstract class methods
    // -------------------------------------------------------------------------------------------

    @Override
    public long insert(@NonNull SQLiteDatabase db, ContentValues newValues) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(@NonNull SQLiteDatabase db, ContentValues values, String selection, String[] selectionArgs) {
        // Check params
        if(values.containsKey(LogData.CREDENTIALS_GROUP_ID) || values.containsKey(LogData.PORTAL_ID) ||
                values.containsKey(LogData.CREDENTIAL_ID)) {
            throw new IllegalArgumentException("You cannot update ID values using this Uri");
        }

        // Split values to two groups
        ContentValues credentialValues = new ContentValues();
        credentialValues.putAll(values);
        long credentialId = getCredentialId(selection);

        ContentValues portalValues = new ContentValues();
        portalValues.putAll(values);
        long portalId = getPortalId(selection);

        for(Map.Entry<String, Object> value: values.valueSet()) {
            if(mCredentialsColumns.contains(value.getKey())) {
                portalValues.remove(value.getKey());
                // leave in credentialValues
            } else {
                credentialValues.remove(value.getKey());
                // leave in portalValues
            }
        }

        int updatedPortalRows =  update(db, Portal.TABLE_NAME, portalValues,
                Portal._ID + " = " + portalId, null);

        // Does not update rows if it's portal only view (used in portal data tests)
        int updatedCredentialRows;
        if(credentialId != PORTAL_ONLY_CREDENTIAL_ID) {
            updatedCredentialRows = update(db, Credential.TABLE_NAME, credentialValues,
                    Credential._ID + " = " + credentialId, null);
        } else {
            updatedCredentialRows = updatedPortalRows;
        }

        return Math.min(updatedCredentialRows, updatedPortalRows);
    }

    @Override
    public int delete(@NonNull SQLiteDatabase db, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Cursor query(@NonNull SQLiteDatabase db, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Using SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        if(isPortalOnlySelection(selection)) {
            queryBuilder.setTables(mPortalOnlyQueryTables);
        } else {
            queryBuilder.setTables(mStandardQueryTables);
        }

        return queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder);
    }

    // -------------------------------------------------------------------------------------------
    // Helping methods
    // -------------------------------------------------------------------------------------------

    private long getCredentialId(String selection) {
        if(selection.contains(LogData.CREDENTIAL_ID)) {
            return getIdFromSelection(selection, LogData.CREDENTIAL_ID);
        } else {
            return ProviderContract.PORTAL_ONLY_CREDENTIAL_ID;
        }
    }

    private long getPortalId(String selection) {
        return getIdFromSelection(selection, LogData.PORTAL_ID);
    }

    private static boolean isPortalOnlySelection(@Nullable String selection) {
        return selection != null && selection.matches(" *\\(? *" + LogData.PORTAL_ID + " *==? *[0-9]+ *\\)? *");
    }

}
