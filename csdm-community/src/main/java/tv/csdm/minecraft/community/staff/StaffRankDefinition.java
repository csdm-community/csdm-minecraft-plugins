package tv.csdm.minecraft.community.staff;

import java.util.List;

public record StaffRankDefinition(
        String id,
        String group,
        String displayName,
        String prefix,
        String nametagLabel,
        int priority,
        RankKind kind,
        List<String> inherits,
        List<String> permissions) {
    public StaffRankDefinition {
        inherits = List.copyOf(inherits);
        permissions = List.copyOf(permissions);
    }
}
