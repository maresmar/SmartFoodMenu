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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cz.maresmar.sfm.BuildConfig;
import cz.maresmar.sfm.db.DbContract;
import cz.maresmar.sfm.db.DbHelper;
import cz.maresmar.sfm.db.controller.ActionController;
import cz.maresmar.sfm.db.controller.DayController;
import cz.maresmar.sfm.db.controller.GroupMenuEntryController;
import cz.maresmar.sfm.db.controller.LogDataController;
import cz.maresmar.sfm.db.controller.PortalGroupController;
import cz.maresmar.sfm.db.controller.SimpleMenuEntryController;
import cz.maresmar.sfm.db.controller.PortalController;
import cz.maresmar.sfm.db.controller.SimpleController;
import cz.maresmar.sfm.db.controller.MenuEntryController;
import cz.maresmar.sfm.provider.repository.ViewType;
import cz.maresmar.sfm.provider.schema.AbstractUriSchema;
import cz.maresmar.sfm.provider.schema.LogDataUriSchema;
import cz.maresmar.sfm.provider.schema.Param;
import cz.maresmar.sfm.provider.schema.IdEndingUriSchema;
import cz.maresmar.sfm.provider.repository.ContentRepository;
import cz.maresmar.sfm.provider.repository.NotifyChangeListener;
import timber.log.Timber;

import static cz.maresmar.sfm.provider.ProviderContract.ACTION_PATH;
import static cz.maresmar.sfm.provider.ProviderContract.AUTHORITY;
import static cz.maresmar.sfm.provider.ProviderContract.CREDENTIALS_GROUP_PATH;
import static cz.maresmar.sfm.provider.ProviderContract.CREDENTIALS_PATH;
import static cz.maresmar.sfm.provider.ProviderContract.DAY_PATH;
import static cz.maresmar.sfm.provider.ProviderContract.GROUP_MENU_ENTRY_PATH;
import static cz.maresmar.sfm.provider.ProviderContract.MENU_ENTRY_PATH;
import static cz.maresmar.sfm.provider.ProviderContract.PORTAL_GROUP_PATH;
import static cz.maresmar.sfm.provider.ProviderContract.PORTAL_PATH;
import static cz.maresmar.sfm.provider.ProviderContract.USER_PATH;
import static cz.maresmar.sfm.provider.PublicProviderContract.DOT;
import static cz.maresmar.sfm.provider.PublicProviderContract.LOGIN_DATA_PATH;
import static cz.maresmar.sfm.provider.repository.ContentRepository.DISABLE_DELETE;
import static cz.maresmar.sfm.provider.repository.ContentRepository.DISABLE_INSERT;
import static cz.maresmar.sfm.provider.repository.ContentRepository.DISABLE_QUERY;
import static cz.maresmar.sfm.provider.repository.ContentRepository.DISABLE_UPDATE;

/**
 * App's {@link ContentProvider} that allows access to SQLite database
 * <p>
 * Defines views (joined tables) that can be easily accessed using {@link Uri} found in
 * {@link ProviderContract}. Each view has columns specified there. The class also handles data
 * changes observing using {@link NotifyChangeListener}s associated with each {@link Uri} schema.</p>
 * <p>
 * The class internally works using {@link ContentRepository} that stores connection between {@link Uri}
 * schema and concrete {@link cz.maresmar.sfm.db.controller.ViewController}</p>
 */
public class DataProvider extends ContentProvider {

    // -------------------------------------------------------------------------------------------
    // View types
    // -------------------------------------------------------------------------------------------

    private static final ViewType TYPE_USER = new ViewType(
            "vnd.cz.maresmar.sfm.user",
            new SimpleController(DbContract.User.TABLE_NAME)
    );
    private static final ViewType TYPE_PORTAL = new ViewType(
            "vnd.cz.maresmar.sfm.portal",
            new PortalController()
    );
    private static final ViewType TYPE_PORTAL_GROUP = new ViewType(
            "vnd.cz.maresmar.sfm.portal-group",
            new PortalGroupController()
    );
    private static final ViewType TYPE_CREDENTIAL = new ViewType(
            "vnd.cz.maresmar.sfm.credentials",
            new SimpleController(DbContract.Credential.TABLE_NAME)
    );
    private static final ViewType TYPE_CREDENTIAL_GROUP = new ViewType(
            "vnd.cz.maresmar.sfm.credentials-group",
            new SimpleController(DbContract.CredentialsGroup.TABLE_NAME)
    );
    private static final ViewType TYPE_PORTAL_MENU = new ViewType(
            "vnd.cz.maresmar.sfm.menu-simple",
            new SimpleMenuEntryController()
    );
    private static final ViewType TYPE_MENU = new ViewType(
            "vnd.cz.maresmar.sfm.menu",
            new MenuEntryController()
    );
    private static final ViewType TYPE_GROUP_MENU = new ViewType(
            "vnd.cz.maresmar.sfm.group-menu",
            new GroupMenuEntryController()
    );
    private static final ViewType TYPE_ACTION = new ViewType(
            "vnd.cz.maresmar.sfm.action",
            new ActionController()
    );
    private static final ViewType TYPE_LOG_DATA = new ViewType(
            "vnd.cz.maresmar.sfm.log-data",
            new LogDataController()
    );
    private static final ViewType TYPE_DAY = new ViewType(
            "vnd.cz.maresmar.sfm.day",
            new DayController()
    );

