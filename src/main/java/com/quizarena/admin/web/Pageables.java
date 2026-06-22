package com.quizarena.admin.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

// Caps the page size and restricts sorting to known columns so request params cannot drive arbitrary queries.
final class Pageables {

    static final int MAX_PAGE_SIZE = 100;

    private Pageables() {
    }

    static Pageable sanitize(Pageable pageable, Set<String> sortable, Sort defaultSort) {
        int size = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        List<Sort.Order> orders = pageable.getSort().stream()
                .filter(order -> sortable.contains(order.getProperty()))
                .toList();
        Sort sort = orders.isEmpty() ? defaultSort : Sort.by(orders);
        return PageRequest.of(Math.max(pageable.getPageNumber(), 0), size, sort);
    }
}
