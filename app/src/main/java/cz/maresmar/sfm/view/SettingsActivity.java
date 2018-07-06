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


import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import cz.maresmar.sfm.R;
import cz.maresmar.sfm.app.SettingsContract;
import cz.maresmar.sfm.service.web.PortalsUpdateService;
import cz.maresmar.sfm.service.plugin.sync.SyncHandler;
import cz.maresmar.sfm.view.credential.LoginListActivity;
import timber.log.Timber;

/**
 * A {@link AppCompatActivity} that presents a set of application settings
 */
public class SettingsActivity extends AppCompatActivity {

    Uri mUserUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUserUri = getIntent().getData();

        // Display the fragment as the main content.
        FragmentManager mFragmentManager = getFragmentManager();
        FragmentTransaction mFragmentTransaction = mFragmentManager
                .beginTransaction();
        PrefsFragment mPrefsFragment = PrefsFragment.newInstance(getIntent().getData());
        mFragmentTransaction.replace(android.R.id.content, mPrefsFragment);
        mFragmentTransaction.commit();
    }

    /**
     * Preference fragment that contains app preference
     */
    public static class PrefsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

        private static final String ARG_USER_URI = "userUri";

        Uri mUserUri;

        /**
         * Create new fragment for specific user
         * @param userUri User Uri prefix
         * @return New fragment with preference
         */
        public static PrefsFragment newInstance(@NonNull Uri userUri) {
            PrefsFragment fragment = new PrefsFragment();
            Bundle args = new Bundle();
            args.putParcelable(ARG_USER_URI, userUri);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (getArguments() != null) {
                mUserUri = getArguments().getParcelable(ARG_USER_URI);
            }

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            enableValueAsSummary(SettingsContract.SYNC_FREQUENCY);
            findPreference(SettingsContract.SYNC_UNMETERED_ONLY).setOnPreferenceChangeListener(this);
            findPreference(SettingsContract.SYNC_CHARGING_ONLY).setOnPreferenceChangeListener(this);

            findPreference("notification").setOnPreferenceClickListener(pref -> {
                Intent intent = new Intent(getActivity(), LoginListActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.setData(mUserUri);
                startActivity(intent);

                return true;
            });

            findPreference("updatePortalsNow").setOnPreferenceClickListener(pref -> {
                PortalsUpdateService.startUpdate(getActivity());

                Toast.makeText(getActivity(), R.string.pref_portal_updating_action, Toast.LENGTH_SHORT)
                        .show();

                return true;
            });

            enableValueAsSummary(SettingsContract.PLUGINS_TIMEOUT);
        }

        private void enableValueAsSummary(@NonNull String key) {
            ListPreference listPreference = (ListPreference) findPreference(key);

            // Set default value
            if (listPreference.getValue() == null) {
                // to ensure we don't get a null value
                // set first value by default
                listPreference.setValueIndex(0);
            }

            // Show actual value
            listPreference.setSummary(listPreference.getEntry());

            // Set change listener
            listPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                // Workaround for https://stackoverflow.com/a/16233612/1392034
                ((ListPreference) preference).setValue(newValue.toString());
                preference.setSummary(((ListPreference) preference).getEntry());
                PrefsFragment.this.onPreferenceChange(preference, newValue);
                return true;
            });
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if(SettingsContract.PLUGINS_TIMEOUT.equals(preference.getKey())) {
                return true;
            }

            // Extract other values
            int frequency = Integer.parseInt(
                    ((ListPreference) findPreference(SettingsContract.SYNC_FREQUENCY)).getValue());
            boolean unmeteredOnly = ((CheckBoxPreference) findPreference(SettingsContract.SYNC_UNMETERED_ONLY))
                    .isChecked();
            boolean chargingOnly = ((CheckBoxPreference) findPreference(SettingsContract.SYNC_CHARGING_ONLY))
                    .isChecked();

            // The changed value is not saved yet, I have to use newValue instead (nice thing of Android ;-))
            switch (preference.getKey()) {
                case SettingsContract.SYNC_FREQUENCY:
                    SyncHandler.planFullSync(getActivity(), Integer.valueOf((String) newValue),
                            unmeteredOnly, chargingOnly);
                    break;
                case SettingsContract.SYNC_UNMETERED_ONLY:
                    SyncHandler.planFullSync(getActivity(), frequency,
                            (boolean) newValue, chargingOnly);
                    break;
                case SettingsContract.SYNC_CHARGING_ONLY:
                    SyncHandler.planFullSync(getActivity(), frequency,
                            unmeteredOnly, (boolean) newValue);
                    break;
                default:
                    Timber.v("Preference %s change ignored", preference.getKey());
            }
            return true;
        }
    }
}
