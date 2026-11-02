package com.gamequiz.service;

import com.gamequiz.domain.common.Team;
import com.gamequiz.web.dto.GameDtos.GameSnapshot;
import com.gamequiz.web.dto.GameDtos.TimerView;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Pushes game updates to every client subscribed to a game. */
@Component
public class GameBroadcaster {

    private final SimpMessagingTemplate messaging;

    public GameBroadcaster(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    public void sendState(String gameCode, GameSnapshot snapshot) {
        messaging.convertAndSend("/topic/game/" + gameCode + "/state", snapshot);
    }

    public void sendTimers(String gameCode, Map<Team, TimerView> timers) {
        messaging.convertAndSend("/topic/game/" + gameCode + "/timers", timers);
    }
}
