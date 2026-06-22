package com.quizarena.repository;

public interface UserRepositoryCustom {

    /**
     * Upserts the registry row for an incoming user and returns the current banned flag in one round-trip.
     * ON CONFLICT keeps the 2c-0 semantics: refresh name/username/last_seen and clear blocked; never touch
     * first_seen or banned. The returned banned drives the dispatch gate.
     */
    boolean touch(long id, String name, String username, long now);
}
