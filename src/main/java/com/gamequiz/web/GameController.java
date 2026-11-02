package com.gamequiz.web;

import com.gamequiz.domain.GameSession;
import com.gamequiz.domain.common.Team;
import com.gamequiz.service.GameCommands;
import com.gamequiz.service.GameRegistry;
import com.gamequiz.service.MediaCatalog;
import com.gamequiz.web.dto.GameDtos.CreateGameRequest;
import com.gamequiz.web.dto.GameDtos.CreateGameResponse;
import com.gamequiz.web.dto.GameDtos.GameSnapshot;
import com.gamequiz.web.dto.GameDtos.HostSnapshot;
import com.gamequiz.web.dto.GameDtos.JoinRequest;
import com.gamequiz.web.dto.GameDtos.JoinResponse;
import com.gamequiz.web.dto.GameDtos.MediaListing;
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

/** Game lifecycle, lobby, teams and scores. Host endpoints require the {@code X-Host-Token} header. */
@RestController
@RequestMapping("/api/games")
public class GameController {

    static final String HOST_TOKEN = "X-Host-Token";

    private final GameRegistry registry;
    private final GameCommands commands;
    private final MediaCatalog media;

    public GameController(GameRegistry registry, GameCommands commands, MediaCatalog media) {
        this.registry = registry;
        this.commands = commands;
        this.media = media;
    }

    // ---- public ----------------------------------------------------------------------------------------------

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateGameResponse create(@RequestBody(required = false) CreateGameRequest request) {
        GameSession session = registry.create(request == null ? null : request.accessCode());
        return new CreateGameResponse(session.code(), session.hostToken());
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

    @GetMapping("/{code}/host/media")
    public MediaListing mediaListing(@PathVariable String code, @RequestHeader(HOST_TOKEN) String token) {
        commands.requireHost(code, token);
        return new MediaListing(media.listStage1Images(), media.listSounds());
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
