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

package cz.maresmar.sfm.view.portal;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import cz.maresmar.sfm.Assert;
import cz.maresmar.sfm.BuildConfig;
import cz.maresmar.sfm.R;
import cz.maresmar.sfm.app.SfmApp;
import cz.maresmar.sfm.plugin.ActionContract;
import cz.maresmar.sfm.provider.ProviderContract;
import cz.maresmar.sfm.service.plugin.PortalTestHandler;
import cz.maresmar.sfm.view.DataForm;
import cz.maresmar.sfm.view.WithExtraFragment;
import timber.log.Timber;

/**
 * Fragment that shows one portal and allows to edit it.
 */
public class PortalDetailFragment extends WithExtraFragment implements LoaderManager.LoaderCallbacks<Cursor>, DataForm {


    private static final int PORTAL_LOADER_ID = 1;
    private static final int PLUGIN_LOADER_ID = 2;
    private static final int PLACE_PICKER_REQUEST = 1;

    private static final LatLng DEFAULT_LOCATION = new LatLng(49.3961, 15.59124);

    // the fragment initialization parameters
    private static final String ARG_PORTAL_URI = "portal_uri";
    private static final String ARG_PORTAL_TEMP_URI = "portal_temp_uri";
    private static final String ARG_PORTAL_GROUP_TEMP_URI = "portal_group_temp_uri";

    // Local parameters
    private Uri mPortalGroupTempUri = null;
    private Uri mPortalTempUri = null;
    private Uri mPortalUri = null;
    private boolean mLoadDataFromDb = true;

    private LatLng mLocation = null;
    private PluginAdapter mPluginAdapter;

    CountDownLatch blockingLoaders = new CountDownLatch(1);

    // UI elements
    EditText mNameText;
    EditText mRefText;
    Spinner mPluginSpinner;
    Spinner mSecuritySpinner;
    ArrayAdapter<CharSequence> mSecuritySpinnerAdapter;
    AppCompatCheckBox mNewMenuNotificationCheckBox;
    MapView mMapView;
    Button mRemoteButton;

    /**
     * Creates new Fragment
     */
    public PortalDetailFragment() {
        super(R.id.extras, ActionContract.FORMAT_TYPE_PORTAL);
        // Required empty public constructor
    }

    /**
     * Creates new fragment for specific portal
     *
     * @param portalUri Uri of portal to be shown or {@code null} for new one
     * @return A new instance of this fragment
     */
    public static PortalDetailFragment newInstance(@Nullable Uri portalUri) {
        PortalDetailFragment fragment = new PortalDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PORTAL_URI, portalUri);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Creates new fragment for new portal
     *
     * @return A new instance of this fragment
     */
    public static PortalDetailFragment newInstance() {
        return newInstance(null);
    }

