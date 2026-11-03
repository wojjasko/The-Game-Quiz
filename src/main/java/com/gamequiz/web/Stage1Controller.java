package com.gamequiz.web;

import com.gamequiz.domain.common.Team;
import com.gamequiz.service.GameCommands;
import com.gamequiz.web.dto.GameDtos.HostSnapshot;
import com.gamequiz.web.dto.GameDtos.TextRequest;
import com.gamequiz.web.dto.GameDtos.TileRequest;
import com.gamequiz.web.dto.GameDtos.ZoomRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

/** Host commands of stage 1 (question matrix). */
@RestController
@RequestMapping("/api/games/{code}/stage1")
public class Stage1Controller {

    private static final String TOKEN = GameController.HOST_TOKEN;

    private final GameCommands commands;

    public Stage1Controller(GameCommands commands) {
        this.commands = commands;
    }

    /** Draws another category for one column (lobby only). */
    @PostMapping("/columns/{col}/reroll")
    public HostSnapshot rerollColumn(@PathVariable String code, @PathVariable int col,
                                     @RequestHeader(TOKEN) String token) {
        return commands.host(code, token, s -> s.rerollStage1Column(col));
    }

    @PutMapping("/tiles/{row}/{col}")
    public HostSnapshot setTile(@PathVariable String code, @PathVariable int row, @PathVariable int col,
                                @RequestHeader(TOKEN) String token, @RequestBody TileRequest request) {
        return commands.host(code, token,
                s -> s.stage1().setTile(row, col, Args.required(request.status(), "status")));
    }

    @PostMapping("/tiles/{row}/{col}/activate")
    public HostSnapshot activateTile(@PathVariable String code, @PathVariable int row, @PathVariable int col,
                                     @RequestHeader(TOKEN) String token) {
        return commands.host(code, token, s -> s.stage1().activateTile(row, col));
    }

    @PutMapping("/current-question")
    public HostSnapshot setCurrentQuestion(@PathVariable String code, @RequestHeader(TOKEN) String token,
                                           @RequestBody TextRequest request) {
        return commands.host(code, token, s -> s.stage1().setCurrentText(request.text()));
    }

    @DeleteMapping("/current-question")
    public HostSnapshot clearCurrentQuestion(@PathVariable String code, @RequestHeader(TOKEN) String token) {
        return commands.host(code, token, s -> s.stage1().clearCurrentQuestion());
    }

    @PutMapping("/image-zoom")
    public HostSnapshot setImageZoom(@PathVariable String code, @RequestHeader(TOKEN) String token,
                                     @RequestBody ZoomRequest request) {
        return commands.host(code, token,
                s -> s.stage1().setImageZoomed(Args.required(request.zoomed(), "zoomed")));
    }

    @PostMapping("/timers/{team}/{action}")
    public HostSnapshot controlTimer(@PathVariable String code, @PathVariable Team team,
                                     @PathVariable String action, @RequestHeader(TOKEN) String token) {
        return commands.host(code, token, s -> {
            switch (action.toLowerCase(Locale.ROOT)) {
                case "start" -> s.stage1().startTimer(team);
                case "stop" -> s.stage1().stopTimer(team);
                case "reset" -> s.stage1().resetTimer(team);
                default -> throw new IllegalArgumentException("Unknown timer action: " + action);
            }
        });
    }

    @PostMapping("/steals/{team}/spend")
    public HostSnapshot spendSteal(@PathVariable String code, @PathVariable Team team,
                                   @RequestHeader(TOKEN) String token) {
        return commands.host(code, token, s -> s.stage1().spendSteal(team));
    }

    @PostMapping("/steals/reset")
    public HostSnapshot resetSteals(@PathVariable String code, @RequestHeader(TOKEN) String token) {
        return commands.host(code, token, s -> s.stage1().resetSteals());
    }

    @PostMapping("/time-bonus/{team}")
    public HostSnapshot awardTimeBonus(@PathVariable String code, @PathVariable Team team,
                                       @RequestHeader(TOKEN) String token) {
        return commands.host(code, token, s -> s.stage1().awardTimeBonus(team));
    }

    @PostMapping("/reset")
    public HostSnapshot reset(@PathVariable String code, @RequestHeader(TOKEN) String token) {
        return commands.host(code, token, s -> s.stage1().reset());
    }
}
