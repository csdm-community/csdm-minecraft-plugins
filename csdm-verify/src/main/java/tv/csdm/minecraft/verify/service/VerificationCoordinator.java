package tv.csdm.minecraft.verify.service;

import java.net.InetAddress;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import tv.csdm.minecraft.verify.backend.BackendClient;
import tv.csdm.minecraft.verify.config.VerifySettings;
import tv.csdm.minecraft.verify.model.VerificationRequest;
import tv.csdm.minecraft.verify.model.VerificationResponse;

public final class VerificationCoordinator {
    public enum Rejection {
        INVALID_CODE,
        COOLDOWN,
        RATE_LIMITED,
        IN_FLIGHT
    }

    public sealed interface StartResult permits Accepted, Rejected {}

    public record Accepted(CompletableFuture<VerificationResponse> response) implements StartResult {}

    public record Rejected(Rejection reason) implements StartResult {}

    private final VerifySettings settings;
    private final BackendClient backendClient;
    private final FixedWindowRateLimiter<UUID> cooldownByUuid;
    private final FixedWindowRateLimiter<UUID> attemptsByUuid;
    private final FixedWindowRateLimiter<InetAddress> attemptsByAddress;
    private final Set<UUID> inFlight = ConcurrentHashMap.newKeySet();

    public VerificationCoordinator(VerifySettings settings, BackendClient backendClient) {
        this.settings = settings;
        this.backendClient = backendClient;
        this.cooldownByUuid = new FixedWindowRateLimiter<>(1, settings.cooldown());
        this.attemptsByUuid = new FixedWindowRateLimiter<>(settings.attemptsPerWindow(), settings.rateLimitWindow());
        this.attemptsByAddress = new FixedWindowRateLimiter<>(settings.attemptsPerWindow(), settings.rateLimitWindow());
    }

    public StartResult start(VerificationRequest request) {
        if (!settings.isValidCode(request.code())) {
            return new Rejected(Rejection.INVALID_CODE);
        }
        if (!inFlight.add(request.minecraftUuid())) {
            return new Rejected(Rejection.IN_FLIGHT);
        }
        if (!cooldownByUuid.tryAcquire(request.minecraftUuid())) {
            inFlight.remove(request.minecraftUuid());
            return new Rejected(Rejection.COOLDOWN);
        }
        if (!attemptsByUuid.tryAcquire(request.minecraftUuid())
                || !attemptsByAddress.tryAcquire(request.address())) {
            inFlight.remove(request.minecraftUuid());
            return new Rejected(Rejection.RATE_LIMITED);
        }

        CompletableFuture<VerificationResponse> response = backendClient.verify(request)
                .whenComplete((ignored, throwable) -> inFlight.remove(request.minecraftUuid()));
        return new Accepted(response);
    }
}

