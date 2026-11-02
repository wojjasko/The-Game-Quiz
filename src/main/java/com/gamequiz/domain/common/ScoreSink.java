package com.gamequiz.domain.common;

/** Receives score changes produced by stage rules (line bonuses, multipliers, time bonuses, ...). */
@FunctionalInterface
public interface ScoreSink {

    void add(Team team, int points);
}
