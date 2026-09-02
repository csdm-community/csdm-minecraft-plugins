package tv.csdm.minecraft.verify.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import tv.csdm.minecraft.verify.model.VerificationResult;

class BackendResponseMapperTest {
    @Test
    void mapsSuccessfulVerification() {
        assertEquals(VerificationResult.VERIFIED, BackendResponseMapper.map(200, "{}").result());
    }

    @Test
    void mapsBothConflictTypes() {
        assertEquals(
                VerificationResult.CODE_USED,
                BackendResponseMapper.map(409, "{\"code\":\"CODE_USED\"}").result());
        assertEquals(
                VerificationResult.UUID_ALREADY_LINKED,
                BackendResponseMapper.map(409, "{\"error\":\"UUID_ALREADY_LINKED\"}").result());
    }

    @Test
    void unknownConflictFailsClosed() {
        assertEquals(
                VerificationResult.SERVER_ERROR,
                BackendResponseMapper.map(409, "{\"code\":\"SOMETHING_NEW\"}").result());
    }
}

