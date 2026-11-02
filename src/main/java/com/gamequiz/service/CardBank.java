package com.gamequiz.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamequiz.domain.stage3.QuizCard;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The stage-3 question pool, loaded once at startup from {@code quiz-data/stage3-cards.json}. */
@Component
public class CardBank {

    private static final String RESOURCE = "/quiz-data/stage3-cards.json";

    private final List<QuizCard> cards;

    public CardBank(ObjectMapper objectMapper) {
        this.cards = load(objectMapper);
    }

    public List<QuizCard> cards() {
        return cards;
    }

    private static List<QuizCard> load(ObjectMapper objectMapper) {
        try (InputStream stream = CardBank.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing classpath resource " + RESOURCE);
            }
            List<QuizCard> loaded = objectMapper.readValue(stream, new TypeReference<List<QuizCard>>() {
            });
            Set<Integer> ids = new HashSet<>();
            for (QuizCard card : loaded) {
                if (!ids.add(card.id())) {
                    throw new IllegalStateException("Duplicate card id " + card.id() + " in " + RESOURCE);
                }
            }
            return List.copyOf(loaded);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + RESOURCE, e);
        }
    }
}
