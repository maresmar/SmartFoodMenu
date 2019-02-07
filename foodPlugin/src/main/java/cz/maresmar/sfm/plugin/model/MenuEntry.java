package cz.maresmar.sfm.plugin.model;

import android.database.Cursor;
import androidx.annotation.NonNull;

import java.util.HashMap;

import cz.maresmar.sfm.plugin.controller.ObjectHandler;
import cz.maresmar.sfm.plugin.controller.ProviderColumn;
import cz.maresmar.sfm.provider.PublicProviderContract;

/**
 * MenuEntry model
 * <p>
 * This class is related to {@link cz.maresmar.sfm.provider.PublicProviderContract.MenuEntry} view
 * </p>
 */
public class MenuEntry {
    // Id entries
    // portalId will be used from Uri
    @ProviderColumn(name = PublicProviderContract.MenuEntry.ME_RELATIVE_ID, id = true)
    public final long relativeId;

    // Data entries
    @ProviderColumn(name = PublicProviderContract.MenuEntry.TEXT)
    public String text;

    @ProviderColumn(name = PublicProviderContract.MenuEntry.GROUP)
    public String group;

    @ProviderColumn(name = PublicProviderContract.MenuEntry.LABEL)
    public String label;

    @ProviderColumn(name = PublicProviderContract.MenuEntry.DATE)
    public long date;

    @ProviderColumn(name = PublicProviderContract.MenuEntry.REMAINING_TO_TAKE)
    public int remainingToTake = PublicProviderContract.NO_INFO;

    @ProviderColumn(name = PublicProviderContract.MenuEntry.REMAINING_TO_ORDER)
    public int remainingToOrder = PublicProviderContract.NO_INFO;

    @ProviderColumn(name = PublicProviderContract.MenuEntry.EXTRA)
    public String extra;

    /**
     * Creates new menu entry
     *
     * @param relativeId ID of menu entry that have to be unique for one portal
     */
    public MenuEntry(long relativeId) {
        this.relativeId = relativeId;
    }

    /**
     * Class that creates new {@link MenuEntry} from {@link Cursor}'s row using {@link MenuEntry}'s constructor
     */
    public static class Initializer extends ObjectHandler.Initializer<MenuEntry> {

        // Workaround https://issuetracker.google.com/issues/37052343
        private HashMap<String, Integer> mColMap;

        public Initializer() {
            super(MenuEntry.class);

            mColMap = getColumnMap();
        }

        @Override
        public @NonNull
        MenuEntry newInstance(@NonNull Cursor cursor) {
            int relIdColId = mColMap.get(PublicProviderContract.MenuEntry.ME_RELATIVE_ID);
            return new MenuEntry(cursor.getLong(relIdColId));
        }

        @Override
        public @NonNull
        Class<MenuEntry> getClass(@NonNull Cursor cursor) {
            return MenuEntry.class;
        }
    }
}
