package com.gamequiz.domain.stage2;

import com.gamequiz.domain.common.Names;

import java.util.List;

/**
 * One question of stage 2 ("which game is this?"): three image file names (from {@code media/images/stage2})
 * and the answer, which is only ever shown to the host.
 */
public record Stage2Question(String answer, List<String> images) {

    public static final int MAX_ANSWER_LENGTH = 100;

    public Stage2Question {
        answer = Names.clean(answer, MAX_ANSWER_LENGTH, "Answer");
        if (images == null || images.size() != Stage2State.IMAGES_PER_QUESTION) {
            throw new IllegalArgumentException(
                    "Question \"" + answer + "\" needs exactly " + Stage2State.IMAGES_PER_QUESTION + " images");
        }
        List<String> cleaned = images.stream().map(name -> Names.fileName(name, "Image")).toList();
        if (cleaned.stream().anyMatch(String::isEmpty)) {
            throw new IllegalArgumentException("Question \"" + answer + "\" has an empty image name");
        }
        images = cleaned;
    }
}
