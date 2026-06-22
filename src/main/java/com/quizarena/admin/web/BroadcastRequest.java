package com.quizarena.admin.web;

public record BroadcastRequest(String segment, String language, String text, String photoUrl, Button button) {

    public record Button(String text, String url) {
    }
}
