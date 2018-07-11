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

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import cz.maresmar.sfm.R;
import cz.maresmar.sfm.view.DataForm;

/**
 * Fragment that shows welcome information like license and some intro to the app
 * <p>
 * Activity that uses this fragment must implement {@link DataValidityListener} interface to handle
 * interaction events.</p>
 */
public class WelcomeFragment extends Fragment implements View.OnClickListener, DataForm {

    private AppCompatCheckBox mAcceptTermsCheckBox;
    private DataValidityListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment
     *
     * @return A new instance of fragment WelcomeFragment
     */
    public static WelcomeFragment newInstance() {
        return new WelcomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_welcome, container, false);

        mAcceptTermsCheckBox = (AppCompatCheckBox) rootView.findViewById(R.id.acceptTermsCheckBox);
        mAcceptTermsCheckBox.setOnClickListener(this);

        return rootView;
    }

    @Override
    @UiThread
    public boolean hasValidData() {
        if(mAcceptTermsCheckBox.isChecked()) {
            return true;
        } else {
            mAcceptTermsCheckBox.setError(getString(R.string.welcome_accept_terms_error));
            Snackbar.make(getView(), R.string.welcome_accept_terms_error, Snackbar.LENGTH_LONG)
                    .setAction(android.R.string.ok, view -> {
                        // Only dismiss message
                    })
                    .show();
            return false;
        }
    }

    @Override
    public void onClick(View v) {
        if(mAcceptTermsCheckBox.isChecked()) {
            // Disable error (when there is any)
            mAcceptTermsCheckBox.setError(null);
            mListener.onDataValidityChanged(this, true);
        } else {
            mListener.onDataValidityChanged(this, false);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof DataValidityListener) {
            mListener = (DataValidityListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement DataValidityListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
