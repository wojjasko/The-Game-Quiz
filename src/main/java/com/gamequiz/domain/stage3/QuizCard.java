package com.gamequiz.domain.stage3;

import java.util.List;

/**
 * An immutable multiple-choice question of stage 3. Cards are loaded once from {@code quiz-data/stage3-cards.json}
 * and shared by all games; per-game state lives in {@link TableCard}.
 *
 * @param answers the four answer texts in the order A, B, C, D
 */
public record QuizCard(int id, String category, String question, List<String> answers, AnswerKey correct) {

    public QuizCard {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Card " + id + ": category is required");
        }
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Card " + id + ": question is required");
        }
        if (answers == null || answers.size() != AnswerKey.values().length) {
            throw new IllegalArgumentException("Card " + id + ": exactly 4 answers are required");
        }
        if (correct == null) {
            throw new IllegalArgumentException("Card " + id + ": the correct answer is required");
        }
        answers = List.copyOf(answers);
    }

    public String answerText(AnswerKey key) {
        return answers.get(key.ordinal());
    }
}
