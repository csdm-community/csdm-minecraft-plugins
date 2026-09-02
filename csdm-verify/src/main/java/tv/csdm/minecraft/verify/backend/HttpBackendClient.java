package tv.csdm.minecraft.verify.backend;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import tv.csdm.minecraft.verify.config.VerifySettings;
import tv.csdm.minecraft.verify.model.VerificationRequest;
import tv.csdm.minecraft.verify.model.VerificationResponse;
import tv.csdm.minecraft.verify.model.VerificationResult;

public final class HttpBackendClient implements BackendClient {
    private final VerifySettings settings;
    private final HttpClient client;

    public HttpBackendClient(VerifySettings settings) {
        this.settings = settings;
        this.client = HttpClient.newBuilder()
                .connectTimeout(settings.requestTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public CompletableFuture<VerificationResponse> verify(VerificationRequest request) {
        HttpRequest httpRequest = HttpRequest.newBuilder(settings.backendUri())
                .timeout(settings.requestTimeout())
                .header("Authorization", "Bearer " + settings.internalSecret())
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .header("User-Agent", "CSDMVerify/0.1")
                .POST(HttpRequest.BodyPublishers.ofString(JsonCodec.encode(request), StandardCharsets.UTF_8))
                .build();

        return client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> BackendResponseMapper.map(response.statusCode(), response.body()))
                .exceptionally(ignored -> new VerificationResponse(VerificationResult.NETWORK_ERROR, 0));
    }
}

