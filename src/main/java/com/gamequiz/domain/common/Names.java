package com.gamequiz.domain.common;

import java.util.regex.Pattern;

/** Validation and normalization of user supplied display names (players, teams, categories). */
public final class Names {

    /** Invisible formatting characters (zero-width spaces, bidi overrides, ...) are dropped entirely. */
    private static final Pattern FORMAT_CHARACTERS = Pattern.compile("\\p{Cf}");
    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("\\p{Cntrl}");
    private static final Pattern WHITESPACE_RUNS = Pattern.compile("\\s+");
    /** A plain file name: no path separators, must not start with a dot (rules out "..", ".hidden"). */
    private static final Pattern FILE_NAME = Pattern.compile("[A-Za-z0-9_][A-Za-z0-9_.\\-]{0,99}");

    private Names() {
    }

    /**
     * Trims, collapses whitespace and removes control characters.
     *
     * @throws IllegalArgumentException when the value is missing, empty or longer than {@code maxLength}
     */
    public static String clean(String raw, int maxLength, String label) {
        if (raw == null) {
            throw new IllegalArgumentException(label + " is required");
        }
        String withoutFormat = FORMAT_CHARACTERS.matcher(raw).replaceAll("");
        String withoutControl = CONTROL_CHARACTERS.matcher(withoutFormat).replaceAll(" ");
        String cleaned = WHITESPACE_RUNS.matcher(withoutControl).replaceAll(" ").trim();
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException(label + " must not be empty");
        }
        if (cleaned.length() > maxLength) {
            throw new IllegalArgumentException(label + " must have at most " + maxLength + " characters");
        }
        return cleaned;
    }

    /**
     * Validates a media file name that will be resolved against the media directory.
     * Blank means "no file" and is returned as an empty string.
     *
     * @throws IllegalArgumentException when the value contains path separators or other unsafe characters
     */
    public static String fileName(String value, String label) {
        String name = value == null ? "" : value.strip();
        if (!name.isEmpty() && !FILE_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException(
                    label + " must be a plain file name (letters, digits, '_', '-' and '.')");
        }
        return name;
    }
}
