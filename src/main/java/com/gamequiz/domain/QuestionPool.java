package com.gamequiz.domain;

import com.gamequiz.domain.stage1.Stage1Column;
import com.gamequiz.domain.stage1.Stage1State;
import com.gamequiz.domain.stage2.Stage2Question;
import com.gamequiz.domain.stage2.Stage2State;
import com.gamequiz.domain.stage3.QuizCard;
import com.gamequiz.domain.stage3.Stage3State;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Everything a game can draw its questions from (the "question bank"). A game picks a random subset
 * of each list, so every list must hold at least as many entries as one game uses.
 */
public record QuestionPool(List<Stage1Column> stage1Columns, List<Stage2Question> stage2Questions,
                           List<QuizCard> stage3Cards) {

    public QuestionPool {
        stage1Columns = requireAtLeast(stage1Columns, Stage1State.SIZE, "stage1-categories.json", "categories");
        stage2Questions = requireAtLeast(stage2Questions, Stage2State.QUESTION_COUNT, "stage2-questions.json", "questions");
        stage3Cards = requireAtLeast(stage3Cards, Stage3State.TABLE_SIZE, "stage3-cards.json", "cards");

        Set<String> names = new HashSet<>();
        for (Stage1Column column : stage1Columns) {
            if (!names.add(column.name().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException(
                        "stage1-categories.json: the category \"" + column.name() + "\" appears twice");
            }
        }
        Set<Integer> ids = new HashSet<>();
        for (QuizCard card : stage3Cards) {
            if (!ids.add(card.id())) {
                throw new IllegalArgumentException("stage3-cards.json: duplicate card id " + card.id());
            }
        }
    }

    private static <T> List<T> requireAtLeast(List<T> list, int minimum, String file, String what) {
        if (list == null || list.size() < minimum) {
            throw new IllegalArgumentException(file + " needs at least " + minimum + " " + what
                    + " (found " + (list == null ? 0 : list.size()) + ")");
        }
        if (list.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException(file + " contains an empty entry");
        }
        return List.copyOf(list);
    }
}
