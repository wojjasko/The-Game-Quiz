package com.gamequiz.domain.stage1;

import com.gamequiz.domain.common.Names;

/**
 * One question of the stage-1 matrix. {@code image} and {@code sound} are plain file names
 * (empty string = no media) that are resolved against the media directory. {@code answer} is only
 * ever shown to the host.
 */
public record Stage1Question(String text, String answer, String image, String sound) {

    public static final int MAX_TEXT_LENGTH = 500;
    public static final int MAX_ANSWER_LENGTH = 200;

    public Stage1Question {
        text = text == null ? "" : text.strip();
        answer = answer == null ? "" : answer.strip();
        image = Names.fileName(image, "Image");
        sound = Names.fileName(sound, "Sound");
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("Question text must have at most " + MAX_TEXT_LENGTH + " characters");
        }
        if (answer.length() > MAX_ANSWER_LENGTH) {
            throw new IllegalArgumentException("Answer must have at most " + MAX_ANSWER_LENGTH + " characters");
        }
    }

    public boolean hasImage() {
        return !image.isEmpty();
    }

    public boolean hasSound() {
        return !sound.isEmpty();
    }
}
