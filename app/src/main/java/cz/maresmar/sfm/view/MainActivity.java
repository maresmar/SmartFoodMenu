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

package cz.maresmar.sfm.view;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.ExpandableDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import java.util.List;
import java.util.concurrent.TimeUnit;

import cz.maresmar.sfm.Assert;
import cz.maresmar.sfm.BuildConfig;
import cz.maresmar.sfm.R;
import cz.maresmar.sfm.app.SettingsContract;
import cz.maresmar.sfm.app.SfmApp;
import cz.maresmar.sfm.plugin.BroadcastContract;
import cz.maresmar.sfm.provider.ProviderContract;
import cz.maresmar.sfm.service.plugin.sync.SyncHandler;
import cz.maresmar.sfm.utils.ActionUtils;
import cz.maresmar.sfm.utils.MenuUtils;
import cz.maresmar.sfm.view.credential.LoginListActivity;
import cz.maresmar.sfm.view.guide.WelcomeActivity;
import cz.maresmar.sfm.view.help.HelpActivity;
import cz.maresmar.sfm.view.menu.CursorPagerFragment;
import cz.maresmar.sfm.view.menu.MenuDetailsFragment;
import cz.maresmar.sfm.view.menu.day.DayMenuFragment;
import cz.maresmar.sfm.view.menu.day.DayMenuPagerFragment;
import cz.maresmar.sfm.view.menu.portal.PortalMenuPagerFragment;
import cz.maresmar.sfm.view.order.OrderFragment;
import cz.maresmar.sfm.view.user.UserListActivity;
import timber.log.Timber;

import static java.util.concurrent.TimeUnit.DAYS;

/**
 * Main app activity
 */
