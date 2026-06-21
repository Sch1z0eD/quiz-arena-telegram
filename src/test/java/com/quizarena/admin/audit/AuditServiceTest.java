package com.quizarena.admin.audit;

import com.quizarena.admin.auth.VerifiedAdmin;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditServiceTest {

    @Test
    void recordPersistsTheEntry() {
        AuditRepository repository = mock(AuditRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-06-21T10:00:00Z"), ZoneOffset.UTC);
        AuditService service = new AuditService(repository, clock);

        service.record(new VerifiedAdmin(7, "Bob"), "category.created", "science", "ru=Наука, en=Science");

        ArgumentCaptor<AuditEntity> captor = ArgumentCaptor.forClass(AuditEntity.class);
        verify(repository).save(captor.capture());
        AuditEntity entry = captor.getValue();
        assertEquals(7, entry.getAdminId());
        assertEquals("category.created", entry.getAction());
        assertEquals("science", entry.getTarget());
        assertEquals("ru=Наука, en=Science", entry.getDetails());
    }
}
