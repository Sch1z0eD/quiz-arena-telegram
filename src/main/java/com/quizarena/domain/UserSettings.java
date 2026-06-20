package com.quizarena.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private String language;

    protected UserSettings() {}

    public UserSettings(long userId, String language) {
        this.userId = userId;
        this.language = language;
    }

    public String getLanguage() {
        return language;
    }
}
