package com.gamequiz.domain.stage2;

import java.util.Arrays;
import java.util.Map;
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

    private final String[][] imageFiles = new String[QUESTION_COUNT][IMAGES_PER_QUESTION];
    private final boolean[][] revealed = new boolean[QUESTION_COUNT][IMAGES_PER_QUESTION];
    private final Stage2Outcome[] outcomes = new Stage2Outcome[QUESTION_COUNT];

    private int selectedQuestion = 1;
    private Integer focusedImage;

    /**
     * @param availableImages base name (e.g. {@code p3_2}) to file name (e.g. {@code p3_2.jfif}); the extension
     *                        is discovered by the media catalog so any image format works
     */
    public Stage2State(Map<String, String> availableImages) {
        Arrays.fill(outcomes, Stage2Outcome.PENDING);
        for (int question = 1; question <= QUESTION_COUNT; question++) {
            for (int image = 1; image <= IMAGES_PER_QUESTION; image++) {
                imageFiles[question - 1][image - 1] = availableImages.get("p" + question + "_" + image);
            }
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

    /** File name of an image, or {@code null} when the file does not exist on the server. */
    public String imageFile(int number, int imageIndex) {
        checkQuestion(number);
        checkImage(imageIndex);
        return imageFiles[number - 1][imageIndex];
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
