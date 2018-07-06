package cz.maresmar.sfm.plugin.model;


import android.database.Cursor;
import android.support.annotation.NonNull;

import java.util.HashMap;

import cz.maresmar.sfm.plugin.controller.ObjectHandler;
import cz.maresmar.sfm.plugin.controller.ProviderColumn;
import cz.maresmar.sfm.provider.PublicProviderContract;
import cz.maresmar.sfm.provider.PublicProviderContract.ActionSyncStatus;

import static cz.maresmar.sfm.provider.PublicProviderContract.ACTION_ENTRY_TYPE_PAYMENT;
import static cz.maresmar.sfm.provider.PublicProviderContract.ACTION_ENTRY_TYPE_STANDARD;
import static cz.maresmar.sfm.provider.PublicProviderContract.ACTION_ENTRY_TYPE_VIRTUAL;
import static cz.maresmar.sfm.provider.PublicProviderContract.ActionEntryType;

/**
 * Action model
 * <p>
 * This class is related to {@link PublicProviderContract.Action} view
 * </p>
 */
public abstract class Action {
    // Id entries
    @ProviderColumn(name = PublicProviderContract.Action._ID)
    final public long internalId;

    // credentialId will be used from Uri
    @ProviderColumn(name = PublicProviderContract.Action.ACTION_RELATIVE_ID, id = true)
    final public long actionRelativeId;

    // Data entries
    @ProviderColumn(name = PublicProviderContract.Action.ENTRY_TYPE)
    public @ActionEntryType
    int entryType;

    @ProviderColumn(name = PublicProviderContract.Action.RESERVED_AMOUNT)
    public int reservedAmount = 1;

    @ProviderColumn(name = PublicProviderContract.Action.OFFERED_AMOUNT)
    public int offeredAmount = 0;

    @ProviderColumn(name = PublicProviderContract.Action.TAKEN_AMOUNT)
    public int takenAmount = 0;

    /**
     * Be aware of changing it, it's read-only value
     */
    @ProviderColumn(name = PublicProviderContract.Action.SYNCED_RESERVED_AMOUNT, ro = true, zeroOnNull = true)
    public int syncedReservedAmount = PublicProviderContract.NO_INFO;

    /**
     * Be aware of changing it, it's read-only value
     */
    @ProviderColumn(name = PublicProviderContract.Action.SYNCED_OFFERED_AMOUNT, ro = true, zeroOnNull = true)
    public int syncedOfferedAmount = PublicProviderContract.NO_INFO;

    @ProviderColumn(name = PublicProviderContract.Action.LAST_CHANGE)
    public long lastChange = ObjectHandler.EXCLUDED;

    @ProviderColumn(name = PublicProviderContract.Action.SYNC_STATUS)
    public @ActionSyncStatus
    int syncStatus = PublicProviderContract.ACTION_SYNC_STATUS_SYNCED;

    @ProviderColumn(name = PublicProviderContract.Action.DESCRIPTION)
    public String description;

    @ProviderColumn(name = PublicProviderContract.Action.PRICE)
    public int price = PublicProviderContract.NO_INFO;


    private Action(long actionRelativeId) {
        this(ObjectHandler.EXCLUDED, actionRelativeId);
    }

    private Action(long internalId, long actionRelativeId) {
        this.internalId = internalId;
        this.actionRelativeId = actionRelativeId;
    }

    /**
     * Action that has {@link Action#entryType} {@link PublicProviderContract#ACTION_ENTRY_TYPE_STANDARD}
     * meaning it is order
     */
    public static class MenuEntryAction extends Action {
        @ProviderColumn(name = PublicProviderContract.Action.ME_RELATIVE_ID)
        final public long relativeMenuEntryId;

        @ProviderColumn(name = PublicProviderContract.Action.ME_PORTAL_ID)
        final public long portalId;

        @ProviderColumn(name = PublicProviderContract.Action.ME_EXTRA, ro = true)
        public String extra;

        /**
         * Creates new order
         * @param actionId ID of action that is unique for one credentials
         * @param relativeMenuEntryId ID of menu entry
         * @param portalId ID of portal
         */
        public MenuEntryAction(long actionId, long relativeMenuEntryId, long portalId) {
            super(actionId);
            this.relativeMenuEntryId = relativeMenuEntryId;
            this.portalId = portalId;
            this.entryType = PublicProviderContract.ACTION_ENTRY_TYPE_STANDARD;
        }