    // -------------------------------------------------------------------------------------------
    // Uri schemas
    // -------------------------------------------------------------------------------------------
    private static final AbstractUriSchema UH_USER = new IdEndingUriSchema(
            USER_PATH,
            DbContract.User.TABLE_NAME + DOT + BaseColumns._ID
    );
    private static final AbstractUriSchema UH_PORTAL = new IdEndingUriSchema(
            PORTAL_PATH,
            DbContract.Portal.TABLE_NAME + DOT + BaseColumns._ID
    );
    private static final AbstractUriSchema UH_PORTAL_GROUP = new IdEndingUriSchema(
            PORTAL_GROUP_PATH,
            DbContract.PortalGroup.TABLE_NAME + DOT + BaseColumns._ID
    );
    private static final AbstractUriSchema UH_CREDENTIALS_GROUP = new IdEndingUriSchema(
            CREDENTIALS_GROUP_PATH,
            DbContract.CredentialsGroup.TABLE_NAME + DOT + BaseColumns._ID
    );
    private static final AbstractUriSchema UH_PORTAL_MENU = new IdEndingUriSchema(
            PORTAL_PATH + "/#/" + MENU_ENTRY_PATH,
            DbContract.MenuEntry.TABLE_NAME + DOT + BaseColumns._ID
    ) {
        @NonNull
        @Override
        public Deque<Param> parseParamsFromUri(@NonNull Uri uri) {
            Deque<Param> params = super.parseParamsFromUri(uri);

            final long portalId = getPortalId(uri);
            params.add(new Param(DbContract.MenuEntry.COLUMN_NAME_PID, portalId));

            return params;
        }
    };
    private static final AbstractUriSchema UH_PORTAL_GROUP_MENU = new IdEndingUriSchema(
            PORTAL_PATH + "/#/" + GROUP_MENU_ENTRY_PATH,
            DbContract.GroupMenuEntry.TABLE_NAME + DOT + BaseColumns._ID
    ) {
        @NonNull
        @Override
        public Deque<Param> parseParamsFromUri(@NonNull Uri uri) {
            Deque<Param> params = super.parseParamsFromUri(uri);

            long portalId = getPortalId(uri);
            params.add(new Param(DbContract.GroupMenuEntry.COLUMN_NAME_ME_PID, portalId));

            return params;
        }
    };
    private static final AbstractUriSchema UH_ACTION = new IdEndingUriSchema(
            ACTION_PATH,
            DbContract.FoodAction.TABLE_NAME + DOT + BaseColumns._ID
    );
    private static final AbstractUriSchema UH_CREDENTIAL_ACTION = new IdEndingUriSchema(
            CREDENTIALS_PATH + "/#/" + ACTION_PATH,
            DbContract.FoodAction.TABLE_NAME + DOT + BaseColumns._ID
    ) {
        @NonNull
        @Override
        public Deque<Param> parseParamsFromUri(@NonNull Uri uri) {
            Deque<Param> params = super.parseParamsFromUri(uri);

            long credentialId = getCredentialId(uri);
            params.add(new Param(DbContract.FoodAction.TABLE_NAME + DOT + DbContract.FoodAction.COLUMN_NAME_CID, credentialId));

            return params;
        }
    };
    private static final AbstractUriSchema UH_LOG_DATA = new LogDataUriSchema(
            LOGIN_DATA_PATH, ProviderContract.LogData.PORTAL_ID, ProviderContract.LogData.CREDENTIAL_ID
    );
    private final AbstractUriSchema UH_USER_ACTION = new IdEndingUriSchema(
            USER_PATH + "/#/" + ACTION_PATH,
            DbContract.FoodAction.TABLE_NAME + DOT + BaseColumns._ID
    ) {
        @NonNull
        @Override
        public Deque<Param> parseParamsFromUri(@NonNull Uri uri) {
            Deque<Param> params = super.parseParamsFromUri(uri);

            long userId = getUserId(uri);
            params.add(new Param(DbContract.Credential.COLUMN_NAME_UID, userId));

            return params;
        }

        @NonNull
        @Override
        public Deque<Param> parseParamsFromUriAndValues(@NonNull Uri uri, @Nullable ContentValues values) {
            Deque<Param> params = super.parseParamsFromUri(uri);

            if (values == null || !values.containsKey(ProviderContract.Action.ME_PORTAL_ID) ||
                    !values.containsKey(ProviderContract.Action.ME_RELATIVE_ID)) {
                throw new IllegalArgumentException("Cannot parse extra args if portal ID or food relative ID is missing");
            } else {
                SQLiteDatabase db = mDbHelper.getReadableDatabase();

                // Prepare params
                long userId = getUserId(uri);
                long portalId = values.getAsLong(ProviderContract.Action.ME_PORTAL_ID);

                // Find corresponding credential ID
                long credentialId = ActionController.findCredentialId(db, userId, portalId);
                params.add(new Param(DbContract.FoodAction.TABLE_NAME + DOT + DbContract.FoodAction.COLUMN_NAME_CID, credentialId));
            }
            return params;
        }
    };
    private final AbstractUriSchema UH_USER_CREDENTIAL = new IdEndingUriSchema(
            USER_PATH + "/#/" + CREDENTIALS_PATH,
            DbContract.Credential.TABLE_NAME + DOT + BaseColumns._ID
    ) {
        @NonNull
        @Override
        public Deque<Param> parseParamsFromUri(@NonNull Uri uri) {
            Deque<Param> params = super.parseParamsFromUri(uri);

            long userId = getUserId(uri);
            params.add(new Param(DbContract.Credential.COLUMN_NAME_UID, userId));

            return params;
        }
    };
    private final AbstractUriSchema UH_USER_MENU = new IdEndingUriSchema(
            USER_PATH + "/#/" + MENU_ENTRY_PATH,
            DbContract.MenuEntry.TABLE_NAME + DOT + BaseColumns._ID
    ) {
        @NonNull
        @Override
        public Deque<Param> parseParamsFromUri(@NonNull Uri uri) {
            Deque<Param> params = super.parseParamsFromUri(uri);

            long userId = getUserId(uri);
            params.add(new Param(DbContract.Credential.COLUMN_NAME_UID, userId));

            return params;
        }
    };
    private final AbstractUriSchema UH_USER_PORTAL = new IdEndingUriSchema(
            USER_PATH + "/#/" + PORTAL_PATH,
            DbContract.Portal.TABLE_NAME + DOT + BaseColumns._ID
    ) {
        @NonNull
        @Override
        public Deque<Param> parseParamsFromUri(@NonNull Uri uri) {
            Deque<Param> params = super.parseParamsFromUri(uri);

            long userId = getUserId(uri);
            params.add(new Param(DbContract.Credential.COLUMN_NAME_UID, userId));

            return params;
        }
    };
    private final AbstractUriSchema UH_USER_DAY = new IdEndingUriSchema(
            USER_PATH + "/#/" + DAY_PATH,
            DbContract.MenuEntry.TABLE_NAME + DOT + DbContract.MenuEntry._ID
    ) {
        @NonNull
        @Override
        public Deque<Param> parseParamsFromUri(@NonNull Uri uri) {
            Deque<Param> params = super.parseParamsFromUri(uri);

            long userId = getUserId(uri);
            params.add(new Param(DbContract.Credential.COLUMN_NAME_UID, userId));

            return params;
        }
    };

