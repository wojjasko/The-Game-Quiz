package com.gamequiz.web;

import com.gamequiz.domain.GameSession;
import com.gamequiz.domain.common.Team;
import com.gamequiz.service.GameCommands;
import com.gamequiz.service.GameRegistry;
import com.gamequiz.web.dto.GameDtos.CreateGameRequest;
import com.gamequiz.web.dto.GameDtos.CreateGameResponse;
import com.gamequiz.web.dto.GameDtos.CurrentGameResponse;
import com.gamequiz.web.dto.GameDtos.GameSnapshot;
import com.gamequiz.web.dto.GameDtos.HostSnapshot;
import com.gamequiz.web.dto.GameDtos.JoinRequest;
import com.gamequiz.web.dto.GameDtos.JoinResponse;
import com.gamequiz.web.dto.GameDtos.NameRequest;
import com.gamequiz.web.dto.GameDtos.PhaseRequest;
import com.gamequiz.web.dto.GameDtos.ScoreDeltaRequest;
import com.gamequiz.web.dto.GameDtos.ScoreValueRequest;
import com.gamequiz.web.dto.GameDtos.TeamRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/** Game lifecycle, lobby, teams and scores. Host endpoints require the {@code X-Host-Token} header. */
@RestController
@RequestMapping("/api/games")
public class GameController {

    static final String HOST_TOKEN = "X-Host-Token";
    static final String ACCESS_CODE = "X-Access-Code";

    private final GameRegistry registry;
    private final GameCommands commands;

    public GameController(GameRegistry registry, GameCommands commands) {
        this.registry = registry;
        this.commands = commands;
    }

    // ---- public ----------------------------------------------------------------------------------------------

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateGameResponse create(@RequestBody(required = false) CreateGameRequest request) {
        GameSession session = registry.create(request == null ? null : request.accessCode());
        return new CreateGameResponse(session.code(), session.hostToken());
    }

    /** The running game (the server hosts one at a time); {@code code} is {@code null} when there is none. */
    @GetMapping("/current")
    public CurrentGameResponse current() {
        return new CurrentGameResponse(registry.current().map(GameSession::code).orElse(null));
    }

    @GetMapping("/{code}")
    public GameSnapshot get(@PathVariable String code) {
        return commands.snapshot(code);
    }

    @PostMapping("/{code}/players")
    @ResponseStatus(HttpStatus.CREATED)
    public JoinResponse join(@PathVariable String code, @RequestBody JoinRequest request) {
        return commands.join(code, request.name(), Args.required(request.team(), "team"));
    }

    @DeleteMapping("/{code}/players/{playerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable String code, @PathVariable String playerId) {
        commands.leave(code, playerId);
    }

    // ---- host ------------------------------------------------------------------------------------------------

    @GetMapping("/{code}/host")
    public HostSnapshot hostView(@PathVariable String code, @RequestHeader(HOST_TOKEN) String token) {
        return commands.hostView(code, token);
    }

    /** Closes the room. Needs the host token or the access code (for a host who lost the token). */
    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(@PathVariable String code,
                      @RequestHeader(value = HOST_TOKEN, required = false) String token,
                      @RequestHeader(value = ACCESS_CODE, required = false) String accessCode) {
        commands.close(code, token, accessCode);
    }

    /** Starts the countdown (10 s) after which the game moves to stage 1 by itself. */
    @PostMapping("/{code}/start")
    public HostSnapshot start(@PathVariable String code, @RequestHeader(HOST_TOKEN) String token) {
        return commands.host(code, token, session -> session.startCountdown(Instant.now()));
    }

    @DeleteMapping("/{code}/start")
    public HostSnapshot cancelStart(@PathVariable String code, @RequestHeader(HOST_TOKEN) String token) {
        return commands.host(code, token, GameSession::cancelCountdown);
    }

    /** Draws a whole new question set for all stages (lobby only). */
    @PostMapping("/{code}/questions/redraw")
    public HostSnapshot redraw(@PathVariable String code, @RequestHeader(HOST_TOKEN) String token) {
        return commands.host(code, token, GameSession::redrawAll);
    }

    @PutMapping("/{code}/phase")
    public HostSnapshot setPhase(@PathVariable String code, @RequestHeader(HOST_TOKEN) String token,
                                 @RequestBody PhaseRequest request) {
        return commands.host(code, token, session -> session.setPhase(Args.required(request.phase(), "phase")));
    }

    @PutMapping("/{code}/teams/{team}/name")
    public HostSnapshot renameTeam(@PathVariable String code, @PathVariable Team team,
                                   @RequestHeader(HOST_TOKEN) String token, @RequestBody NameRequest request) {
        return commands.host(code, token, session -> session.renameTeam(team, request.name()));
    }

    @PostMapping("/{code}/teams/{team}/score")
    public HostSnapshot addScore(@PathVariable String code, @PathVariable Team team,
                                 @RequestHeader(HOST_TOKEN) String token, @RequestBody ScoreDeltaRequest request) {
        return commands.host(code, token,
                session -> session.addScore(team, Args.required(request.delta(), "delta")));
    }

    @PutMapping("/{code}/teams/{team}/score")
    public HostSnapshot setScore(@PathVariable String code, @PathVariable Team team,
                                 @RequestHeader(HOST_TOKEN) String token, @RequestBody ScoreValueRequest request) {
        return commands.host(code, token,
                session -> session.setScore(team, Args.required(request.value(), "value")));
    }

    @PutMapping("/{code}/players/{playerId}/team")
    public HostSnapshot movePlayer(@PathVariable String code, @PathVariable String playerId,
                                   @RequestHeader(HOST_TOKEN) String token, @RequestBody TeamRequest request) {
        return commands.host(code, token,
                session -> session.movePlayer(playerId, Args.required(request.team(), "team")));
    }
}
