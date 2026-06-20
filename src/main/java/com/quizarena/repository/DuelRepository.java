package com.quizarena.repository;

import com.quizarena.domain.DuelRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DuelRepository extends JpaRepository<DuelRecord, Long> {
}
