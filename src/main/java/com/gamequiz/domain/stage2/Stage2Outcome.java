package com.gamequiz.domain.stage2;

/** Who answered a stage-2 question. The names are part of the public JSON API. */
public enum Stage2Outcome {
    /** Still in progress (shown white). */
    PENDING,
    BLUE,
    RED,
    /** Nobody answered (shown gray). */
    NONE
}
