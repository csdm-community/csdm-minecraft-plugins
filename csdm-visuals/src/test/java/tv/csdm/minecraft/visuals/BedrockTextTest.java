package tv.csdm.minecraft.visuals;

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.PlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.*;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BedrockTextTest {
    @Test void preservesJavaAndAccentsWhileReplacingOnlyKnownDecorations() {
        Component original = Component.text("✦ DIRECCIÓN • ÉLITE: sesión, pingüino, ñ » ✓");
        assertSame(original, BedrockText.forViewer(original, false));
        assertEquals(Component.text("* DIRECCIÓN | ÉLITE: sesión, pingüino, ñ » ✓"),
                BedrockText.forViewer(original, true).compact());
        assertNull(BedrockText.forViewer(null, true));
    }

    @Test void retainsNestedStylesAndInteractionsWithoutMutatingSharedComponent() {
        Component original = Component.text("✦ ", NamedTextColor.AQUA)
                .append(Component.text("DIRECCIÓN", NamedTextColor.BLUE).decorate(TextDecoration.BOLD))
                .append(Component.text(" • 7245", NamedTextColor.WHITE))
                .clickEvent(ClickEvent.suggestCommand("/rangos ver 7245"));
        Component expected = Component.text("* ", NamedTextColor.AQUA)
                .append(Component.text("DIRECCIÓN", NamedTextColor.BLUE).decorate(TextDecoration.BOLD))
                .append(Component.text(" | 7245", NamedTextColor.WHITE))
                .clickEvent(ClickEvent.suggestCommand("/rangos ver 7245"));
        Component bedrock = BedrockText.forViewer(original, true);
        assertEquals(expected.compact(), bedrock.compact());
        assertSame(original, BedrockText.forViewer(original, false));
        assertEquals(bedrock, BedrockText.forViewer(bedrock, true));
    }

    @Test void copiesTeamInfoAndPreservesVisibilityCollisionAndStyles() {
        var original = new ScoreBoardTeamInfo(Component.text("DIRECCIÓN"),
                Component.text("DIRECCIÓN • ", NamedTextColor.AQUA), Component.text(" • ÉLITE"),
                NameTagVisibility.ALWAYS, CollisionRule.NEVER, NamedTextColor.BLUE, OptionData.ALL);
        var copy = BedrockTextListener.compatibleTeam(original);
        assertNotSame(original, copy);
        assertEquals(Component.text("DIRECCIÓN | ", NamedTextColor.AQUA), copy.getPrefix().compact());
        assertEquals(Component.text("DIRECCIÓN • ", NamedTextColor.AQUA), original.getPrefix());
        assertEquals(original.getTagVisibility(), copy.getTagVisibility());
        assertEquals(original.getCollisionRule(), copy.getCollisionRule());
        assertEquals(original.getColor(), copy.getColor());
        assertEquals(original.getOptionData(), copy.getOptionData());
    }

    @Test void copiesTabEntryWithoutChangingIdentityOrJavaDisplay() {
        UUID identity = UUID.randomUUID();
        var original = new PlayerInfo(identity);
        original.setDisplayName(Component.text("USUARIO • .sepultacion1"));
        original.setLatency(42);
        original.setListed(true);
        original.setListOrder(17);
        var copy = BedrockTextListener.compatibleEntry(original);
        assertNotSame(original, copy);
        assertEquals(identity, copy.getProfileId());
        assertSame(original.getGameProfile(), copy.getGameProfile());
        assertEquals(42, copy.getLatency());
        assertEquals(17, copy.getListOrder());
        assertTrue(copy.isListed());
        assertEquals(Component.text("USUARIO | .sepultacion1"), copy.getDisplayName().compact());
        assertEquals(Component.text("USUARIO • .sepultacion1"), original.getDisplayName());
        original.setDisplayName(null);
        assertNull(BedrockTextListener.compatibleEntry(original).getDisplayName());
    }
}
