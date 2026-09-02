package tv.csdm.minecraft.community.medal;

import java.util.UUID;

public interface MedalRepository {
    MedalProfile find(UUID playerUuid);

    boolean grant(UUID playerUuid, String medalId);

    boolean revoke(UUID playerUuid, String medalId);

    boolean feature(UUID playerUuid, String medalId);
}

