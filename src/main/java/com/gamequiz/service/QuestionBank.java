package com.gamequiz.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamequiz.config.QuizProperties;
import com.gamequiz.domain.QuestionPool;
import com.gamequiz.domain.stage1.Stage1Column;
import com.gamequiz.domain.stage2.Stage2Question;
import com.gamequiz.domain.stage3.QuizCard;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * The editable question bank: three JSON files in the data directory ({@code quiz.data-dir}).
 * <p>
 * The files are read again every time a game is created, so you can edit them and just create a new game,
 * no restart needed. A mistake in a file is reported with the file name and the reason.
 */
@Component
public class QuestionBank {

    public static final String STAGE1_FILE = "stage1-categories.json";
    public static final String STAGE2_FILE = "stage2-questions.json";
    public static final String STAGE3_FILE = "stage3-cards.json";

    private final Path directory;
    /**
     * A copy of the application mapper that rejects unknown fields, so a typo like "anwser" is not silently ignored.
     */
    private final ObjectMapper mapper;

    public QuestionBank(QuizProperties properties, ObjectMapper objectMapper) {
        this.directory = Path.of(properties.getDataDir()).toAbsolutePath().normalize();
        this.mapper = objectMapper.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * Reads and validates the whole bank.
     *
     * @throws IllegalStateException with a readable message when a file is missing or invalid
     */
    public QuestionPool load() {
        List<Stage1Column> columns = read(STAGE1_FILE, new TypeReference<List<Stage1Column>>() {
        });
        List<Stage2Question> questions = read(STAGE2_FILE, new TypeReference<List<Stage2Question>>() {
        });
        List<QuizCard> cards = read(STAGE3_FILE, new TypeReference<List<QuizCard>>() {
        });
        try {
            return new QuestionPool(columns, questions, cards);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Question bank: " + e.getMessage(), e);
        }
    }

    private <T> T read(String fileName, TypeReference<T> type) {
        Path file = directory.resolve(fileName);
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("Question bank: missing file " + file);
        }
        try (InputStream stream = Files.newInputStream(file)) {
            T value = mapper.readValue(stream, type);
            if (value == null) {
                throw new IllegalStateException("Question bank: " + fileName + " is empty");
            }
            return value;
        } catch (IOException e) {
            throw new IllegalStateException("Question bank: " + fileName + ": " + describe(e), e);
        }
    }

    /**
     * Prefers the message of our own validation (e.g. "Category name must not be empty") over Jackson's wrapper.
     */
    private static String describe(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof IllegalArgumentException && t.getMessage() != null) {
                return t.getMessage();
            }
        }
        String message = String.valueOf(error.getMessage());
        int newline = message.indexOf('\n');
        return newline < 0 ? message : message.substring(0, newline);
    }
}
