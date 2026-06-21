package com.quizarena.admin.web;

import java.util.Map;

public record CategoryRow(String slug, long total, Map<String, Long> byLanguage) {
}
