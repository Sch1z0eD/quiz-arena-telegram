package com.quizarena.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String text;

    @Column(name = "option_a", nullable = false)
    private String optionA;

    @Column(name = "option_b", nullable = false)
    private String optionB;

    @Column(name = "option_c", nullable = false)
    private String optionC;

    @Column(name = "option_d", nullable = false)
    private String optionD;

    @Column(name = "correct_option", nullable = false)
    private int correctOption;

    @Column(name = "category")
    private String category;

    @Column(name = "difficulty")
    private String difficulty;

    @Column(name = "language", nullable = false)
    private String language;

    @Column(name = "question_hash", nullable = false, unique = true)
    private String questionHash;

    protected Question() {}

    public Question(String text, String optionA, String optionB, String optionC, String optionD,
                    int correctOption, String category, String difficulty, String language, String questionHash) {
        this.text = text;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctOption = correctOption;
        this.category = category;
        this.difficulty = difficulty;
        this.language = language;
        this.questionHash = questionHash;
    }

    public Long getId() { return id; }
    public String getText() { return text; }
    public String getOptionA() { return optionA; }
    public String getOptionB() { return optionB; }
    public String getOptionC() { return optionC; }
    public String getOptionD() { return optionD; }
    public int getCorrectOption() { return correctOption; }
    public String getCategory() { return category; }
    public String getDifficulty() { return difficulty; }
    public String getLanguage() { return language; }
    public String getQuestionHash() { return questionHash; }

    public String getOptionByIndex(int index) {
        return switch (index) {
            case 0 -> optionA;
            case 1 -> optionB;
            case 2 -> optionC;
            case 3 -> optionD;
            default -> throw new IllegalArgumentException("Option index must be 0-3");
        };
    }
}
