package com.gamequiz.domain;

import com.gamequiz.domain.common.GamePhase;
import com.gamequiz.domain.common.Names;
import com.gamequiz.domain.common.ScoreSink;
import com.gamequiz.domain.common.Team;
import com.gamequiz.domain.stage1.Stage1State;
import com.gamequiz.domain.stage2.Stage2State;
import com.gamequiz.domain.stage3.QuizCard;
import com.gamequiz.domain.stage3.Stage3State;

import java.time.Instant;
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

    private final Stage1State stage1;
    private final Stage2State stage2;
    private final Stage3State stage3;

    /**
     * @param stage2Images base name to file name of the stage-2 images found on disk
     * @param cardPool     the stage-3 questions to draw the table from
     */
    public GameSession(String code, String hostToken, int maxPlayersPerTeam, Map<String, String> stage2Images,
                       List<QuizCard> cardPool, RandomGenerator random) {
        this.code = code;
        this.hostToken = hostToken;
        this.maxPlayersPerTeam = maxPlayersPerTeam;
        teams.put(Team.BLUE, new TeamState(DEFAULT_BLUE_NAME));
        teams.put(Team.RED, new TeamState(DEFAULT_RED_NAME));

        ScoreSink scoreSink = this::addScore;
        this.stage1 = new Stage1State(scoreSink);
        this.stage2 = new Stage2State(stage2Images);
        this.stage3 = new Stage3State(scoreSink, cardPool, random);
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
        this.phase = newPhase;
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

    // ---- stages ----------------------------------------------------------------------------------------------

    public Stage1State stage1() {
        return stage1;
    }

    public Stage2State stage2() {
        return stage2;
    }

    public Stage3State stage3() {
        return stage3;
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
