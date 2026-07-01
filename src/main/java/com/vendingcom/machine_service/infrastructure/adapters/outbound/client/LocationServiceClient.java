package com.vendingcom.machine_service.infrastructure.adapters.outbound.client;

import com.vendingcom.machine_service.application.port.output.client.LocationValidationPort;
import com.vendingcom.machine_service.util.request.RequestContext;
import com.vendingcom.machine_service.util.request.RequestContextFilter;
import com.vendingcom.machine_service.util.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Adapter de salida: valida la ubicación llamando por HTTP al location-service
 * (NO accede a su base de datos). Reenvía el mismo JWT del request actual y el
 * X-Request-Id (correlación entre servicios), ambos por el contexto reactivo.
 *
 * Resiliencia: timeout configurable + reintento con backoff SOLO ante errores
 * transitorios (timeout, fallo de conexión, 5xx) — útil contra cold starts de
 * Render. NO reintenta 404/401/403 (no son transitorios).
 */
@Component
public class LocationServiceClient implements LocationValidationPort {

    private static final int MAX_RETRIES = 2;
    private static final Duration RETRY_BACKOFF = Duration.ofSeconds(2);

    private final WebClient webClient;
    private final Duration requestTimeout;

    public LocationServiceClient(
            WebClient.Builder webClientBuilder,
            @Value("${location-service.base-url}") String locationServiceBaseUrl,
            @Value("${location-service.timeout-seconds:10}") long timeoutSeconds
    ) {
        this.webClient = webClientBuilder.baseUrl(locationServiceBaseUrl).build();
        this.requestTimeout = Duration.ofSeconds(timeoutSeconds);
    }

    @Override
    public Mono<Boolean> locationExists(Integer locationId) {
        return Mono.deferContextual(ctx -> {
            String token = ctx.hasKey(JwtAuthenticationFilter.AUTH_TOKEN_KEY)
                    ? ctx.get(JwtAuthenticationFilter.AUTH_TOKEN_KEY)
                    : null;
            String requestId = ctx.hasKey(RequestContextFilter.REQUEST_CONTEXT_KEY)
                    ? ((RequestContext) ctx.get(RequestContextFilter.REQUEST_CONTEXT_KEY)).requestId()
                    : null;

            WebClient.RequestHeadersSpec<?> request = webClient.get()
                    .uri("/api/v1/locations/{id}", locationId);

            if (token != null) {
                request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }
            if (requestId != null) {
                request = request.header(RequestContextFilter.REQUEST_ID_HEADER, requestId);
            }

            return request.retrieve()
                    .toBodilessEntity()
                    .timeout(requestTimeout)
                    // Reintento con backoff solo ante errores transitorios; al agotarse, propaga el error original.
                    .retryWhen(Retry.backoff(MAX_RETRIES, RETRY_BACKOFF)
                            .filter(LocationServiceClient::isTransient)
                            .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                    .map(response -> Boolean.TRUE)
                    // 404 -> la ubicación no existe
                    .onErrorResume(WebClientResponseException.NotFound.class, error -> Mono.just(Boolean.FALSE))
                    // 401/403 -> el JWT reenviado fue rechazado: error de autorización, NO indisponibilidad.
                    .onErrorResume(WebClientResponseException.Unauthorized.class, error -> Mono.error(
                            new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                    "No se pudo validar la ubicación: autorización rechazada por location-service.")))
                    .onErrorResume(WebClientResponseException.Forbidden.class, error -> Mono.error(
                            new ResponseStatusException(HttpStatus.FORBIDDEN,
                                    "No se pudo validar la ubicación: acceso denegado por location-service.")))
                    // 5xx -> caído (503); otro 4xx -> respuesta inesperada (502).
                    .onErrorResume(WebClientResponseException.class, error -> error.getStatusCode().is5xxServerError()
                            ? Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                    "No se pudo validar la ubicación: location-service no está disponible."))
                            : Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                                    "No se pudo validar la ubicación: respuesta inesperada de location-service.")))
                    // Conexión rechazada/DNS, timeout y demás: 503.
                    .onErrorResume(error -> !(error instanceof ResponseStatusException),
                            error -> Mono.error(new ResponseStatusException(
                                    HttpStatus.SERVICE_UNAVAILABLE,
                                    "No se pudo validar la ubicación: location-service no está disponible.")));
        });
    }

    /** Errores transitorios que vale la pena reintentar (cold start de Render, red, 5xx). */
    private static boolean isTransient(Throwable error) {
        if (error instanceof WebClientResponseException webError) {
            return webError.getStatusCode().is5xxServerError();
        }
        return error instanceof TimeoutException || error instanceof WebClientRequestException;
    }
}
