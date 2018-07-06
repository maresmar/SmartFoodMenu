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

package cz.maresmar.sfm.view.menu;


import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import cz.maresmar.sfm.Assert;
import cz.maresmar.sfm.BuildConfig;
import cz.maresmar.sfm.R;
import cz.maresmar.sfm.provider.ProviderContract;
import cz.maresmar.sfm.utils.ActionUtils;
import cz.maresmar.sfm.utils.MenuUtils;
import timber.log.Timber;

/**
 * Fragment that allows full editing of orders (internally actions connected to menu entry) and showing
 * of menu details
 */
public class MenuDetailsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String ARG_USER_URI = "userUri";
    private static final String ARG_MENU_RELATIVE_ID = "menuRelativeId";
    private static final String ARG_PORTAL_ID = "portalId";

    private static final int MENU_LOADER_ID = 1;

    private Uri mUserUri;
    private long mMenuRelativeId;
    private long mPortalId;

    private TextView mDateText, mPriceText, mTextText, mPortalName;
    private Button mChangedReservedEditText, mChangedOfferedEditText;
    private TextView mToSyncReservedText, mToSyncOfferedText;
    private TextView mSyncedReservedText, mSyncedOfferedText;
    private TextView mTakenAmountText, mToTakeText, mToOrderText, mLastChangeText;
    private View mToTakeGroup, mOfferGroup;

    private int mMaxReserved = Integer.MAX_VALUE;
    private int mMinReserved = 0;
    private int mSyncedTaken = 0;
    private int mSyncedReserved = -1;

    /**
     * Creates new instance of fragment with specific menu entry
     *
     * @param userUri        User Uri prefix
     * @param menuRelativeId Relative ID of menu entry
     * @param portalId       ID of portal
     * @return A new instance of fragment
     */
    public static MenuDetailsFragment newInstance(Uri userUri, long menuRelativeId, long portalId) {
        MenuDetailsFragment fragment = new MenuDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_USER_URI, userUri);
        args.putLong(ARG_MENU_RELATIVE_ID, menuRelativeId);
        args.putLong(ARG_PORTAL_ID, portalId);
        fragment.setArguments(args);
        return fragment;
    }

    // -------------------------------------------------------------------------------------------
    // Fragment lifecycle
    // -------------------------------------------------------------------------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mUserUri = getArguments().getParcelable(ARG_USER_URI);
            mMenuRelativeId = getArguments().getLong(ARG_MENU_RELATIVE_ID);
            mPortalId = getArguments().getLong(ARG_PORTAL_ID);
        }

        getLoaderManager().initLoader(MENU_LOADER_ID, null, this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_menu_details, container, false);

        mDateText = view.findViewById(R.id.menu_date_text);
        mPriceText = view.findViewById(R.id.menu_price_text);
        mTextText = view.findViewById(R.id.menu_text_text);
        mPortalName = view.findViewById(R.id.menu_portal_text);

        mChangedReservedEditText = view.findViewById(R.id.menu_reserve_button);
        mChangedReservedEditText.setOnClickListener(v -> showPickerDialog(
                R.string.menu_detail_reserved,
                mMinReserved,
                mMaxReserved,
                Integer.valueOf(mChangedReservedEditText.getText().toString()),
                newVal -> {
                    mChangedReservedEditText.setText(String.valueOf(newVal));
                    saveChanges();
                }
        ));

        mChangedOfferedEditText = view.findViewById(R.id.menu_offer_button);
        mChangedOfferedEditText.setOnClickListener(v -> {
            int reserved = Integer.valueOf(mChangedReservedEditText.getText().toString());
            showPickerDialog(
                    R.string.menu_detail_offered,
                    0,
                    Math.min(mSyncedReserved - mSyncedTaken, reserved - mSyncedTaken),
                    Integer.valueOf(mChangedOfferedEditText.getText().toString()),
                    newVal -> {
                        mChangedOfferedEditText.setText(String.valueOf(newVal));
                        saveChanges();
                    }
            );
        });

        mToSyncReservedText = view.findViewById(R.id.menu_to_sync_reserved_text);
        mToSyncOfferedText = view.findViewById(R.id.menu_to_sync_offered_text);

        mSyncedReservedText = view.findViewById(R.id.menu_synced_reserved_text);
        mSyncedOfferedText = view.findViewById(R.id.menu_synced_offered_text);

        mTakenAmountText = view.findViewById(R.id.menu_taken_text);
        mToTakeText = view.findViewById(R.id.menu_to_take_text);
        mToOrderText = view.findViewById(R.id.menu_to_order_text);

        mLastChangeText = view.findViewById(R.id.menu_last_change_text);

        mToTakeGroup = view.findViewById(R.id.menu_to_take_group);
        mOfferGroup = view.findViewById(R.id.menu_offered_group);

        return view;
    }

    // -------------------------------------------------------------------------------------------
    // Loader callbacks
    // -------------------------------------------------------------------------------------------

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        switch (id) {
            case MENU_LOADER_ID: {
                Uri menuUri = Uri.withAppendedPath(mUserUri, ProviderContract.MENU_ENTRY_PATH);
                return new CursorLoader(
                        getContext(),
                        menuUri,
                        new String[]{
                                ProviderContract.MenuEntry.STATUS,
                                ProviderContract.MenuEntry.PORTAL_FEATURES,
                                ProviderContract.MenuEntry.DATE,
                                ProviderContract.MenuEntry.PRICE,
                                ProviderContract.MenuEntry.TEXT,
                                ProviderContract.MenuEntry.PORTAL_NAME,
                                ProviderContract.MenuEntry.SYNCED_RESERVED_AMOUNT,
                                ProviderContract.MenuEntry.SYNCED_OFFERED_AMOUNT,
                                ProviderContract.MenuEntry.LOCAL_RESERVED_AMOUNT,
                                ProviderContract.MenuEntry.LOCAL_OFFERED_AMOUNT,
                                ProviderContract.MenuEntry.EDIT_RESERVED_AMOUNT,
                                ProviderContract.MenuEntry.EDIT_OFFERED_AMOUNT,
                                ProviderContract.MenuEntry.SYNCED_TAKEN_AMOUNT,
                                ProviderContract.MenuEntry.REMAINING_TO_TAKE,
                                ProviderContract.MenuEntry.REMAINING_TO_ORDER,
                                ProviderContract.MenuEntry.LAST_ACTION_CHANGE,
                                ProviderContract.MenuEntry.LABEL
                        },
                        ProviderContract.MenuEntry.ME_RELATIVE_ID + " = " + mMenuRelativeId + " AND " +
                                ProviderContract.MenuEntry.PORTAL_ID + " = " + mPortalId,
                        null,
                        null
                );
            }
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + id);
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case MENU_LOADER_ID: {
                Timber.d("Menu data loaded");

                if (BuildConfig.DEBUG) {
                    Assert.isOne(cursor.getCount());
                }

                cursor.moveToFirst();

                @ProviderContract.MenuStatus int status = cursor.getInt(0);
                @ProviderContract.PortalFeatures int features = cursor.getInt(1);

                // Menu detail text
                final long rawDate = cursor.getLong(2);
                mDateText.setText(MenuUtils.getDateStr(mDateText.getContext(), rawDate));
                final int rawPrice = cursor.getInt(3);
                mPriceText.setText(MenuUtils.getPriceStr(mPriceText.getContext(), rawPrice));
                mTextText.setText(cursor.getString(4));
                mPortalName.setText(cursor.getString(5));

                // Taken amount handling
                mSyncedTaken = cursor.getInt(12);
                mTakenAmountText.setText(String.valueOf(mSyncedTaken));

                // Amount table
                // Synced
                int reserved = cursor.getInt(6);
                int offered = cursor.getInt(7);
                mSyncedReservedText.setText(String.valueOf(reserved));
                mSyncedReserved = reserved;
                if ((status & ProviderContract.MENU_STATUS_CANCELABLE) == ProviderContract.MENU_STATUS_CANCELABLE) {
                    mMinReserved = mSyncedTaken;
                } else {
                    mMinReserved = mSyncedReserved;
                }
                mSyncedOfferedText.setText(String.valueOf(offered));
                // To sync
                if (!cursor.isNull(8)) {
                    reserved = cursor.getInt(8);
                    offered = cursor.getInt(9);
                    mToSyncReservedText.setText(String.valueOf(reserved));
                    mToSyncOfferedText.setText(String.valueOf(offered));
                } else {
                    mToSyncReservedText.setText(R.string.menu_detail_empty);
                    mToSyncOfferedText.setText(R.string.menu_detail_empty);
                }
                // Change
                if (!cursor.isNull(10)) {
                    reserved = cursor.getInt(10);
                    offered = cursor.getInt(11);
                }
                mChangedReservedEditText.setText(String.valueOf(reserved));
                mChangedOfferedEditText.setText(String.valueOf(offered));

                // To take handling
                final int toTake = cursor.getInt(13);
                if (toTake != ProviderContract.NO_INFO) {
                    mToTakeText.setText(String.valueOf(toTake));
                    mToTakeGroup.setVisibility(View.VISIBLE);
                } else {
                    mToTakeText.setText(R.string.menu_detail_not_available);
                    mToTakeGroup.setVisibility(View.GONE);
                }

                // To order handling
                final int toOrder = cursor.getInt(14);
                if (toOrder != ProviderContract.NO_INFO) {
                    mToOrderText.setText(String.valueOf(toOrder));
                    if ((features & ProviderContract.FEATURE_MULTIPLE_ORDERS) == ProviderContract.FEATURE_MULTIPLE_ORDERS) {
                        mMaxReserved = mSyncedReserved + toOrder;
                    } else {
                        mMaxReserved = Math.min(1, mSyncedReserved + toOrder);
                    }
                } else {
                    if ((status & ProviderContract.MENU_STATUS_ORDERABLE) == ProviderContract.MENU_STATUS_ORDERABLE) {
                        mToOrderText.setText(R.string.menu_detail_unlimited);
                        if ((features & ProviderContract.FEATURE_MULTIPLE_ORDERS) == ProviderContract.FEATURE_MULTIPLE_ORDERS) {
                            mMaxReserved = Integer.MAX_VALUE;
                        } else {
                            mMaxReserved = 1;
                        }
                    } else {
                        mToOrderText.setText("0");
                        mMaxReserved = mSyncedReserved;
                    }
                }

                // Last change
                final long rawLastChange = cursor.getLong(15);
                if (rawLastChange != ProviderContract.NO_INFO) {
                    mLastChangeText.setText(MenuUtils.getDateTimeStr(mLastChangeText.getContext(), rawLastChange));
                } else {
                    mLastChangeText.setText(R.string.menu_detail_not_available);
                }

                // Reserved change button
                if (rawDate >= MenuUtils.getTodayDate()) {
                    mChangedReservedEditText.setEnabled(mMinReserved != mMaxReserved);
                } else {
                    mChangedReservedEditText.setEnabled(false);
                }

                // Offer change button
                if ((status & ProviderContract.FEATURE_FOOD_STOCK) == ProviderContract.FEATURE_FOOD_STOCK) {
                    mChangedOfferedEditText.setEnabled(rawDate >= MenuUtils.getTodayDate() && 0 != mSyncedReserved);
                    mOfferGroup.setVisibility(View.VISIBLE);
                } else {
                    mChangedOfferedEditText.setEnabled(false);
                    mOfferGroup.setVisibility(View.INVISIBLE);
                }

                // Label
                getActivity().setTitle(cursor.getString(16));

                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + loader.getId());
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        switch (loader.getId()) {
            case MENU_LOADER_ID:
                Timber.e("Menu data with user %s is no longer valid", mUserUri);
                break;
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + loader.getId());
        }
    }

    private void showPickerDialog(@StringRes int title, int min, int max, int value, IntConsumer newValueConsumer) {
        // Prepare number picker
        NumberPicker picker = new NumberPicker(getContext());
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setValue(value);
        picker.setWrapSelectorWheel(false);

        FrameLayout layout = new FrameLayout(getContext());
        layout.addView(picker, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));

        // Show alert
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(R.string.menu_detail_pick_new_value)
                .setView(layout)
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    // do something with picker.getValue()
                    newValueConsumer.accept(picker.getValue());
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public void saveChanges() {
        try {
            int changeReserved = Integer.parseInt(mChangedReservedEditText.getText().toString());
            int changeOffered = Integer.parseInt(mChangedOfferedEditText.getText().toString());

            AsyncTask.execute(() ->
                    ActionUtils.makeEdit(getContext(), mUserUri, mMenuRelativeId, mPortalId,
                            changeReserved, changeOffered));
        } catch (NumberFormatException e) {
            Timber.w(e, "Cannot save changes");
        }
    }
}

/**
 * {@link java.util.function.IntConsumer} for older Androids
 */
interface IntConsumer {
    /**
     * {@link java.util.function.IntConsumer#accept(int)} for older Androids
     */
    void accept(int value);
}