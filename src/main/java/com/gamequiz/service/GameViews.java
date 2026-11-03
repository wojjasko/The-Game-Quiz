package com.gamequiz.service;

import com.gamequiz.domain.GameSession;
import com.gamequiz.domain.Player;
import com.gamequiz.domain.common.GamePhase;
import com.gamequiz.domain.common.Team;
import com.gamequiz.domain.stage1.Line;
import com.gamequiz.domain.stage1.Stage1State;
import com.gamequiz.domain.stage1.TileStatus;
import com.gamequiz.domain.stage2.Stage2State;
import com.gamequiz.domain.stage2.Stage2Outcome;
import com.gamequiz.domain.stage3.QuizCard;
import com.gamequiz.domain.stage3.Stage3State;
import com.gamequiz.domain.stage3.TableCard;
import com.gamequiz.web.dto.GameDtos.CardView;
import com.gamequiz.web.dto.GameDtos.GameSnapshot;
import com.gamequiz.web.dto.GameDtos.HostSnapshot;
import com.gamequiz.web.dto.GameDtos.ImageView;
import com.gamequiz.web.dto.GameDtos.PlayerView;
import com.gamequiz.web.dto.GameDtos.QuestionView;
import com.gamequiz.web.dto.GameDtos.Stage1View;
import com.gamequiz.web.dto.GameDtos.Stage2View;
import com.gamequiz.web.dto.GameDtos.Stage3HostCard;
import com.gamequiz.web.dto.GameDtos.Stage3HostView;
import com.gamequiz.web.dto.GameDtos.Stage3View;
import com.gamequiz.web.dto.GameDtos.TeamView;
import com.gamequiz.web.dto.GameDtos.TimerView;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Maps the mutable domain model to immutable JSON views. Every method must be called while holding the session
 * lock, so the returned snapshot is consistent and can be sent after the lock is released.
 */
@Component
public class GameViews {

    private final MediaCatalog media;

    public GameViews(MediaCatalog media) {
        this.media = media;
    }

    /** What every client may see. Only the stage of the current phase is filled in. */
    public GameSnapshot publicSnapshot(GameSession session) {
        Map<Team, TeamView> teams = new EnumMap<>(Team.class);
        for (Team team : Team.values()) {
            teams.put(team, new TeamView(session.teamName(team), session.score(team), session.playerNames(team)));
        }
        GamePhase phase = session.phase();
        return new GameSnapshot(
                session.code(),
                phase,
                session.countdownSeconds(),
                teams,
                phase == GamePhase.STAGE_1 ? stage1(session.stage1()) : null,
                phase == GamePhase.STAGE_2 ? stage2(session.stage2()) : null,
                phase == GamePhase.STAGE_3 ? stage3(session.stage3()) : null);
    }

    /** The public snapshot plus everything only the host may see. */
    public HostSnapshot hostSnapshot(GameSession session) {
        List<PlayerView> players = new ArrayList<>();
        for (Player player : session.players()) {
            players.add(new PlayerView(player.id(), player.name(), player.team()));
        }

        List<Stage3HostCard> cards = new ArrayList<>();
        for (TableCard tableCard : session.stage3().table()) {
            QuizCard card = tableCard.card();
            cards.add(new Stage3HostCard(card.id(), card.category(), card.question(),
                    card.correct() + ": " + card.answerText(card.correct()), tableCard.used()));
        }

        Stage3HostView stage3 = null;
        TableCard active = session.stage3().activeCard();
        if (active != null) {
            QuizCard card = active.card();
            stage3 = new Stage3HostView(card.id(), card.correct(), card.answerText(card.correct()));
        }
        return new HostSnapshot(publicSnapshot(session), players, session.stage1().columns(),
                session.stage2().questions(), cards, stage3);
    }

    /** The two stage-1 timers, sent on their own every second while a timer runs. */
    public Map<Team, TimerView> timers(GameSession session) {
        return timers(session.stage1());
    }

    // ---- stages ----------------------------------------------------------------------------------------------

    private Stage1View stage1(Stage1State state) {
        List<List<TileStatus>> board = new ArrayList<>();
        for (int row = 0; row < Stage1State.SIZE; row++) {
            List<TileStatus> tiles = new ArrayList<>();
            for (int col = 0; col < Stage1State.SIZE; col++) {
                tiles.add(state.tile(row, col));
            }
            board.add(tiles);
        }
        List<int[][]> lines = new ArrayList<>();
        for (Line line : state.completedLines()) {
            lines.add(line.cells());
        }
        Map<Team, Integer> steals = new EnumMap<>(Team.class);
        for (Team team : Team.values()) {
            steals.put(team, state.steals(team));
        }
        return new Stage1View(
                board,
                state.categories(),
                state.currentText(),
                state.currentImage() == null ? null : media.stage1ImageUrl(state.currentImage()),
                state.currentSound() == null ? null : media.soundUrl(state.currentSound()),
                state.isImageZoomed(),
                timers(state),
                steals,
                lines);
    }

    private Map<Team, TimerView> timers(Stage1State state) {
        Map<Team, TimerView> timers = new EnumMap<>(Team.class);
        for (Team team : Team.values()) {
            timers.put(team, new TimerView(state.remainingSeconds(team), state.isTimerRunning(team)));
        }
        return timers;
    }

    private Stage2View stage2(Stage2State state) {
        List<Stage2Outcome> outcomes = new ArrayList<>();
        for (int number = 1; number <= Stage2State.QUESTION_COUNT; number++) {
            outcomes.add(state.outcome(number));
        }
        int selected = state.selectedQuestion();
        List<ImageView> images = new ArrayList<>();
        for (int index = 0; index < Stage2State.IMAGES_PER_QUESTION; index++) {
            String file = state.imageFile(selected, index);
            images.add(new ImageView(index, media.stage2ImageExists(file) ? media.stage2ImageUrl(file) : null,
                    state.isRevealed(selected, index)));
        }
        return new Stage2View(selected, outcomes, images, state.focusedImage());
    }

    private Stage3View stage3(Stage3State state) {
        List<CardView> table = new ArrayList<>();
        for (TableCard card : state.table()) {
            table.add(new CardView(card.card().id(), card.card().category(), card.used(), card.result()));
        }
        Map<Team, Integer> multipliers = new EnumMap<>(Team.class);
        Map<Team, Boolean> lifelines = new EnumMap<>(Team.class);
        for (Team team : Team.values()) {
            multipliers.put(team, state.multiplier(team));
            lifelines.put(team, state.isLifelineUsed(team));
        }
        QuestionView question = null;
        TableCard active = state.activeCard();
        if (active != null) {
            QuizCard card = active.card();
            question = new QuestionView(card.category(), card.question(), card.answers(),
                    new ArrayList<>(state.hiddenAnswers()));
        }
        return new Stage3View(table, multipliers, lifelines, question);
    }
}
