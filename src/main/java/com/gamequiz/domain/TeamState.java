package com.gamequiz.domain;

/** Mutable name and score of one team. Always accessed under the lock of the owning {@link GameSession}. */
public final class TeamState {

    private String name;
    private int score;

    public TeamState(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int score() {
        return score;
    }

    public void addScore(int delta) {
        this.score += delta;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
