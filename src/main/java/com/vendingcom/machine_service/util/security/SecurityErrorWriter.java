package com.vendingcom.machine_service.util.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vendingcom.machine_service.application.dto.response.SecurityErrorResponse;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Component
public class SecurityErrorWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> write(
            ServerWebExchange exchange,
            HttpStatus status,
            String code,
            String message
    ) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        SecurityErrorResponse response = new SecurityErrorResponse(
                LocalDateTime.now(),
                status.value(),
                code,
                message,
                exchange.getRequest().getPath().value()
        );

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(response);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException exception) {
            return exchange.getResponse().setComplete();
        }
    }
}
