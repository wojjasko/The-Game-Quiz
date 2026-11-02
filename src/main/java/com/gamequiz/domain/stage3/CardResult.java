package com.gamequiz.domain.stage3;

import com.gamequiz.domain.common.Team;

/** How a stage-3 card was resolved. The names are part of the public JSON API. */
public enum CardResult {
    NONE,
    BLUE_CORRECT,
    BLUE_WRONG,
    RED_CORRECT,
    RED_WRONG;

    public static CardResult of(Team team, boolean correct) {
        if (team == Team.BLUE) {
            return correct ? BLUE_CORRECT : BLUE_WRONG;
        }
        return correct ? RED_CORRECT : RED_WRONG;
    }
}
