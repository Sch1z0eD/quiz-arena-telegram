package com.quizarena.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "answers")
public class AnswerRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false)
    private long gameId;

    @Column(name = "chat_id", nullable = false)
    private long chatId;

    @Column(name = "user_id", nullable = false)
    private long userId;

    @Column(name = "question_id", nullable = false)
    private long questionId;

    @Column(nullable = false)
    private boolean correct;

    @Column(nullable = false)
    private int points;

    @Column(name = "answered_at", nullable = false)
    private long answeredAt;

    @Column(name = "mode", nullable = false)
    private String mode;

    protected AnswerRecord() {}

    public AnswerRecord(long gameId, long chatId, long userId, long questionId,
                        boolean correct, int points, long answeredAt, String mode) {
        this.gameId = gameId;
        this.chatId = chatId;
        this.userId = userId;
        this.questionId = questionId;
        this.correct = correct;
        this.points = points;
        this.answeredAt = answeredAt;
        this.mode = mode;
    }
}