    // -------------------------------------------------------------------------------------------
    // Lifecycle events
    // -------------------------------------------------------------------------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mPortalUri = getArguments().getParcelable(ARG_PORTAL_URI);
        }

        if (savedInstanceState != null) {
            mPortalUri = savedInstanceState.getParcelable(ARG_PORTAL_URI);
            mPortalTempUri = savedInstanceState.getParcelable(ARG_PORTAL_TEMP_URI);
            mPortalGroupTempUri = savedInstanceState.getParcelable(ARG_PORTAL_GROUP_TEMP_URI);
            mLoadDataFromDb = false;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_portal_detail, container, false);

        mNameText = view.findViewById(R.id.nameText);
        mRefText = view.findViewById(R.id.refText);

        // Plugin options
        mPluginSpinner = (Spinner) view.findViewById(R.id.pluginSpinner);
        mPluginSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                requestExtraFormat(mPluginAdapter.getItem(position).id);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        // Loads plugins
        getLoaderManager().initLoader(PLUGIN_LOADER_ID, null, new LoaderManager.LoaderCallbacks<List<PluginInfo>>() {
            @NonNull
            @Override
            public Loader<List<PluginInfo>> onCreateLoader(int id, Bundle args) {
                switch (id) {
                    case PLUGIN_LOADER_ID:
                        return new PluginListLoader(getContext());
                    default:
                        throw new UnsupportedOperationException("Unknown loader id: " + id);
                }
            }

            @Override
            public void onLoadFinished(@NonNull Loader<List<PluginInfo>> loader, List<PluginInfo> data) {
                //noinspection ConstantConditions
                mPluginAdapter = new PluginAdapter(getContext(), data);
                mPluginSpinner.setAdapter(mPluginAdapter);
                blockingLoaders.countDown();
            }

            @Override
            public void onLoaderReset(@NonNull Loader<List<PluginInfo>> loader) {
                switch (loader.getId()) {
                    case PLUGIN_LOADER_ID:
                        Timber.d("Action loader reset");
                        return;
                    default:
                        throw new UnsupportedOperationException("Unknown loader id: " + loader.getId());
                }
            }
        });

        // Security options
        mSecuritySpinner = (Spinner) view.findViewById(R.id.securitySpinner);
        //noinspection ConstantConditions
        mSecuritySpinnerAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.connection_security_values, R.layout.support_simple_spinner_dropdown_item);
        mSecuritySpinner.setAdapter(mSecuritySpinnerAdapter);

        // New menu notification
        mNewMenuNotificationCheckBox = view.findViewById(R.id.portalNewMenuNotify);

        // Gets the MapView from the XML layout and creates it
        mMapView = (MapView) view.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);


        // Gets to GoogleMap from the MapView and does initialization stuff
        mMapView.getMapAsync(map -> {
            // Needs to call MapsInitializer before doing any CameraUpdateFactory calls
            //noinspection ConstantConditions
            MapsInitializer.initialize(getContext());

            // Updates the location and zoom of the MapView to Jihlava, CZ
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 10);
            map.animateCamera(cameraUpdate);
        });

        // Pick location button
        view.findViewById(R.id.pickButton).setOnClickListener(locationView -> {
            Timber.i("Place picker intent request fired");
            PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
            try {
                //noinspection ConstantConditions
                startActivityForResult(builder.build(getActivity()), PLACE_PICKER_REQUEST);
            } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
                boolean serviceAvailable = SfmApp.checkPlayServices(getActivity());
                Timber.e(e, "Cannot start Place picker intent, " +
                        "(Google play service available = %b)", serviceAvailable);
                e.printStackTrace();
            }
        });

        // Remove location button
        mRemoteButton = (Button) view.findViewById(R.id.remoteButton);
        mRemoteButton.setOnClickListener(buttonView -> removeLocation());

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Loads portal data from DB
        if (mPortalUri != null && mLoadDataFromDb) {
            getLoaderManager().initLoader(PORTAL_LOADER_ID, null, this);
        }
    }

    @Override
    public void onResume() {
        mMapView.onResume();
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    // -------------------------------------------------------------------------------------------
    // UI save and restore
    // -------------------------------------------------------------------------------------------

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ARG_PORTAL_URI, mPortalUri);
        outState.putParcelable(ARG_PORTAL_TEMP_URI, mPortalTempUri);
        outState.putParcelable(ARG_PORTAL_GROUP_TEMP_URI, mPortalGroupTempUri);
        mMapView.onSaveInstanceState(outState);
    }

    // -------------------------------------------------------------------------------------------
    // Callbacks
    // -------------------------------------------------------------------------------------------

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                //noinspection ConstantConditions
                setLocation(PlacePicker.getPlace(getContext(), data).getLatLng());
            }
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (BuildConfig.DEBUG) {
            Assert.that(mPortalUri != null, "portalUri cannot be null");
        }
        switch (id) {
            case PORTAL_LOADER_ID:
                //noinspection ConstantConditions
                return new CursorLoader(
                        getContext(),
                        mPortalUri,
                        new String[]{
                                ProviderContract.Portal.NAME,
                                ProviderContract.Portal.REFERENCE,
                                ProviderContract.Portal.PLUGIN,
                                ProviderContract.Portal.SECURITY,
                                ProviderContract.Portal.LOC_N,
                                ProviderContract.Portal.LOC_E,
                                ProviderContract.Portal.EXTRA,
                                ProviderContract.Portal.FLAGS
                        },
                        null,
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
            case PORTAL_LOADER_ID:
                Timber.d("Portal data loaded");

                cursor.moveToFirst();
                if (BuildConfig.DEBUG) {
                    Assert.isOne(cursor.getCount());
                }

                // Portal name
                mNameText.setText(cursor.getString(0));
                // Portal reference
                mRefText.setText(cursor.getString(1));
                // Security spinner
                setConnectionSecurity(cursor.getInt(3));
                // Location
                LatLng location = new LatLng(cursor.getDouble(4), cursor.getDouble(5));
                if (location.latitude == 0 && location.longitude == 0)
                    removeLocation();
                else
                    setLocation(location);
                // Extra
                setExtraData(cursor.getString(6));
                // Plugin
                String plugin = cursor.getString(2);
                AsyncTask.execute(() -> {
                    try {
                        blockingLoaders.await();
                    } catch (InterruptedException e) {
                        Timber.e(e);
                    }
                    getActivity().runOnUiThread(() -> setPlugin(plugin));
                });

                // New menu notification
                @ProviderContract.PortalFlags int flags = cursor.getInt(7);
                mNewMenuNotificationCheckBox.setChecked(
                        !((flags & ProviderContract.PORTAL_FLAG_DISABLE_NEW_MENU_NOTIFICATION) ==
                                ProviderContract.PORTAL_FLAG_DISABLE_NEW_MENU_NOTIFICATION)
                );
                break;
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + loader.getId());
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        switch (loader.getId()) {
            case PORTAL_LOADER_ID:
                Timber.e("Portal data %s is no longer valid", mPortalUri);
                // Let's tread current user data as new entry
                reset(null);
                break;
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + loader.getId());
        }
    }

    // -------------------------------------------------------------------------------------------
    // Data form manipulating methods
    // -------------------------------------------------------------------------------------------

    /**
     * Changes portal and resets fragment
     *
     * @param portalUri New portal to be shown or {@code null} for new one
     */
    @UiThread
    public void reset(@Nullable Uri portalUri) {
        // Delete old temp data
        if (portalUri != mPortalTempUri) {
            discardTempData(getContext());
        }

        // Loads new data
        mPortalUri = portalUri;
        if (mPortalUri != null) {
            getLoaderManager().restartLoader(PORTAL_LOADER_ID, null, this);
        } else {
            mNameText.setText("");
            mRefText.setText("");
            removeLocation();
            setExtraData(null);

        }
    }

    // -------------------------------------------------------------------------------------------
    // Plugins handling
    // -------------------------------------------------------------------------------------------

    private void setPlugin(@Nullable String pluginId) {
        int position = -1;
        // There are only few entries in adapter so its ok to do it with for cycle
        for (int i = 0; i < mPluginAdapter.getCount(); i++) {
            if (((PluginInfo) mPluginAdapter.getItem(i)).id.equals(pluginId)) {
                position = i;
            }
        }
        if (position < 0) {
            Timber.e("Plugin %s not found", pluginId);
            //noinspection ConstantConditions
            Snackbar.make(getView(), getString(R.string.portal_plugin_not_found_error, pluginId), Snackbar.LENGTH_LONG)
                    .setAction(android.R.string.ok, view -> {
                        // Only dismiss message
                    })
                    .show();
        } else {
            mPluginSpinner.setSelection(position);
            requestExtraFormat(pluginId);
        }
    }

    @Nullable
    private String getSelectedPluginId() {
        PluginInfo selectedPlugin = (PluginInfo) mPluginSpinner.getSelectedItem();
        if (selectedPlugin != null)
            return selectedPlugin.id;
        else
            return null;
    }

    // -------------------------------------------------------------------------------------------
    // Connection security handling handling
    // -------------------------------------------------------------------------------------------

    private void setConnectionSecurity(int securityId) {
        if (securityId == 0)
            return;
        // Parse selected id
        int position = -1;
        // Find position for id
        // There are only few entries in adapter so its ok to do it with for cycle
        int[] ids = getResources().getIntArray(R.array.connection_security_ids);
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] == securityId) {
                position = i;
            }
        }
        if (BuildConfig.DEBUG) {
            Assert.that(position >= 0, "Illegal security value %d", securityId);
        }
        // Set it in UI
        mSecuritySpinner.setSelection(position);
    }

    // -------------------------------------------------------------------------------------------
    // Location
    // -------------------------------------------------------------------------------------------
    @UiThread
    private void setLocation(final LatLng position) {
        mLocation = position;
        mMapView.setVisibility(View.VISIBLE);
        mMapView.getMapAsync(googleMap -> {
            // Add a marker in Sydney, Australia,
            // and move the map's camera to the same location.
            googleMap.addMarker(new MarkerOptions().position(position)
                    .title(getString(R.string.portal_location_label)));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
        });
        mRemoteButton.setEnabled(true);
    }

    @UiThread
    private void removeLocation() {
        mLocation = null;
        mMapView.setVisibility(View.GONE);
        mRemoteButton.setEnabled(false);
    }

    // -------------------------------------------------------------------------------------------
    // Data form events
    // -------------------------------------------------------------------------------------------

    @UiThread
    @Override
    public boolean hasValidData() {
        boolean isValid = true;

        // Portal name test
        if (mNameText.getText().length() == 0) {
            mNameText.setError(getString(R.string.portal_name_empty_error));
            isValid = false;
        } else {
            // Test if portal name is unique
            long currentId;
            if (mPortalUri != null) {
                currentId = ContentUris.parseId(mPortalUri);
            } else {
                currentId = -1;
            }

            String portalName = mNameText.getText().toString();
            try (Cursor cursor = getContext().getContentResolver().query(
                    ProviderContract.Portal.getUri(),
                    new String[]{ProviderContract.Portal.PORTAL_ID},
                    ProviderContract.Portal.NAME + " == ? AND " + ProviderContract.Portal.PORTAL_ID + " != ?",
                    new String[]{portalName, "" + currentId},
                    null
            )) {
                if (cursor.getCount() == 0) {
                    mNameText.setError(null);
                } else {
                    mNameText.setError(getString(R.string.portal_name_used_error));
                    isValid = false;
                }
            }
        }

        if (!hasValidExtraData()) {
            isValid = false;
        }

        if (!isValid) {
            //noinspection ConstantConditions
            Snackbar.make(getView(), R.string.extra_invalid_input_error, Snackbar.LENGTH_LONG)
                    .setAction(android.R.string.ok, view -> {
                        // Only dismiss message
                    })
                    .show();
        }

        return isValid;
    }

    @NonNull
    @Override
    public Uri saveData() {
        Timber.i("Saving portal data");

        // Defines an object to contain the new values to insert
        ContentValues values = new ContentValues();

        /*
         * Sets the values of each column and inserts the word. The arguments to the "put"
         * method are "column name" and "value"
         */
        values.put(ProviderContract.Portal.NAME, mNameText.getText().toString());
        values.put(ProviderContract.Portal.REFERENCE, mRefText.getText().toString());
        values.put(ProviderContract.Portal.PLUGIN, getSelectedPluginId());

        int[] ids = getResources().getIntArray(R.array.connection_security_ids);
        int selectedId = ids[mSecuritySpinner.getSelectedItemPosition()];
        values.put(ProviderContract.Portal.SECURITY, selectedId);
        if (mLocation != null) {
            values.put(ProviderContract.Portal.LOC_N, mLocation.latitude);
            values.put(ProviderContract.Portal.LOC_E, mLocation.longitude);
        }
        values.put(ProviderContract.Portal.EXTRA, getExtraData());

        // New menu notification
        @ProviderContract.PortalFlags int flags;
        if (mNewMenuNotificationCheckBox.isChecked()) {
            flags = 0;
        } else {
            flags = ProviderContract.PORTAL_FLAG_DISABLE_NEW_MENU_NOTIFICATION;
        }
        values.put(ProviderContract.Portal.FLAGS, flags);

        //noinspection ConstantConditions
        ContentResolver contentResolver = getContext().getContentResolver();
        if (mPortalUri == null) {
            // Ads new portal group
            mPortalGroupTempUri = contentResolver.insert(ProviderContract.PortalGroup.getUri(), null);
            //noinspection ConstantConditions
            long portalGroupId = Long.parseLong(mPortalGroupTempUri.getLastPathSegment());
            // Ads portal group to values
            values.put(ProviderContract.Portal.PORTAL_GROUP_ID, portalGroupId);
            // Insert them
            mPortalTempUri = contentResolver.insert(ProviderContract.Portal.getUri(), values);
            mPortalUri = mPortalTempUri;
        } else {
            int updatedRows = contentResolver.update(mPortalUri, values, null, null);
            if (BuildConfig.DEBUG) {
                Assert.isOne(updatedRows);
            }
        }
        return mPortalUri;
    }

    /**
     * Return {@link Uri} of saved portal
     *
     * @return {@link Uri} of saved portal
     */
    @Nullable
    public Uri getDataUri() {
        return mPortalUri;
    }

    @Override
    public void discardTempData(@NonNull Context context) {
        Timber.i("Discarding portal data");

        if (mPortalTempUri != null) {
            int affectedRows = context.getContentResolver().
                    delete(mPortalTempUri, null, null);
            if (BuildConfig.DEBUG) {
                Assert.isOne(affectedRows);
            }

            affectedRows = context.getContentResolver().
                    delete(mPortalGroupTempUri, null, null);
            if (BuildConfig.DEBUG) {
                Assert.isOne(affectedRows);
            }

            mPortalTempUri = null;
            mPortalUri = null;
        }
    }

    /**
     * Saves data from UI a do portal test in plugin
     *
     * @return {@link Uri} of saved portal
     */
    public Uri saveAndTestData() {
        // Gets data from portal fragment
        Uri portalUri = saveData();
        long portalId = ContentUris.parseId(portalUri);
        // Test the new data
        String plugin = getSelectedPluginId();
        PortalTestHandler.requestTest(getContext(), plugin, portalId);

        return portalUri;
    }
}