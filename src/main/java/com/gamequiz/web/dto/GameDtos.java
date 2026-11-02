package com.gamequiz.web.dto;

import com.gamequiz.domain.common.GamePhase;
import com.gamequiz.domain.common.Team;
import com.gamequiz.domain.stage1.Stage1Question;
import com.gamequiz.domain.stage1.TileStatus;
import com.gamequiz.domain.stage2.Stage2Outcome;
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

    /** Exactly one of the stage views is non-null: the one that matches {@code phase}. */
    public record GameSnapshot(String code, GamePhase phase, Map<Team, TeamView> teams,
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

    /** {@code stage1Categories} and {@code stage1Questions} are available in every phase so they can be prepared in the lobby. */
    public record HostSnapshot(GameSnapshot game, List<PlayerView> players, List<String> stage1Categories,
                               List<List<Stage1Question>> stage1Questions, Stage3HostView stage3) {
    }

    public record MediaListing(List<String> stage1Images, List<String> sounds) {
    }

    // ---- requests and responses -------------------------------------------------------------------------------

    public record CreateGameRequest(String accessCode) {
    }

    public record CreateGameResponse(String code, String hostToken) {
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

    public record CategoriesRequest(List<String> categories) {
    }

    public record TileRequest(TileStatus status) {
    }

    public record QuestionRequest(String text, String image, String sound) {
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
