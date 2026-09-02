package tv.csdm.minecraft.verify.backend;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tv.csdm.minecraft.verify.model.VerificationRequest;

final class JsonCodec {
    private JsonCodec() {}

    static String encode(VerificationRequest request) {
        return "{" +
                field("code", request.code()) + "," +
                field("minecraftUuid", request.minecraftUuid().toString()) + "," +
                field("minecraftUsername", request.minecraftUsername()) + "," +
                nullableNumber("clientProtocol", request.clientProtocol()) + "," +
                nullableString("clientVersion", request.clientVersion()) + "," +
                field("serverTimestamp", request.serverTimestamp().toString()) +
                "}";
    }

    static String encodeStatus(UUID minecraftUuid) {
        return "{" + field("action", "status") + "," +
                field("minecraftUuid", minecraftUuid.toString()) + "}";
    }

    static String stringField(String body, String fieldName) {
        if (body == null || body.isBlank()) {
            return null;
        }
        Pattern pattern = Pattern.compile("\\"" + Pattern.quote(fieldName)
                + "\\"\\s*:\\s*\\"((?:\\\\.|[^\\"])*)\\"");
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? unescape(matcher.group(1)) : null;
    }

    static boolean booleanField(String body, String fieldName) {
        if (body == null || body.isBlank()) {
            return false;
        }
        Pattern pattern = Pattern.compile("\\"" + Pattern.quote(fieldName)
                + "\\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(body);
        return matcher.find() && Boolean.parseBoolean(matcher.group(1));
    }

    private static String field(String name, String value) {
        return "\"" + escape(name) + "\":\"" + escape(value) + "\"";
    }

    private static String nullableString(String name, String value) {
        return value == null ? "\"" + escape(name) + "\":null" : field(name, value);
    }

    private static String nullableNumber(String name, Integer value) {
        return "\"" + escape(name) + "\":" + (value == null ? "null" : value);
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private static String unescape(String value) {
        return value.replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\");
    }
}
