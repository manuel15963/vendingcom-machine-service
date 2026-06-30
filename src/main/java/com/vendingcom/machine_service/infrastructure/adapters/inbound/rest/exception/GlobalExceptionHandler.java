package com.vendingcom.machine_service.infrastructure.adapters.inbound.rest.exception;

import com.vendingcom.machine_service.domain.exception.BusinessRuleException;
import com.vendingcom.machine_service.domain.exception.DuplicateResourceException;
import com.vendingcom.machine_service.domain.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleNotFound(ResourceNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleDataIntegrity(DataIntegrityViolationException exception) {
        log.warn("Violación de integridad en BD", exception);
        return build(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION",
                "El registro viola una restricción de unicidad o integridad.");
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleDuplicate(DuplicateResourceException exception) {
        return build(HttpStatus.CONFLICT, exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleOptimisticLock(OptimisticLockingFailureException exception) {
        return build(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "El registro fue modificado por otra operación. Vuelve a cargarlo e inténtalo de nuevo.");
    }

    @ExceptionHandler(BusinessRuleException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBusinessRule(BusinessRuleException exception) {
        return build(HttpStatus.CONFLICT, exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidation(WebExchangeBindException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleResponseStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String message = exception.getReason() != null ? exception.getReason() : status.getReasonPhrase();
        return build(status, "REQUEST_ERROR", message);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleUnexpected(Exception exception) {
        log.error("Error inesperado no controlado", exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Ocurrió un error inesperado. Inténtelo nuevamente más tarde.");
    }

    private Mono<ResponseEntity<ErrorResponse>> build(HttpStatus status, String code, String message) {
        ErrorResponse body = new ErrorResponse(LocalDateTime.now(), status.value(), code, message);
        return Mono.just(ResponseEntity.status(status).body(body));
    }
}
