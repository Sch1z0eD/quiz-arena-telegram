package com.quizarena.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "broadcasts")
public class Broadcast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private long adminId;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(nullable = false, length = 16)
    private String segment;

    @Column(length = 8)
    private String language;

    @Column(nullable = false)
    private String text;

    @Column(name = "photo_url")
    private String photoUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "buttons")
    private List<List<BroadcastButton>> buttons;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(nullable = false)
    private int sent;

    @Column(nullable = false)
    private int failed;

    @Column(nullable = false)
    private int total;

    @Column(name = "confirm_token", length = 64)
    private String confirmToken;

    protected Broadcast() {
    }

    public Broadcast(long adminId, long createdAt, String segment, String language, String text, String photoUrl,
                     List<List<BroadcastButton>> buttons, String status, int total, String confirmToken) {
        this.adminId = adminId;
        this.createdAt = createdAt;
        this.segment = segment;
        this.language = language;
        this.text = text;
        this.photoUrl = photoUrl;
        this.buttons = buttons;
        this.status = status;
        this.total = total;
        this.confirmToken = confirmToken;
    }

    public Long getId() {
        return id;
    }

    public long getAdminId() {
        return adminId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getSegment() {
        return segment;
    }

    public String getLanguage() {
        return language;
    }

    public String getText() {
        return text;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public List<List<BroadcastButton>> getButtons() {
        return buttons;
    }

    public String getStatus() {
        return status;
    }

    public int getSent() {
        return sent;
    }

    public int getFailed() {
        return failed;
    }

    public int getTotal() {
        return total;
    }

    public String getConfirmToken() {
        return confirmToken;
    }
}
