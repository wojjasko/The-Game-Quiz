package com.gamequiz.web;

import com.gamequiz.service.GameCommands;
import com.gamequiz.web.dto.GameDtos.AnswerRequest;
import com.gamequiz.web.dto.GameDtos.HostSnapshot;
import com.gamequiz.web.dto.GameDtos.LifelineRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Host commands of stage 3 (cards). */
@RestController
@RequestMapping("/api/games/{code}/stage3")
public class Stage3Controller {

    private static final String TOKEN = GameController.HOST_TOKEN;

    private final GameCommands commands;

    public Stage3Controller(GameCommands commands) {
        this.commands = commands;
    }

    @PostMapping("/cards/{id}/swap")
    public HostSnapshot swapCard(@PathVariable String code, @PathVariable int id,
                                 @RequestHeader(TOKEN) String token) {
        return commands.host(code, token, s -> s.stage3().swapCard(id));
    }

    @PostMapping("/cards/{id}/open")
    public HostSnapshot openCard(@PathVariable String code, @PathVariable int id,
                                 @RequestHeader(TOKEN) String token) {
        return commands.host(code, token, s -> s.stage3().openCard(id));
    }

    @PostMapping("/lifeline")
    public HostSnapshot useLifeline(@PathVariable String code, @RequestHeader(TOKEN) String token,
                                    @RequestBody LifelineRequest request) {
        return commands.host(code, token,
                s -> s.stage3().useLifeline(Args.required(request.team(), "team")));
    }

    @PostMapping("/answer")
    public HostSnapshot answer(@PathVariable String code, @RequestHeader(TOKEN) String token,
                               @RequestBody AnswerRequest request) {
        return commands.host(code, token, s -> s.stage3().answer(
                Args.required(request.team(), "team"), Args.required(request.correct(), "correct")));
    }

    @PostMapping("/close")
    public HostSnapshot close(@PathVariable String code, @RequestHeader(TOKEN) String token) {
        return commands.host(code, token, s -> s.stage3().closeQuestion());
    }
}
