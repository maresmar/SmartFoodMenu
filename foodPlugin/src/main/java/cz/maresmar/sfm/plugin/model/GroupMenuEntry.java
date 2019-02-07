package cz.maresmar.sfm.plugin.model;

import android.database.Cursor;
import androidx.annotation.NonNull;

import cz.maresmar.sfm.plugin.controller.ObjectHandler;
import cz.maresmar.sfm.plugin.controller.ProviderColumn;
import cz.maresmar.sfm.provider.PublicProviderContract;

/**
 * GroupMenuEntry model, this class stores menu info that could be valid only for group of users
 * <p>
 * This class is related to {@link cz.maresmar.sfm.provider.PublicProviderContract.GroupMenuEntry} view
 * </p>
 *
 * @see MenuEntry
 */
public class GroupMenuEntry {
    // Id entries
    // portalId will be used from Uri
    @ProviderColumn(name = PublicProviderContract.GroupMenuEntry.ME_RELATIVE_ID, id = true)
    final public long menuEntryRelativeId;

    @ProviderColumn(name = PublicProviderContract.GroupMenuEntry.GROUP_ID, id = true)
    final public long credentialGroupId;

    // Data entries
    @ProviderColumn(name = PublicProviderContract.GroupMenuEntry.PRICE)
    public int price = PublicProviderContract.NO_INFO;

    @ProviderColumn(name = PublicProviderContract.GroupMenuEntry.STATUS)
    public @PublicProviderContract.MenuStatus
    int menuStatus;

    /**
     * Creates new GroupMenuEntry
     *
     * @param menuEntryRelativeId ID of menu entry
     * @param credentialGroupId   ID of credentials group
     */
    public GroupMenuEntry(long menuEntryRelativeId, long credentialGroupId) {
        this.menuEntryRelativeId = menuEntryRelativeId;
        this.credentialGroupId = credentialGroupId;
    }

    /**
     * Class that creates new {@link GroupMenuEntry} from {@link Cursor}'s row using
     * {@link GroupMenuEntry}'s constructor
     */
    public static class Initializer extends ObjectHandler.Initializer<GroupMenuEntry> {

        public Initializer() {
            super(GroupMenuEntry.class);
        }

        @Override
        public @NonNull
        GroupMenuEntry newInstance(@NonNull Cursor cursor) {
            int meRelIdColId = cursor.getColumnIndex(PublicProviderContract.GroupMenuEntry.ME_RELATIVE_ID);
            int groupIdColId = cursor.getColumnIndex(PublicProviderContract.GroupMenuEntry.GROUP_ID);

            return new GroupMenuEntry(cursor.getLong(meRelIdColId), cursor.getLong(groupIdColId));
        }

        @Override
        public @NonNull
        Class<GroupMenuEntry> getClass(@NonNull Cursor cursor) {
            return GroupMenuEntry.class;
        }
    }
}
