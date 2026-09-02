package tv.csdm.minecraft.community.staff;

import java.util.List;

public record StaffRankDefinition(
        String id,
        String group,
        String displayName,
        String prefix,
        int priority,
        List<String> permissions) {
    public StaffRankDefinition {
        permissions = List.copyOf(permissions);
    }
}

