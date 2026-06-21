package com.quizarena.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "username")
    private String username;

    @Column(name = "first_seen", nullable = false)
    private long firstSeen;

    @Column(name = "last_seen", nullable = false)
    private long lastSeen;

    @Column(name = "blocked", nullable = false)
    private boolean blocked;

    @Column(name = "banned", nullable = false)
    private boolean banned;

    protected User() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public long getFirstSeen() {
        return firstSeen;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public boolean isBanned() {
        return banned;
    }
}
