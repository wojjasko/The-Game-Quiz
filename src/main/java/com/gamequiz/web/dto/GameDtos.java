package com.gamequiz.web.dto;

import com.gamequiz.domain.common.GamePhase;
import com.gamequiz.domain.common.Team;
import com.gamequiz.domain.stage1.Stage1Column;
import com.gamequiz.domain.stage1.TileStatus;
import com.gamequiz.domain.stage2.Stage2Outcome;
import com.gamequiz.domain.stage2.Stage2Question;
import com.gamequiz.domain.stage3.AnswerKey;
import com.gamequiz.domain.stage3.CardResult;

import java.util.List;
import java.util.Map;

/**
 * JSON shapes sent to clients. See {@code docs/API.md} for the contract.
 * Public views never contain secrets such as the correct answer of a stage-3 card.
 */
public final class GameDtos {

    private GameDtos() {
    }

    // ---- public snapshot (broadcast to everybody) --------------------------------------------------------------

    /**
     * Exactly one of the stage views is non-null: the one that matches {@code phase}.
     * {@code countdown} is the number of seconds left until the game starts, or {@code null}.
     */
    public record GameSnapshot(String code, GamePhase phase, Integer countdown, Map<Team, TeamView> teams,
                               Stage1View stage1, Stage2View stage2, Stage3View stage3) {
    }

    public record TeamView(String name, int score, List<String> players) {
    }

    public record TimerView(int remainingSeconds, boolean running) {
    }

    public record Stage1View(List<List<TileStatus>> board, List<String> categories, String question,
                             String imageUrl, String soundUrl, boolean imageZoomed,
                             Map<Team, TimerView> timers, Map<Team, Integer> steals,
                             List<int[][]> completedLines) {
    }

    public record ImageView(int index, String url, boolean revealed) {
    }

    /** {@code images} describes only the selected question so that other images are not leaked early. */
    public record Stage2View(int selectedQuestion, List<Stage2Outcome> outcomes, List<ImageView> images,
                             Integer focusedImageIndex) {
    }

    public record CardView(int id, String category, boolean used, CardResult result) {
    }

    public record QuestionView(String category, String text, List<String> answers, List<AnswerKey> hiddenAnswers) {
    }

    public record Stage3View(List<CardView> table, Map<Team, Integer> multipliers,
                             Map<Team, Boolean> lifelineUsed, QuestionView question) {
    }

    // ---- host snapshot (only returned to the host) ---------------------------------------------------------------

    public record PlayerView(String id, String name, Team team) {
    }

    public record Stage3HostView(int activeCardId, AnswerKey correctAnswer, String correctAnswerText) {
    }

    /** A stage-3 card as the host sees it: with the question and the correct answer. */
    public record Stage3HostCard(int id, String category, String question, String correctAnswer, boolean used) {
    }

    /**
     * Everything the host needs: the drawn question set of every stage (with answers) is available in every
     * phase so it can be reviewed and re-drawn in the lobby.
     */
    public record HostSnapshot(GameSnapshot game, List<PlayerView> players, List<Stage1Column> stage1Columns,
                               List<Stage2Question> stage2Questions, List<Stage3HostCard> stage3Cards,
                               Stage3HostView stage3) {
    }

    // ---- requests and responses -------------------------------------------------------------------------------

    public record CreateGameRequest(String accessCode) {
    }

    public record CreateGameResponse(String code, String hostToken) {
    }

    /** {@code code} is {@code null} when no game is running. */
    public record CurrentGameResponse(String code) {
    }

    public record JoinRequest(String name, Team team) {
    }

    public record JoinResponse(String playerId, String name, Team team) {
    }

    public record PhaseRequest(GamePhase phase) {
    }

    public record NameRequest(String name) {
    }

    public record ScoreDeltaRequest(Integer delta) {
    }

    public record ScoreValueRequest(Integer value) {
    }

    public record TeamRequest(Team team) {
    }

    public record TileRequest(TileStatus status) {
    }

    public record TextRequest(String text) {
    }

    public record ZoomRequest(Boolean zoomed) {
    }

    public record QuestionNumberRequest(Integer number) {
    }

    public record OutcomeRequest(Stage2Outcome outcome) {
    }

    public record FocusRequest(Integer imageIndex) {
    }

    public record LifelineRequest(Team team) {
    }

    public record AnswerRequest(Team team, Boolean correct) {
    }

    public record ErrorResponse(String error) {
    }
}
