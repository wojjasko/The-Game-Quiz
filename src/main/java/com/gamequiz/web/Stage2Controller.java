package com.gamequiz.web;

import com.gamequiz.service.GameCommands;
import com.gamequiz.web.dto.GameDtos.FocusRequest;
import com.gamequiz.web.dto.GameDtos.HostSnapshot;
import com.gamequiz.web.dto.GameDtos.OutcomeRequest;
import com.gamequiz.web.dto.GameDtos.QuestionNumberRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Host commands of stage 2 ("which game is this?"). */
@RestController
@RequestMapping("/api/games/{code}/stage2")
public class Stage2Controller {

    private static final String TOKEN = GameController.HOST_TOKEN;

    private final GameCommands commands;

    public Stage2Controller(GameCommands commands) {
        this.commands = commands;
    }

    @PutMapping("/question")
    public HostSnapshot selectQuestion(@PathVariable String code, @RequestHeader(TOKEN) String token,
                                       @RequestBody QuestionNumberRequest request) {
        return commands.host(code, token,
                s -> s.stage2().selectQuestion(Args.required(request.number(), "number")));
    }

    @PutMapping("/questions/{number}/outcome")
    public HostSnapshot setOutcome(@PathVariable String code, @PathVariable int number,
                                   @RequestHeader(TOKEN) String token, @RequestBody OutcomeRequest request) {
        return commands.host(code, token,
                s -> s.stage2().setOutcome(number, Args.required(request.outcome(), "outcome")));
    }

    @PutMapping("/focus")
    public HostSnapshot focusImage(@PathVariable String code, @RequestHeader(TOKEN) String token,
                                   @RequestBody FocusRequest request) {
        return commands.host(code, token,
                s -> s.stage2().focusImage(Args.required(request.imageIndex(), "imageIndex")));
    }

    @DeleteMapping("/focus")
    public HostSnapshot clearFocus(@PathVariable String code, @RequestHeader(TOKEN) String token) {
        return commands.host(code, token, s -> s.stage2().clearFocus());
    }
}
