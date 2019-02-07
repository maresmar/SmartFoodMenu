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

package cz.maresmar.sfm.provider.repository;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import androidx.annotation.CheckResult;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import cz.maresmar.sfm.db.controller.ViewController;
import cz.maresmar.sfm.provider.schema.AbstractUriSchema;
import cz.maresmar.sfm.provider.schema.Param;
import timber.log.Timber;

/**
 * This repository helps to find {@link ViewController} for concrete {@link Uri} and calls correct method
 * with respect to set permissions.
 * <p>
 * The repository connect Uri schemas from {@link AbstractUriSchema} with corresponding {@link ViewType}.
 * The class also allow disable some operations on {@link ViewController} using permissions.
 * </p>
 */
public class ContentRepository {

    public static final int DISABLE_NOTHING = 0;
    public static final int DISABLE_QUERY = 1;
    public static final int DISABLE_UPDATE = 1 << 1;
    public static final int DISABLE_INSERT = 1 << 2;
    public static final int DISABLE_DELETE = 1 << 3;

    @IntDef(value = {DISABLE_QUERY, DISABLE_UPDATE, DISABLE_INSERT, DISABLE_DELETE}, flag = true)
    @Retention(RetentionPolicy.SOURCE)
    public @interface ControllerPermission {
    }

    private Context mContext;
    private String mAuthority;
    private List<RepositoryEntry> mRepositoryEntries;
    private UriMatcher mUriMatcher;
    private int mLastUriIndex = 0;

    /**
     * Create new empty repository
     *
     * @param context   Some valid context
     * @param authority {@link android.content.ContentProvider} authority
     */
    public ContentRepository(Context context, String authority) {
        mContext = context;
        mAuthority = authority;
        mRepositoryEntries = new ArrayList<>();
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    }

    /**
     * Register {@link Uri} schema with corresponding {@link ViewType} without any permission restrictions
     *
     * @param schema        Scheme to be registered
     * @param viewType      Type and controller of schema
     * @param notifyChanger Listener about data changes
     */
    public void registerSchema(@NonNull AbstractUriSchema schema, @NonNull ViewType viewType,
                               @NonNull NotifyChangeListener notifyChanger) {
        registerSchema(schema, viewType, notifyChanger, DISABLE_NOTHING);
    }

    /**
     * Register {@link Uri} schema with corresponding {@link ViewType}
     *
     * @param schema        Scheme to be registered
     * @param viewType      Type and controller of schema
     * @param notifyChanger Listener about data changes
     * @param permission    Controller operation permissions
     */
    public void registerSchema(@NonNull AbstractUriSchema schema, @NonNull ViewType viewType,
                               @NonNull NotifyChangeListener notifyChanger, @ControllerPermission int permission) {
        String[] uris = schema.getUris();

        mRepositoryEntries.add(mLastUriIndex, new RepositoryEntry(schema, viewType, permission, notifyChanger));

        for (String uri : uris) {
            mUriMatcher.addURI(mAuthority, uri, mLastUriIndex);
        }
        mLastUriIndex++;
    }

    /**
     * Inserts new values to view associated with given {@link Uri}
     *
     * @param db     Database to work with
     * @param uri    Uri to insert into
     * @param values Values to be inserted
     * @return ID of new inserted row or throws {@link IllegalArgumentException} (if some constraint is
     * violated)
     */
    @NonNull
    public Uri doInsert(@NonNull SQLiteDatabase db, @NonNull Uri uri, ContentValues values) {
        RepositoryEntry entry = findEntry(uri);

        if ((entry.permissions & DISABLE_INSERT) == DISABLE_INSERT) {
            Timber.e("Using Uri %s for insert is forbidden", uri.toString());
            throw new IllegalArgumentException("Using this Uri for insert is forbidden");
        }

        Deque<Param> uriParams = entry.uriHandler.parseParamsFromUriAndValues(uri, values);

        values = buildContentValues(values, uriParams);

        long newEntryId = entry.viewType.getController().insert(db, values);
        Uri newEntryUri = entry.uriHandler.buildEntryUri(uri, newEntryId);

        entry.notifyChanger.notifyChange(mContext, newEntryUri);

        return newEntryUri;
    }

    /**
     * Updates values in view associated with given {@link Uri}
     *
     * @param db            Database to work with
     * @param uri           Uri to view that will be updated
     * @param values        Values to be updated
     * @param selection     Update selection
     * @param selectionArgs Update selection args
     * @return Number of changed rows
     */
    public int doUpdate(@NonNull SQLiteDatabase db, @NonNull Uri uri, ContentValues values, String selection,
                        String[] selectionArgs) {
        RepositoryEntry entry = findEntry(uri);

        if ((entry.permissions & DISABLE_UPDATE) == DISABLE_UPDATE) {
            Timber.e("Using Uri %s for update is forbidden", uri.toString());
            throw new IllegalArgumentException("Using this Uri for update is forbidden");
        }

        Deque<Param> uriParams = entry.uriHandler.parseParamsFromUri(uri);

        selection = buildSelection(selection, uriParams);

        int updateRows = entry.viewType.getController().update(db, values, selection, selectionArgs);

        if (updateRows > 0) {
            entry.notifyChanger.notifyChange(mContext, uri);
        }

        return updateRows;
    }

