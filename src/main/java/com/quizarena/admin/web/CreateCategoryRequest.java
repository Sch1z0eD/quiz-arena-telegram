package com.quizarena.admin.web;

import java.util.Map;

public record CreateCategoryRequest(Map<String, String> names, boolean active) {
}
