package com.quizarena.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizarena.domain.Category;
import com.quizarena.domain.Difficulty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Component
public class OpenTriviaClient {

    private static final Logger log = LoggerFactory.getLogger(OpenTriviaClient.class);
    private static final String QUESTIONS_URL =
            "https://opentdb.com/api.php?amount=%d&category=%d&difficulty=%s&type=multiple&encode=base64";
    private static final String COUNT_URL = "https://opentdb.com/api_count.php?category=%d";

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public Optional<CategoryCount> count(Category category) {
        try {
            String body = get(COUNT_URL.formatted(category.openTdbId()));
            if (body == null) {
                return Optional.empty();
            }
            CountResponse parsed = mapper.readValue(body, CountResponse.class);
            CategoryQuestionCount count = parsed.count();
            if (count == null) {
                return Optional.empty();
            }
            return Optional.of(new CategoryCount(count.easy(), count.medium(), count.hard()));
        } catch (Exception e) {
            log.warn("OpenTDB count failed for {}", category.slug(), e);
            return Optional.empty();
        }
    }

    public List<ImportedQuestion> fetch(Category category, Difficulty difficulty, int amount) {
        try {
            String body = get(QUESTIONS_URL.formatted(amount, category.openTdbId(), difficulty.value()));
            if (body == null) {
                return List.of();
            }
            TriviaResponse parsed = mapper.readValue(body, TriviaResponse.class);
            if (parsed.responseCode() != 0 || parsed.results() == null) {
                return List.of();
            }
            List<ImportedQuestion> questions = new ArrayList<>();
            for (TriviaResult result : parsed.results()) {
                List<String> incorrect = new ArrayList<>();
                for (String option : result.incorrectAnswers()) {
                    incorrect.add(decode(option));
                }
                questions.add(new ImportedQuestion(decode(result.question()), decode(result.correctAnswer()), incorrect));
            }
            return questions;
        } catch (Exception e) {
            log.warn("OpenTDB fetch failed for {}/{}", category.slug(), difficulty.value(), e);
            return List.of();
        }
    }

    private String get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.warn("OpenTDB HTTP {} for {}", response.statusCode(), url);
            return null;
        }
        return response.body();
    }

    private static String decode(String base64) {
        return new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
    }

    public record ImportedQuestion(String question, String correct, List<String> incorrect) {}

    public record CategoryCount(int easy, int medium, int hard) {

        public int forDifficulty(Difficulty difficulty) {
            return switch (difficulty) {
                case EASY -> easy;
                case MEDIUM -> medium;
                case HARD -> hard;
            };
        }
    }

    record TriviaResponse(@JsonProperty("response_code") int responseCode, List<TriviaResult> results) {}

    record TriviaResult(
            @JsonProperty("question") String question,
            @JsonProperty("correct_answer") String correctAnswer,
            @JsonProperty("incorrect_answers") List<String> incorrectAnswers) {}

    record CountResponse(@JsonProperty("category_question_count") CategoryQuestionCount count) {}

    record CategoryQuestionCount(
            @JsonProperty("total_easy_question_count") int easy,
            @JsonProperty("total_medium_question_count") int medium,
            @JsonProperty("total_hard_question_count") int hard) {}
}
