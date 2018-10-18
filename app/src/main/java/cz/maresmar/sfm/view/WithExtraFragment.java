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

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.widget.TextViewCompat;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import cz.maresmar.sfm.Assert;
import cz.maresmar.sfm.BuildConfig;
import cz.maresmar.sfm.R;
import cz.maresmar.sfm.plugin.ActionContract;
import cz.maresmar.sfm.plugin.ExtraFormat;
import cz.maresmar.sfm.service.plugin.ExtraFormatHandler;
import timber.log.Timber;

/**
 * Fragment that contains plugin extras that can be shown in UI. Extras specify value-pair settings
 * that are specific for each plugin.
 *
 * @see ExtraFormat
 * @see ExtraFormatHandler
 */
public abstract class WithExtraFragment extends Fragment implements ExtraFormatHandler.ResultListener {

    private static final String ARG_EXTRA_DATA = "WithExtraFragment_extra";
    private static final String ARG_LAST_PLUGIN = "WithExtraFragment_lastPlugin";

    // Local variables
    @NonNull
    private List<ExtraFormat> mExtrasFormat = new ArrayList<>();
    private String mExtraData = null;
    private String mLastPlugin = null;

    // Auto variables
    @IdRes
    int mExtraLinearLayoutId;
    @ActionContract.ExtraFormatType
    int mExtraFormatType;

    private ExtraFormatHandler.ResultReceiver mExtraReceiver = new ExtraFormatHandler.ResultReceiver(this);

    // Ui
    private LinearLayout mExtraLinearLayout;
    private EditText[] mExtraUiBindings;

    /**
     * Creates fragment with {@link LinearLayout} that contains extras of specific type
     *
     * @param extraLinearLayoutId Resource ID of {@link LinearLayout} that will contains extras
     * @param extraFormatType     Type of extras (portal specific or credential specific)
     */
    public WithExtraFragment(@IdRes int extraLinearLayoutId, @ActionContract.ExtraFormatType int extraFormatType) {
        super();
        mExtraLinearLayoutId = extraLinearLayoutId;
        mExtraFormatType = extraFormatType;
    }

    @Override
    public void onStart() {
        super.onStart();

        //noinspection ConstantConditions
        mExtraLinearLayout = getView().findViewById(mExtraLinearLayoutId);

        //noinspection ConstantConditions
        getContext().registerReceiver(mExtraReceiver, ExtraFormatHandler.INTENT_FILTER);
    }

    @Override
    public void onStop() {
        super.onStop();

        //noinspection ConstantConditions
        getContext().unregisterReceiver(mExtraReceiver);
    }

