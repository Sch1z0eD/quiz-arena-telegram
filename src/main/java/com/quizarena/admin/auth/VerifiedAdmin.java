package com.quizarena.admin.auth;

import java.io.Serializable;

public record VerifiedAdmin(long id, String name) implements Serializable {
}
