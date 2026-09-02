package tv.csdm.minecraft.verify.model;

public enum VerificationResult {
    VERIFIED,
    INVALID_CODE,
    CODE_USED,
    UUID_ALREADY_LINKED,
    CODE_EXPIRED,
    RATE_LIMITED,
    SERVER_ERROR,
    NETWORK_ERROR
}

