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

package cz.maresmar.sfm.view.credential;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import cz.maresmar.sfm.R;
import cz.maresmar.sfm.plugin.BroadcastContract;
import cz.maresmar.sfm.service.plugin.PortalTestHandler;
import cz.maresmar.sfm.service.plugin.sync.SyncHandler;
import cz.maresmar.sfm.utils.MenuUtils;
import cz.maresmar.sfm.view.DataForm;
import cz.maresmar.sfm.view.portal.PortalDetailFragment;
import timber.log.Timber;

/**
 * Activity where user could add or edit portal and credential
 */
public class LoginDetailActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener,
        PortalTestHandler.PortalTestResultListener {

    public static final String PORTAL_URI = "portalUri";
    public static final String CREDENTIAL_URI = "credentialUri";
    public static final String TAB_ID = "tabId";

    private static final String VALIDATED_PORTAL_ID = "validatedPortalId";
    private static final String VALIDATED_CREDENTIAL_ID = "validatedCredentialId";
    private static final String REFRESHING = "refreshing";

    public static final int PORTAL_TAB = 0;
    public static final int CREDENTIAL_TAB = 1;

    private DataForm mPortalFormDestroyer;
    private DataForm mCredentialFormDestroyer;

    private final BroadcastReceiver mSyncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long credentialId = intent.getExtras().getLong(BroadcastContract.EXTRA_CREDENTIAL_ID, -1);
            int worstResult = intent.getExtras().getInt(BroadcastContract.EXTRA_WORST_RESULT);
            onCredentialValidationResult(credentialId, worstResult);
        }
    };

    private final BroadcastReceiver mPortalTestReceiver = new PortalTestHandler.PortalTestResultReceiver(this);

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private Uri mPortalUri;
    private Uri mCredentialUri;
    private Uri mUserUri;

    long mValidatedPortalId;
    long mValidatedCredentialId = -1;

    private Toast mActiveToast;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_detail);

        mUserUri = getIntent().getData();
        mPortalUri = getIntent().getParcelableExtra(PORTAL_URI);
        mCredentialUri = getIntent().getParcelableExtra(CREDENTIAL_URI);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));

        // Set default value
        mViewPager.setCurrentItem(getIntent().getIntExtra(TAB_ID, CREDENTIAL_TAB));

        // Enable validating of screens
        mViewPager.addOnPageChangeListener(this);

        mSwipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setColorSchemeColors(
                getResources().getIntArray(R.array.swipeRefreshColors)
        );
        mSwipeRefreshLayout.setEnabled(false);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.main_discard_fab);
        fab.setOnClickListener(view -> confirmData());

        registerReceiver(mPortalTestReceiver, new IntentFilter(PortalTestHandler.BROADCAST_PORTAL_TEST_RESULT));
        registerReceiver(mSyncReceiver, new IntentFilter(BroadcastContract.BROADCAST_PLUGIN_SYNC_RESULT));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mPortalTestReceiver);
        unregisterReceiver(mSyncReceiver);

        // Discard saved data from db if not finished
        if (mPortalFormDestroyer != null)
            mPortalFormDestroyer.discardTempData(this);

        if (mCredentialFormDestroyer != null)
            mCredentialFormDestroyer.discardTempData(this);
    }

    // -------------------------------------------------------------------------------------------
    // Instance state
    // -------------------------------------------------------------------------------------------

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(PORTAL_URI, mPortalUri);
        outState.putParcelable(CREDENTIAL_URI, mCredentialUri);
        outState.putLong(VALIDATED_PORTAL_ID, mValidatedPortalId);
        outState.putLong(VALIDATED_CREDENTIAL_ID, mValidatedCredentialId);
        outState.putBoolean(REFRESHING, mSwipeRefreshLayout.isRefreshing());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mPortalUri = savedInstanceState.getParcelable(PORTAL_URI);
        mCredentialUri = savedInstanceState.getParcelable(CREDENTIAL_URI);
        mValidatedPortalId = savedInstanceState.getLong(VALIDATED_PORTAL_ID);
        mValidatedCredentialId = savedInstanceState.getLong(VALIDATED_CREDENTIAL_ID);
        mSwipeRefreshLayout.setRefreshing(savedInstanceState.getBoolean(REFRESHING));
    }

    // -------------------------------------------------------------------------------------------
    // UI events
    // -------------------------------------------------------------------------------------------

    private void confirmData() {
        saveAndValidatePortal();
    }

    private void saveAndValidatePortal() {
        PortalDetailFragment portalDetailFragment = (PortalDetailFragment) mSectionsPagerAdapter
                .instantiateItem(mViewPager, PORTAL_TAB);
        mPortalFormDestroyer = portalDetailFragment;

        // Check portal details
        Uri savePortalUri;
        if (portalDetailFragment.hasValidData()) {
            Uri portalUri = portalDetailFragment.saveAndTestData();
            mValidatedPortalId = ContentUris.parseId(portalUri);
            mActiveToast = Toast.makeText(this, R.string.login_detail_checking_data, Toast.LENGTH_LONG);
            mActiveToast.show();
            mSwipeRefreshLayout.setRefreshing(true);
        } else {
            mViewPager.setCurrentItem(PORTAL_TAB);
        }
    }

    @Override
    public void onPortalTestResult(long portalId, @BroadcastContract.TestResult int result) {
        Timber.i("Received portal %d test result %d", portalId, result);
        switch (result) {
            case BroadcastContract.TEST_RESULT_OK: {
                saveAndValidateCredential();
                break;
            }
            case BroadcastContract.TEST_RESULT_INVALID_DATA:
                if (mActiveToast != null) {
                    mActiveToast.cancel();
                }
                mSwipeRefreshLayout.setRefreshing(false);

                mViewPager.setCurrentItem(PORTAL_TAB);
                Snackbar.make(mSwipeRefreshLayout,
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

    private void saveAndValidateCredential() {
        CredentialDetailFragment credentialDetailFragment = (CredentialDetailFragment) mSectionsPagerAdapter
                .instantiateItem(mViewPager, CREDENTIAL_TAB);
        mCredentialFormDestroyer = credentialDetailFragment;
        PortalDetailFragment portalDetailFragment = (PortalDetailFragment) mSectionsPagerAdapter
                .instantiateItem(mViewPager, PORTAL_TAB);
        mPortalFormDestroyer = portalDetailFragment;

        // If I don't create new portal
        if (mPortalUri != null) {
            // Check credentials details
            if (credentialDetailFragment.hasValidData()) {
                Uri credentialUri = credentialDetailFragment.saveData();
                mValidatedCredentialId = ContentUris.parseId(credentialUri);
                // Start plugin
                SyncHandler.startFullSync(this);
            } else {
                mViewPager.setCurrentItem(CREDENTIAL_TAB);
                mSwipeRefreshLayout.setRefreshing(false);
            }
        } else {
            // Show credential tab to fill it with data
            mPortalUri = portalDetailFragment.getDataUri();
            credentialDetailFragment.reset(mUserUri, mPortalUri);
            mViewPager.setCurrentItem(CREDENTIAL_TAB);
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    private void onCredentialValidationResult(long credentialId, @BroadcastContract.SyncResult int worstResult) {
        Timber.i("Received credential %d (expected %d) validation result %d", credentialId, mValidatedCredentialId, worstResult);
        if (credentialId != mValidatedCredentialId) {
            return;
        }

        if (mActiveToast != null) {
            mActiveToast.cancel();
        }
        mSwipeRefreshLayout.setRefreshing(false);

        if(worstResult == BroadcastContract.RESULT_OK) {
            // Return to parent activity
            mPortalFormDestroyer = null;
            mCredentialFormDestroyer = null;
            finish();
        } else {
            mViewPager.setCurrentItem(CREDENTIAL_TAB);

            @StringRes
            int errMsg = MenuUtils.getSyncErrorMessage(worstResult);
            mActiveToast = Toast.makeText(this, errMsg, Toast.LENGTH_LONG);
            mActiveToast.show();

            /* Disappears in 1 s (probably bug in support library)
            Snackbar.make(mSwipeRefreshLayout, errMsg, Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_ok, view -> {
                        // Only dismiss message
                    })
                    .show();
                    */
        }
    }

    // -------------------------------------------------------------------------------------------
    // Page change listener
    // -------------------------------------------------------------------------------------------

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public void onPageSelected(int position) {
        PortalDetailFragment portalDetailFragment = (PortalDetailFragment) mSectionsPagerAdapter
                .instantiateItem(mViewPager, PORTAL_TAB);
        CredentialDetailFragment credentialDetailFragment = (CredentialDetailFragment) mSectionsPagerAdapter
                .instantiateItem(mViewPager, CREDENTIAL_TAB);

        switch (position) {
            case CREDENTIAL_TAB: {
                if (portalDetailFragment.hasValidData()) {
                    if (mPortalUri == null) {
                        new AsyncTask<Void, Void, Uri>() {
                            @Override
                            protected Uri doInBackground(Void... voids) {
                                return portalDetailFragment.saveData();
                            }

                            @Override
                            protected void onPostExecute(Uri uri) {
                                // Sets portal in credential screen
                                credentialDetailFragment.reset(mUserUri, mPortalUri);
                            }
                        }.execute();
                    } else {
                        AsyncTask.execute(
                                portalDetailFragment::saveData
                        );
                    }
                } else {
                    mViewPager.setCurrentItem(PORTAL_TAB);
                }
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    // -------------------------------------------------------------------------------------------
    // Pager adapter
    // -------------------------------------------------------------------------------------------

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            switch (position) {
                case PORTAL_TAB:
                    if (mPortalUri == null)
                        return PortalDetailFragment.newInstance();
                    else
                        return PortalDetailFragment.newInstance(mPortalUri);
                case CREDENTIAL_TAB:
                    if (mCredentialUri == null) {
                        return CredentialDetailFragment.newEmptyInstance(mUserUri, mPortalUri);
                    } else {
                        return CredentialDetailFragment.newInstance(mCredentialUri);
                    }
                default:
                    throw new UnsupportedOperationException("Unknown position " + position);
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
