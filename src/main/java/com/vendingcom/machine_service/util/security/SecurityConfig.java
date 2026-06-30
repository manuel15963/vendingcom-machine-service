package com.vendingcom.machine_service.util.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityErrorWriter securityErrorWriter;
    private final List<String> allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            SecurityErrorWriter securityErrorWriter,
            @Value("${cors.allowed-origins}") List<String> allowedOrigins
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.securityErrorWriter = securityErrorWriter;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((exchange, exception) ->
                                securityErrorWriter.write(exchange, HttpStatus.UNAUTHORIZED,
                                        "UNAUTHORIZED", "Token no enviado, inválido o expirado."))
                        .accessDeniedHandler((exchange, exception) ->
                                securityErrorWriter.write(exchange, HttpStatus.FORBIDDEN,
                                        "FORBIDDEN", "No tiene permisos para acceder a este recurso.")))

                .authorizeExchange(exchange -> exchange
                        // CORS preflight
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Documentación y monitoreo
                        .pathMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/webjars/**",
                                "/actuator/health"
                        ).permitAll()

                        // Consulta de máquinas (y sus sub-recursos): ADMIN y SUPERVISOR
                        .pathMatchers(HttpMethod.GET, "/api/v1/machines/**").hasAnyRole("ADMIN", "SUPERVISOR")

                        // Sub-recursos OPERACIONALES (eventos y documentos): ADMIN y SUPERVISOR.
                        // Reglas más específicas ANTES de las genéricas de máquina.
                        .pathMatchers(HttpMethod.POST, "/api/v1/machines/*/events").hasAnyRole("ADMIN", "SUPERVISOR")
                        .pathMatchers(HttpMethod.POST, "/api/v1/machines/*/documents").hasAnyRole("ADMIN", "SUPERVISOR")
                        .pathMatchers(HttpMethod.DELETE, "/api/v1/machines/*/documents/**").hasAnyRole("ADMIN", "SUPERVISOR")

                        // Administración de la máquina en sí: solo ADMIN
                        .pathMatchers(HttpMethod.POST, "/api/v1/machines/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/v1/machines/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PATCH, "/api/v1/machines/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/v1/machines/**").hasRole("ADMIN")

                        // Auditoría: solo ADMIN
                        .pathMatchers("/api/v1/machine-audit-logs/**").hasRole("ADMIN")

                        // Catálogos (estados, tipos de evento, tipos de documento) y demás: autenticado
                        .anyExchange().authenticated())

                .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
