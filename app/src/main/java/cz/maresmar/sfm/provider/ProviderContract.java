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

package cz.maresmar.sfm.provider;

import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import cz.maresmar.sfm.db.DbContract;

/**
 * API contract used in app for access to main {@link android.content.ContentProvider}.
 *
 * <p>Contains views column names and column values constants. These views are created using table
 * columns from {@link DbContract}. Internally the {@link cz.maresmar.sfm.db.controller.ViewController}
 * joins them to one view.</p>
 *
 * @see DataProvider
 */
@SuppressWarnings("WeakerAccess")
public class ProviderContract extends PublicProviderContract {

    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private ProviderContract() {
        super();
    }

    public static final int PORTAL_FLAG_DISABLE_NEW_MENU_NOTIFICATION = 1;

    @IntDef(flag = true, value = {
            PORTAL_FLAG_DISABLE_NEW_MENU_NOTIFICATION
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface PortalFlags {
    }

    public static final int CREDENTIAL_FLAG_DISABLE_LOW_CREDIT_NOTIFICATION = 1;
    public static final int CREDENTIAL_FLAG_DISABLE_CREDIT_INCREASE_NOTIFICATION = 1 << 1;

    @IntDef(flag = true, value = {
            CREDENTIAL_FLAG_DISABLE_LOW_CREDIT_NOTIFICATION,
            CREDENTIAL_FLAG_DISABLE_CREDIT_INCREASE_NOTIFICATION
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface CredentialFlags {
    }


    // Uri parts
    public static final String USER_PATH = "user";
    public static final String PORTAL_GROUP_PATH = "portal-group";
    public static final String CREDENTIALS_GROUP_PATH = "credentials-group";
    public static final String DAY_PATH = "day";

    public static class User implements BaseColumns {
        public static final String NAME = DbContract.User.COLUMN_NAME_NAME;
        public static final String PICTURE = DbContract.User.COLUMN_NAME_PICTURE;

        public static Uri getUri() {
            return Uri.parse("content://" + AUTHORITY + "/" + USER_PATH);
        }
    }

    public static class Portal implements BaseColumns {
        // RW
        public static final String PORTAL_ID = DbContract.Portal.TABLE_NAME + DOT + DbContract.Portal._ID;
        public static final String PORTAL_GROUP_ID = DbContract.Portal.COLUMN_NAME_PGID;
        public static final String NAME = DbContract.Portal.COLUMN_NAME_NAME;
        public static final String PLUGIN = DbContract.Portal.COLUMN_NAME_PLUGIN;
        public static final String REFERENCE = DbContract.Portal.COLUMN_NAME_REF;
        public static final String LOC_N = DbContract.Portal.COLUMN_NAME_LOC_N;
        public static final String LOC_E = DbContract.Portal.COLUMN_NAME_LOC_E;
        public static final String EXTRA = DbContract.Portal.COLUMN_NAME_EXTRA;
        public static final String SECURITY = DbContract.Portal.COLUMN_NAME_SECURITY;
        public static final String FEATURES = DbContract.Portal.COLUMN_NAME_FEATURES;
        public static final String FLAGS = DbContract.Portal.COLUMN_NAME_FLAGS;
        // RO
        public static final String PORTAL_GROUP_NAME = DbContract.PortalGroup.COLUMN_NAME_NAME;
        public static final String PORTAL_GROUP_DESCRIPTION = DbContract.PortalGroup.COLUMN_NAME_DESCRIPTION;
        // RO from user path
        public static final String CREDENTIAL_ID = DbContract.Credential.TABLE_NAME + DOT + DbContract.Credential._ID;
        public static final String CREDIT = DbContract.Credential.COLUMN_NAME_CREDIT;
        public static final String CREDENTIAL_NAME = DbContract.Credential.COLUMN_NAME_NAME;
        public static final String CREDENTIAL_PASSWORD = DbContract.Credential.COLUMN_NAME_PASS;

        public static Uri getUri() {
            return Uri.parse("content://" + AUTHORITY + "/" + PORTAL_PATH);
        }

        public static Uri getUserUri(long userId) {
            return Uri.parse("content://" + AUTHORITY + "/" +
                    USER_PATH + "/" + userId + "/" + PORTAL_PATH);
        }
    }

    public static class Credentials implements BaseColumns {
        public static final String CREDENTIALS_GROUP_ID = DbContract.Credential.COLUMN_NAME_CGID;
        public static final String PORTAL_GROUP_ID = DbContract.Credential.COLUMN_NAME_PGID;
        public static final String USER_NAME = DbContract.Credential.COLUMN_NAME_NAME;
        public static final String USER_PASS = DbContract.Credential.COLUMN_NAME_PASS;
        public static final String CREDIT = DbContract.Credential.COLUMN_NAME_CREDIT;
        public static final String EXTRA = DbContract.Credential.COLUMN_NAME_EXTRA;
        public static final String FLAGS = DbContract.Credential.COLUMN_NAME_FLAGS;
        public static final String LOW_CREDIT_THRESHOLD = DbContract.Credential.COLUMN_NAME_CREDENTIAL_LOW_THRESHOLD;

        public static Uri getUserUri(long userId) {
            return Uri.parse("content://" + AUTHORITY + "/" +
                    USER_PATH + "/" + userId + "/" + CREDENTIALS_PATH);
        }
    }

    public static class CredentialsGroup implements BaseColumns {
        public static final String NAME = DbContract.CredentialsGroup.COLUMN_NAME_NAME;
        public static final String DESCRIPTION = DbContract.CredentialsGroup.COLUMN_NAME_DESCRIPTION;

        public static Uri getUri() {
            return Uri.parse("content://" + AUTHORITY + "/" + CREDENTIALS_GROUP_PATH);
        }
    }

    public static class PortalGroup implements BaseColumns {
        public static final String NAME = DbContract.PortalGroup.COLUMN_NAME_NAME;
        public static final String DESCRIPTION = DbContract.PortalGroup.COLUMN_NAME_DESCRIPTION;

        public static Uri getUri() {
            return Uri.parse("content://" + AUTHORITY + "/" + PORTAL_GROUP_PATH);
        }
    }

    public static class Day {
        // RO
        public static final String _ID = DbContract.MenuEntry.COLUMN_NAME_DATE + " AS " + BaseColumns._ID;
        public static final String DATE = DbContract.MenuEntry.COLUMN_NAME_DATE;

        public static Uri getUserUri(long userId) {
            return Uri.parse("content://" + AUTHORITY + "/" +
                    USER_PATH + "/" + userId + "/" + DAY_PATH);
        }
    }
}
