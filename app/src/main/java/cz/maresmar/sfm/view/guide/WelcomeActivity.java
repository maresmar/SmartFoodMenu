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

package cz.maresmar.sfm.view.guide;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.UiThread;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Hashtable;

import cz.maresmar.sfm.R;
import cz.maresmar.sfm.app.SettingsContract;
import cz.maresmar.sfm.app.SfmApp;
import cz.maresmar.sfm.plugin.BroadcastContract;
import cz.maresmar.sfm.provider.ProviderContract;
import cz.maresmar.sfm.service.plugin.PortalTestHandler;
import cz.maresmar.sfm.service.plugin.sync.SyncHandler;
import cz.maresmar.sfm.utils.MenuUtils;
import cz.maresmar.sfm.view.DataForm;
import cz.maresmar.sfm.view.MainActivity;
import cz.maresmar.sfm.view.credential.CredentialDetailFragment;
import cz.maresmar.sfm.view.help.HelpFragment;
import cz.maresmar.sfm.view.portal.PortalDetailFragment;
import cz.maresmar.sfm.view.portal.PortalListFragment;
import cz.maresmar.sfm.view.user.UserDetailFragment;
import timber.log.Timber;

/**
 * Welcome guide activity
 * <p>
 * The activity shows when user opens app for the first time. It helps him add new user, select
 * portal and add credentials
 * </p>
 */
