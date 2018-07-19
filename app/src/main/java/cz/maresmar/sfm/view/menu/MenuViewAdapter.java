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

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import cz.maresmar.sfm.Assert;
import cz.maresmar.sfm.BuildConfig;
import cz.maresmar.sfm.R;
import cz.maresmar.sfm.provider.ProviderContract;
import cz.maresmar.sfm.provider.PublicProviderContract;
import cz.maresmar.sfm.utils.ActionUtils;
import cz.maresmar.sfm.utils.MenuUtils;
import cz.maresmar.sfm.view.FragmentChangeRequestListener;

/**
 * {@link RecyclerView} adapter that provides some group of menu (eg with same portal or same day)
 */
public class MenuViewAdapter
        extends SectionCursorRecycleViewAdapter<MenuViewAdapter.ViewHolder> {

    private static final int ICON_KIND_SELECT = 0;
    private static final int ICON_KIND_EMPTY = 1;
    private static final int ICON_KIND_STOCK = 2;
    private static final int ICON_KIND_NUMBER = 3;
    private static final int ICON_KIND_WITHDRAWAL = 4;

    @IntDef(value = {
            ICON_KIND_SELECT,
            ICON_KIND_EMPTY,
            ICON_KIND_STOCK,
            ICON_KIND_NUMBER,
            ICON_KIND_WITHDRAWAL
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface IconKind {
    }

    private static final int[][] ICON_RES = new int[][]{ // [SyncStatus][IconKind]
            {R.drawable.ic_menu_select_edit, R.drawable.ic_menu_remove_edit, R.drawable.ic_menu_stock_edit},
            {R.drawable.ic_menu_select_local, R.drawable.ic_menu_remove_local, R.drawable.ic_menu_stock_edit},
            {R.drawable.ic_menu_select_synced, R.drawable.ic_menu_empty, R.drawable.ic_menu_stock_synced},
    };

    private static final int ACTION_DISABLED = 0;
    private static final int ACTION_RESERVE_NEW = 1;
    private static final int ACTION_CANCEL_OLD = 2;
    private static final int ACTION_RESERVED_FROM_STOCK = 3;
    private static final int ACTION_OFFER_IN_STOCK = 4;
    private static final int ACTION_REMOVE_FROM_STOCK = 5;
    private static final int ACTION_SHOW_DETAIL = 6;
    private static final int ACTION_CANCEL_EDIT = 7;

    @IntDef(value = {
            ACTION_DISABLED,
            ACTION_RESERVE_NEW,
            ACTION_CANCEL_OLD,
            ACTION_RESERVED_FROM_STOCK,
            ACTION_OFFER_IN_STOCK,
            ACTION_REMOVE_FROM_STOCK,
            ACTION_SHOW_DETAIL,
            ACTION_CANCEL_EDIT
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Action {
    }

    /**
     * Projection needed in {@link Cursor} for this class
     */
    public static final String[] PROJECTION = new String[]{
            ProviderContract.MenuEntry._ID,
            ProviderContract.MenuEntry.DATE,
            ProviderContract.MenuEntry.LABEL,
            ProviderContract.MenuEntry.PRICE,
            ProviderContract.MenuEntry.TEXT,
            ProviderContract.MenuEntry.REMAINING_TO_ORDER,
            ProviderContract.MenuEntry.REMAINING_TO_TAKE,
            ProviderContract.MenuEntry.STATUS,
            ProviderContract.MenuEntry.SYNCED_TAKEN_AMOUNT,
            ProviderContract.MenuEntry.SYNCED_RESERVED_AMOUNT,
            ProviderContract.MenuEntry.SYNCED_OFFERED_AMOUNT,
            ProviderContract.MenuEntry.LOCAL_RESERVED_AMOUNT,
            ProviderContract.MenuEntry.LOCAL_OFFERED_AMOUNT,
            ProviderContract.MenuEntry.EDIT_RESERVED_AMOUNT,
            ProviderContract.MenuEntry.EDIT_OFFERED_AMOUNT,
            ProviderContract.MenuEntry.ME_RELATIVE_ID,
            ProviderContract.MenuEntry.PORTAL_ID
    };

    public static final int DATE_COLUMN_INDEX = 1;
    public static final int PORTAL_COLUMN_INDEX = PROJECTION.length - 1;

    // -------------------------------------------------------------------------------------------
    // Variables
    // -------------------------------------------------------------------------------------------

    private Uri mUserUri;
    private RecyclerView mRecyclerView;
    private View mEmptyView;
    private FragmentChangeRequestListener mFragmentRequestListener;

    // -------------------------------------------------------------------------------------------
    // Click listeners
    // -------------------------------------------------------------------------------------------

    private final View.OnClickListener mOnClickListener = view -> {
        MenuEntryInfo menuInfo = (MenuEntryInfo) view.getTag();
        Context context = view.getContext();

        switch (menuInfo.onClickAction) {
            case ACTION_DISABLED:
                Toast.makeText(context, R.string.menu_change_disabled, Toast.LENGTH_SHORT).show();
                break;
            case ACTION_RESERVE_NEW:
                makeAsyncEdit(context, mUserUri, menuInfo.relativeId, menuInfo.portalId,
                        menuInfo.reservedCount + 1, menuInfo.offeredCount);
                break;
            case ACTION_CANCEL_OLD:
                makeAsyncEdit(context, mUserUri, menuInfo.relativeId, menuInfo.portalId,
                        menuInfo.reservedCount - 1, menuInfo.offeredCount);
                break;
            case ACTION_RESERVED_FROM_STOCK:
                makeAsyncEdit(context, mUserUri, menuInfo.relativeId, menuInfo.portalId,
                        menuInfo.reservedCount + 1, menuInfo.offeredCount);
                Toast.makeText(context, R.string.menu_change_order_from_stock, Toast.LENGTH_SHORT).show();
                break;
            case ACTION_OFFER_IN_STOCK:
                makeAsyncEdit(context, mUserUri, menuInfo.relativeId, menuInfo.portalId,
                        menuInfo.reservedCount, menuInfo.offeredCount + 1);
                Toast.makeText(context, R.string.menu_change_offer_in_stock, Toast.LENGTH_SHORT).show();
                break;
            case ACTION_REMOVE_FROM_STOCK:
                makeAsyncEdit(context, mUserUri, menuInfo.relativeId, menuInfo.portalId,
                        menuInfo.reservedCount, menuInfo.offeredCount - 1);
                Toast.makeText(context, R.string.menu_change_remove_from_stock, Toast.LENGTH_SHORT).show();
                break;
            case ACTION_CANCEL_EDIT:
                makeAsyncEdit(context, mUserUri, menuInfo.relativeId, menuInfo.portalId,
                        menuInfo.reservedCount, menuInfo.offeredCount);
                break;
            case ACTION_SHOW_DETAIL:
                mFragmentRequestListener.showMenuDetail(menuInfo.relativeId, menuInfo.portalId);
                break;
            default:
                throw new UnsupportedOperationException("Unknown action " + menuInfo.onClickAction);
        }
    };

    private final View.OnLongClickListener mOnLongClickListener = view -> {
        MenuEntryInfo menuInfo = (MenuEntryInfo) view.getTag();
        mFragmentRequestListener.showMenuDetail(menuInfo.relativeId, menuInfo.portalId);
        return true;
    };

    // -------------------------------------------------------------------------------------------
    // Main methods
    // -------------------------------------------------------------------------------------------

    /**
     * Creates new adapter with column that defines group (rows with equal values in that cursor
     * determine one group)
     *
     * @param fragmentRequestListener Callbacks that allows showing of {@link MenuDetailsFragment}
     * @param columnIndex             Index of {@link Cursor} column that defines group
     * @param emptyView               View that will be used if group will be empty
     */
    public MenuViewAdapter(FragmentChangeRequestListener fragmentRequestListener, int columnIndex,
                           View emptyView) {
        super(columnIndex);
        mFragmentRequestListener = fragmentRequestListener;
        mUserUri = null;
        mEmptyView = emptyView;
    }

    /**
     * Changes user and cursors to show new data
     *
     * @param userUri         User Uri prefix
     * @param menuItemsCursor Cursor that follows {@link #PROJECTION} (the cursor have to be sorted
     *                        {@code ASC} by column that defines group)
     * @param portalId        ID of portal to be shown
     */
    public void swapData(Uri userUri, Cursor menuItemsCursor, long portalId) {
        mUserUri = userUri;
        swapData(menuItemsCursor, portalId);

        if (getItemCount() > 0) {
            mEmptyView.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        } else {
            mRecyclerView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
        }
    }

    private void makeAsyncEdit(@NonNull Context context, @NonNull Uri userUri, long relativeId,
                               long portalId, int reserved, int offered) {
        AsyncTask.execute(() ->
                ActionUtils.makeEdit(context, userUri, relativeId, portalId, reserved, offered));
    }

    // -------------------------------------------------------------------------------------------
    // Adapter lifecycle
    // -------------------------------------------------------------------------------------------

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_menu, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor) {
        viewHolder.mLabel.setText(cursor.getString(2));

        int rawCredit = cursor.getInt(3);
        viewHolder.mPrice.setText(MenuUtils.getPriceStr(viewHolder.mPrice.getContext(), rawCredit));
        viewHolder.mText.setText(cursor.getString(4));
        // Remaining orders
        int remainingToOrder = cursor.getInt(5);
        int remainingToTake = cursor.getInt(6);
        if (remainingToTake >= 0) {
            viewHolder.mRemaining.setText(String.valueOf(remainingToTake));
        } else {
            if (remainingToOrder > 0) {
                viewHolder.mRemaining.setText(String.valueOf(remainingToOrder));
            } else {
                viewHolder.mRemaining.setText("");
            }
        }

        // Status icon
        int menuStatus = cursor.getInt(7);
        int syncedTakenAmount = cursor.getInt(8);
        int syncedReservedAmount = cursor.getInt(9);
        int syncedOfferedAmount = cursor.getInt(10);

        @ProviderContract.ActionSyncStatus
        int lowestSyncStatus;
        int lowestReserved;
        int lowestOffered;
        if (!cursor.isNull(13)) {
            lowestSyncStatus = ProviderContract.ACTION_SYNC_STATUS_EDIT;
            lowestReserved = cursor.getInt(13);
            lowestOffered = cursor.getInt(14);
        } else if (!cursor.isNull(11)) {
            lowestSyncStatus = ProviderContract.ACTION_SYNC_STATUS_LOCAL;
            lowestReserved = cursor.getInt(11);
            lowestOffered = cursor.getInt(12);
        } else {
            lowestSyncStatus = ProviderContract.ACTION_SYNC_STATUS_SYNCED;
            lowestReserved = cursor.getInt(9);
            lowestOffered = cursor.getInt(10);
        }

        @Action int onClickAction = getAction(lowestSyncStatus, lowestReserved, lowestOffered,
                syncedReservedAmount, syncedOfferedAmount, syncedTakenAmount, menuStatus, remainingToOrder);

        long date = cursor.getLong(DATE_COLUMN_INDEX);
        if (date < MenuUtils.getTodayDate()) {
            onClickAction = ACTION_DISABLED;
        }

        showIcon(viewHolder, onClickAction != ACTION_DISABLED, lowestSyncStatus, lowestReserved,
                lowestOffered, syncedTakenAmount);

        // OnClick listener
        long relativeId = cursor.getLong(15);
        long portalId = cursor.getLong(PORTAL_COLUMN_INDEX);
        MenuEntryInfo menuInfo = new MenuEntryInfo(relativeId, portalId, onClickAction,
                syncedReservedAmount, syncedOfferedAmount);
        viewHolder.itemView.setTag(menuInfo);

        viewHolder.itemView.setOnClickListener(mOnClickListener);
        viewHolder.itemView.setOnLongClickListener(mOnLongClickListener);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        // Fix toolbar scroll issue on older Android versions
        if (Build.VERSION.SDK_INT < 21) {
            recyclerView.setNestedScrollingEnabled(false);
        }
        mRecyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);

        mRecyclerView = null;
    }

    // -------------------------------------------------------------------------------------------
    // List item actions and icons
    // -------------------------------------------------------------------------------------------

    @Action
    private int getAction(@ProviderContract.ActionSyncStatus int lowestSyncStatus, int lowestReserved,
                          int lowestOffered, int syncedReserved, int syncedOffered, int syncedTaken,
                          @PublicProviderContract.MenuStatus int menuStatus, int toOrder) {
        int lowestEditableCount = lowestReserved - syncedTaken;
        int syncedEditableCount = syncedReserved - syncedTaken;

        if (lowestEditableCount > 1 || syncedEditableCount > 1) {
            boolean canOrderNew = (menuStatus & ProviderContract.MENU_STATUS_ORDERABLE) == ProviderContract.MENU_STATUS_ORDERABLE;
            boolean canCancelOld = (menuStatus & ProviderContract.MENU_STATUS_CANCELABLE) == ProviderContract.MENU_STATUS_CANCELABLE;
            boolean canUsesStock = (menuStatus & ProviderContract.MENU_STATUS_COULD_USE_STOCK) == ProviderContract.MENU_STATUS_COULD_USE_STOCK;

            if(canCancelOld || canOrderNew || canUsesStock) {
                return ACTION_SHOW_DETAIL;
            } else {
                return ACTION_DISABLED;
            }
        }

        switch (lowestSyncStatus) {
            case ProviderContract.ACTION_SYNC_STATUS_EDIT:
                // If actual edit hides local values with synced (usually after previous ACTION_CANCEL_EDIT)
                if (lowestReserved == syncedReserved && lowestOffered == syncedOffered) {
                    // Get same action as in synced
                    return getActionFromSynced(syncedReserved, syncedOffered, syncedTaken, menuStatus, toOrder);
                } else {
                    return ACTION_CANCEL_EDIT;
                }
            case ProviderContract.ACTION_SYNC_STATUS_LOCAL:
                // This cannot "hide" synced (because of app sync algorithm) so I can only return back to synced
                return ACTION_CANCEL_EDIT;
            case ProviderContract.ACTION_SYNC_STATUS_SYNCED: {
                // The most complicated state
                if (BuildConfig.DEBUG) {
                    Assert.that(syncedReserved == lowestReserved && syncedOffered == lowestOffered,
                            "Lowest and synced values should be same");
                }
                return getActionFromSynced(syncedReserved, syncedOffered, syncedTaken, menuStatus, toOrder);
            }
            case PublicProviderContract.ACTION_SYNC_STATUS_FAILED:
            default:
                throw new UnsupportedOperationException("Unsupported operation " + lowestSyncStatus);
        }
    }

    @Action
    private int getActionFromSynced(int syncedReservedAmount, int syncedOfferedAmount, int syncedTakenAmount,
                                    @PublicProviderContract.MenuStatus int menuStatus, int remainingToOrder) {
        int editableCount = syncedReservedAmount - syncedTakenAmount;

        boolean canOrderNew = (menuStatus & ProviderContract.MENU_STATUS_ORDERABLE) == ProviderContract.MENU_STATUS_ORDERABLE;
        boolean canCancelOld = (menuStatus & ProviderContract.MENU_STATUS_CANCELABLE) == ProviderContract.MENU_STATUS_CANCELABLE;
        boolean canUsesStock = (menuStatus & ProviderContract.MENU_STATUS_COULD_USE_STOCK) == ProviderContract.MENU_STATUS_COULD_USE_STOCK;

        switch (syncedOfferedAmount) {
            case 0:
                switch (editableCount) {
                    case 0:
                        if (canOrderNew) {
                            return ACTION_RESERVE_NEW;
                        } else if (canUsesStock && remainingToOrder > 0) {
                            return ACTION_RESERVED_FROM_STOCK;
                        } else {
                            return ACTION_DISABLED;
                        }
                    case 1:
                        if (canCancelOld) {
                            return ACTION_CANCEL_OLD;
                        } else if (canUsesStock) {
                            return ACTION_OFFER_IN_STOCK;
                        } else {
                            return ACTION_DISABLED;
                        }
                    default:
                        if(canCancelOld || canOrderNew || canUsesStock) {
                            return ACTION_SHOW_DETAIL;
                        } else {
                            return ACTION_DISABLED;
                        }
                }
            case 1:
                if(canUsesStock) {
                    return ACTION_REMOVE_FROM_STOCK;
                } else {
                    return ACTION_DISABLED;
                }
            default:
                if(canCancelOld || canOrderNew || canUsesStock) {
                    return ACTION_SHOW_DETAIL;
                } else {
                    return ACTION_DISABLED;
                }
        }
    }

    private void showIcon(ViewHolder viewHolder, boolean enabled, @ProviderContract.ActionSyncStatus int lowestSyncStatus,
                          int lowestReservedAmount, int lowestOfferedAmount, int takenAmount) {
        // Set icon's tint
        int tintColor;
        if (enabled) {
            tintColor = ContextCompat.getColor(viewHolder.mAmountImage.getContext(), R.color.colorPrimary);
        } else {
            tintColor = ContextCompat.getColor(viewHolder.mAmountImage.getContext(), R.color.textColor);
        }
        viewHolder.mAmountImage.setColorFilter(tintColor, android.graphics.PorterDuff.Mode.SRC_IN);
        viewHolder.mAmountText.setTextColor(tintColor);

        // Find correct kind of icon
        @IconKind int iconKind;
        if (lowestReservedAmount == 0) {
            iconKind = ICON_KIND_EMPTY;
        } else if (lowestReservedAmount == 1) {
            iconKind = ICON_KIND_SELECT;
        } else {
            iconKind = ICON_KIND_NUMBER;
        }

        if (lowestOfferedAmount > 0) {
            iconKind = ICON_KIND_STOCK;
        }

        if (lowestReservedAmount == takenAmount && takenAmount > 0) {
            iconKind = ICON_KIND_WITHDRAWAL;
        }

        // Show icon
        if (iconKind < ICON_RES[0].length) {
            viewHolder.mAmountImage.setImageResource(ICON_RES[lowestSyncStatus - ProviderContract.ACTION_SYNC_STATUS_EDIT][iconKind]);
            viewHolder.mAmountText.setText("");
        } else if (iconKind == ICON_KIND_NUMBER) {
            if (lowestSyncStatus < ProviderContract.ACTION_SYNC_STATUS_SYNCED) {
                viewHolder.mAmountImage.setImageResource(R.drawable.ic_menu_empty);
            } else {
                viewHolder.mAmountImage.setImageResource(R.drawable.ic_menu_full);
                viewHolder.mAmountText.setTextColor(Color.WHITE);
            }
            viewHolder.mAmountText.setText(String.valueOf(lowestReservedAmount));
        } else if (iconKind == ICON_KIND_WITHDRAWAL) {
            viewHolder.mAmountImage.setImageResource(R.drawable.ic_menu_withdrawal);
            viewHolder.mAmountText.setText("");
        } else {
            throw new UnsupportedOperationException("Unknown icon kind " + iconKind);
        }
    }

    // -------------------------------------------------------------------------------------------
    // View Holder
    // -------------------------------------------------------------------------------------------

    /**
     * Standard {@link RecyclerView.ViewHolder} that stores one menu entry in UI
     */
    class ViewHolder extends RecyclerView.ViewHolder {
        final TextView mLabel;
        final TextView mPrice;
        final TextView mRemaining;
        final TextView mText;
        // IconKind
        final ImageView mAmountImage;
        final TextView mAmountText;

        ViewHolder(View view) {
            super(view);

            mLabel = view.findViewById(R.id.menu_name_text);
            mPrice = view.findViewById(R.id.menu_price_text);
            mRemaining = view.findViewById(R.id.menu_remaining_text);
            mText = view.findViewById(R.id.menu_text_text);
            // IconKind
            mAmountImage = view.findViewById(R.id.menu_amount_image);
            mAmountText = view.findViewById(R.id.menu_amount_text);
        }
    }

    /**
     * Menu entry model that stores extra information about entry
     */
    class MenuEntryInfo {
        final long relativeId;
        final long portalId;
        @Action
        final int onClickAction;
        final int reservedCount;
        final int offeredCount;

        MenuEntryInfo(long relativeId, long portalId, @Action int onClickAction,
                      int reservedCount, int offeredCount) {
            this.relativeId = relativeId;
            this.portalId = portalId;
            this.onClickAction = onClickAction;
            this.reservedCount = reservedCount;
            this.offeredCount = offeredCount;
        }
    }
}
