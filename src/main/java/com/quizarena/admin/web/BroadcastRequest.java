package com.quizarena.admin.web;

import java.util.List;

public record BroadcastRequest(String segment, String language, String text, String photoUrl,
                               List<List<Button>> buttons) {

    public record Button(String text, String url) {
    }
}