    /**
     * Delete from view associated with given {@link Uri}
     *
     * @param db            Database to work with
     * @param uri           Uri to view where the delete happen
     * @param selection     Delete selection
     * @param selectionArgs Delete selection args
     * @return Number of deleted rows
     */
    public int doDelete(@NonNull SQLiteDatabase db, @NonNull Uri uri, String selection, String[] selectionArgs) {
        RepositoryEntry entry = findEntry(uri);

        if ((entry.permissions & DISABLE_DELETE) == DISABLE_DELETE) {
            Timber.e("Using Uri %s for delete is forbidden", uri.toString());
            throw new IllegalArgumentException("Using this Uri for delete is forbidden");
        }

        Deque<Param> uriParams = entry.uriHandler.parseParamsFromUri(uri);

        selection = buildSelection(selection, uriParams);

        int deletedRows = entry.viewType.getController().delete(db, selection, selectionArgs);

        if (deletedRows > 0) {
            entry.notifyChanger.notifyChange(mContext, uri);
        }

        return deletedRows;
    }

    /**
     * Query view associated with given {@link Uri}
     *
     * @param db            Database to work with
     * @param uri           Uri to view that will be query
     * @param projection    Select projection
     * @param selection     Select selection
     * @param selectionArgs Select selection args
     * @param sortOrder     Select sort order
     * @return Cursor with data or throws {@link IllegalArgumentException}
     */
    @NonNull
    public Cursor doQuery(@NonNull SQLiteDatabase db, @NonNull Uri uri, String[] projection, String selection,
                          String[] selectionArgs, String sortOrder) {
        RepositoryEntry entry = findEntry(uri);

        if ((entry.permissions & DISABLE_QUERY) == DISABLE_QUERY) {
            Timber.e("Using Uri %s for query is forbidden", uri.toString());
            throw new IllegalArgumentException("Using this Uri for query is forbidden");
        }

        Deque<Param> uriParams = entry.uriHandler.parseParamsFromUri(uri);

        selection = buildSelection(selection, uriParams);

        ViewController viewController = entry.viewType.getController();
        Cursor cursor = viewController.query(db, projection, selection, selectionArgs, sortOrder);

        // make sure that potential listeners are getting notified
        cursor.setNotificationUri(mContext.getContentResolver(), uri);

        return cursor;
    }

    /**
     * Returns mine type of given {@link Uri}
     *
     * @return View mine type
     * @see android.content.ContentProvider#getType(Uri)
     */
    @NonNull
    public String getMineType(@NonNull Uri uri) {
        RepositoryEntry entry = findEntry(uri);

        final AbstractUriSchema handler = entry.uriHandler;
        final ViewType type = entry.viewType;

        switch (handler.getKind(uri)) {
            case AbstractUriSchema.MINE_KIND_ITEM:
                return "vnd.android.cursor.item/" + type.getMineType();
            case AbstractUriSchema.MINE_KIND_DIR:
                return "vnd.android.cursor.dir/" + type.getMineType();
            default:
                throw new IllegalStateException("Unsupported MINE kind " + handler.getKind(uri));

        }
    }

    @Nullable
    private RepositoryEntry tryFindEntry(@NonNull Uri uri) {
        int uriIndex = mUriMatcher.match(uri);

        if (uriIndex == UriMatcher.NO_MATCH)
            return null;
        else
            return mRepositoryEntries.get(uriIndex);
    }

    @NonNull
    private RepositoryEntry findEntry(@NonNull Uri uri) {
        RepositoryEntry entry = tryFindEntry(uri);
        if (entry == null) {
            Timber.e("Cannot handle Uri %s", uri.toString());
            throw new IllegalArgumentException("Cannot handle Uri " + uri.toString());
        } else {
            return entry;
        }
    }

    @CheckResult
    @Nullable
    private ContentValues buildContentValues(@Nullable ContentValues values, @Nullable Deque<Param> params) {
        if (params == null)
            return values;

        if (values == null)
            values = new ContentValues();
        for (Param param : params) {
            values.put(param.paramName, param.paramValue);
        }

        return values;
    }

    @CheckResult
    @Nullable
    private String buildSelection(@Nullable String selection, @Nullable Deque<Param> params) {
        if (params == null)
            return selection;

        StringBuilder sb = new StringBuilder();
        if (selection != null)
            sb.append(selection);

        int selectionLen = selection == null ? 0 : selection.length();
        boolean firstClause = selectionLen == 0;

        for (Param param : params) {
            if (!firstClause) {
                sb.append(" AND ");
            } else {
                firstClause = false;
            }

            sb.append('(');
            sb.append(param.paramName).append(" = ").append(param.paramValue);
            sb.append(')');
        }

        return sb.toString();
    }

    private class RepositoryEntry {

        final AbstractUriSchema uriHandler;
        final ViewType viewType;
        final @ControllerPermission
        int permissions;
        final NotifyChangeListener notifyChanger;

        private RepositoryEntry(AbstractUriSchema uriHandler, ViewType viewType,
                                @ControllerPermission int permissions, NotifyChangeListener notifyChanger) {
            this.uriHandler = uriHandler;
            this.viewType = viewType;
            this.permissions = permissions;
            this.notifyChanger = notifyChanger;
        }
    }
}

