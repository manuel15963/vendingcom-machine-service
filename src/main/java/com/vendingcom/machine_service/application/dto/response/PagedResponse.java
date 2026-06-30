package com.vendingcom.machine_service.application.dto.response;

import java.util.List;
import java.util.function.Function;

/**
 * Respuesta paginada genérica (contenido + metadatos de página).
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PagedResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return new PagedResponse<>(content, page, size, totalElements, totalPages);
    }

    public <R> PagedResponse<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = content.stream().<R>map(mapper).toList();
        return new PagedResponse<>(mapped, page, size, totalElements, totalPages);
    }
}
