package com.quizarena.domain;

public enum Category {
    GENERAL("general", 9),
    SCIENCE("science", 17),
    COMPUTERS("computers", 18),
    GEOGRAPHY("geography", 22),
    HISTORY("history", 23),
    SPORTS("sports", 21),
    FILM("film", 11),
    MUSIC("music", 12),
    GAMES("games", 15),
    MYTHOLOGY("mythology", 20);

    private final String slug;
    private final int openTdbId;

    Category(String slug, int openTdbId) {
        this.slug = slug;
        this.openTdbId = openTdbId;
    }

    public String slug() {
        return slug;
    }

    public int openTdbId() {
        return openTdbId;
    }

    public static Category fromSlug(String slug) {
        for (Category category : values()) {
            if (category.slug.equals(slug)) {
                return category;
            }
        }
        return null;
    }
}
