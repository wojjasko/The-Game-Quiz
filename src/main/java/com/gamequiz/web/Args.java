package com.gamequiz.web;

/** Small helpers for validating request bodies whose fields may be missing. */
final class Args {

    private Args() {
    }

    /** @throws IllegalArgumentException (HTTP 400) when a required value is missing */
    static <T> T required(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
