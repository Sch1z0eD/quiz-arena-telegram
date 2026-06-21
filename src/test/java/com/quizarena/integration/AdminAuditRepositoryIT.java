package com.quizarena.integration;

import com.quizarena.admin.audit.AuditEntity;
import com.quizarena.admin.audit.AuditRepository;
import com.quizarena.admin.web.AdminAuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminAuditRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private AuditRepository audit;

    private static final Pageable TS_DESC = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("ts")));
    // The exact bounds the service substitutes when no date filter is given (the default list view).
    private static final Instant MIN = AdminAuditService.MIN_TS;
    private static final Instant MAX = AdminAuditService.MAX_TS;

    @Test
    void filtersAndSortDescWorkTogether() {
        long admin = 7001L;
        audit.save(new AuditEntity(Instant.parse("2026-06-01T10:00:00Z"), admin, "category.created", "science", "ru=Наука"));
        audit.save(new AuditEntity(Instant.parse("2026-06-02T10:00:00Z"), admin, "question.created", "42", "text"));
        audit.save(new AuditEntity(Instant.parse("2026-06-03T10:00:00Z"), admin, "category.disabled", "science", null));

        List<AuditEntity> all = audit.search("", admin, "", MIN, MAX, TS_DESC).getContent();
        assertEquals(3, all.size());
        assertEquals("category.disabled", all.get(0).getAction(), "newest must come first under ts desc");

        assertEquals(1, audit.search("question.created", admin, "", MIN, MAX, TS_DESC).getTotalElements());
        assertEquals(2, audit.search("", admin, "SCI", MIN, MAX, TS_DESC).getTotalElements());
        assertEquals(0, audit.search("", 9999L, "", MIN, MAX, TS_DESC).getTotalElements());

        // a single calendar day expressed as inclusive boundaries must capture that day's row
        long onDayTwo = audit.search("", admin, "",
                Instant.parse("2026-06-02T00:00:00Z"), Instant.parse("2026-06-02T23:59:59.999Z"), TS_DESC).getTotalElements();
        assertEquals(1, onDayTwo);
    }

    @Test
    void paginates() {
        long admin = 7002L;
        audit.save(new AuditEntity(Instant.parse("2026-07-01T10:00:00Z"), admin, "x", null, null));
        audit.save(new AuditEntity(Instant.parse("2026-07-02T10:00:00Z"), admin, "x", null, null));
        audit.save(new AuditEntity(Instant.parse("2026-07-03T10:00:00Z"), admin, "x", null, null));

        Page<AuditEntity> page = audit.search("", admin, "", MIN, MAX,
                PageRequest.of(0, 2, Sort.by(Sort.Order.desc("ts"))));
        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
        assertEquals(2, page.getContent().size());
    }

    @Test
    void defaultBoundsReturnAllRowsAgainstPostgres() {
        long admin = 7004L;
        audit.save(new AuditEntity(Instant.parse("2026-09-01T10:00:00Z"), admin, "category.created", "science", "ru=Наука"));
        audit.save(new AuditEntity(Instant.parse("2026-09-02T10:00:00Z"), admin, "category.disabled", "science", null));

        // The unfiltered default path: service coalesces null from/to to MIN_TS/MAX_TS. These must stay
        // within timestamptz range or the most common list view would fail at runtime.
        assertEquals(2, audit.search("", admin, "", MIN, MAX, TS_DESC).getTotalElements());
    }

    @Test
    void distinctActionsContainsInsertedAction() {
        audit.save(new AuditEntity(Instant.parse("2026-08-01T10:00:00Z"), 7003L, "zeta.action", null, null));
        assertTrue(audit.distinctActions().contains("zeta.action"));
    }
}
