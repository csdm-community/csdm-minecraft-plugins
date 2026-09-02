package tv.csdm.minecraft.community.medal;

import java.util.Set;

public record MedalProfile(Set<String> unlocked, String featured) {
    public MedalProfile {
        unlocked = Set.copyOf(unlocked);
    }
}

