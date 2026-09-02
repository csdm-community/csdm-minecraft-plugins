package tv.csdm.minecraft.verify.backend;

import java.util.UUID;
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
        String token = "\"" + fieldName + "\":\"";
        int start = body.indexOf(token);
        if (start < 0) {
            return null;
        }
        start += token.length();
        int end = body.indexOf('"', start);
        return end < 0 ? null : unescape(body.substring(start, end));
    }

    static boolean booleanField(String body, String fieldName) {
        if (body == null || body.isBlank()) {
            return false;
        }
        String compact = body.replace(" ", "").replace("\n", "").replace("\r", "").replace("\t", "");
        return compact.contains("\"" + fieldName + "\":true");
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
