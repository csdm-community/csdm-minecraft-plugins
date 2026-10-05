package tv.csdm.minecraft.visuals;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BedrockRankStyleTest {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    @Test void directionGradientBecomesUniformWithoutLosingAccentOrClick() {
        Component original = MM.deserialize("<gradient:#00E5FF:#9B5CFF><bold>DIRECCIÓN</bold></gradient>")
                .clickEvent(ClickEvent.suggestCommand("/rangos ver 7245"));
        Component bedrock = BedrockRankStyle.display(original);
        assertUniform(bedrock, NamedTextColor.AQUA);
        assertEquals("DIRECCIÓN", plain(bedrock));
        assertEquals(original.clickEvent(), bedrock.clickEvent());
        assertNotEquals(original, bedrock);
        assertEquals(bedrock, BedrockRankStyle.display(bedrock));
        assertSame(original, BedrockText.forViewer(original, false));
    }

    @Test void displayKeepsUsernameAndUsesSeparateFunctionalAndPrestigeColors() {
        Component functional = MM.deserialize("<gradient:#FFD166:#FF62C7><bold>PERSONALIDAD</bold></gradient> <dark_gray>•</dark_gray> ");
        Component prestige = MM.deserialize("<light_purple><bold>LEYENDA</bold></light_purple> <dark_gray>•</dark_gray> ");
        Component original = Component.empty().append(functional).append(prestige).append(Component.text(".nsntx"));
        Component bedrock = BedrockRankStyle.name(original);
        assertEquals(plain(original), plain(bedrock));
        assertUniform(bedrock.children().get(0), NamedTextColor.GOLD);
        assertUniform(bedrock.children().get(1), NamedTextColor.LIGHT_PURPLE);
        assertEquals(".nsntx", plain(bedrock.children().get(2)));
        assertEquals(NamedTextColor.WHITE, bedrock.color());
        assertEquals(TextDecoration.State.FALSE, bedrock.decoration(TextDecoration.ITALIC));
        assertEquals(functional, original.children().get(0));
    }

    @Test void handlesLeadingPrestigeSeparatorAndLeavesCustomLabelsAlone() {
        Component suffix = MM.deserialize("<dark_gray> • </dark_gray><aqua><bold>ÉLITE</bold></aqua>");
        assertUniform(BedrockRankStyle.display(suffix), NamedTextColor.AQUA);
        Component custom = MM.deserialize("<gradient:#FF0000:#0000FF>INVITADO</gradient>");
        assertEquals(custom, BedrockRankStyle.display(custom));
        assertNull(BedrockRankStyle.name(null));
    }

    @Test void simplifiesOnlyRankEntranceMessagesNotOrdinaryChatBodies() {
        Component entrance = MM.deserialize("<gradient:#FFD166:#FF62C7><bold>✦ PERSONALIDAD · .nsntx visita el Archivo.</bold></gradient>");
        assertUniform(BedrockRankStyle.systemMessage(entrance), NamedTextColor.GOLD);
        assertEquals(plain(entrance), plain(BedrockRankStyle.systemMessage(entrance)));
        Component body = MM.deserialize("<red><bold>PERSONALIDAD es mi rango</bold></red>");
        assertSame(body, BedrockRankStyle.systemMessage(body));
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static void assertUniform(Component component, NamedTextColor color) {
        if (component instanceof TextComponent text && !text.content().isEmpty()) {
            assertEquals(color, component.color());
            assertEquals(TextDecoration.State.FALSE, component.decoration(TextDecoration.BOLD));
            assertEquals(TextDecoration.State.FALSE, component.decoration(TextDecoration.ITALIC));
        }
        component.children().forEach(child -> assertUniform(child, color));
    }
}
