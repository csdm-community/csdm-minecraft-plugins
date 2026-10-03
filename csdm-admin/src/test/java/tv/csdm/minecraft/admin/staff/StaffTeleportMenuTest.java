package tv.csdm.minecraft.admin.staff;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class StaffTeleportMenuTest {
    @Test
    void groupsByPermissionAndExcludesSelfOfflineAndHiddenPlayers() {
        Player viewer = player("viewer");
        Player regular = player("regular");
        Player staff = player("staff");
        Player priority = player("priority");
        Player hidden = player("hidden");
        Player offline = player("offline");
        when(viewer.canSee(any(Player.class))).thenReturn(true);
        when(viewer.canSee(hidden)).thenReturn(false);
        when(offline.isOnline()).thenReturn(false);
        when(staff.hasPermission("csdm.staffmode.use")).thenReturn(true);
        when(priority.hasPermission(StaffTeleportMenu.PRIORITY_PERMISSION)).thenReturn(true);
        var page = StaffTeleportMenu.paginate(viewer, List.of(viewer, regular, staff, priority, hidden, offline), 0);
        assertEquals(List.of(priority, staff), page.staff());
        assertEquals(List.of(regular), page.players());
        assertEquals(1, page.total());
    }

    @Test
    void pagesKeepBothGroupsWithoutLosingOrDuplicatingPlayers() {
        Player viewer = player("viewer");
        when(viewer.canSee(any(Player.class))).thenReturn(true);
        List<Player> staff = new ArrayList<>();
        List<Player> regular = new ArrayList<>();
        for (int i = 0; i < 41; i++) {
            Player target = player(String.format("Player%02d", i));
            regular.add(target);
            if (i < 20) {
                Player moderator = player(String.format("Staff%02d", i));
                when(moderator.hasPermission("csdm.staffmode.use")).thenReturn(true);
                staff.add(moderator);
            }
        }
        List<Player> all = new ArrayList<>(regular);
        all.addAll(staff);
        Collections.reverse(all);
        List<Player> seenStaff = new ArrayList<>();
        List<Player> seenRegular = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            var page = StaffTeleportMenu.paginate(viewer, all, i);
            assertEquals(3, page.total());
            assertTrue(page.staff().size() <= 18);
            assertTrue(page.players().size() <= 18);
            seenStaff.addAll(page.staff());
            seenRegular.addAll(page.players());
        }
        assertEquals(staff, seenStaff);
        assertEquals(regular, seenRegular);
        assertEquals(2, StaffTeleportMenu.paginate(viewer, all, 999).number());
        assertEquals(0, StaffTeleportMenu.paginate(viewer, all, -1).number());
        var empty = StaffTeleportMenu.paginate(viewer, List.of(), 2);
        assertEquals(0, empty.number());
        assertEquals(1, empty.total());
        assertTrue(empty.staff().isEmpty() && empty.players().isEmpty());
    }

    private static Player player(String name) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        return player;
    }
}
