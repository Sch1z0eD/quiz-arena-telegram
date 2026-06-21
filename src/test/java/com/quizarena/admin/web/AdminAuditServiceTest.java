package com.quizarena.admin.web;

import com.quizarena.admin.audit.AuditEntity;
import com.quizarena.admin.audit.AuditRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminAuditServiceTest {

    private final AuditRepository audit = mock(AuditRepository.class);
    private final AdminAuditService service = new AdminAuditService(audit);

    @Test
    void mapsPageMetadataAndEntry() {
        Instant ts = Instant.parse("2026-06-21T10:00:00Z");
        AuditEntity entity = new AuditEntity(ts, 7L, "category.created", "science", "ru=Наука, en=Science");
        ReflectionTestUtils.setField(entity, "id", 5L);
        when(audit.search(eq(""), anyLong(), eq(""), any(), any(), eq(PageRequest.of(0, 20))))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1));

        PageResponse<AuditEntry> response = service.list(null, null, null, null, null, PageRequest.of(0, 20));

        assertEquals(1, response.totalElements());
        AuditEntry entry = response.content().get(0);
        assertEquals(5L, entry.id());
        assertEquals(ts, entry.ts());
        assertEquals(7L, entry.adminId());
        assertEquals("category.created", entry.action());
        assertEquals("science", entry.target());
        assertEquals("ru=Наука, en=Science", entry.details());
    }

    @Test
    void actionsDelegatesToRepository() {
        when(audit.distinctActions()).thenReturn(List.of("category.created", "question.created"));
        assertEquals(List.of("category.created", "question.created"), service.actions());
    }
}
