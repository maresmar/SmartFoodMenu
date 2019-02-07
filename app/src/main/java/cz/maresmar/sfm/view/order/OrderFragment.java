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

package cz.maresmar.sfm.view.order;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.core.content.ContextCompat;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import cz.maresmar.sfm.Assert;
import cz.maresmar.sfm.BuildConfig;
import cz.maresmar.sfm.R;
import cz.maresmar.sfm.provider.ProviderContract;
import cz.maresmar.sfm.utils.MenuUtils;
import cz.maresmar.sfm.view.CursorRecyclerViewAdapter;
import cz.maresmar.sfm.view.FragmentChangeRequestListener;
import timber.log.Timber;

/**
 * Fragment that shows orders and payments
 */
public class OrderFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    // the fragment initialization parameters
    private static final String ARG_USER_URI = "userUri";

    private static final int ACTION_LOADER_ID = 1;

    private Uri mUserUri;

    private FragmentChangeRequestListener mFragmentRequestListener;
    private RecyclerView mRecyclerView;
    private ActionsViewAdapter mActionsAdapter;
    private View mEmptyView;

    /**
     * Creates new fragment for specific user
     *
     * @param userUri User Uri prefix
     * @return A new instance of fragment
     */
    public static OrderFragment newInstance(Uri userUri) {
        OrderFragment fragment = new OrderFragment();
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

        getLoaderManager().initLoader(ACTION_LOADER_ID, null, this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_orders, container, false);

        mRecyclerView = view.findViewById(R.id.orders_recycle_view);
        mActionsAdapter = new ActionsViewAdapter();
        mRecyclerView.setAdapter(mActionsAdapter);

        // Prepare empty view
        mEmptyView = view.findViewById(R.id.empty_content);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof FragmentChangeRequestListener) {
            mFragmentRequestListener = (FragmentChangeRequestListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement FragmentChangeRequestListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFragmentRequestListener = null;
    }

    // -------------------------------------------------------------------------------------------
    // Loader callbacks
    // -------------------------------------------------------------------------------------------

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        switch (id) {
            case ACTION_LOADER_ID: {
                Uri actionsUri = Uri.withAppendedPath(mUserUri, ProviderContract.ACTION_PATH);
                return new CursorLoader(
                        getContext(),
                        actionsUri,
                        new String[]{
                                ProviderContract.Action._ID,
                                ProviderContract.Action.ENTRY_TYPE,
                                ProviderContract.Action.SYNC_STATUS,
                                ProviderContract.Action.ME_DATE,
                                ProviderContract.Action.PRICE,
                                ProviderContract.Action.ME_PORTAL_NAME,
                                ProviderContract.Action.ME_LABEL,
                                ProviderContract.Action.RESERVED_AMOUNT,
                                ProviderContract.Action.OFFERED_AMOUNT,
                                ProviderContract.Action.TAKEN_AMOUNT,
                                ProviderContract.Action.LAST_CHANGE,
                                ProviderContract.Action.DESCRIPTION,
                                ProviderContract.Action.ME_RELATIVE_ID,
                                ProviderContract.Action.ME_PORTAL_ID
                        },
                        ProviderContract.Action.ENTRY_TYPE + " != " + ProviderContract.ACTION_ENTRY_TYPE_VIRTUAL,
                        null,
                        "MAX(" + ProviderContract.Action.ME_DATE + ", " + ProviderContract.Action.LAST_CHANGE
                                + " ) DESC, " + ProviderContract.Action.SYNC_STATUS + " ASC"
                );
            }
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + id);
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case ACTION_LOADER_ID: {
                Timber.d("Action data loaded");

                // Empty state
                if (cursor.getCount() > 0) {
                    mEmptyView.setVisibility(View.GONE);
                    mRecyclerView.setVisibility(View.VISIBLE);
                } else {
                    mRecyclerView.setVisibility(View.GONE);
                    mEmptyView.setVisibility(View.VISIBLE);
                }

                mActionsAdapter.swapCursor(cursor);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + loader.getId());
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        switch (loader.getId()) {
            case ACTION_LOADER_ID:
                Timber.e("Action data with user %s is no longer valid", mUserUri);
                mActionsAdapter.swapCursor(null);
                break;
            default:
                throw new UnsupportedOperationException("Unknown loader id: " + loader.getId());
        }
    }

    /**
     * Allows change of user (and restarts fragment)
     * @param userUri User Uri prefix
     */
    public void reset(Uri userUri) {
        if (!mUserUri.equals(userUri)) {
            mUserUri = userUri;
            getLoaderManager().restartLoader(ACTION_LOADER_ID, null, this);
        }
    }

    // -------------------------------------------------------------------------------------------
    // List callbacks
    // -------------------------------------------------------------------------------------------

    private void deleteOrder(long orderId) {
        AsyncTask.execute(() -> {
            Uri orderUri = ContentUris.withAppendedId(Uri.withAppendedPath(mUserUri, ProviderContract.ACTION_PATH), orderId);

            int deletedRows = getContext().getContentResolver().delete(orderUri, null, null);
            if (BuildConfig.DEBUG) {
                Assert.isOne(deletedRows);
            }
        });
    }

    private void showMenuDetail(long menuRelativeId, long portalId) {
        mFragmentRequestListener.showMenuDetail(menuRelativeId, portalId);
    }

    // -------------------------------------------------------------------------------------------
    // Recycler View
    // -------------------------------------------------------------------------------------------

    /**
     * {@link RecyclerView} adapter that shows orders
     */
    public class ActionsViewAdapter
            extends CursorRecyclerViewAdapter<ActionsViewAdapter.ViewHolder> {

        private final View.OnClickListener mOnClickListener = view -> {
            OrderInfo orderInfo = (OrderInfo) view.getTag();
            showMenuDetail(orderInfo.menuRelativeId, orderInfo.portalId);
        };

        private final View.OnLongClickListener mOnLongClickListener = view -> {
            // Prepare data
            OrderInfo orderInfo = (OrderInfo) view.getTag();

            // Create popup menu
            PopupMenu popup = new PopupMenu(view.getContext(), view);
            popup.inflate(R.menu.popup_order_list);
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.order_delete:
                        deleteOrder(orderInfo.actionId);
                        return true;
                    case R.id.order_show_menu:
                        showMenuDetail(orderInfo.menuRelativeId, orderInfo.portalId);
                        return true;
                    default:
                        return false;
                }
            });

            // Synced actions shouldn't be deleted
            if (orderInfo.syncStatus == ProviderContract.ACTION_SYNC_STATUS_SYNCED) {
                popup.getMenu().findItem(R.id.order_delete).setVisible(false);
            }

            popup.show();
            return true;
        };

        /**
         * Creates new adapter
         */
        ActionsViewAdapter() {
            super(null);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.card_order, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor) {
            @ProviderContract.ActionEntryType
            int entryType = cursor.getInt(1);

            switch (entryType) {
                case ProviderContract.ACTION_ENTRY_TYPE_STANDARD:
                    onBindStandardEntry(viewHolder, cursor);
                    break;
                case ProviderContract.ACTION_ENTRY_TYPE_PAYMENT:
                    onBindPaymentEntry(viewHolder, cursor);
                    break;
                case ProviderContract.ACTION_ENTRY_TYPE_VIRTUAL:
                default:
                    throw new UnsupportedOperationException("Unknown entryType " + entryType);

            }
        }

        private void onBindStandardEntry(ViewHolder viewHolder, Cursor cursor) {
            // Icon
            @ProviderContract.ActionSyncStatus
            int syncStatus = cursor.getInt(2);
            setStandardIcon(viewHolder.mIcon, syncStatus);

            // Date
            long menuEntryDate = cursor.getLong(3);
            viewHolder.mDate.setText(MenuUtils.getDateStr(viewHolder.mDate.getContext(), menuEntryDate));

            // Price
            int rawPrice = cursor.getInt(4);
            viewHolder.mPrice.setText(MenuUtils.getPriceStr(viewHolder.mPrice.getContext(), rawPrice));

            // Info text
            viewHolder.mInfo.setText(
                    getString(R.string.order_standard_info_text, cursor.getString(5), cursor.getString(6))
            );

            int reservedAmount = cursor.getInt(7);
            int offeredAmount = cursor.getInt(8);
            int takenAmount = cursor.getInt(9);

            String description = getString(R.string.order_standard_action_description, reservedAmount,
                    offeredAmount, takenAmount);
            viewHolder.mDetails.setText(description);
            viewHolder.mDetails.setVisibility(View.VISIBLE);

            // Last change
            long changeDate = cursor.getLong(10);
            viewHolder.mLastChange.setText(MenuUtils.getDateTimeStr(viewHolder.mDate.getContext(), changeDate));
            viewHolder.mLastChange.setVisibility(View.VISIBLE);

            long actionId = cursor.getLong(0);
            long menuRelativeId = cursor.getLong(12);
            long portalId = cursor.getLong(13);
            OrderInfo model = new OrderInfo(actionId, syncStatus, menuRelativeId, portalId);

            viewHolder.itemView.setTag(model);
            viewHolder.itemView.setOnClickListener(mOnClickListener);
            viewHolder.itemView.setOnLongClickListener(mOnLongClickListener);
        }

        private void setStandardIcon(ImageView icon, @ProviderContract.ActionSyncStatus int syncStatus) {
            // Select icon
            int tintColor;
            switch (syncStatus) {
                case ProviderContract.ACTION_SYNC_STATUS_EDIT:
                    icon.setImageResource(R.drawable.ic_order_edit);
                    tintColor = ContextCompat.getColor(icon.getContext(), R.color.textColor);
                    break;
                case ProviderContract.ACTION_SYNC_STATUS_LOCAL:
                    icon.setImageResource(R.drawable.ic_order_local);
                    tintColor = ContextCompat.getColor(icon.getContext(), R.color.textColor);
                    break;
                case ProviderContract.ACTION_SYNC_STATUS_SYNCED:
                    icon.setImageResource(R.drawable.ic_order_synced);
                    tintColor = ContextCompat.getColor(icon.getContext(), R.color.textColor);
                    break;
                case ProviderContract.ACTION_SYNC_STATUS_FAILED:
                    icon.setImageResource(R.drawable.ic_order_error);
                    tintColor = ContextCompat.getColor(icon.getContext(), R.color.colorPrimary);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown syncStatus " + syncStatus);
            }

            // Tint
            icon.setColorFilter(tintColor, android.graphics.PorterDuff.Mode.SRC_IN);
        }

        private void onBindPaymentEntry(ViewHolder viewHolder, Cursor cursor) {
            // Icon
            viewHolder.mIcon.setImageResource(R.drawable.ic_order_payment);
            int tintColor = ContextCompat.getColor(viewHolder.mIcon.getContext(), R.color.textColor);
            viewHolder.mIcon.setColorFilter(tintColor, android.graphics.PorterDuff.Mode.SRC_IN);

            // Date == lastChange
            long menuEntryDate = cursor.getLong(10);
            viewHolder.mDate.setText(MenuUtils.getDateTimeStr(viewHolder.mDate.getContext(), menuEntryDate));

            // Price
            int rawPrice = cursor.getInt(4);
            viewHolder.mPrice.setText(MenuUtils.getPriceStr(viewHolder.mPrice.getContext(), rawPrice));

            // Info text
            viewHolder.mInfo.setText(cursor.getString(11));

            viewHolder.mDetails.setVisibility(View.INVISIBLE);
            viewHolder.mLastChange.setVisibility(View.INVISIBLE);
        }

        /**
         * Standard {@link RecyclerView.ViewHolder} that stores one order or payment
         */
        class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView mIcon;
            final TextView mDate;
            final TextView mPrice;
            final TextView mInfo;
            final TextView mDetails;
            final TextView mLastChange;

            ViewHolder(View view) {
                super(view);
                mIcon = view.findViewById(R.id.order_icon_image);
                mDate = view.findViewById(R.id.order_date_text);
                mPrice = view.findViewById(R.id.order_price_text);
                mInfo = view.findViewById(R.id.order_info_text);
                mDetails = view.findViewById(R.id.order_detail_text);
                mLastChange = view.findViewById(R.id.order_last_change_text);
            }
        }

        /**
         * Order entry model that stores extra information about order or payment
         */
        class OrderInfo {
            final long actionId;
            final long menuRelativeId;
            final long portalId;
            @ProviderContract.ActionSyncStatus
            final int syncStatus;

            OrderInfo(long actionId, @ProviderContract.ActionSyncStatus int syncStatus, long menuRelativeId, long portalId) {
                this.actionId = actionId;
                this.syncStatus = syncStatus;
                this.menuRelativeId = menuRelativeId;
                this.portalId = portalId;
            }
        }
    }
}