    // -------------------------------------------------------------------------------------------
    // UI save and restore
    // -------------------------------------------------------------------------------------------

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(ARG_EXTRA_DATA, getExtraData());
        outState.putString(ARG_LAST_PLUGIN, mLastPlugin);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            setExtraData(savedInstanceState.getString(ARG_EXTRA_DATA));
            mLastPlugin = savedInstanceState.getString(ARG_LAST_PLUGIN);
        }
    }

    // -------------------------------------------------------------------------------------------
    // Data form manipulating methods
    // -------------------------------------------------------------------------------------------

    /**
     * Test if extras in UI contains valid data
     *
     * @return {@code true} if data are valid, {@code false} otherwise
     */
    public boolean hasValidExtraData() {
        boolean isValid = true;

        int i = 0;
        for (ExtraFormat extraFormat : mExtrasFormat) {
            EditText extraEditText = mExtraUiBindings[i];

            if (extraFormat.valuesList.length == 0) {
                String extraValue = extraEditText.getText().toString();
                boolean matches = Pattern.matches(extraFormat.pattern, extraValue);

                if (!matches) {
                    String errorText = getString(R.string.extra_value_not_follow_pattern_error, extraFormat.pattern);
                    extraEditText.setError(errorText);

                    isValid = false;
                } else {
                    extraEditText.setError(null);
                }
            }

            i++;
        }

        return isValid;
    }

    /**
     * Returns extras values from UI (as JSON String)
     *
     * @return Extras value-pairs form {@link JSONObject#toString()}
     * @see JSONObject
     */
    @NonNull
    public String getExtraData() {
        try {
            JSONObject jsonObject = new JSONObject();

            int i = 0;
            for (ExtraFormat extraFormat : mExtrasFormat) {
                EditText extraEditText = mExtraUiBindings[i];

                String extraValue = extraEditText.getText().toString();
                jsonObject.put(extraFormat.code, extraValue);

                i++;
            }

            return jsonObject.toString();
        } catch (JSONException e) {
            Timber.wtf(e, "Cannot save extras");
            throw new IllegalStateException("Cannot save extras");
        }
    }

    /**
     * Loads extras values to UI from JSON String
     *
     * @param extras value-pairs form {@link JSONObject#toString()} or {@code null} if fields should be empty
     * @see JSONObject
     */
    public void setExtraData(@Nullable String extras) {
        mExtraData = extras;
        refreshExtraDataInUi();
    }

    /**
     * Request extra format from plugin and inflates format to UI
     *
     * @param pluginName Name of plugin
     * @see ExtraFormatHandler
     */
    protected void requestExtraFormat(@Nullable String pluginName) {
        if(BuildConfig.DEBUG) {
            Assert.that(mExtraLinearLayout != null, "" +
                    "You should call this method after onStart() event in fragment's lifecycle");
        }

        // If no plugin is selected
        if (pluginName == null || pluginName.length() == 0) {
            // Clear UI
            if (mExtrasFormat.size() > 0) {
                mExtrasFormat = new ArrayList<>();
                inflateExtraFormat();
            }
            return;
        }

        // If the plugin is changed
        if (mLastPlugin == null || !pluginName.equals(mLastPlugin)) {
            mLastPlugin = pluginName;

            // Clear UI
            mExtrasFormat = new ArrayList<>();
            inflateExtraFormat();

            // Ask for new format
            //noinspection ConstantConditions
            ExtraFormatHandler.requestExtraFormat(getContext(), pluginName, mExtraFormatType);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @UiThread
    private void inflateExtraFormat() {
        // Remove old extras from UI
        mExtraLinearLayout.removeAllViewsInLayout();

        mExtraUiBindings = new EditText[mExtrasFormat.size()];

        final LinearLayout.LayoutParams matchWrapParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        // For each extra
        int index = 0;
        for (ExtraFormat extraFormat : mExtrasFormat) {
            //noinspection ConstantConditions
            TextInputLayout textInputLayout = new TextInputLayout(getContext());
            textInputLayout.setLayoutParams(matchWrapParams);
            textInputLayout.setHint(extraFormat.name);

            // If edit text extra
            if (extraFormat.valuesList.length == 0) {
                TextInputEditText editText = new TextInputEditText(getContext());
                editText.setLayoutParams(matchWrapParams);

                mExtraUiBindings[index] = editText;

                textInputLayout.addView(editText);
            } else { // If extra with dropdown
                // Prepare adapter for values
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                        R.layout.support_simple_spinner_dropdown_item, extraFormat.valuesList);

                AutoCompleteTextView autoCompleteTextView = new AutoCompleteTextView(getContext());
                autoCompleteTextView.setLayoutParams(matchWrapParams);
                autoCompleteTextView.setAdapter(adapter);
                // Some UI tweaks to make it look nice
                autoCompleteTextView.setKeyListener(null);
                autoCompleteTextView.setOnTouchListener((v, event) -> {
                    ((AutoCompleteTextView) v).showDropDown();
                    return false;
                });
                // Set default value
                autoCompleteTextView.setText(extraFormat.valuesList[0]);
                // setText disable other values so I have un-filter them
                adapter.getFilter().filter(null);

                mExtraUiBindings[index] = autoCompleteTextView;

                textInputLayout.addView(autoCompleteTextView);
            }
            mExtraLinearLayout.addView(textInputLayout);

            // Adds optimal extra description
            if (extraFormat.description != null) {
                TextView description = new TextView(getContext());
                description.setText(extraFormat.description);
                TextViewCompat.setTextAppearance(description, R.style.StaticLabel);

                LinearLayout.LayoutParams descriptionLayoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                descriptionLayoutParams.setMargins(getResources().getDimensionPixelSize(R.dimen.content_margin), 0, 0, 0);
                description.setLayoutParams(descriptionLayoutParams);

                mExtraLinearLayout.addView(description);
            }

            index++;
        }
    }

    private void refreshExtraDataInUi() {
        if (mExtraData == null || mExtraData.length() == 0) {
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject(mExtraData);

            int i = 0;
            for (ExtraFormat extraFormat : mExtrasFormat) {
                EditText extraEditText = mExtraUiBindings[i];

                try {
                    String extraValue = jsonObject.getString(extraFormat.code);
                    extraEditText.setText(extraValue);
                    if (extraFormat.valuesList.length != 0) {
                        // Show all values in drop down (as setText disable another values)
                        AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView) extraEditText;
                        ((ArrayAdapter<String>) autoCompleteTextView.getAdapter()).getFilter().filter(null);
                    }
                } catch (JSONException e) {
                    Timber.w(e, "Cannot parse saved extras for %s", extraFormat.code);
                    // I can leave default values so user can change it
                }

                i++;
            }
        } catch (JSONException e) {
            Timber.w(e, "Cannot parse saved extras");
            // I can leave default values so user can change it
        }
    }

    // -------------------------------------------------------------------------------------------
    // Callbacks
    // -------------------------------------------------------------------------------------------

    @Override
    public void onExtraFormatResult(@NonNull String plugin, @ActionContract.ExtraFormatType int formatType,
                                    @NonNull List<ExtraFormat> extraFormat) {
        if (formatType == mExtraFormatType) {
            // Add forms to UI
            mExtrasFormat = extraFormat;
            inflateExtraFormat();
            // Sets data in UI
            refreshExtraDataInUi();
        }
    }
}
