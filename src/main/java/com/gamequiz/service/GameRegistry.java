package com.gamequiz.service;

import com.gamequiz.config.QuizProperties;
import com.gamequiz.domain.GameSession;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Holds all running games in memory, creates new ones and drops idle ones. */
@Service
public class GameRegistry {

    /** No ambiguous characters (0/O, 1/I/L) so that codes are easy to read out loud. */
    private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 5;

    private final Map<String, GameSession> games = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final QuizProperties properties;
    private final CardBank cardBank;
    private final MediaCatalog media;

    public GameRegistry(QuizProperties properties, CardBank cardBank, MediaCatalog media) {
        this.properties = properties;
        this.cardBank = cardBank;
        this.media = media;
    }

    /**
     * Creates a new game.
     *
     * @param accessCode the code the caller knows; must match {@code quiz.access-code} when that is configured
     * @throws ForbiddenException    when the access code is wrong
     * @throws IllegalStateException when the maximum number of games is reached
     */
    public GameSession create(String accessCode) {
        String required = properties.getAccessCode();
        if (required != null && !required.isBlank() && !constantTimeEquals(required, accessCode)) {
            throw new ForbiddenException("Wrong access code");
        }
        evictIdle();
        if (games.size() >= properties.getMaxGames()) {
            throw new IllegalStateException("The server is full, try again later");
        }
        while (true) {
            String code = generateCode();
            GameSession session = new GameSession(code, UUID.randomUUID().toString(),
                    properties.getMaxPlayersPerTeam(), media.scanStage2Images(), cardBank.cards(), random);
            if (games.putIfAbsent(code, session) == null) {
                return session;
            }
        }
    }

    public GameSession require(String code) {
        GameSession session = code == null ? null : games.get(code.trim().toUpperCase(Locale.ROOT));
        if (session == null) {
            throw new NoSuchElementException("Game not found");
        }
        return session;
    }

    public Collection<GameSession> all() {
        return List.copyOf(games.values());
    }

    @Scheduled(fixedDelay = 600_000)
    public void evictIdle() {
        Instant threshold = Instant.now().minus(Duration.ofMinutes(properties.getGameIdleTimeoutMinutes()));
        games.values().removeIf(session -> session.lastActivity().isBefore(threshold));
    }

    static boolean constantTimeEquals(String expected, String actual) {
        if (actual == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        }
        return code.toString();
    }
}