    // -------------------------------------------------------------------------------------------
    // NotifyChange listeners
    // -------------------------------------------------------------------------------------------

    private static final NotifyChangeListener NCL_SELF = new NotifyChangeListener();

    private final NotifyChangeListener NCL_PORTAL_GROUP = new NotifyChangeListener() {
        @Override
        public void notifyChange(@NonNull Context context, @NonNull Uri uri) {
            // Notify self
            super.notifyChange(context, uri);

            // Notify /portal
            NCL_PORTAL.notifyChange(context, new Uri.Builder()
                    .authority(AUTHORITY)
                    .appendPath(PORTAL_PATH)
                    .build());
        }
    };

    // /portal
    private final NotifyChangeListener NCL_PORTAL = new NotifyChangeListener() {
        @Override
        public void notifyChange(@NonNull Context context, @NonNull Uri uri) {
            // Notify self
            super.notifyChange(context, uri);

            // Notify all /user/#/portal
            for (int i = 0; i < mActiveUsersCount; i++) {
                long userId = mActiveUsersList.get(i);
                NCL_SELF.notifyChange(context, new Uri.Builder()
                        .authority(AUTHORITY)
                        .appendPath(USER_PATH)
                        .appendPath("" + userId)
                        .appendPath(PORTAL_PATH)
                        .build());
            }
        }
    };

