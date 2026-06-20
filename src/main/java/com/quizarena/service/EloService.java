package com.quizarena.service;

import com.quizarena.domain.UserRating;
import com.quizarena.repository.UserRatingRepository;
import org.springframework.stereotype.Service;

@Service
public class EloService {

    private static final int START = 1000;
    private static final int K = 32;

    private final UserRatingRepository repository;

    public EloService(UserRatingRepository repository) {
        this.repository = repository;
    }

    public int rating(long userId) {
        return repository.findById(userId).map(UserRating::getElo).orElse(START);
    }

    public EloOutcome applyDuel(long userA, long userB, double scoreA) {
        int ratingA = rating(userA);
        int ratingB = rating(userB);
        int newA = nextRating(ratingA, ratingB, scoreA);
        int newB = nextRating(ratingB, ratingA, 1.0 - scoreA);
        repository.save(new UserRating(userA, newA));
        repository.save(new UserRating(userB, newB));
        return new EloOutcome(newA, newA - ratingA, newB, newB - ratingB);
    }

    static int nextRating(int rating, int opponent, double score) {
        double expected = 1.0 / (1.0 + Math.pow(10, (opponent - rating) / 400.0));
        return (int) Math.round(rating + K * (score - expected));
    }

    public record EloOutcome(int newA, int deltaA, int newB, int deltaB) {}
}
