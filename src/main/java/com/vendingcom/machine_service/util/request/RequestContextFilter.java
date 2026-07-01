package com.vendingcom.machine_service.util.request;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

/**
 * Captura un id de correlación (X-Request-Id), la IP y el User Agent del request,
 * y los deja en el contexto reactivo (auditoría) + en un atributo del exchange (logs).
 * El X-Request-Id se reenvía a los servicios downstream y se devuelve en la respuesta,
 * para poder seguir una petición a través de todo el sistema.
 */
@Component
public class RequestContextFilter implements WebFilter {

    public static final String REQUEST_CONTEXT_KEY = "requestContext";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_ATTR = "requestId";

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String requestId = resolveRequestId(exchange);
        exchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, requestId);
        exchange.getAttributes().put(REQUEST_ID_ATTR, requestId);

        RequestContext requestContext = RequestContext.from(exchange, requestId);

        return chain.filter(exchange)
                .contextWrite(Context.of(REQUEST_CONTEXT_KEY, requestContext));
    }

    /** Usa el X-Request-Id entrante (si el llamante lo trae) o genera uno nuevo. */
    private String resolveRequestId(ServerWebExchange exchange) {
        String incoming = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
        return (incoming != null && !incoming.isBlank()) ? incoming : UUID.randomUUID().toString();
    }
}
