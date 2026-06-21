package com.quizarena.service;

import com.quizarena.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    /**
     * Registers or refreshes the user behind an incoming update. Best-effort: the registry must never break
     * gameplay, so any failure is logged and swallowed. Skips bots and updates without a from-user.
     */
    public void touch(User from) {
        if (from == null || Boolean.TRUE.equals(from.getIsBot())) {
            return;
        }
        try {
            users.touch(from.getId(), from.getFirstName(), from.getUserName(), System.currentTimeMillis());
        } catch (RuntimeException e) {
            log.warn("Failed to touch user {}", from.getId(), e);
        }
    }
}
