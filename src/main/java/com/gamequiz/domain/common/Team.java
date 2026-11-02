package com.gamequiz.domain.common;

/** The two teams playing against each other. The names are part of the public JSON API. */
public enum Team {
    BLUE,
    RED;

    public Team opponent() {
        return this == BLUE ? RED : BLUE;
    }
}
