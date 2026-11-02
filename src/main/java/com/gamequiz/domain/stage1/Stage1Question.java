package com.gamequiz.domain.stage1;

import java.util.regex.Pattern;

/**
 * One question of the stage-1 matrix. {@code image} and {@code sound} are plain file names
 * (empty string = no media) that are resolved against the media directory.
 */
public record Stage1Question(String text, String image, String sound) {

    public static final int MAX_TEXT_LENGTH = 500;

    /** A plain file name: no path separators, must not start with a dot (rules out "..", ".hidden"). */
    private static final Pattern FILE_NAME = Pattern.compile("[A-Za-z0-9_][A-Za-z0-9_.\\-]{0,99}");

    public Stage1Question {
        text = text == null ? "" : text.strip();
        image = normalizeFileName(image, "Image");
        sound = normalizeFileName(sound, "Sound");
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("Question text must have at most " + MAX_TEXT_LENGTH + " characters");
        }
    }

    public static Stage1Question placeholder(int number) {
        return new Stage1Question("Pytanie " + number, "", "");
    }

    public boolean hasImage() {
        return !image.isEmpty();
    }

    public boolean hasSound() {
        return !sound.isEmpty();
    }

    private static String normalizeFileName(String value, String label) {
        String name = value == null ? "" : value.strip();
        if (!name.isEmpty() && !FILE_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException(
                    label + " must be a plain file name (letters, digits, '_', '-' and '.')");
        }
        return name;
    }
}
