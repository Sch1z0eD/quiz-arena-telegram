package com.quizarena.admin.web;

import java.util.Map;

public record UpdateCategoryRequest(Map<String, String> names) {
}
