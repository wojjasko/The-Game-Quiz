package com.gamequiz.service;

/** Thrown when a caller is not allowed to perform an action (wrong host token or access code). */
public class ForbiddenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ForbiddenException(String message) {
        super(message);
    }
}