public class WelcomeActivity extends AppCompatActivity
        implements PortalListFragment.OnPortalSelectedListener, DataForm.DataValidityListener,
        PortalTestHandler.PortalTestResultListener {

    // Fragments IDs
    static private final int WELCOME_FRAGMENT_ID = 0;
    static private final int USER_FRAGMENT_ID = 1;
    static private final int PORTALS_LIST_FRAGMENT_ID = 2;
    static private final int PORTAL_FRAGMENT_ID = 3;
    static private final int CREDENTIALS_FRAGMENT_ID = 4;
    static private final int HELP_FRAGMENT_ID = 5;

    @IntDef(value = {
            WELCOME_FRAGMENT_ID,
            USER_FRAGMENT_ID,
            PORTALS_LIST_FRAGMENT_ID,
            PORTAL_FRAGMENT_ID,
            CREDENTIALS_FRAGMENT_ID,
            HELP_FRAGMENT_ID
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface FragmentId {
    }

    // Save instance state constants
    private static final String PAGE_ID = "page_id";
    private static final String MAX_FRAGMENT_ID = "max_fragment_id";
    private static final String ADVANCED_SETTINGS = "advanced_settings";
    private static final String USER_URI = "user_uri";
    private static final String VALIDATED_CREDENTIAL_ID = "validatedCredentialId";
    private static final String REFRESHING = "refreshing";

    private final BroadcastReceiver mSyncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long credentialId = intent.getExtras().getLong(BroadcastContract.EXTRA_CREDENTIAL_ID, -1);
            int worstResult = intent.getExtras().getInt(BroadcastContract.EXTRA_WORST_RESULT);
            onCredentialValidationResult(credentialId, worstResult);
        }
    };

    private final BroadcastReceiver mPortalTestReceiver = new PortalTestHandler.PortalTestResultReceiver(this);

    // UI elements
    private ViewPager mViewPager;
    private FloatingActionButton mFab;
    private MenuItem mAdvSettingsMenuItem;
    private Toast mActiveToast;

    // Internal state
    private boolean mAdvSettings = false;
    @FragmentId
    private int mMaxFragmentId = WELCOME_FRAGMENT_ID;
    private Uri mUserUri;
    private long mValidatedCredentialId = -1;
    private boolean mOnEndDiscardData = true;

    // Data sources
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private PagerAdapter mPagerAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    // -------------------------------------------------------------------------------------------
    // Activity lifecycle
    // -------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_app);
        setSupportActionBar(toolbar);

        mSwipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setColorSchemeColors(
                getResources().getIntArray(R.array.swipeRefreshColors)
        );
        mSwipeRefreshLayout.setEnabled(false);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mPagerAdapter = new PagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mPagerAdapter);
        // Watch for pages without FAB button
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                // Limit last page in into
                if (position > mMaxFragmentId) {
                    position = mMaxFragmentId;
                    mViewPager.setCurrentItem(position, true);
                    if (position != PORTALS_LIST_FRAGMENT_ID) {
                        tryToMoveToNextPage();
                    }
                }

                // Fab button
                mFab.setVisibility(position == PORTALS_LIST_FRAGMENT_ID ? View.GONE : View.VISIBLE);

                // Set page title
                toolbar.setTitle(mPagerAdapter.getPageTitle(position));
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        // Floating action button
        mFab = (FloatingActionButton) findViewById(R.id.main_discard_fab);
        mFab.setOnClickListener(view -> tryToMoveToNextPage());

        registerReceiver(mPortalTestReceiver, new IntentFilter(PortalTestHandler.BROADCAST_PORTAL_TEST_RESULT));
        registerReceiver(mSyncReceiver, new IntentFilter(BroadcastContract.BROADCAST_PLUGIN_SYNC_RESULT));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_welcome, menu);

        mAdvSettingsMenuItem = (MenuItem) menu.findItem(R.id.advanced_check_box);
        mAdvSettingsMenuItem.setChecked(mAdvSettings);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Preload next fragments after some time
        new Handler().postDelayed(() -> mViewPager.setOffscreenPageLimit(3), 1000);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mPortalTestReceiver);
        unregisterReceiver(mSyncReceiver);

        if (mOnEndDiscardData) {
            for (int i = 0; i < mPagerAdapter.getCount(); i++) {
                DataForm form = (DataForm) mPagerAdapter.getFragment(i);
                if (form == null)
                    break;

                form.discardTempData(this);
            }
        }
    }

    // -------------------------------------------------------------------------------------------
    // UI save and restore
    // -------------------------------------------------------------------------------------------

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(MAX_FRAGMENT_ID, mMaxFragmentId);
        outState.putInt(PAGE_ID, mViewPager.getCurrentItem());
        outState.putBoolean(ADVANCED_SETTINGS, mAdvSettings);
        outState.putParcelable(USER_URI, mUserUri);
        outState.putLong(VALIDATED_CREDENTIAL_ID, mValidatedCredentialId);
        outState.putBoolean(REFRESHING, mSwipeRefreshLayout.isRefreshing());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //noinspection WrongConstant
        mMaxFragmentId = savedInstanceState.getInt(MAX_FRAGMENT_ID, WELCOME_FRAGMENT_ID);
        mViewPager.setCurrentItem(savedInstanceState.getInt(PAGE_ID, WELCOME_FRAGMENT_ID), false);
        mAdvSettings = savedInstanceState.getBoolean(ADVANCED_SETTINGS, false);
        mUserUri = savedInstanceState.getParcelable(USER_URI);
        mValidatedCredentialId = savedInstanceState.getLong(VALIDATED_CREDENTIAL_ID, -1);
        mSwipeRefreshLayout.setRefreshing(savedInstanceState.getBoolean(REFRESHING));
    }

    // -------------------------------------------------------------------------------------------
    // UI events
    // -------------------------------------------------------------------------------------------

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_about:
                SfmApp.startAboutActivity(this);
                return true;
            case R.id.action_feedback:
                SfmApp app = (SfmApp)getApplication();
                app.sendFeedback(this);
                return true;
            case R.id.advanced_check_box:
                // Android doesn't handle checked <-> unchecked behavior automatically
                // in popup menu
                mAdvSettings = !mAdvSettings;
                item.setChecked(mAdvSettings);
                // Make it survive inflate from fragments
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Done key on software keyboard and back key behaviour
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
                // Done pressed
                tryToMoveToNextPage();
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (mViewPager.getCurrentItem() != 0) {
                    //Return to previous fragment
                    mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1, true);
                    return true;
                }
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    @UiThread
    private void tryToMoveToNextPage() {
        Timber.i("FAB clicked on page %d", mViewPager.getCurrentItem());

        if(mViewPager.getCurrentItem() == HELP_FRAGMENT_ID) {
            startMainActivity();
            return;
        }

        DataForm formFragment = (DataForm) mPagerAdapter.getFragment(mViewPager.getCurrentItem());
        if (formFragment == null) {
            // Skipping move forward when doing onRestoreInstanceState
            return;
        }
        if (formFragment.hasValidData()) {
            switch (mViewPager.getCurrentItem()) {
                case WELCOME_FRAGMENT_ID:
                    moveToNextPage();
                    break;
                case USER_FRAGMENT_ID: {
                    mUserUri = formFragment.saveData();
                    moveToNextPage();
                    break;
                }
                case PORTAL_FRAGMENT_ID: {
                    PortalDetailFragment portalDetailFragment = (PortalDetailFragment) formFragment;
                    ((PortalDetailFragment) formFragment).saveAndTestData();
                    mActiveToast = Toast.makeText(this, R.string.portal_checking_portal_data, Toast.LENGTH_LONG);
                    mActiveToast.show();
                    mSwipeRefreshLayout.setRefreshing(true);
                    break;
                }
                case CREDENTIALS_FRAGMENT_ID: {
                    CredentialDetailFragment credentialDetailFragment = (CredentialDetailFragment) formFragment;
                    Uri credentialUri = credentialDetailFragment.saveData();
                    mValidatedCredentialId = ContentUris.parseId(credentialUri);
                    // Start plugin
                    SyncHandler.startFullSync(this);
                    mActiveToast = Toast.makeText(getBaseContext(), R.string.credential_detail_checking_credential_data, Toast.LENGTH_LONG);
                    mActiveToast.show();
                    mSwipeRefreshLayout.setRefreshing(true);
                    break;
                }
                default:
                    throw new UnsupportedOperationException("Unsupported page " + mViewPager.getCurrentItem());

            }
        }
    }

    private void moveToNextPage() {
        if (mMaxFragmentId == mViewPager.getCurrentItem())
            //noinspection WrongConstant
            mMaxFragmentId = mViewPager.getCurrentItem() + 1;
        mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1, true);
    }

    @UiThread
    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        // Plan auto sync in the background
        SyncHandler.planFullSync(this, Integer.parseInt(SettingsContract.SYNC_FREQUENCY_DEFAULT),
                SettingsContract.SYNC_UNMETERED_ONLY_DEFAULT, SettingsContract.SYNC_CHARGING_ONLY_DEFAULT);
    }

    // -------------------------------------------------------------------------------------------
    // Specific fragment events
    // -------------------------------------------------------------------------------------------

    @Override
    public void onPortalSelected(@Nullable Uri portalUri) {
        // Create new data
        if (portalUri == null) {
            mAdvSettings = true;
            mAdvSettingsMenuItem.setCheckable(true);
        }

        // Updates UI with selected portal
        PortalDetailFragment portalDetailFragment = (PortalDetailFragment) mPagerAdapter.getFragment(PORTAL_FRAGMENT_ID);
        portalDetailFragment.reset(portalUri);

        // Show correct fragment
        if (mAdvSettings) {
            mMaxFragmentId = PORTAL_FRAGMENT_ID;
            mViewPager.setCurrentItem(PORTAL_FRAGMENT_ID, true);
        } else {
            // Update credentials data according to selection
            CredentialDetailFragment credentialsFragment = (CredentialDetailFragment) mPagerAdapter.
                    getFragment(CREDENTIALS_FRAGMENT_ID);
            credentialsFragment.reset(mUserUri, portalUri);
            // Show Credentials fragment
            mMaxFragmentId = CREDENTIALS_FRAGMENT_ID;
            mViewPager.setCurrentItem(CREDENTIALS_FRAGMENT_ID, true);
        }
    }

    @Override
    public void onDataValidityChanged(@NonNull Fragment source, boolean validState) {
        if (validState) {
            if (mMaxFragmentId == mViewPager.getCurrentItem())
                //noinspection WrongConstant
                mMaxFragmentId = mViewPager.getCurrentItem() + 1;
        } else {
            if (source instanceof WelcomeFragment) {
                mMaxFragmentId = WELCOME_FRAGMENT_ID;
            } else if (source instanceof UserDetailFragment) {
                mMaxFragmentId = USER_FRAGMENT_ID;
            } else if (source instanceof PortalDetailFragment) {
                mMaxFragmentId = PORTAL_FRAGMENT_ID;
            }
        }
    }

    @Override
    public void onPortalTestResult(long portalId, @BroadcastContract.TestResult int result) {
        Timber.i("Received portal %d test result %d", portalId, result);
        // Hide message about sync
        mActiveToast.cancel();
        mSwipeRefreshLayout.setRefreshing(false);

        switch (result) {
            case BroadcastContract.TEST_RESULT_OK: {
                Uri portalUri = ContentUris.withAppendedId(ProviderContract.Portal.getUri(), portalId);

                // Update credential fragment with new data
                CredentialDetailFragment credentialsFragment = (CredentialDetailFragment) mPagerAdapter.
                        getFragment(CREDENTIALS_FRAGMENT_ID);
                credentialsFragment.reset(mUserUri, portalUri);
                moveToNextPage();
                break;
            }
            case BroadcastContract.TEST_RESULT_INVALID_DATA:
                Snackbar.make(findViewById(android.R.id.content),
                        R.string.extra_invalid_input_error, Snackbar.LENGTH_LONG)
                        .setAction(android.R.string.ok, view -> {
                            // Only dismiss message
                        })
                        .show();
                break;
            default:
                throw new UnsupportedOperationException("Unexpected test result" + result);
        }
    }

    private void onCredentialValidationResult(long credentialId, @BroadcastContract.SyncResult int worstResult) {
        Timber.i("Received credential %d (expected %d) test result %d", credentialId, mValidatedCredentialId, worstResult);
        if (credentialId != mValidatedCredentialId) {
            return;
        }
        if (mActiveToast != null) {
            mActiveToast.cancel();
        }
        mSwipeRefreshLayout.setRefreshing(false);

        if( worstResult == BroadcastContract.RESULT_OK) {
            mOnEndDiscardData = false;
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putBoolean(SettingsContract.FIRST_RUN, false)
                    .apply();
            moveToNextPage();
        } else {
            @StringRes
            int errMsg = MenuUtils.getSyncErrorMessage(worstResult);

            Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                    errMsg, Snackbar.LENGTH_LONG);

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

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of fragment in welcome guide
     */
    public class PagerAdapter extends FragmentPagerAdapter {

        private Fragment mRenewFragment = null;
        private Hashtable<Integer, String> mFragmentsTags = new Hashtable<>();
        @FragmentId
        private int mLastFragmentId = WELCOME_FRAGMENT_ID;

        public PagerAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            switch (position) {
                case WELCOME_FRAGMENT_ID:
                    return WelcomeFragment.newInstance();
                case USER_FRAGMENT_ID:
                    return UserDetailFragment.newEmptyInstance();
                case PORTALS_LIST_FRAGMENT_ID:
                    return PortalListFragment.newInstance();
                case PORTAL_FRAGMENT_ID:
                    return PortalDetailFragment.newInstance();
                case CREDENTIALS_FRAGMENT_ID:
                    return CredentialDetailFragment.newEmptyInstance(null, null);
                case HELP_FRAGMENT_ID:
                    return HelpFragment.newInstance();
                default:
                    throw new UnsupportedOperationException("Unsupported viewPager page: " + position);
            }
        }

        /**
         * Saves fragment tag for later, it's used for communication from activity to fragment. See
         * <a href="https://stackoverflow.com/a/29269509/1392034">this answer on StackOverflow</a>
         * for more.
         *
         * @return new created fragment or saved instance
         */
        @NonNull
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            mFragmentsTags.put(position, fragment.getTag());
            return fragment;
        }

        /**
         * Gets lasted created Fragment on specific position
         *
         * @param position Position of Fragment in PagerAdapter
         * @return found Fragment or null if Fragment is not created or not exists
         */
        @Nullable
        Fragment getFragment(@FragmentId int position) {
            if (mFragmentsTags.containsKey(position)) {
                return getSupportFragmentManager().findFragmentByTag(mFragmentsTags.get(position));
            }
            return null;
        }

        @Override
        public int getCount() {
            return 6;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case WELCOME_FRAGMENT_ID:
                    return getString(R.string.welcome_activity_title);
                case USER_FRAGMENT_ID:
                    return getString(R.string.new_user_screen_title);
                case PORTALS_LIST_FRAGMENT_ID:
                    return getString(R.string.portal_list_screen_title);
                case PORTAL_FRAGMENT_ID:
                    return getString(R.string.portal_screen_title);
                case CREDENTIALS_FRAGMENT_ID:
                    return getString(R.string.credential_screen_title);
                case HELP_FRAGMENT_ID:
                    return getString(R.string.help_screen_title);
                default:
                    throw new UnsupportedOperationException("Unsupported viewPager page: " + position);
            }
        }
    }
}
