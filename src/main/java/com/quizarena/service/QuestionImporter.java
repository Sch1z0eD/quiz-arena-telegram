package com.quizarena.service;

import com.quizarena.config.ImportProperties;
import com.quizarena.domain.Category;
import com.quizarena.domain.Difficulty;
import com.quizarena.domain.Question;
import com.quizarena.repository.QuestionRepository;
import com.quizarena.service.OpenTriviaClient.CategoryCount;
import com.quizarena.service.OpenTriviaClient.ImportedQuestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
public class QuestionImporter implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(QuestionImporter.class);
    private static final long RATE_LIMIT_PAUSE_SECONDS = 5;

    private final OpenTriviaClient client;
    private final QuestionRepository questionRepository;
    private final ImportProperties properties;
    private final Random random = new Random();

    public QuestionImporter(OpenTriviaClient client, QuestionRepository questionRepository,
                            ImportProperties properties) {
        this.client = client;
        this.questionRepository = questionRepository;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        if (!properties.enabled()) {
            return;
        }
        Thread.ofVirtual().name("question-import").start(this::importAll);
    }

    private void importAll() {
        int imported = 0;
        for (Category category : Category.values()) {
            Optional<CategoryCount> count = client.count(category);
            pause();
            for (Difficulty difficulty : Difficulty.values()) {
                int amount = startAmount(count, difficulty);
                if (amount <= 0) {
                    continue;
                }
                imported += importPair(category, difficulty, amount);
            }
        }
        log.info("OpenTDB import finished, {} new questions added", imported);
    }

    private int startAmount(Optional<CategoryCount> count, Difficulty difficulty) {
        int cap = properties.amountPerCall();
        return count.map(value -> Math.min(value.forDifficulty(difficulty), cap)).orElse(cap);
    }

    // api_count counts all types, but we fetch type=multiple only - so a sized request can still
    // overshoot the multiple-choice subset (response_code 1, empty). Step the amount down until a
    // batch comes back, or stop when even one question isn't available.
    private int importPair(Category category, Difficulty difficulty, int startAmount) {
        int amount = startAmount;
        while (amount >= 1) {
            List<ImportedQuestion> batch = client.fetch(category, difficulty, amount);
            pause();
            if (!batch.isEmpty()) {
                return saveBatch(category, difficulty, batch);
            }
            amount /= 2;
        }
        return 0;
    }

    private int saveBatch(Category category, Difficulty difficulty, List<ImportedQuestion> batch) {
        int saved = 0;
        for (ImportedQuestion source : batch) {
            if (source.incorrect().size() != 3) {
                continue;
            }
            String hash = QuestionHash.of(source.question());
            if (questionRepository.existsByQuestionHash(hash)) {
                continue;
            }
            questionRepository.save(toQuestion(source, category, difficulty, hash));
            saved++;
        }
        return saved;
    }

    private Question toQuestion(ImportedQuestion source, Category category, Difficulty difficulty, String hash) {
        List<String> options = new ArrayList<>(source.incorrect());
        options.add(source.correct());
        Collections.shuffle(options, random);
        int correctIndex = options.indexOf(source.correct());
        return new Question(source.question(),
                options.get(0), options.get(1), options.get(2), options.get(3),
                correctIndex, category.slug(), difficulty.value(), "en", hash);
    }

    private void pause() {
        try {
            TimeUnit.SECONDS.sleep(RATE_LIMIT_PAUSE_SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
