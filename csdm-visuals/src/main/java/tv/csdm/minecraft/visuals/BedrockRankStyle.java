package tv.csdm.minecraft.visuals;

import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/** Bedrock presentation of CSDM labels, without changing Java or account identity. */
final class BedrockRankStyle {
    private static final Map<String, NamedTextColor> COLORS = Map.of(
            "USUARIO", NamedTextColor.GRAY,
            "PERSONALIDAD", NamedTextColor.GOLD,
            "MOD", NamedTextColor.BLUE,
            "COORDINACIÓN", NamedTextColor.AQUA,
            "DIRECCIÓN", NamedTextColor.AQUA,
            "ÉLITE", NamedTextColor.AQUA,
            "LEYENDA", NamedTextColor.LIGHT_PURPLE,
            "INMORTAL", NamedTextColor.GOLD);

    private BedrockRankStyle() { }

    static Component display(Component component) {
        if (component == null) return null;
        String label = plain(component).trim().replaceFirst("^[•|]\\s*", "")
                .replaceFirst("\\s*[•|]$", "").trim();
        NamedTextColor color = COLORS.get(label);
        if (color != null) return uniform(component, color);
        return component.children(component.children().stream().map(BedrockRankStyle::display).toList());
    }

    static Component name(Component component) {
        return component == null ? null : display(component.colorIfAbsent(NamedTextColor.WHITE)
                .decoration(TextDecoration.BOLD, false).decoration(TextDecoration.ITALIC, false));
    }

    static Component systemMessage(Component component) {
        if (component == null) return null;
        String text = plain(component);
        // Only the CSDM rank entrance template; never recolor arbitrary chat bodies.
        for (var entry : COLORS.entrySet()) {
            if (text.startsWith("✦ " + entry.getKey() + " · ")
                    || text.startsWith("* " + entry.getKey() + " · ")) {
                return uniform(component, entry.getValue());
            }
        }
        return component;
    }

    private static Component uniform(Component component, NamedTextColor color) {
        return component.color(color).decoration(TextDecoration.BOLD, false)
                .decoration(TextDecoration.ITALIC, false)
                .children(component.children().stream().map(child -> uniform(child, color)).toList());
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
