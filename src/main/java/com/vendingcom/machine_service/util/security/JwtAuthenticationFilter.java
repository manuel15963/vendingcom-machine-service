package com.vendingcom.machine_service.util.security;

import io.jsonwebtoken.Claims;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Valida el JWT emitido por auth-service y deja la autenticación + el id del usuario
 * disponibles para el resto de la cadena. machine-service no emite tokens.
 */
@Component
public class JwtAuthenticationFilter implements WebFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    /** Clave de contexto reactivo con el id del usuario autenticado (para auditoría). */
    public static final String AUTH_USER_ID_KEY = "authUserId";

    /** Clave de contexto reactivo con el JWT en crudo (para reenviarlo a otros microservicios). */
    public static final String AUTH_TOKEN_KEY = "authToken";

    private final JwtService jwtService;
    private final SecurityErrorWriter securityErrorWriter;

    public JwtAuthenticationFilter(JwtService jwtService, SecurityErrorWriter securityErrorWriter) {
        this.jwtService = jwtService;
        this.securityErrorWriter = securityErrorWriter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        if (isPublicPath(request.getPath().value())) {
            return chain.filter(exchange);
        }

        String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return securityErrorWriter.write(exchange, HttpStatus.UNAUTHORIZED,
                    "UNAUTHORIZED", "Token no enviado o formato inválido.");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());

        if (!jwtService.isTokenValid(token)) {
            return securityErrorWriter.write(exchange, HttpStatus.UNAUTHORIZED,
                    "INVALID_TOKEN", "Token inválido o expirado.");
        }

        Claims claims = jwtService.extractClaims(token);
        String username = claims.getSubject();
        Integer authenticatedUserId = claims.get("userId", Integer.class);

        // El userId es obligatorio para la trazabilidad de auditoría: un token sin él
        // se rechaza (en lugar de procesar escrituras/lecturas sin usuario auditable).
        if (authenticatedUserId == null) {
            return securityErrorWriter.write(exchange, HttpStatus.UNAUTHORIZED,
                    "INVALID_TOKEN", "Token sin identificador de usuario.");
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, extractAuthorities(claims));

        return chain.filter(exchange)
                .contextWrite(context -> context
                        .put(AUTH_TOKEN_KEY, token)
                        .put(AUTH_USER_ID_KEY, authenticatedUserId))
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }

    private boolean isPublicPath(String path) {
        return path.equals("/actuator/health")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/webjars/");
    }

    private List<SimpleGrantedAuthority> extractAuthorities(Claims claims) {
        Object rolesObject = claims.get("roles");
        if (!(rolesObject instanceof List<?> roles)) {
            return List.of();
        }
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (Object role : roles) {
            if (role != null) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
        }
        return authorities;
    }
}
