package com.vendingcom.machine_service.infrastructure.adapters.outbound.client;

import com.vendingcom.machine_service.application.port.output.client.CustomerValidationPort;
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
 * Adapter de salida: valida el cliente llamando por HTTP al customer-service
 * (NO accede a su base de datos). Reenvía el mismo JWT del request actual,
 * que viaja por el contexto reactivo.
 */
@Component
public class CustomerServiceClient implements CustomerValidationPort {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);

    private final WebClient webClient;

    public CustomerServiceClient(
            WebClient.Builder webClientBuilder,
            @Value("${customer-service.base-url}") String customerServiceBaseUrl
    ) {
        this.webClient = webClientBuilder.baseUrl(customerServiceBaseUrl).build();
    }

    @Override
    public Mono<Boolean> customerExists(Integer customerId) {
        return Mono.deferContextual(ctx -> {
            String token = ctx.hasKey(JwtAuthenticationFilter.AUTH_TOKEN_KEY)
                    ? ctx.get(JwtAuthenticationFilter.AUTH_TOKEN_KEY)
                    : null;

            WebClient.RequestHeadersSpec<?> request = webClient.get()
                    .uri("/api/v1/customers/{id}", customerId);

            if (token != null) {
                request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }

            return request.retrieve()
                    .toBodilessEntity()
                    .timeout(REQUEST_TIMEOUT)
                    .map(response -> Boolean.TRUE)
                    // 404 -> el cliente no existe
                    .onErrorResume(WebClientResponseException.NotFound.class, error -> Mono.just(Boolean.FALSE))
                    // 401/403 -> el JWT reenviado fue rechazado: es un error de autorización, NO indisponibilidad.
                    // Se propaga el status real (no lo enmascaramos como 503).
                    .onErrorResume(WebClientResponseException.Unauthorized.class, error -> Mono.error(
                            new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                    "No se pudo validar el cliente: autorización rechazada por customer-service.")))
                    .onErrorResume(WebClientResponseException.Forbidden.class, error -> Mono.error(
                            new ResponseStatusException(HttpStatus.FORBIDDEN,
                                    "No se pudo validar el cliente: acceso denegado por customer-service.")))
                    // timeout / conexión / 5xx -> customer-service no disponible (503; no inventamos un "no existe").
                    // Cualquier otro 4xx (p.ej. 400) también se trata como error de petición, no como caída.
                    .onErrorResume(WebClientResponseException.class, error -> error.getStatusCode().is5xxServerError()
                            ? Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                    "No se pudo validar el cliente: customer-service no está disponible."))
                            : Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                                    "No se pudo validar el cliente: respuesta inesperada de customer-service.")))
                    // WebClientRequestException (conexión rechazada/DNS), TimeoutException y demás: 503.
                    .onErrorResume(error -> !(error instanceof ResponseStatusException),
                            error -> Mono.error(new ResponseStatusException(
                                    HttpStatus.SERVICE_UNAVAILABLE,
                                    "No se pudo validar el cliente: customer-service no está disponible.")));
        });
    }
}
