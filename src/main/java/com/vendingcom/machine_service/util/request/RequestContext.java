package com.vendingcom.machine_service.util.request;

import org.springframework.web.server.ServerWebExchange;

/**
 * Información del request HTTP (id de correlación, IP, User Agent) que viaja por el contexto reactivo.
 */
public record RequestContext(
        String requestId,
        String clientIp,
        String userAgent
) {
    public static RequestContext from(ServerWebExchange exchange, String requestId) {
        return new RequestContext(
                requestId,
                RequestContextUtils.extractClientIp(exchange),
                RequestContextUtils.extractUserAgent(exchange)
        );
    }
}
