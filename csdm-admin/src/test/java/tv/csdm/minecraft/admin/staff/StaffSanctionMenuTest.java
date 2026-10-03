package tv.csdm.minecraft.admin.staff;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tv.csdm.minecraft.admin.moderation.Sanction;
import tv.csdm.minecraft.admin.moderation.SanctionType;

class StaffSanctionMenuTest {
    private static final Instant NOW = Instant.parse("2026-09-20T12:00:00Z");
    private static final UUID PLAYER = UUID.randomUUID();

    @Test
    void activeFilterMatchesEnforcementAndStatesDistinguishCompletedActions() {
        Sanction warning = sanction(PLAYER, SanctionType.WARNING, false, null, 1);
        Sanction kick = sanction(PLAYER, SanctionType.KICK, false, null, 2);
        Sanction expired = sanction(PLAYER, SanctionType.TEMPBAN, true, NOW, 3);
        Sanction active = sanction(PLAYER, SanctionType.TEMPBAN, true, NOW.plusSeconds(1), 4);
        Sanction ban = sanction(PLAYER, SanctionType.BAN, true, null, 5);
        Sanction withdrawn = sanction(PLAYER, SanctionType.BAN, false, null, 6);
        Sanction pardon = sanction(PLAYER, SanctionType.PARDON, false, null, 7);
        List<Sanction> all = List.of(warning, kick, expired, active, ban, withdrawn, pardon);
        assertEquals(List.of(ban, active), StaffSanctionMenu.paginate(all, null, true, 0, NOW).entries());
        assertEquals("Finalizada", StaffSanctionMenu.status(expired, NOW));
        assertEquals("Activa", StaffSanctionMenu.status(active, NOW));
        assertEquals("Retirada", StaffSanctionMenu.status(withdrawn, NOW));
        assertEquals("Advertencia registrada", StaffSanctionMenu.status(warning, NOW));
        assertEquals("Expulsión ejecutada", StaffSanctionMenu.status(kick, NOW));
        assertEquals("Perdón aplicado", StaffSanctionMenu.status(pardon, NOW));
        assertEquals(7, StaffSanctionMenu.paginate(all, PLAYER, false, 0, NOW).count());
    }

    @Test
    void historyIsNewestFirstPaginatedAndFilteredByUuidEvenAcrossNameChanges() {
        List<Sanction> history = new ArrayList<>();
        for (int i = 0; i < 96; i++) history.add(sanction(PLAYER, SanctionType.WARNING, false, null, i));
        history.add(sanction(UUID.randomUUID(), SanctionType.BAN, true, null, 999));
        List<Sanction> seen = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            var page = StaffSanctionMenu.paginate(history, PLAYER, false, i, NOW);
            assertEquals(3, page.total());
            assertEquals(96, page.count());
            seen.addAll(page.entries());
        }
        assertEquals(96, seen.size());
        assertEquals(96, seen.stream().map(Sanction::id).distinct().count());
        assertEquals(history.get(95), seen.getFirst());
        assertEquals(history.getFirst(), seen.getLast());
        var empty = StaffSanctionMenu.paginate(history, UUID.randomUUID(), false, 999, NOW);
        assertEquals(0, empty.number());
        assertEquals(1, empty.total());
        assertTrue(empty.entries().isEmpty());
        assertEquals(0, StaffSanctionMenu.paginate(history, PLAYER, false, -1, NOW).number());
        assertEquals(2, StaffSanctionMenu.paginate(history, PLAYER, false, 999, NOW).number());
    }

    @Test
    void longReasonsAreReadableWithoutDroppingWordsOrInterpretingMarkup() {
        String reason = "<red>Texto literal</red> " + "motivo ".repeat(30);
        List<String> lines = StaffSanctionMenu.wrap(reason);
        assertTrue(lines.stream().allMatch(line -> line.length() <= 48));
        assertEquals(reason.strip(), String.join(" ", lines));
        assertEquals("x".repeat(240), String.join("", StaffSanctionMenu.wrap("x".repeat(240))));
    }

    private static Sanction sanction(UUID target, SanctionType type, boolean active, Instant expires, int sequence) {
        return new Sanction(UUID.randomUUID(), target, "Nombre" + sequence, UUID.randomUUID(), "Staff", type,
                "Motivo", NOW.minusSeconds(1000).plusSeconds(sequence), expires, active, false);
    }
}
