package tv.csdm.minecraft.verify.backend;

import java.util.Locale;
import tv.csdm.minecraft.verify.model.VerificationResponse;
import tv.csdm.minecraft.verify.model.VerificationResult;

public final class BackendResponseMapper {
    private BackendResponseMapper() {}

    public static VerificationResponse map(int statusCode, String body) {
        VerificationResult result = switch (statusCode) {
            case 200 -> VerificationResult.VERIFIED;
            case 400 -> VerificationResult.INVALID_CODE;
            case 410 -> VerificationResult.CODE_EXPIRED;
            case 429 -> VerificationResult.RATE_LIMITED;
            case 409 -> mapConflict(body);
            default -> VerificationResult.SERVER_ERROR;
        };
        return new VerificationResponse(result, statusCode);
    }

    private static VerificationResult mapConflict(String body) {
        String code = JsonCodec.stringField(body, "code");
        if (code == null) {
            code = JsonCodec.stringField(body, "error");
        }
        if (code == null) {
            return VerificationResult.SERVER_ERROR;
        }
        return switch (code.toUpperCase(Locale.ROOT)) {
            case "CODE_USED" -> VerificationResult.CODE_USED;
            case "UUID_ALREADY_LINKED" -> VerificationResult.UUID_ALREADY_LINKED;
            default -> VerificationResult.SERVER_ERROR;
        };
    }
}

