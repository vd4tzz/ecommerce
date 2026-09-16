package io.d4tzz.ecommerce.query;

import java.util.List;

public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext,
    boolean hasPrevious
) {
    public static <T> PageResponse<T> of(
        List<T> content,
        int page,
        int size,
        long totalElements
    ) {
        int safeSize = size <= 0 ? 20 : size;
        int safePage = Math.max(0, page);
        int totalPages = safeSize <= 0
            ? 0
            : (int) Math.ceil((double) totalElements / safeSize);

        boolean hasNext = safePage < totalPages - 1;
        boolean hasPrevious = safePage > 0;

        return new PageResponse<>(
            content != null ? content : List.of(),
            safePage,
            safeSize,
            totalElements,
            totalPages,
            hasNext,
            hasPrevious
        );
    }

    public static <T> PageResponse<T> empty(int page, int size) {
        return of(List.of(), page, size, 0L);
    }
}