    // /user/#/credential
    private final NotifyChangeListener NCL_USER_CREDENTIAL = new NotifyChangeListener() {
        @Override
        public void notifyChange(@NonNull Context context, @NonNull Uri uri) {
            // Notify self
            super.notifyChange(context, uri);

            long userId = getUserId(uri);

            // Notify /user/#/portal
            NCL_SELF.notifyChange(context, new Uri.Builder()
                    .authority(AUTHORITY)
                    .appendPath(USER_PATH)
                    .appendPath("" + userId)
                    .appendPath(PORTAL_PATH)
                    .build());
        }
    };

    // /log-data
    private final NotifyChangeListener NCL_LOG_DATA = new NotifyChangeListener() {
        @Override
        public void notifyChange(@NonNull Context context, @NonNull Uri uri) {
            // Notify self
            super.notifyChange(context, uri);

            // Notify all /user/#/credential
            for (int i = 0; i < mActiveUsersCount; i++) {
                long userId = mActiveUsersList.get(i);
                NCL_USER_CREDENTIAL.notifyChange(context, new Uri.Builder()
                        .authority(AUTHORITY)
                        .appendPath(USER_PATH)
                        .appendPath("" + userId)
                        .appendPath(CREDENTIALS_PATH)
                        .build());
            }

            // Notify only /portal (without recursion)
            NCL_SELF.notifyChange(context, new Uri.Builder()
                    .authority(AUTHORITY)
                    .appendPath(PORTAL_PATH)
                    .build());
        }
    };

    // /portal/#/menu, /portal/#/group-menu and /credential/#/action
    private final NotifyChangeListener NCL_PLUGIN_MENU_ACTION = new NotifyChangeListener() {
        @Override
        public void notifyChange(@NonNull Context context, @NonNull Uri uri) {
            // Notify self
            super.notifyChange(context, uri);

            // Notify all /user/#/menu, /user/#/action and /user/#/day
            for (int i = 0; i < mActiveUsersCount; i++) {
                long userId = mActiveUsersList.get(i);

                // /user/#/menu
                NCL_SELF.notifyChange(context, new Uri.Builder()
                        .authority(AUTHORITY)
                        .appendPath(USER_PATH)
                        .appendPath("" + userId)
                        .appendPath(MENU_ENTRY_PATH)
                        .build());

                // /user/#/action
                NCL_SELF.notifyChange(context, new Uri.Builder()
                        .authority(AUTHORITY)
                        .appendPath(USER_PATH)
                        .appendPath("" + userId)
                        .appendPath(ACTION_PATH)
                        .build());

                // /user/#/day
                NCL_SELF.notifyChange(context, new Uri.Builder()
                        .authority(AUTHORITY)
                        .appendPath(USER_PATH)
                        .appendPath("" + userId)
                        .appendPath(DAY_PATH)
                        .build());
            }
        }
    };

    // /user/#/action
    private final NotifyChangeListener NCL_USER_ACTION = new NotifyChangeListener() {
        @Override
        public void notifyChange(@NonNull Context context, @NonNull Uri uri) {
            // Notify self
            super.notifyChange(context, uri);

            long userId = getUserId(uri);

            // /user/#/menu
            NCL_SELF.notifyChange(context, new Uri.Builder()
                    .authority(AUTHORITY)
                    .appendPath(USER_PATH)
                    .appendPath("" + userId)
                    .appendPath(MENU_ENTRY_PATH)
                    .build());
        }
    };

    // -------------------------------------------------------------------------------------------
    // Variables
    // -------------------------------------------------------------------------------------------

    private DbHelper mDbHelper;
    private ContentRepository mRepository;
    private Set<Long> mActiveUsersSet = new HashSet<>();
    private final List<Long> mActiveUsersList = new ArrayList<>();
    private int mActiveUsersCount = 0;

