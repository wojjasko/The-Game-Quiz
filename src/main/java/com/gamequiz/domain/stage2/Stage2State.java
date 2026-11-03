package com.gamequiz.domain.stage2;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Rules and state of stage 2 ("which game is this?"): 8 questions with 3 blurred images each.
 * The host selects a question, reveals/zooms its images and records who answered.
 * <p>
 * Not thread-safe: callers must hold the lock of the owning game session.
 */
public final class Stage2State {

    public static final int QUESTION_COUNT = 8;
    public static final int IMAGES_PER_QUESTION = 3;

    private final Stage2Question[] questions = new Stage2Question[QUESTION_COUNT];
    private final boolean[][] revealed = new boolean[QUESTION_COUNT][IMAGES_PER_QUESTION];
    private final Stage2Outcome[] outcomes = new Stage2Outcome[QUESTION_COUNT];

    private int selectedQuestion = 1;
    private Integer focusedImage;

    /** @param drawn exactly {@link #QUESTION_COUNT} questions drawn from the question bank */
    public Stage2State(List<Stage2Question> drawn) {
        if (drawn == null || drawn.size() != QUESTION_COUNT) {
            throw new IllegalArgumentException("Exactly " + QUESTION_COUNT + " questions are required");
        }
        Arrays.fill(outcomes, Stage2Outcome.PENDING);
        for (int i = 0; i < QUESTION_COUNT; i++) {
            questions[i] = Objects.requireNonNull(drawn.get(i), "question");
        }
    }

    public int selectedQuestion() {
        return selectedQuestion;
    }

    /** Selects a question (1..8). Switching question puts a zoomed image back. */
    public void selectQuestion(int number) {
        checkQuestion(number);
        this.selectedQuestion = number;
        this.focusedImage = null;
    }

    public Stage2Outcome outcome(int number) {
        checkQuestion(number);
        return outcomes[number - 1];
    }

    public void setOutcome(int number, Stage2Outcome outcome) {
        checkQuestion(number);
        outcomes[number - 1] = Objects.requireNonNull(outcome, "outcome");
    }

    /** Centers an image of the selected question on the screen and reveals it for good. */
    public void focusImage(int imageIndex) {
        checkImage(imageIndex);
        revealed[selectedQuestion - 1][imageIndex] = true;
        focusedImage = imageIndex;
    }

    /** Puts the zoomed image back; it stays revealed. */
    public void clearFocus() {
        focusedImage = null;
    }

    /** Index (0..2) of the zoomed image of the selected question, or {@code null}. */
    public Integer focusedImage() {
        return focusedImage;
    }

    public boolean isRevealed(int number, int imageIndex) {
        checkQuestion(number);
        checkImage(imageIndex);
        return revealed[number - 1][imageIndex];
    }

    public Stage2Question question(int number) {
        checkQuestion(number);
        return questions[number - 1];
    }

    public List<Stage2Question> questions() {
        return List.of(questions);
    }

    /** Puts another question into a slot and forgets everything that happened to the old one. */
    public void replaceQuestion(int number, Stage2Question question) {
        checkQuestion(number);
        questions[number - 1] = Objects.requireNonNull(question, "question");
        Arrays.fill(revealed[number - 1], false);
        outcomes[number - 1] = Stage2Outcome.PENDING;
        if (selectedQuestion == number) {
            focusedImage = null;
        }
    }

    /** File name of an image of a question (the file may still be missing on disk). */
    public String imageFile(int number, int imageIndex) {
        checkImage(imageIndex);
        return question(number).images().get(imageIndex);
    }

    private static void checkQuestion(int number) {
        if (number < 1 || number > QUESTION_COUNT) {
            throw new IllegalArgumentException("Question number must be between 1 and " + QUESTION_COUNT);
        }
    }

    private static void checkImage(int imageIndex) {
        if (imageIndex < 0 || imageIndex >= IMAGES_PER_QUESTION) {
            throw new IllegalArgumentException("Image index must be between 0 and " + (IMAGES_PER_QUESTION - 1));
        }
    }
}
