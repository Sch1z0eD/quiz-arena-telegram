package com.quizarena.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_rating")
public class UserRating {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private int elo;

    protected UserRating() {}

    public UserRating(long userId, int elo) {
        this.userId = userId;
        this.elo = elo;
    }

    public int getElo() {
        return elo;
    }
}
