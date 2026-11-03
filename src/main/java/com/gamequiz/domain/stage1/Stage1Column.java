package com.gamequiz.domain.stage1;

import com.gamequiz.domain.common.Names;

import java.util.List;

/**
 * One column of the stage-1 board: a category and its four questions worth 5, 10, 15 and 20 points
 * (top to bottom). The question bank is a list of these; a game draws four of them.
 */
public record Stage1Column(String name, List<Stage1Question> questions) {

    public Stage1Column {
        name = Names.clean(name, Stage1State.MAX_CATEGORY_LENGTH, "Category name");
        if (questions == null || questions.size() != Stage1State.SIZE) {
            throw new IllegalArgumentException(
                    "Category \"" + name + "\" needs exactly " + Stage1State.SIZE + " questions (5, 10, 15 and 20 points)");
        }
        if (questions.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Category \"" + name + "\" contains an empty question");
        }
        questions = List.copyOf(questions);
    }
}
