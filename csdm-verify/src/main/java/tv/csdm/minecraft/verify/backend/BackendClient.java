package tv.csdm.minecraft.verify.backend;

import java.util.concurrent.CompletableFuture;
import tv.csdm.minecraft.verify.model.VerificationRequest;
import tv.csdm.minecraft.verify.model.VerificationResponse;

public interface BackendClient {
    CompletableFuture<VerificationResponse> verify(VerificationRequest request);
}