        /**
         * Creates new order
         * @param internalId ID of row used in app's database
         * @param actionId ID of action that is unique for one credentials
         * @param relativeMenuEntryId ID of menu entry
         * @param portalId ID of portal
         */
        private MenuEntryAction(long internalId, long actionId, long relativeMenuEntryId, long portalId) {
            super(internalId, actionId);
            this.relativeMenuEntryId = relativeMenuEntryId;
            this.portalId = portalId;
            this.entryType = PublicProviderContract.ACTION_ENTRY_TYPE_STANDARD;
        }
    }

    /**
     * Action that has {@link Action#entryType} {@link PublicProviderContract#ACTION_ENTRY_TYPE_PAYMENT}
     * meaning it is payment log
     */
    public static class PaymentAction extends Action {

        /**
         * Creates new payment
         * @param actionId ID of action that is unique for one credentials
         * @param description Description of payment
         * @param price Price of payment
         */
        public PaymentAction(long actionId, String description, int price) {
            super(actionId);
            this.description = description;
            this.price = price;
            this.entryType = PublicProviderContract.ACTION_ENTRY_TYPE_PAYMENT;
        }
    }

    /**
     * Class that creates new {@link Action} from {@link Cursor}'s row using {@link Action}'s constructor
     */
    public static class Initializer extends ObjectHandler.Initializer<Action> {

        // Workaround https://issuetracker.google.com/issues/37052343
        private HashMap<String, Integer> mColMap;

        public Initializer() {
            super(null);

            mColMap = getColumnMap();
        }

        @Override
        public @NonNull
        Action newInstance(@NonNull Cursor cursor) {
            int typeColId = mColMap.get(PublicProviderContract.Action.ENTRY_TYPE);
            @ActionEntryType int type = cursor.getInt(typeColId);

            int actionRelIdColId = mColMap.get(PublicProviderContract.Action.ACTION_RELATIVE_ID);
            long actionRelId = cursor.getLong(actionRelIdColId);

            int idColId = mColMap.get(PublicProviderContract.Action._ID);
            long _id = cursor.getLong(idColId);

            switch (type) {
                case ACTION_ENTRY_TYPE_STANDARD: {
                    int relMenuIdColId = mColMap.get(PublicProviderContract.Action.ME_RELATIVE_ID);
                    int portalIdColId = mColMap.get(PublicProviderContract.Action.ME_PORTAL_ID);

                    return new MenuEntryAction(_id, actionRelId, cursor.getLong(relMenuIdColId),
                            cursor.getLong(portalIdColId));
                }
                case ACTION_ENTRY_TYPE_PAYMENT: {
                    int desColId = mColMap.get(PublicProviderContract.Action.DESCRIPTION);
                    int priceColId = mColMap.get(PublicProviderContract.Action.PRICE);

                    return new PaymentAction(actionRelId, cursor.getString(desColId),
                            cursor.getInt(priceColId));
                }
                case ACTION_ENTRY_TYPE_VIRTUAL: // Should be already filtered in select
                default:
                    throw new UnsupportedOperationException("Unsupported order type " + type);
            }
        }

        @Override
        public @NonNull
        Class<? extends Action> getClass(@NonNull Cursor cursor) {
            int typeColId = mColMap.get(PublicProviderContract.Action.ENTRY_TYPE);
            @ActionEntryType int type = cursor.getInt(typeColId);

            switch (type) {
                case ACTION_ENTRY_TYPE_STANDARD:
                    return MenuEntryAction.class;
                case ACTION_ENTRY_TYPE_PAYMENT:
                    return PaymentAction.class;
                case ACTION_ENTRY_TYPE_VIRTUAL: // Should be already filtered in select
                default:
                    throw new UnsupportedOperationException("Unsupported order type " + type);
            }
        }

        @Override
        public @NonNull
        String[] getInflateProjection() {
            return new String[]{
                    PublicProviderContract.Action._ID,
                    PublicProviderContract.Action.ACTION_RELATIVE_ID,
                    PublicProviderContract.Action.ENTRY_TYPE,
                    PublicProviderContract.Action.RESERVED_AMOUNT,
                    PublicProviderContract.Action.OFFERED_AMOUNT,
                    PublicProviderContract.Action.TAKEN_AMOUNT,
                    PublicProviderContract.Action.SYNCED_RESERVED_AMOUNT,
                    PublicProviderContract.Action.SYNCED_OFFERED_AMOUNT,
                    PublicProviderContract.Action.LAST_CHANGE,
                    PublicProviderContract.Action.SYNC_STATUS,
                    PublicProviderContract.Action.DESCRIPTION,
                    PublicProviderContract.Action.PRICE,
                    PublicProviderContract.Action.ME_RELATIVE_ID,
                    PublicProviderContract.Action.ME_PORTAL_ID,
                    PublicProviderContract.Action.ME_EXTRA
            };
        }
    }

}
