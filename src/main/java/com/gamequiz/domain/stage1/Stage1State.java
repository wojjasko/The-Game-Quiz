package com.gamequiz.domain.stage1;

import com.gamequiz.domain.common.Names;
import com.gamequiz.domain.common.ScoreSink;
import com.gamequiz.domain.common.Team;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Rules and state of stage 1 ("question matrix"): a 4x4 board of tiles, a question database,
 * a countdown timer and a number of steals per team.
 * <p>
 * Not thread-safe: callers must hold the lock of the owning game session.
 */
public final class Stage1State {

    public static final int SIZE = 4;
    public static final int INITIAL_TIME_SECONDS = 360;
    public static final int INITIAL_STEALS = 2;
    public static final int LINE_BONUS_POINTS = 5;
    public static final int TIME_BONUS_STEP_SECONDS = 15;
    public static final int TIME_BONUS_STEP_POINTS = 5;
    public static final int TIME_BONUS_CAP_POINTS = 40;
    public static final int MAX_CATEGORY_LENGTH = 30;
    public static final int MAX_FREE_TEXT_LENGTH = Stage1Question.MAX_TEXT_LENGTH;

    public static final String IDLE_PROMPT = "Wybierz kafelek, aby wyświetlić pytanie.";
    public static final String RESET_PROMPT = "Plansza zresetowana. Wybierz kafelek.";

    private final ScoreSink scores;
    private final TileStatus[][] board = new TileStatus[SIZE][SIZE];
    private final Stage1Question[][] questions = new Stage1Question[SIZE][SIZE];
    private final Set<Line> completedLines = new LinkedHashSet<>();
    private final Map<Team, CountdownTimer> timers = new EnumMap<>(Team.class);
    private final Map<Team, Integer> steals = new EnumMap<>(Team.class);

    private List<String> categories = List.of("Kategoria 1", "Kategoria 2", "Kategoria 3", "Kategoria 4");
    private String currentText = IDLE_PROMPT;
    private String currentImage;
    private String currentSound;
    private boolean imageZoomed;

