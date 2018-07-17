package cz.maresmar.sfm.plugin.model;

import android.database.Cursor;
import android.support.annotation.NonNull;

import java.util.HashMap;

import cz.maresmar.sfm.plugin.controller.ObjectHandler;
import cz.maresmar.sfm.plugin.controller.ProviderColumn;
import cz.maresmar.sfm.provider.PublicProviderContract;
import cz.maresmar.sfm.provider.PublicProviderContract.SecurityType;

import static cz.maresmar.sfm.provider.PublicProviderContract.PortalFeatures;

/**
 * LogData model
 * <p>
 * This class is related to {@link cz.maresmar.sfm.provider.PublicProviderContract.LogData} view
 * </p>
 */
public class LogData {

    @ProviderColumn(name = PublicProviderContract.LogData.PORTAL_ID, id = true, ro = true)
    public final long portalId;

    @ProviderColumn(name = PublicProviderContract.LogData.CREDENTIAL_ID, id = true, ro = true)
    public final long credentialId;

    // Credential
    @ProviderColumn(name = PublicProviderContract.LogData.CREDENTIAL_NAME)
    public String credentialName;

    @ProviderColumn(name = PublicProviderContract.LogData.CREDENTIAL_PASS)
    public String credentialPassword;

    @ProviderColumn(name = PublicProviderContract.LogData.CREDIT)
    public int credit;

    @ProviderColumn(name = PublicProviderContract.LogData.CREDENTIALS_GROUP_ID, ro = true)
    public final long credentialGroupId;

    @ProviderColumn(name = PublicProviderContract.LogData.CREDENTIAL_EXTRA)
    public String credentialExtra;
    // Portal

    @ProviderColumn(name = PublicProviderContract.LogData.PORTAL_NAME)
    public String portalName;

    @ProviderColumn(name = PublicProviderContract.LogData.PORTAL_PLUGIN)
    public String portalPlugin;

    @ProviderColumn(name = PublicProviderContract.LogData.PORTAL_REFERENCE)
    public String portalReference;

    @ProviderColumn(name = PublicProviderContract.LogData.PORTAL_EXTRA)
    public String portalExtra;

    @ProviderColumn(name = PublicProviderContract.LogData.PORTAL_SECURITY)
    public @SecurityType
    int portalSecurity;

    @ProviderColumn(name = PublicProviderContract.LogData.PORTAL_FEATURES)
    public @PortalFeatures
    int portalFeatures;

    /**
     * Creates new LogData model
     *
     * @param portalId          ID of portal
     * @param credentialId      ID of credentials
     * @param credentialGroupId ID of credentials group
     */
    public LogData(long portalId, long credentialId, long credentialGroupId) {
        this.portalId = portalId;
        this.credentialId = credentialId;
        this.credentialGroupId = credentialGroupId;
    }

    /**
     * Class that creates new {@link LogData} from {@link Cursor}'s row using {@link LogData}'s constructor
     */
    public static class LogDataInitializer extends ObjectHandler.Initializer<LogData> {

        // Workaround https://issuetracker.google.com/issues/37052343
        private HashMap<String, Integer> mColMap;

        public LogDataInitializer() {
            super(LogData.class);

            mColMap = getColumnMap();
        }

        @NonNull
        @Override
        public LogData newInstance(@NonNull Cursor cursor) {
            int portalIdColId = mColMap.get(PublicProviderContract.LogData.PORTAL_ID);
            int credentialIdColId = mColMap.get(PublicProviderContract.LogData.CREDENTIAL_ID);
            int credentialGroupColId = mColMap.get(PublicProviderContract.LogData.CREDENTIALS_GROUP_ID);

            return new LogData(cursor.getLong(portalIdColId), cursor.getLong(credentialIdColId), cursor.getLong(credentialGroupColId));
        }

        @NonNull
        @Override
        public Class<LogData> getClass(@NonNull Cursor cursor) {
            return LogData.class;
        }
    }
}
