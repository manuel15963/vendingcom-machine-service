package com.vendingcom.machine_service.infrastructure.adapters.outbound.client;

import com.vendingcom.machine_service.application.port.output.client.LocationValidationPort;
import com.vendingcom.machine_service.util.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Adapter de salida: valida la ubicación llamando por HTTP al location-service
 * (NO accede a su base de datos). Reenvía el mismo JWT del request actual,
 * que viaja por el contexto reactivo.
 */
@Component
public class LocationServiceClient implements LocationValidationPort {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);

    private final WebClient webClient;

    public LocationServiceClient(
            WebClient.Builder webClientBuilder,
            @Value("${location-service.base-url}") String locationServiceBaseUrl
    ) {
        this.webClient = webClientBuilder.baseUrl(locationServiceBaseUrl).build();
    }

    @Override
    public Mono<Boolean> locationExists(Integer locationId) {
        return Mono.deferContextual(ctx -> {
            String token = ctx.hasKey(JwtAuthenticationFilter.AUTH_TOKEN_KEY)
                    ? ctx.get(JwtAuthenticationFilter.AUTH_TOKEN_KEY)
                    : null;

            WebClient.RequestHeadersSpec<?> request = webClient.get()
                    .uri("/api/v1/locations/{id}", locationId);

            if (token != null) {
                request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }

            return request.retrieve()
                    .toBodilessEntity()
                    .timeout(REQUEST_TIMEOUT)
                    .map(response -> Boolean.TRUE)
                    // 404 -> la ubicación no existe
                    .onErrorResume(WebClientResponseException.NotFound.class, error -> Mono.just(Boolean.FALSE))
                    // 401/403 -> el JWT reenviado fue rechazado: es un error de autorización, NO indisponibilidad.
                    // Se propaga el status real (no lo enmascaramos como 503).
                    .onErrorResume(WebClientResponseException.Unauthorized.class, error -> Mono.error(
                            new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                    "No se pudo validar la ubicación: autorización rechazada por location-service.")))
                    .onErrorResume(WebClientResponseException.Forbidden.class, error -> Mono.error(
                            new ResponseStatusException(HttpStatus.FORBIDDEN,
                                    "No se pudo validar la ubicación: acceso denegado por location-service.")))
                    // timeout / conexión / 5xx -> location-service no disponible (503; no inventamos un "no existe").
                    // Cualquier otro 4xx (p.ej. 400) también se trata como error de petición, no como caída.
                    .onErrorResume(WebClientResponseException.class, error -> error.getStatusCode().is5xxServerError()
                            ? Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                    "No se pudo validar la ubicación: location-service no está disponible."))
                            : Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                                    "No se pudo validar la ubicación: respuesta inesperada de location-service.")))
                    // WebClientRequestException (conexión rechazada/DNS), TimeoutException y demás: 503.
                    .onErrorResume(error -> !(error instanceof ResponseStatusException),
                            error -> Mono.error(new ResponseStatusException(
                                    HttpStatus.SERVICE_UNAVAILABLE,
                                    "No se pudo validar la ubicación: location-service no está disponible.")));
        });
    }
}