    public Stage1State(ScoreSink scores) {
        this.scores = Objects.requireNonNull(scores);
        for (Team team : Team.values()) {
            timers.put(team, new CountdownTimer(INITIAL_TIME_SECONDS));
            steals.put(team, INITIAL_STEALS);
        }
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                questions[row][col] = Stage1Question.placeholder(row * SIZE + col + 1);
            }
        }
        reset();
    }

    // ---- board -----------------------------------------------------------------------------------------------

    /** Changes a tile and awards the line bonus to a team that has just completed a new line. */
    public void setTile(int row, int col, TileStatus status) {
        checkCell(row, col);
        board[row][col] = Objects.requireNonNull(status, "status");
        awardCompletedLines();
    }

    public TileStatus tile(int row, int col) {
        checkCell(row, col);
        return board[row][col];
    }

    /** Lines that already gave their bonus. A line is rewarded only once, even if tiles change later. */
    public List<Line> completedLines() {
        return List.copyOf(completedLines);
    }

    private void awardCompletedLines() {
        for (Line line : Line.ALL) {
            if (completedLines.contains(line)) {
                continue;
            }
            for (Team team : Team.values()) {
                if (ownsWholeLine(team, line)) {
                    completedLines.add(line);
                    scores.add(team, LINE_BONUS_POINTS);
                    break;
                }
            }
        }
    }

    private boolean ownsWholeLine(Team team, Line line) {
        for (int[] cell : line.cells()) {
            if (!board[cell[0]][cell[1]].isOwnedBy(team)) {
                return false;
            }
        }
        return true;
    }

    // ---- categories and question database -------------------------------------------------------------------------

    public List<String> categories() {
        return categories;
    }

    /** Exactly {@link #SIZE} names; blank entries fall back to "Kategoria N". */
    public void setCategories(List<String> names) {
        if (names == null || names.size() != SIZE) {
            throw new IllegalArgumentException("Exactly " + SIZE + " categories are required");
        }
        List<String> cleaned = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            String raw = names.get(i);
            cleaned.add(raw == null || raw.isBlank()
                    ? "Kategoria " + (i + 1)
                    : Names.clean(raw, MAX_CATEGORY_LENGTH, "Category name"));
        }
        this.categories = List.copyOf(cleaned);
    }

    public Stage1Question question(int row, int col) {
        checkCell(row, col);
        return questions[row][col];
    }

    public void setQuestion(int row, int col, Stage1Question question) {
        checkCell(row, col);
        questions[row][col] = Objects.requireNonNull(question, "question");
    }

    // ---- what is currently shown on the screen --------------------------------------------------------------------

    /** Loads the stored question (text, image, sound) of a tile onto the screen. */
    public void activateTile(int row, int col) {
        Stage1Question question = question(row, col);
        currentText = question.text();
        currentImage = question.hasImage() ? question.image() : null;
        currentSound = question.hasSound() ? question.sound() : null;
        imageZoomed = false;
    }

    /** Shows free text on the screen without touching the media. */
    public void setCurrentText(String text) {
        String value = text == null ? "" : text.strip();
        if (value.length() > MAX_FREE_TEXT_LENGTH) {
            throw new IllegalArgumentException("Text must have at most " + MAX_FREE_TEXT_LENGTH + " characters");
        }
        currentText = value;
    }

    public void clearCurrentQuestion() {
        currentText = IDLE_PROMPT;
        currentImage = null;
        currentSound = null;
        imageZoomed = false;
    }

    public void setImageZoomed(boolean zoomed) {
        this.imageZoomed = zoomed;
    }

    public String currentText() {
        return currentText;
    }

    /** File name of the image on the screen, or {@code null}. */
    public String currentImage() {
        return currentImage;
    }

    /** File name of the sound on the screen, or {@code null}. */
    public String currentSound() {
        return currentSound;
    }

    public boolean isImageZoomed() {
        return imageZoomed;
    }

    // ---- timers ----------------------------------------------------------------------------------------------------

    public void startTimer(Team team) {
        timers.get(team).start();
    }

    public void stopTimer(Team team) {
        timers.get(team).stop();
    }

    public void resetTimer(Team team) {
        timers.get(team).reset();
    }

    public void stopTimers() {
        timers.values().forEach(CountdownTimer::stop);
    }

    /** Advances all running timers by one second. @return {@code true} when any timer changed */
    public boolean tick() {
        boolean changed = false;
        for (CountdownTimer timer : timers.values()) {
            changed |= timer.tick();
        }
        return changed;
    }

    public int remainingSeconds(Team team) {
        return timers.get(team).remainingSeconds();
    }

    public boolean isTimerRunning(Team team) {
        return timers.get(team).isRunning();
    }

    /** Points for the remaining time: 5 per full 15 seconds, at most 40. @return the awarded points */
    public int awardTimeBonus(Team team) {
        int steps = remainingSeconds(team) / TIME_BONUS_STEP_SECONDS;
        int points = Math.min(TIME_BONUS_CAP_POINTS, steps * TIME_BONUS_STEP_POINTS);
        if (points > 0) {
            scores.add(team, points);
        }
        return points;
    }

    // ---- steals ----------------------------------------------------------------------------------------------------

    public void spendSteal(Team team) {
        steals.put(team, Math.max(0, steals.get(team) - 1));
    }

    public void resetSteals() {
        steals.replaceAll((team, count) -> INITIAL_STEALS);
    }

    public int steals(Team team) {
        return steals.get(team);
    }

    // ---- reset -----------------------------------------------------------------------------------------------------

    /** Board, screen, timers and steals back to the initial state. Scores and the question database stay. */
    public void reset() {
        for (TileStatus[] row : board) {
            java.util.Arrays.fill(row, TileStatus.DEFAULT);
        }
        completedLines.clear();
        timers.values().forEach(CountdownTimer::reset);
        resetSteals();
        clearCurrentQuestion();
        currentText = RESET_PROMPT;
    }

    private static void checkCell(int row, int col) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            throw new IllegalArgumentException("Tile (" + row + ", " + col + ") is outside of the board");
        }
    }
}
