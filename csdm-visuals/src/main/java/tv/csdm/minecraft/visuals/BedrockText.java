package tv.csdm.minecraft.visuals;

import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;

/** Replace only the decorative glyphs implicated by the Java/Bedrock comparison. */
final class BedrockText {
    private static final TextReplacementConfig SEPARATORS = TextReplacementConfig.builder()
            .match(Pattern.compile("[✦•]"))
            .replacement((match, builder) -> builder.content(match.group().equals("✦") ? "*" : "|"))
            .build();

    private BedrockText() { }

    static Component forViewer(Component text, boolean bedrock) {
        return text == null || !bedrock ? text : text.replaceText(SEPARATORS);
    }
}
