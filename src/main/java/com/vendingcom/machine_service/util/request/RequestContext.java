package com.vendingcom.machine_service.util.request;

import org.springframework.web.server.ServerWebExchange;

/**
 * Información del request HTTP (IP, User Agent) que viaja por el contexto reactivo.
 */
public record RequestContext(
        String clientIp,
        String userAgent
) {
    public static RequestContext from(ServerWebExchange exchange) {
        return new RequestContext(
                RequestContextUtils.extractClientIp(exchange),
                RequestContextUtils.extractUserAgent(exchange)
        );
    }
}