    // -------------------------------------------------------------------------------------------
    // Functions
    // -------------------------------------------------------------------------------------------

    @Override
    public boolean onCreate() {
        mDbHelper = new DbHelper(getContext());
        mRepository = new ContentRepository(getContext(), AUTHORITY);

        mRepository.registerSchema(UH_ACTION, TYPE_ACTION, NCL_PLUGIN_MENU_ACTION,
                DISABLE_QUERY | DISABLE_INSERT);
        mRepository.registerSchema(UH_CREDENTIAL_ACTION, TYPE_ACTION, NCL_PLUGIN_MENU_ACTION);
        mRepository.registerSchema(UH_USER_ACTION, TYPE_ACTION, NCL_USER_ACTION);
        mRepository.registerSchema(UH_USER_CREDENTIAL, TYPE_CREDENTIAL, NCL_USER_CREDENTIAL);
        mRepository.registerSchema(UH_CREDENTIALS_GROUP, TYPE_CREDENTIAL_GROUP, NCL_SELF);
        mRepository.registerSchema(UH_PORTAL_GROUP_MENU, TYPE_GROUP_MENU, NCL_PLUGIN_MENU_ACTION);
        mRepository.registerSchema(UH_PORTAL_MENU, TYPE_PORTAL_MENU, NCL_PLUGIN_MENU_ACTION);
        mRepository.registerSchema(UH_USER_MENU, TYPE_MENU, NCL_SELF,
                DISABLE_UPDATE | DISABLE_INSERT | DISABLE_DELETE);
        mRepository.registerSchema(UH_LOG_DATA, TYPE_LOG_DATA, NCL_LOG_DATA,
                DISABLE_INSERT | DISABLE_DELETE);
        mRepository.registerSchema(UH_USER_PORTAL, TYPE_PORTAL, NCL_SELF,
                DISABLE_UPDATE | DISABLE_INSERT | DISABLE_DELETE);
        mRepository.registerSchema(UH_PORTAL, TYPE_PORTAL, NCL_PORTAL);
        mRepository.registerSchema(UH_USER, TYPE_USER, NCL_SELF);
        mRepository.registerSchema(UH_PORTAL_GROUP, TYPE_PORTAL_GROUP, NCL_PORTAL_GROUP);
        mRepository.registerSchema(UH_USER_DAY, TYPE_DAY, NCL_SELF,
                DISABLE_UPDATE | DISABLE_INSERT | DISABLE_DELETE);

        return true;
    }

    private long getUserId(@NonNull Uri uri) {
        long userId = getSecondSegmentId(uri, USER_PATH);

        // Protection against ConcurrentModificationException
        if(mActiveUsersSet.add(userId)) {
            synchronized (mActiveUsersList) {
                mActiveUsersList.add(userId);
                mActiveUsersCount++;
            }
        }

        return userId;
    }

    private static long getPortalId(@NonNull Uri uri) {
        return getSecondSegmentId(uri, PORTAL_PATH);
    }

    private static long getCredentialId(@NonNull Uri uri) {
        return getSecondSegmentId(uri, CREDENTIALS_PATH);
    }

    private static long getSecondSegmentId(@NonNull Uri uri, @NonNull String firstSegName) {
        if (BuildConfig.DEBUG)
            if (!firstSegName.equals(uri.getPathSegments().get(0))) {
                Timber.w("Uri %s does not match \"/%s/#\"", uri.toString(), firstSegName);
                throw new IllegalArgumentException("Illegal Uri " + uri.toString() + " for " + firstSegName);
            }
        return Long.valueOf(uri.getPathSegments().get(1));
    }

    @Override
    public String getType(@NonNull Uri uri) {
        return mRepository.getMineType(uri);
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        try {
            return mRepository.doInsert(db, uri, values);
        } catch (IllegalArgumentException e) {
            Timber.e(e, "Cannot insert values to %s", uri.toString());
            throw e;
        }
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        try {
            return mRepository.doDelete(db, uri, selection, selectionArgs);
        } catch (IllegalArgumentException e) {
            Timber.e(e, "Cannot delete values from %s", uri.toString());
            throw e;
        }
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        try {
            return mRepository.doUpdate(db, uri, values, selection, selectionArgs);
        } catch (IllegalArgumentException e) {
            Timber.e(e, "Cannot update values from %s", uri.toString());
            throw e;
        }
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        return mRepository.doQuery(db, uri, projection, selection, selectionArgs, sortOrder);
    }
}
