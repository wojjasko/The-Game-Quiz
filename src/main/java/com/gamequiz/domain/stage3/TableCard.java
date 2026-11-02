package com.gamequiz.domain.stage3;

/** A card lying on the table of one particular game: the shared {@link QuizCard} plus its per-game state. */
public final class TableCard {

    private final QuizCard card;
    private boolean used;
    private CardResult result = CardResult.NONE;

    TableCard(QuizCard card) {
        this.card = card;
    }

    public QuizCard card() {
        return card;
    }

    /** {@code true} once the card was opened and closed (with or without points). */
    public boolean used() {
        return used;
    }

    public CardResult result() {
        return result;
    }

    void markUsed() {
        this.used = true;
    }

    void setResult(CardResult result) {
        this.result = result;
    }
}
