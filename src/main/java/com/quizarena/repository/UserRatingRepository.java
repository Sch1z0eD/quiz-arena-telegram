package com.quizarena.repository;

import com.quizarena.domain.UserRating;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRatingRepository extends JpaRepository<UserRating, Long> {
}