public class MainActivity extends AppCompatActivity
        implements AccountHeader.OnAccountHeaderListener, LoaderManager.LoaderCallbacks<Cursor>,
        Drawer.OnDrawerItemClickListener, Drawer.OnDrawerNavigationListener, CursorPagerFragment.PagerPageChangedListener,
        FragmentChangeRequestListener, SyncHandler.SyncResultListener {

    private static final String SHOW_ORDERS_ACTION = "showOrders";
    private static final String SHOW_CREDIT_ACTION = "showCredit";

    private static final int USER_LOADER_ID = 1;
    private static final int PORTAL_LOADER_ID = 2;
    private static final int CREDENTIAL_LOADER_ID = 3;
    private static final int EDIT_ACTIONS_COUNT_LOADER_ID = 4;

    private static final String ARG_USER_ID = "userId";
    private static final String ARG_FRAGMENT_ID = "fragmentId";
    private static final String ARG_TITLE = "title";
    private static final String ARG_REFRESHING = "refreshing";

    // User part of drawer
    private static final int ADD_USER_DRAWER_ITEM_ID = 400_000;
    private static final ProfileSettingDrawerItem ADD_USER_DRAWER_ITEM = new ProfileSettingDrawerItem()
            .withName(R.string.drawer_add_user)
            .withDescription(R.string.drawer_add_user_description)
            .withIcon(R.drawable.ic_add_user)
            .withIconTinted(true)
            .withIdentifier(ADD_USER_DRAWER_ITEM_ID);
    private static final int MANAGE_USERS_DRAWER_ID = 400_001;
    private static final ProfileSettingDrawerItem MANAGE_USER_DRAWER_ITEM = new ProfileSettingDrawerItem()
            .withName(R.string.drawer_manage_users)
            .withIcon(R.drawable.ic_settings)
            .withIconTinted(true)
            .withIdentifier(MANAGE_USERS_DRAWER_ID);
    // Main part of drawer
    private static final int TODAY_DRAWER_ID = 400_002;
    private static final int DAY_DRAWER_ID = 400_003;
    private static final int ORDERS_DRAWER_ID = 400_004;
    private static final int FEEDBACK_DRAWER_ID = 400_005;
    private static final int SETTINGS_DRAWER_ID = 400_006;
    private static final int HELP_DRAWER_ID = 400_007;
    private static final int ABOUT_DRAWER_ID = 400_008;
    // Portal part of drawer
    private static final int PORTAL_EXPANDABLE_DRAWER_ID = 400_009;
    private static final int PORTAL_ITEM_DRAWER_TAG = 400_010;
    private static final int CONTROL_DRAWER_TAG = 400_011;
    private static final int ADD_PORTAL_DRAWER_ID = 400_012;
    private static final SecondaryDrawerItem ADD_PORTAL_DRAWER_ITEM = new SecondaryDrawerItem()
            .withName(R.string.drawer_add_portal)
            .withIcon(R.drawable.ic_add)
            .withIconTintingEnabled(true)
            .withIdentifier(ADD_PORTAL_DRAWER_ID)
            .withTag(CONTROL_DRAWER_TAG)
            .withSelectable(false);
    private static final int MANAGE_PORTAL_DRAWER_ID = 400_013;
    private static final SecondaryDrawerItem MANAGE_PORTAL_DRAWER_ITEM = new SecondaryDrawerItem()
            .withName(R.string.drawer_manage_portals)
            .withIcon(R.drawable.ic_settings)
            .withIconTintingEnabled(true)
            .withIdentifier(MANAGE_PORTAL_DRAWER_ID)
            .withTag(CONTROL_DRAWER_TAG)
            .withSelectable(false);

    // Fragments id's
    public static final int TODAY_FRAGMENT_ID = -1;
    public static final int DAY_PAGER_FRAGMENT_ID = -2;
    public static final int ORDER_FRAGMENT_ID = -3;
    // PORTAL_FRAGMENT_ID is _ID of portal to be shown (always positive)

    AppBarLayout mAppBarLayout;
    SwipeRefreshLayout mSwipeRefreshLayout;
    Drawer mDrawer;
    AccountHeader mProfiles;
    ExpandableDrawerItem mPortalDrawerItem;
    FloatingActionButton mOkFab, mDiscardFab;
    SharedPreferences mPrefs;

    long mSelectedUserId = SettingsContract.LAST_USER_UNKNOWN;
    long mSelectedFragmentId = SettingsContract.LAST_FRAGMENT_UNKNOWN;
    boolean mIsRefreshing = false;

    private final BroadcastReceiver mSyncResultReceiver = new SyncHandler.SyncResultReceiver(this);

    private final BroadcastReceiver mActionsEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra(ActionUtils.ARG_EVENT);
            if (action == null) {
                action = "null";
            }

            switch (action) {
                case ActionUtils.EVENT_SYNC_FAILED:
                    onActionsSyncFailed();
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown action " + action);
            }
        }
    };

    // -------------------------------------------------------------------------------------------
    // External handlers
    // -------------------------------------------------------------------------------------------

    /**
     * Returns {@link PendingIntent} that can start this activity with last used fragment
     *
     * @param context Some valid context
     * @return PendingIntent that starts activity
     */
    public static PendingIntent getDefaultIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(context, 0, intent, 0);
    }

    /**
     * Returns {@link PendingIntent} that can start this activity with {@link OrderFragment}
     *
     * @param context Some valid context
     * @return PendingIntent that starts activity
     */
    public static PendingIntent getShowOrdersIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setAction(SHOW_ORDERS_ACTION);
        return PendingIntent.getActivity(context, 0, intent, 0);
    }

    /**
     * Returns {@link PendingIntent} that can start this activity with open {@link Drawer}
     *
     * @param context Some valid context
     * @return PendingIntent that starts activity
     */
    public static PendingIntent getShowCreditIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setAction(SHOW_CREDIT_ACTION);
        return PendingIntent.getActivity(context, 0, intent, 0);
    }

    // -------------------------------------------------------------------------------------------
    // Main lifecycle
    // -------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Welcome guide
        if (mPrefs.getBoolean(SettingsContract.FIRST_RUN, SettingsContract.FIRST_RUN_DEFAULT)) {
            Intent firstRunIntent = new Intent(this, WelcomeActivity.class);
            firstRunIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(firstRunIntent);
            finish();
        }
        // Main UI
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Floating button
        mOkFab = findViewById(R.id.main_ok_fab);
        mOkFab.setOnClickListener(view -> {
            AsyncTask.execute(() ->
                    ActionUtils.saveEdits(this, getUserUri()));
            setMenuEditUiShown(false);
            Snackbar.make(view, R.string.main_edit_saved_text, Snackbar.LENGTH_LONG)
                    .setAction("Ok", v -> {
                    }).show();
        });
        mDiscardFab = findViewById(R.id.main_discard_fab);
        mDiscardFab.setOnClickListener(view -> {
            AsyncTask.execute(() ->
                    ActionUtils.discardEdits(this, getUserUri()));
            setMenuEditUiShown(false);
        });

        // Material Drawer
        // Prepare users section
        mProfiles = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.header)
                .addProfiles(
                        ADD_USER_DRAWER_ITEM,
                        MANAGE_USER_DRAWER_ITEM)
                .withOnAccountHeaderListener(this)
                .build();

        // Prepare menu section
        mDrawer = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withAccountHeader(mProfiles)
                .addDrawerItems(
                        new PrimaryDrawerItem()
                                .withName(R.string.drawer_today)
                                .withIcon(R.drawable.ic_today)
                                .withIconTintingEnabled(true)
                                .withIdentifier(TODAY_DRAWER_ID),
                        new PrimaryDrawerItem()
                                .withName(R.string.drawer_day)
                                .withIcon(R.drawable.ic_days)
                                .withIconTintingEnabled(true)
                                .withIdentifier(DAY_DRAWER_ID),
                        mPortalDrawerItem = new ExpandableDrawerItem()
                                .withName(R.string.drawer_portal)
                                .withIcon(R.drawable.ic_location)
                                .withIconTintingEnabled(true)
                                .withIdentifier(PORTAL_EXPANDABLE_DRAWER_ID)
                                .withSelectable(false)
                                .withSubItems(
                                        ADD_PORTAL_DRAWER_ITEM,
                                        MANAGE_PORTAL_DRAWER_ITEM),
                        new PrimaryDrawerItem()
                                .withName(R.string.drawer_orders)
                                .withIcon(R.drawable.ic_shopping_cart)
                                .withIconTintingEnabled(true)
                                .withIdentifier(ORDERS_DRAWER_ID),
                        new SectionDrawerItem()
                                .withName(R.string.drawer_help_and_settings_group),
                        new SecondaryDrawerItem()
                                .withName(R.string.drawer_feedback)
                                .withIcon(R.drawable.ic_send)
                                .withIconTintingEnabled(true)
                                .withIdentifier(FEEDBACK_DRAWER_ID)
                                .withSelectable(false),
                        new SecondaryDrawerItem()
                                .withName(R.string.drawer_settings)
                                .withIcon(R.drawable.ic_settings)
                                .withIconTintingEnabled(true)
                                .withIdentifier(SETTINGS_DRAWER_ID)
                                .withSelectable(false),
                        new SecondaryDrawerItem()
                                .withName(R.string.drawer_help)
                                .withIcon(R.drawable.ic_help)
                                .withIconTintingEnabled(true)
                                .withIdentifier(HELP_DRAWER_ID)
                                .withSelectable(false),
                        new SecondaryDrawerItem()
                                .withName(R.string.drawer_about)
                                .withIcon(R.drawable.ic_info)
                                .withIconTintingEnabled(true)
                                .withIdentifier(ABOUT_DRAWER_ID)
                                .withSelectable(false)
                )
                .withOnDrawerItemClickListener(this)
                .withOnDrawerNavigationListener(this)
                .withActionBarDrawerToggleAnimated(true)
                .withSavedInstance(savedInstanceState)
                .build();

        // Selected user
        if (mSelectedUserId == SettingsContract.LAST_USER_UNKNOWN) {
            mSelectedUserId = mPrefs.getLong(SettingsContract.LAST_USER,
                    SettingsContract.LAST_USER_UNKNOWN);
        }

        // Selected fragment
        if (mSelectedFragmentId == SettingsContract.LAST_FRAGMENT_UNKNOWN) {
            mSelectedFragmentId = mPrefs.getLong(SettingsContract.LAST_FRAGMENT,
                    SettingsContract.LAST_FRAGMENT_UNKNOWN);
        }

        String action;
        if (getIntent().getAction() != null) {
            action = getIntent().getAction();
        } else {
            action = Intent.ACTION_MAIN;
        }

        switch (action) {
            case Intent.ACTION_MAIN:
                break;
            case SHOW_ORDERS_ACTION:
                mSelectedFragmentId = ORDER_FRAGMENT_ID;
                break;
            case SHOW_CREDIT_ACTION:
                mDrawer.openDrawer();
                break;
            default:
                throw new UnsupportedOperationException("Unknown action " + action);

        }
        mAppBarLayout = findViewById(R.id.appbar);

        mSwipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setColorSchemeColors(
                getResources().getIntArray(R.array.swipeRefreshColors)
        );
        mSwipeRefreshLayout.setOnRefreshListener(this::startRefresh);

        // Load users from db
        getSupportLoaderManager().initLoader(USER_LOADER_ID, null, this);

        LocalBroadcastManager.getInstance(this).registerReceiver(mSyncResultReceiver,
                new IntentFilter(SyncHandler.BROADCAST_SYNC_EVENT));
        LocalBroadcastManager.getInstance(this).registerReceiver(mActionsEventReceiver,
                new IntentFilter(ActionUtils.BROADCAST_ACTION_EVENT));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        mDrawer.saveInstanceState(outState);

        outState.putLong(ARG_USER_ID, mSelectedUserId);
        outState.putLong(ARG_FRAGMENT_ID, mSelectedFragmentId);
        outState.putCharSequence(ARG_TITLE, getTitle());
        outState.putBoolean(ARG_REFRESHING, mIsRefreshing);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mSelectedUserId = savedInstanceState.getLong(ARG_USER_ID, SettingsContract.LAST_USER_UNKNOWN);
        mSelectedFragmentId = savedInstanceState.getLong(ARG_FRAGMENT_ID, SettingsContract.LAST_FRAGMENT_UNKNOWN);
        setTitle(savedInstanceState.getCharSequence(ARG_TITLE));

        mIsRefreshing = savedInstanceState.getBoolean(ARG_REFRESHING);
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Save last user and selected fragment
        mPrefs.edit()
                .putLong(SettingsContract.LAST_USER, mSelectedUserId)
                .putLong(SettingsContract.LAST_FRAGMENT, mSelectedFragmentId)
                .apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.unregisterReceiver(mSyncResultReceiver);
        manager.unregisterReceiver(mActionsEventReceiver);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Update if user opens app without lasted data
        long lastUpdate = mPrefs.getLong(SettingsContract.LAST_DONE_SYNC, SettingsContract.LAST_DONE_SYNC_DEFAULT);
        long syncFreq = DAYS.toMillis(Integer.parseInt(mPrefs.getString(SettingsContract.SYNC_FREQUENCY,
                SettingsContract.SYNC_FREQUENCY_DEFAULT)));

        if (mPrefs.getBoolean(SettingsContract.SYNC_WHEN_APP_OPENS, SettingsContract.SYNC_WHEN_APP_OPENS_DEFAULT) &&
                ((lastUpdate + TimeUnit.HOURS.toMillis(12)) < System.currentTimeMillis())) {
            SyncHandler.startFullSync(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_refresh:
                startRefresh();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onNavigationClickListener(View clickedView) {
        //this method is only called if the Arrow icon is shown. The hamburger is automatically managed by the MaterialDrawer
        //if the back arrow is shown. close the activity
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        if (mDrawer.isDrawerOpen()) {
            mDrawer.closeDrawer();
        } else {
            showHamburgerIcon(true);
            showToolbar();
            super.onBackPressed();
        }
    }

    // -------------------------------------------------------------------------------------------
    // UI helping methods
    // -------------------------------------------------------------------------------------------

    private void setMenuEditUiShown(boolean visible) {
        if (visible) {
            mOkFab.show();
            mDiscardFab.show();
        } else {
            mOkFab.hide();
            mDiscardFab.hide();
        }
    }

    private void showHamburgerIcon(boolean show) {
        if (show) {
            // Disable the back arrow in the toolbar and show the hamburger icon
            if (getSupportActionBar() != null)
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            mDrawer.getActionBarDrawerToggle().setDrawerIndicatorEnabled(true);
        } else {
            // Set the back arrow in the toolbar
            mDrawer.getActionBarDrawerToggle().setDrawerIndicatorEnabled(false);
            if (getSupportActionBar() != null)
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void showToolbar() {
        mAppBarLayout.setExpanded(true, true);
    }

    // -------------------------------------------------------------------------------------------
    // General helping methods
    // -------------------------------------------------------------------------------------------

    private Uri getUserUri() {
        return ContentUris.withAppendedId(ProviderContract.User.getUri(), mSelectedUserId);
    }

    private void startRefresh() {
        if (!mIsRefreshing) {
            mIsRefreshing = true;
            mSwipeRefreshLayout.setRefreshing(true);

            if (mSelectedFragmentId == TODAY_FRAGMENT_ID) {
                SyncHandler.startRemainingSync(this, mSelectedUserId);
            } else {
                SyncHandler.startFullSync(this);
            }
        }
    }

    @Override
    public void onSyncStarted() {
        mIsRefreshing = true;
        mSwipeRefreshLayout.setRefreshing(true);
    }

    @Override
    public void onSyncFinished(int worstResult) {
        mIsRefreshing = false;
        mSwipeRefreshLayout.setRefreshing(false);

        if (worstResult != BroadcastContract.RESULT_OK) {
            @StringRes int errMsg = MenuUtils.getSyncErrorMessage(worstResult);

            Snackbar snackbar = Snackbar.make(mSwipeRefreshLayout, errMsg, Snackbar.LENGTH_LONG);

            if (errMsg == BroadcastContract.RESULT_UNKNOWN_PORTAL_FORMAT) {
                // Give user option to send logs
                snackbar.setAction(R.string.action_feedback_send, view -> {
                    SfmApp app = (SfmApp)getApplication();
                    app.sendFeedback(this);
                });
            } else {
                // Only dismiss
                snackbar.setAction(android.R.string.ok, view -> {
                    // Only dismiss message
                });
            }

            snackbar.show();
        }
    }

    private void onActionsSyncFailed() {
        Snackbar.make(mSwipeRefreshLayout, R.string.actions_sync_failed, Snackbar.LENGTH_INDEFINITE)
                .setAction(android.R.string.ok, view -> {
                })
                .show();
    }

    // -------------------------------------------------------------------------------------------
    // Loader callbacks
    // -------------------------------------------------------------------------------------------

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        switch (id) {
            case USER_LOADER_ID:
                return new CursorLoader(
                        this,
                        ProviderContract.User.getUri(),
                        new String[]{
                                ProviderContract.User._ID,
                                ProviderContract.User.NAME,
                                ProviderContract.User.PICTURE},
                        null,
                        null,
                        null
                );
            case PORTAL_LOADER_ID:
                return new CursorLoader(
                        this,
                        ProviderContract.Portal.getUserUri(mSelectedUserId),
                        new String[]{
                                ProviderContract.Portal._ID,
                                ProviderContract.Portal.NAME,
                                ProviderContract.Portal.CREDIT,
                        },
                        null,
                        null,
                        null
                );
            case CREDENTIAL_LOADER_ID:
                return new CursorLoader(
                        this,
                        ProviderContract.Credentials.getUserUri(mSelectedUserId),
                        new String[]{
                                ProviderContract.Credentials._ID,
                                ProviderContract.Credentials.CREDIT
                        },
                        null,
                        null,
                        null
                );
            case EDIT_ACTIONS_COUNT_LOADER_ID:
                Uri actionUri = Uri.withAppendedPath(getUserUri(), ProviderContract.ACTION_PATH);
                return new CursorLoader(
                        this,
                        actionUri,
                        new String[]{
                                "COUNT(" + ProviderContract.Action._ID + ")"
                        },
                        ProviderContract.Action.SYNC_STATUS + " = " + ProviderContract.ACTION_SYNC_STATUS_EDIT,
                        null,
                        null
                );
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + id);
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case USER_LOADER_ID: {
                Timber.d("User data loaded");

                // Removes old profiles
                mProfiles.clear();

                // Insert new profiles
                if (cursor.moveToFirst()) {
                    do {
                        Long userId = cursor.getLong(0);
                        String userName = cursor.getString(1);
                        Uri iconUri = Uri.parse(cursor.getString(2));

                        ProfileDrawerItem profile = new ProfileDrawerItem()
                                .withIdentifier(userId)
                                .withName(userName)
                                .withIcon(iconUri);
                        mProfiles.addProfiles(profile);
                    } while (cursor.moveToNext());
                }

                // Select default user
                if (mSelectedUserId == SettingsContract.LAST_USER_UNKNOWN &&
                        mProfiles.getActiveProfile() != null) {
                    mSelectedUserId = mProfiles.getActiveProfile().getIdentifier();
                }

                // Set last user in UI
                if (mSelectedUserId != SettingsContract.LAST_USER_UNKNOWN) {
                    mProfiles.setActiveProfile(mSelectedUserId);
                }

                // Insert control entries
                mProfiles.addProfiles(ADD_USER_DRAWER_ITEM, MANAGE_USER_DRAWER_ITEM);

                // Load correct user info for selected user (could be from backup)
                Loader portalLoader = getSupportLoaderManager().getLoader(PORTAL_LOADER_ID);
                if (portalLoader == null || !portalLoader.isStarted()) {
                    getSupportLoaderManager().initLoader(PORTAL_LOADER_ID, null, this);
                    getSupportLoaderManager().initLoader(EDIT_ACTIONS_COUNT_LOADER_ID, null, this);
                }
                // Should be always reused because of users recreation
                getSupportLoaderManager().initLoader(CREDENTIAL_LOADER_ID, null, this);
                break;
            }
            case PORTAL_LOADER_ID: {
                Timber.d("Portal data loaded");

                // Clear old portals
                int oldPortalCount = mPortalDrawerItem.getSubItems().size();
                mPortalDrawerItem.getSubItems().clear();

                // Insert new portals
                boolean selectedFound = false;
                if (cursor.moveToFirst()) {
                    do {
                        Long portalId = cursor.getLong(0);
                        String portalName = cursor.getString(1);
                        int credit = cursor.getInt(2);

                        SecondaryDrawerItem portalItem = new SecondaryDrawerItem()
                                .withIdentifier(portalId)
                                .withName(portalName)
                                .withDescription(MenuUtils.getPriceStr(this, credit))
                                .withTag(PORTAL_ITEM_DRAWER_TAG);

                        // Updates selected fragment if new user hasn't the old one
                        if(portalId == mSelectedFragmentId) {
                            portalItem.withSetSelected(true);
                            setTitle(portalName);
                            selectedFound = true;
                        } else if(cursor.isLast() && !selectedFound && mSelectedFragmentId >= 0) {
                            mSelectedFragmentId = portalId;
                            portalItem.withSetSelected(true);
                            setTitle(portalName);
                            showToolbar();
                        }

                        mPortalDrawerItem.withSubItems(portalItem);
                    } while (cursor.moveToNext());
                }

                // Insert control entries
                mPortalDrawerItem.withSubItems(ADD_PORTAL_DRAWER_ITEM, MANAGE_PORTAL_DRAWER_ITEM);

                // Notify about changes
                if (mPortalDrawerItem.isExpanded()) {
                    mDrawer.getExpandableExtension().notifyAdapterSubItemsChanged(
                            mDrawer.getPosition(mPortalDrawerItem), oldPortalCount);
                }

                // Set default title as title on empty cursor
                if(cursor.getCount() == 0) {
                    setTitle(R.string.drawer_portal);
                    showToolbar();
                }

                // Show saved fragment in UI
                showOrResetFragment(mSelectedFragmentId);

                break;
            }
            case CREDENTIAL_LOADER_ID: {
                Timber.d("Credential data loaded");

                if (mProfiles.getActiveProfile() == null)
                    return;

                // Prepare result
                StringBuilder builder = new StringBuilder();
                if (cursor.moveToFirst()) {
                    do {

                        if (builder.length() > 0) {
                            builder.append("; ");
                        }

                        int rawCredit = cursor.getInt(1);
                        builder.append(MenuUtils.getPriceStr(this, rawCredit));
                    } while (cursor.moveToNext());
                }

                // Show result
                ((ProfileDrawerItem) mProfiles.getActiveProfile())
                        .withEmail(builder.toString())
                        .withNameShown(true);
                mProfiles.updateProfile(mProfiles.getActiveProfile());
                break;
            }
            case EDIT_ACTIONS_COUNT_LOADER_ID:
                Timber.i("Edit actions count loaded");
                if (cursor.moveToFirst()) {
                    if (cursor.getInt(0) > 0) {
                        setMenuEditUiShown(true);
                    } else {
                        setMenuEditUiShown(false);
                    }
                }

                if (BuildConfig.DEBUG) {
                    Assert.isOne(cursor.getCount());
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + loader.getId());
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        switch (loader.getId()) {
            case USER_LOADER_ID:
                Timber.e("User data is no longer valid");

                // Removes old profiles
                mProfiles.clear();
                mSelectedUserId = SettingsContract.LAST_USER_UNKNOWN;

                // Insert helping entries
                mProfiles.addProfiles(ADD_USER_DRAWER_ITEM, MANAGE_USER_DRAWER_ITEM);

                break;
            case PORTAL_LOADER_ID: {
                Timber.e("Portal data with user %d is no longer valid",
                        mSelectedUserId);

                // Clear old portals
                int oldPortalCount = mPortalDrawerItem.getSubItems().size();
                mPortalDrawerItem.getSubItems().clear();

                // Insert control entries
                mPortalDrawerItem.withSubItems(ADD_PORTAL_DRAWER_ITEM, MANAGE_PORTAL_DRAWER_ITEM);

                // Notify about changes
                if (mPortalDrawerItem.isExpanded()) {
                    mDrawer.getExpandableExtension().notifyAdapterSubItemsChanged(
                            mDrawer.getPosition(mPortalDrawerItem), oldPortalCount);
                }
                break;
            }
            case CREDENTIAL_LOADER_ID:
                Timber.e("Credential data with user %d is no longer valid",
                        mSelectedUserId);
                mProfiles.getActiveProfile().withEmail(null);
                mProfiles.updateProfile(mProfiles.getActiveProfile());
                break;
            case EDIT_ACTIONS_COUNT_LOADER_ID:
                Timber.e("Actions data with user %d is no longer valid",
                        mSelectedUserId);
                setMenuEditUiShown(false);
                break;
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + loader.getId());
        }
    }

    // -------------------------------------------------------------------------------------------
    // Drawer events
    // -------------------------------------------------------------------------------------------

    @Override
    public boolean onProfileChanged(View view, IProfile profile, boolean current) {
        switch (view.getId()) {
            case ADD_USER_DRAWER_ITEM_ID: {
                Intent intent = new Intent(this, UserListActivity.class);
                intent.setAction(Intent.ACTION_EDIT);
                startActivity(intent);
                return true;
            }
            case MANAGE_USERS_DRAWER_ID: {
                Intent intent = new Intent(this, UserListActivity.class);
                startActivity(intent);
                return true;
            }
            default: {
                mSelectedUserId = profile.getIdentifier();
                mProfiles.setActiveProfile(profile);
                // Update other UIs data
                getSupportLoaderManager().restartLoader(PORTAL_LOADER_ID, null, this);
                getSupportLoaderManager().restartLoader(CREDENTIAL_LOADER_ID, null, this);
                getSupportLoaderManager().restartLoader(EDIT_ACTIONS_COUNT_LOADER_ID, null, this);
                return true;
            }
        }
    }

    @Override
    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
        switch (view.getId()) {
            case TODAY_DRAWER_ID:
                showOrResetToday();
                mDrawer.closeDrawer();
                return true;
            case DAY_DRAWER_ID:
                showOrResetDayPager();
                mDrawer.closeDrawer();
                return true;
            case ADD_PORTAL_DRAWER_ID:
                startLoginListActivity(Intent.ACTION_INSERT);
                return true;
            case MANAGE_PORTAL_DRAWER_ID:
                startLoginListActivity(Intent.ACTION_VIEW);
                return true;
            case ORDERS_DRAWER_ID:
                showOrResetOrders();
                mDrawer.closeDrawer();
                return true;
            case FEEDBACK_DRAWER_ID:
                startSendFeedback();
                mDrawer.closeDrawer();
                return true;
            case SETTINGS_DRAWER_ID:
                startSettingsActivity();
                mDrawer.closeDrawer();
                return true;
            case HELP_DRAWER_ID:
                startHelpActivity();
                mDrawer.closeDrawer();
                return true;
            case ABOUT_DRAWER_ID:
                SfmApp.startAboutActivity(this);
                mDrawer.closeDrawer();
                return true;
            default: {
                if (drawerItem.getTag() != null && (int) drawerItem.getTag() == PORTAL_ITEM_DRAWER_TAG) {
                    showOrResetPortalPager(drawerItem.getIdentifier());
                    mDrawer.closeDrawer();
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    // -------------------------------------------------------------------------------------------
    // View manipulating methods
    // -------------------------------------------------------------------------------------------

    private void showOrResetToday() {
        Timber.i("Showing Today fragment");

        mSelectedFragmentId = TODAY_FRAGMENT_ID;
        Uri userUri = getUserUri();

        Fragment actualFragment = getSupportFragmentManager().findFragmentById(R.id.main_content);
        if ((actualFragment instanceof DayMenuFragment)) {
            ((DayMenuFragment) actualFragment).reset(userUri);
        } else {
            DayMenuFragment newFragment = DayMenuFragment.newTodayInstance(userUri);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_content, newFragment)
                    .commit();
        }

        setTitle(R.string.main_today_fragment_title);
        showToolbar();
    }

    private void showOrResetDayPager() {
        Timber.i("Showing DayPager fragment");

        mSelectedFragmentId = DAY_PAGER_FRAGMENT_ID;
        Uri userUri = getUserUri();

        Fragment actualFragment = getSupportFragmentManager().findFragmentById(R.id.main_content);
        if (actualFragment instanceof DayMenuPagerFragment) {
            ((DayMenuPagerFragment) actualFragment).reset(userUri);
        } else {
            DayMenuPagerFragment newFragment = DayMenuPagerFragment.newInstance(userUri);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_content, newFragment)
                    .commit();
        }

        showToolbar();
    }

    private void showOrResetPortalPager(long portalId) {
        Timber.i("Showing PortalPager fragment");

        // Show toolbar portal is changed
        if(portalId != mSelectedFragmentId) {
            showToolbar();
        }

        // Switch fragment category tu portals
        if(mSelectedFragmentId < 0) {
            mSelectedFragmentId = Integer.MAX_VALUE;
            // The mSelectedFragmentId ID will be returned from listener
        }

        Uri userUri = getUserUri();

        Fragment actualFragment = getSupportFragmentManager().findFragmentById(R.id.main_content);
        if (actualFragment instanceof PortalMenuPagerFragment) {
            ((PortalMenuPagerFragment) actualFragment).reset(userUri, portalId);
        } else {
            PortalMenuPagerFragment newFragment = PortalMenuPagerFragment.newInstance(userUri, portalId);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_content, newFragment)
                    .commit();
        }
    }

    private void startLoginListActivity(String action) {
        Timber.i("Starting LoginListActivity with action %s", action);

        Intent intent = new Intent(getApplication(), LoginListActivity.class);
        intent.setAction(action);
        intent.setData(ContentUris.withAppendedId(ProviderContract.User.getUri(), mSelectedUserId));
        startActivity(intent);
    }

    private void showOrResetOrders() {
        Timber.i("Showing Orders fragment");

        mSelectedFragmentId = ORDER_FRAGMENT_ID;
        Uri userUri = getUserUri();

        Fragment actualFragment = getSupportFragmentManager().findFragmentById(R.id.main_content);
        if (actualFragment instanceof OrderFragment) {
            ((OrderFragment) actualFragment).reset(userUri);
        } else {
            OrderFragment newFragment = OrderFragment.newInstance(userUri);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_content, newFragment)
                    .commit();
        }

        setTitle(R.string.main_orders_fragment_title);
        showToolbar();
    }

    @Override
    public void showMenuDetail(long menuRelativeId, long portalId) {
        Uri userUri = getUserUri();
        MenuDetailsFragment newFragment = MenuDetailsFragment.newInstance(userUri, menuRelativeId, portalId);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_content, newFragment)
                .addToBackStack(null)
                .commit();

        // Uncheck drawer item
        mDrawer.setSelection(-1);
        showHamburgerIcon(false);
        showToolbar();
    }

    private void startSendFeedback() {
        Timber.i("Starting send feedback");

        SfmApp app = (SfmApp) getApplication();
        app.sendFeedback(this);
    }

    private void startHelpActivity() {
        Timber.i("Starting help activity");

        Intent intent = new Intent(getApplication(), HelpActivity.class);
        startActivity(intent);
    }

    private void startSettingsActivity() {
        Timber.i("Starting settings activity");

        Intent intent = new Intent(getApplication(), SettingsActivity.class);
        intent.setData(getUserUri());
        startActivity(intent);
    }

    // -------------------------------------------------------------------------------------------
    // Fragment's restore calls
    // -------------------------------------------------------------------------------------------

    private void showOrResetFragment(long fragmentId) {
        if (BuildConfig.DEBUG) {
            //Portal ID newer should overflow int (but SQLite primary key is long)
            Assert.that(mSelectedFragmentId <= Integer.MAX_VALUE, "FragmentId %d overflow int", mSelectedFragmentId);
            Assert.that(mSelectedFragmentId >= Integer.MIN_VALUE, "FragmentId %d overflow int", mSelectedFragmentId);
        }
        switch ((int) fragmentId) {
            case TODAY_FRAGMENT_ID:
                mDrawer.setSelection(TODAY_DRAWER_ID, false);
                showOrResetToday();
                break;
            case DAY_PAGER_FRAGMENT_ID:
                mDrawer.setSelection(DAY_DRAWER_ID, false);
                showOrResetDayPager();
                break;
            case ORDER_FRAGMENT_ID:
                mDrawer.setSelection(ORDERS_DRAWER_ID, false);
                showOrResetOrders();
                break;
            default:
                mDrawer.setSelection(fragmentId, false);
                showOrResetPortalPager(fragmentId);
                break;
        }

        showHamburgerIcon(true);
    }

    // -------------------------------------------------------------------------------------------
    // Pager change listener
    // -------------------------------------------------------------------------------------------

    @Override
    public void onPageChanged(long pageId) {
        if (BuildConfig.DEBUG) {
            //Portal ID newer should overflow int (but SQLite primary key is long)
            Assert.that(mSelectedFragmentId <= Integer.MAX_VALUE, "FragmentId %d overflow int", mSelectedFragmentId);
            Assert.that(mSelectedFragmentId >= Integer.MIN_VALUE, "FragmentId %d overflow int", mSelectedFragmentId);
        }
        switch ((int) mSelectedFragmentId) {
            case DAY_PAGER_FRAGMENT_ID:
                onDayChanged(pageId);
                break;
            case TODAY_FRAGMENT_ID:
            case ORDER_FRAGMENT_ID:
                // Result received withing fragment switching
                Timber.w("onPageChanged event received when there is fragment without paging");
                break;
            default:
                onPortalChanged(pageId);

        }
    }

    private void onDayChanged(long date) {
        Timber.i("DayPager changed to date %d", date);

        if (date > 0)
            setTitle(MenuUtils.getDateStr(this, date));
        else {
            setTitle(R.string.drawer_day);
            showToolbar();
        }
    }

    private void onPortalChanged(long portalId) {
        Timber.i("PortalPager changed to portal %d", portalId);

        if(mSelectedFragmentId != portalId) {
            mSelectedFragmentId = portalId;
            List<IDrawerItem> subItems = mPortalDrawerItem.getSubItems();

            for (IDrawerItem subItem : subItems) {
                if ((subItem.getIdentifier() == portalId) &&
                        ((int)subItem.getTag() == PORTAL_ITEM_DRAWER_TAG)) {
                    SecondaryDrawerItem portalItem = (SecondaryDrawerItem) subItem;

                    // Show portal name
                    setTitle(portalItem.getName().getText());
                    // Show portal selected in UI
                    portalItem.withSetSelected(true);
                } else {
                    subItem.withSetSelected(false);
                }
            }

            if (mPortalDrawerItem.isExpanded()) {
                mDrawer.getExpandableExtension().notifyAdapterSubItemsChanged(
                        mDrawer.getPosition(mPortalDrawerItem), subItems.size());
            }
        }
    }

    @Override
    public void enableSwipeRefresh(boolean enabled) {
        mSwipeRefreshLayout.setEnabled(enabled);
        if (enabled) {
            mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
        }
    }
}