package com.gamequiz.domain.stage1;

import com.gamequiz.domain.common.Team;

/** State of one tile of the stage-1 board. The names are part of the public JSON API. */
public enum TileStatus {
    DEFAULT,
    BLUE,
    RED,
    INCORRECT,
    BLUE_STEAL,
    RED_STEAL,
    SELECTED;

    /** A stolen tile counts for the team that stole it. */
    public boolean isOwnedBy(Team team) {
        return switch (this) {
            case BLUE, BLUE_STEAL -> team == Team.BLUE;
            case RED, RED_STEAL -> team == Team.RED;
            default -> false;
        };
    }
}
