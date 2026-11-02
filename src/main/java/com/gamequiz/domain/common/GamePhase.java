package com.gamequiz.domain.common;

/** The phase a game is currently in. Only the host can switch phases; every screen follows. */
public enum GamePhase {
    LOBBY,
    STAGE_1,
    STAGE_2,
    STAGE_3,
    FINISHED
}
