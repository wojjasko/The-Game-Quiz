package com.gamequiz.domain;

import com.gamequiz.domain.common.GamePhase;
import com.gamequiz.domain.common.Names;
import com.gamequiz.domain.common.ScoreSink;
import com.gamequiz.domain.common.Team;
import com.gamequiz.domain.stage1.Stage1Column;
import com.gamequiz.domain.stage1.Stage1State;
import com.gamequiz.domain.stage2.Stage2Question;
import com.gamequiz.domain.stage2.Stage2State;
import com.gamequiz.domain.stage3.Stage3State;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * One game room: teams, scores, players, the current phase and the state of the three stages.
 * <p>
 * Every access to a session must happen inside {@link #locked(Supplier)} because HTTP threads and the timer
 * thread work on the same instance. The lock is reentrant, so nested calls are fine.
 */
public final class GameSession {

    public static final int MAX_PLAYER_NAME_LENGTH = 24;
    public static final int MAX_TEAM_NAME_LENGTH = 30;
    /** Length of the "start game" countdown shown to everybody before stage 1 begins. */
    public static final int START_COUNTDOWN_SECONDS = 10;
    public static final String DEFAULT_BLUE_NAME = "KANGURY Z KOSMOSU";
    public static final String DEFAULT_RED_NAME = "BUNIE";

    private final String code;
    private final String hostToken;
    private final int maxPlayersPerTeam;
    private final Instant createdAt = Instant.now();
    private final ReentrantLock lock = new ReentrantLock();
    private volatile Instant lastActivity = createdAt;

    private final Map<Team, TeamState> teams = new EnumMap<>(Team.class);
    private final Map<String, Player> players = new LinkedHashMap<>();
    private GamePhase phase = GamePhase.LOBBY;

    private final QuestionPool pool;
    private final RandomGenerator random;
    private Stage1State stage1;
    private Stage2State stage2;
    private Stage3State stage3;

    /** End of the running start countdown, or {@code null}. */
    private Instant countdownEnd;
    private int countdownSeconds;

    /**
     * @param pool the question bank; the game draws its own set from it and may re-draw before it starts
     */
    public GameSession(String code, String hostToken, int maxPlayersPerTeam, QuestionPool pool,
                       RandomGenerator random) {
        this.code = code;
        this.hostToken = hostToken;
        this.maxPlayersPerTeam = maxPlayersPerTeam;
        this.pool = pool;
        this.random = random;
        teams.put(Team.BLUE, new TeamState(DEFAULT_BLUE_NAME));
        teams.put(Team.RED, new TeamState(DEFAULT_RED_NAME));
        drawAll();
    }

    // ---- locking and bookkeeping -----------------------------------------------------------------------------

    public <T> T locked(Supplier<T> action) {
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    public void locked(Runnable action) {
        locked(() -> {
            action.run();
            return null;
        });
    }

    /** Marks the game as recently used so that it is not evicted as idle. */
    public void touch() {
        lastActivity = Instant.now();
    }

    public String code() {
        return code;
    }

    public String hostToken() {
        return hostToken;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant lastActivity() {
        return lastActivity;
    }

    // ---- phase -----------------------------------------------------------------------------------------------

    public GamePhase phase() {
        return phase;
    }

    /** Switches the phase. Leaving stage 1 stops its timers so that they do not run in the background. */
    public void setPhase(GamePhase newPhase) {
        if (newPhase == null) {
            throw new IllegalArgumentException("phase is required");
        }
        if (phase == GamePhase.STAGE_1 && newPhase != GamePhase.STAGE_1) {
            stage1.stopTimers();
        }
        this.countdownEnd = null;
        this.phase = newPhase;
    }

    // ---- start countdown -------------------------------------------------------------------------------------

    /** Starts the countdown after which the game moves from the lobby to stage 1 by itself. */
    public void startCountdown(Instant now) {
        if (phase != GamePhase.LOBBY) {
            throw new IllegalStateException("The game can only be started from the lobby");
        }
        if (countdownEnd != null) {
            throw new IllegalStateException("The countdown is already running");
        }
        countdownEnd = now.plusSeconds(START_COUNTDOWN_SECONDS);
        countdownSeconds = START_COUNTDOWN_SECONDS;
    }

    public void cancelCountdown() {
        countdownEnd = null;
    }

    /** Remaining whole seconds of the running countdown, or {@code null} when none is running. */
    public Integer countdownSeconds() {
        return countdownEnd == null ? null : countdownSeconds;
    }

    /**
     * Called about once per second by the ticker. Moves to stage 1 when the countdown has elapsed.
     *
     * @return {@code true} when the visible countdown value (or the phase) changed
     */
    public boolean tickCountdown(Instant now) {
        if (countdownEnd == null) {
            return false;
        }
        long millisLeft = Duration.between(now, countdownEnd).toMillis();
        int secondsLeft = (int) Math.max(0, (millisLeft + 999) / 1000);
        if (secondsLeft == 0) {
            setPhase(GamePhase.STAGE_1);
            return true;
        }
        if (secondsLeft == countdownSeconds) {
            return false;
        }
        countdownSeconds = secondsLeft;
        return true;
    }

    // ---- teams and scores ------------------------------------------------------------------------------------

    public String teamName(Team team) {
        return teams.get(team).name();
    }

    public void renameTeam(Team team, String name) {
        teams.get(requireTeam(team)).setName(Names.clean(name, MAX_TEAM_NAME_LENGTH, "Team name"));
    }

    public int score(Team team) {
        return teams.get(team).score();
    }

    public void addScore(Team team, int delta) {
        teams.get(requireTeam(team)).addScore(delta);
    }

    public void setScore(Team team, int value) {
        teams.get(requireTeam(team)).setScore(value);
    }

    // ---- players ---------------------------------------------------------------------------------------------

    public Player addPlayer(String rawName, Team team) {
        String name = Names.clean(rawName, MAX_PLAYER_NAME_LENGTH, "Player name");
        requireTeam(team);
        boolean nameTaken = players.values().stream().anyMatch(p -> p.name().equalsIgnoreCase(name));
        if (nameTaken) {
            throw new IllegalStateException("This player name is already taken");
        }
        if (countPlayers(team) >= maxPlayersPerTeam) {
            throw new IllegalStateException("This team is full");
        }
        Player player = new Player(UUID.randomUUID().toString(), name, team);
        players.put(player.id(), player);
        return player;
    }

    public void movePlayer(String playerId, Team team) {
        Player player = requirePlayer(playerId);
        if (requireTeam(team) == player.team()) {
            return;
        }
        if (countPlayers(team) >= maxPlayersPerTeam) {
            throw new IllegalStateException("This team is full");
        }
        players.put(playerId, player.withTeam(team));
    }

    /** @return {@code true} when a player was removed */
    public boolean removePlayer(String playerId) {
        return players.remove(playerId) != null;
    }

    public List<Player> players() {
        return List.copyOf(players.values());
    }

    public List<String> playerNames(Team team) {
        return players.values().stream().filter(p -> p.team() == team).map(Player::name).toList();
    }

    // ---- stages and the drawn question set ---------------------------------------------------------------------

    public Stage1State stage1() {
        return stage1;
    }

    public Stage2State stage2() {
        return stage2;
    }

    public Stage3State stage3() {
        return stage3;
    }

    /** Draws a completely new set of questions for all three stages. Only possible in the lobby. */
    public void redrawAll() {
        requireLobby();
        drawAll();
    }

    /** Replaces one stage-1 category (column) with a random one that is not on the board. Lobby only. */
    public void rerollStage1Column(int col) {
        requireLobby();
        List<String> inUse = stage1.categories();
        List<Stage1Column> candidates = pool.stage1Columns().stream()
                .filter(column -> !inUse.contains(column.name())).toList();
        if (candidates.isEmpty()) {
            throw new IllegalStateException("The question bank has no other categories to draw");
        }
        stage1.replaceColumn(col, candidates.get(random.nextInt(candidates.size())));
    }

    /** Replaces one stage-2 question with a random one that is not in the game. Lobby only. */
    public void rerollStage2Question(int number) {
        requireLobby();
        List<Stage2Question> inUse = stage2.questions();
        stage2.question(number); // validates the number
        List<Stage2Question> candidates = pool.stage2Questions().stream()
                .filter(question -> !inUse.contains(question)).toList();
        if (candidates.isEmpty()) {
            throw new IllegalStateException("The question bank has no other stage-2 questions to draw");
        }
        stage2.replaceQuestion(number, candidates.get(random.nextInt(candidates.size())));
    }

    private void drawAll() {
        ScoreSink scoreSink = this::addScore;
        List<Stage1Column> columns = new ArrayList<>(pool.stage1Columns());
        Collections.shuffle(columns, random);
        this.stage1 = new Stage1State(scoreSink, columns.subList(0, Stage1State.SIZE));

        List<Stage2Question> questions = new ArrayList<>(pool.stage2Questions());
        Collections.shuffle(questions, random);
        this.stage2 = new Stage2State(questions.subList(0, Stage2State.QUESTION_COUNT));

        this.stage3 = new Stage3State(scoreSink, pool.stage3Cards(), random);
    }

    private void requireLobby() {
        if (phase != GamePhase.LOBBY) {
            throw new IllegalStateException("The questions can only be changed in the lobby, before the game starts");
        }
    }

    // ---- helpers ---------------------------------------------------------------------------------------------

    private int countPlayers(Team team) {
        return (int) players.values().stream().filter(p -> p.team() == team).count();
    }

    private Player requirePlayer(String playerId) {
        Player player = playerId == null ? null : players.get(playerId);
        if (player == null) {
            throw new NoSuchElementException("Player not found");
        }
        return player;
    }

    private static Team requireTeam(Team team) {
        if (team == null) {
            throw new IllegalArgumentException("team is required");
        }
        return team;
    }
}
