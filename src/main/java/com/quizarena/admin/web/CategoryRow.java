package com.quizarena.admin.web;

import java.util.Map;

public record CategoryRow(String slug, Map<String, String> names, boolean active, long questionCount,
                          Map<String, Long> byLanguage) {
}
