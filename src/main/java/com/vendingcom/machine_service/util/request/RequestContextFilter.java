package com.vendingcom.machine_service.util.request;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * Captura IP y User Agent del request y los deja disponibles en el contexto reactivo
 * para la auditoría.
 */
@Component
public class RequestContextFilter implements WebFilter {

    public static final String REQUEST_CONTEXT_KEY = "requestContext";

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        RequestContext requestContext = RequestContext.from(exchange);

        return chain.filter(exchange)
                .contextWrite(Context.of(REQUEST_CONTEXT_KEY, requestContext));
    }
}
