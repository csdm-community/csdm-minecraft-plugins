package tv.csdm.minecraft.admin.moderation;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {
    private static final Pattern DURATION = Pattern.compile("^(\\d{1,4})([mhd])$", Pattern.CASE_INSENSITIVE);

    private DurationParser() {
    }

    public static Duration parse(String input) {
        Matcher matcher = DURATION.matcher(input.trim().toLowerCase(Locale.ROOT));
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Usa una duración como 30m, 12h o 7d.");
        }
        long amount = Long.parseLong(matcher.group(1));
        if (amount == 0L) {
            throw new IllegalArgumentException("La duración debe ser mayor que cero.");
        }
        return switch (matcher.group(2)) {
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            case "d" -> Duration.ofDays(amount);
            default -> throw new IllegalArgumentException("Unidad de duración no válida.");
        };
    }
}
