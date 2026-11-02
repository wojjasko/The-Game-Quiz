package com.gamequiz.domain.stage3;

import com.gamequiz.domain.common.ScoreSink;
import com.gamequiz.domain.common.Team;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Rules and state of stage 3 ("cards"): ten category cards lie on the table, the host opens one, the answering team
 * gets its current multiplier in points for a correct answer. A correct answer raises that team's multiplier,
 * a wrong one resets it. Each team has one 50:50 lifeline.
 * <p>
 * Not thread-safe: callers must hold the lock of the owning game session.
 */
public final class Stage3State {

    public static final int TABLE_SIZE = 10;
    public static final int INITIAL_MULTIPLIER = 15;
    public static final int MULTIPLIER_STEP = 5;

    private final ScoreSink scores;
    private final RandomGenerator random;
    private final List<TableCard> table = new ArrayList<>();
    /** Cards that are not on the table; used to replace swapped cards. */
    private final Deque<QuizCard> reserve = new ArrayDeque<>();
    private final Map<Team, Integer> multipliers = new EnumMap<>(Team.class);
    private final Map<Team, Boolean> lifelineUsed = new EnumMap<>(Team.class);
    private final Set<AnswerKey> hiddenAnswers = EnumSet.noneOf(AnswerKey.class);

    private TableCard activeCard;

    public Stage3State(ScoreSink scores, List<QuizCard> pool, RandomGenerator random) {
        this.scores = Objects.requireNonNull(scores);
        this.random = Objects.requireNonNull(random);

        List<QuizCard> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, random);
        int tableSize = Math.min(TABLE_SIZE, shuffled.size());
        for (int i = 0; i < shuffled.size(); i++) {
            if (i < tableSize) {
                table.add(new TableCard(shuffled.get(i)));
            } else {
                reserve.addLast(shuffled.get(i));
            }
        }
        for (Team team : Team.values()) {
            multipliers.put(team, INITIAL_MULTIPLIER);
            lifelineUsed.put(team, false);
        }
    }

    /** Replaces an unused table card with the next reserve card; the replaced card is discarded for this game. */
    public void swapCard(int cardId) {
        int index = indexOfCard(cardId);
        TableCard current = table.get(index);
        if (current.used()) {
            throw new IllegalStateException("The card was already used");
        }
        if (current == activeCard) {
            throw new IllegalStateException("Close the open question before swapping its card");
        }
        QuizCard replacement = reserve.pollFirst();
        if (replacement == null) {
            throw new IllegalStateException("There are no replacement cards left");
        }
        table.set(index, new TableCard(replacement));
    }

    /** Opens the question of a card. Opening another card while one is open simply switches to it. */
    public void openCard(int cardId) {
        TableCard card = table.get(indexOfCard(cardId));
        if (card.used()) {
            throw new IllegalStateException("The card was already used");
        }
        activeCard = card;
        hiddenAnswers.clear();
    }

    /** Hides two wrong answers of the open question. Each team can use its lifeline once. */
    public void useLifeline(Team team) {
        requireActiveCard();
        if (lifelineUsed.get(team)) {
            throw new IllegalStateException("This team has already used its 50:50 lifeline");
        }
        lifelineUsed.put(team, true);

        List<AnswerKey> wrongAnswers = new ArrayList<>(List.of(AnswerKey.values()));
        wrongAnswers.remove(activeCard.card().correct());
        Collections.shuffle(wrongAnswers, random);
        hiddenAnswers.clear();
        hiddenAnswers.add(wrongAnswers.get(0));
        hiddenAnswers.add(wrongAnswers.get(1));
    }

    /**
     * Resolves the open question for a team and closes it.
     *
     * @return the points awarded (0 for a wrong answer)
     */
    public int answer(Team team, boolean correct) {
        requireActiveCard();
        activeCard.setResult(CardResult.of(team, correct));
        int awarded = 0;
        if (correct) {
            awarded = multipliers.get(team);
            scores.add(team, awarded);
            multipliers.put(team, awarded + MULTIPLIER_STEP);
        } else {
            multipliers.put(team, INITIAL_MULTIPLIER);
        }
        closeQuestion();
        return awarded;
    }

    /** Closes the open question without points; its card counts as used. Does nothing when no card is open. */
    public void closeQuestion() {
        if (activeCard != null) {
            activeCard.markUsed();
        }
        activeCard = null;
        hiddenAnswers.clear();
    }

    public List<TableCard> table() {
        return List.copyOf(table);
    }

    /** The open card, or {@code null}. */
    public TableCard activeCard() {
        return activeCard;
    }

    public int multiplier(Team team) {
        return multipliers.get(team);
    }

    public boolean isLifelineUsed(Team team) {
        return lifelineUsed.get(team);
    }

    public Set<AnswerKey> hiddenAnswers() {
        return Collections.unmodifiableSet(EnumSet.copyOf(hiddenAnswers));
    }

    public int reserveSize() {
        return reserve.size();
    }

    private int indexOfCard(int cardId) {
        for (int i = 0; i < table.size(); i++) {
            if (table.get(i).card().id() == cardId) {
                return i;
            }
        }
        throw new NoSuchElementException("Card " + cardId + " is not on the table");
    }

    private void requireActiveCard() {
        if (activeCard == null) {
            throw new IllegalStateException("No question is open");
        }
    }
}
