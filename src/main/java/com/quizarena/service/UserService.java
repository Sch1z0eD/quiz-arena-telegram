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
     * Registers or refreshes the user behind an incoming update and returns whether they are banned.
     * Best-effort and fail-open: any failure (or no from-user / a bot) yields false so a DB hiccup never
     * blocks everyone - a banned user merely slips one update through and is caught on the next.
     */
    public boolean touch(User from) {
        if (from == null || Boolean.TRUE.equals(from.getIsBot())) {
            return false;
        }
        try {
            return users.touch(from.getId(), from.getFirstName(), from.getUserName(), System.currentTimeMillis());
        } catch (RuntimeException e) {
            log.warn("Failed to touch user {}", from.getId(), e);
            return false;
        }
    }
}
