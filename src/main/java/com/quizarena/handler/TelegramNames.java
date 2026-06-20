package com.quizarena.handler;

import org.telegram.telegrambots.meta.api.objects.User;

final class TelegramNames {

    private TelegramNames() {
    }

    static String displayName(User user) {
        if (user.getFirstName() != null && !user.getFirstName().isBlank()) {
            return user.getFirstName();
        }
        if (user.getUserName() != null) {
            return "@" + user.getUserName();
        }
        return String.valueOf(user.getId());
    }
}
