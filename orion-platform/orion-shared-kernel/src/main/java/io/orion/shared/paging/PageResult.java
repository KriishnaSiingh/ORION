package io.orion.shared.paging;

import java.util.List;
import java.util.function.Function;

public record PageResult<T>(List<T> items, int page, int size, long totalElements) {
    public <R> PageResult<R> map(Function<T, R> fn) {
        return new PageResult<>(items.stream().map(fn).toList(), page, size, totalElements);
    }
}
