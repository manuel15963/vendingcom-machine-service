package com.vendingcom.machine_service.util.request;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * Utilidades para extraer IP real y User Agent del request HTTP.
 */
public class RequestContextUtils {

    private static final String UNKNOWN = "UNKNOWN";
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_REAL_IP = "X-Real-IP";
    private static final String CF_CONNECTING_IP = "CF-Connecting-IP";
    private static final String TRUE_CLIENT_IP = "True-Client-IP";

    public static String extractClientIp(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();

        String xForwardedFor = headers.getFirst(X_FORWARDED_FOR);
        if (hasText(xForwardedFor)) {
            String[] ips = xForwardedFor.split(",");
            if (ips.length > 0 && hasText(ips[0])) {
                return normalizeIp(ips[0].trim());
            }
        }

        String xRealIp = headers.getFirst(X_REAL_IP);
        if (hasText(xRealIp)) {
            return normalizeIp(xRealIp.trim());
        }

        String cfConnectingIp = headers.getFirst(CF_CONNECTING_IP);
        if (hasText(cfConnectingIp)) {
            return normalizeIp(cfConnectingIp.trim());
        }

        String trueClientIp = headers.getFirst(TRUE_CLIENT_IP);
        if (hasText(trueClientIp)) {
            return normalizeIp(trueClientIp.trim());
        }

        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return normalizeIp(remoteAddress.getAddress().getHostAddress());
        }

        return UNKNOWN;
    }

    public static String extractUserAgent(ServerWebExchange exchange) {
        String userAgent = exchange.getRequest().getHeaders().getFirst(HttpHeaders.USER_AGENT);
        return Optional.ofNullable(userAgent)
                .filter(RequestContextUtils::hasText)
                .orElse(UNKNOWN);
    }

    private static String normalizeIp(String ip) {
        if (!hasText(ip)) {
            return UNKNOWN;
        }
        String cleanIp = ip.trim();
        if ("0:0:0:0:0:0:0:1".equals(cleanIp) || "::1".equals(cleanIp)) {
            return "127.0.0.1";
        }
        return cleanIp;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
