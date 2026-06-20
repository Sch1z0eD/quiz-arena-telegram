package com.quizarena.domain;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "duels")
public class DuelRecord implements Persistable<Long> {

    @Id
    private Long id;

    @Column(name = "user_a", nullable = false)
    private long userA;

    @Column(name = "user_b", nullable = false)
    private long userB;

    @Column(name = "score_a", nullable = false)
    private int scoreA;

    @Column(name = "score_b", nullable = false)
    private int scoreB;

    @Column(name = "winner_id")
    private Long winnerId;

    @Column(name = "category")
    private String category;

    @Column(name = "difficulty")
    private String difficulty;

    @Column(name = "finished_at", nullable = false)
    private long finishedAt;

    protected DuelRecord() {}

    public DuelRecord(long id, long userA, long userB, int scoreA, int scoreB, Long winnerId,
                      String category, String difficulty, long finishedAt) {
        this.id = id;
        this.userA = userA;
        this.userB = userB;
        this.scoreA = scoreA;
        this.scoreB = scoreB;
        this.winnerId = winnerId;
        this.category = category;
        this.difficulty = difficulty;
        this.finishedAt = finishedAt;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    @Transient
    public boolean isNew() {
        return true;
    }
}
