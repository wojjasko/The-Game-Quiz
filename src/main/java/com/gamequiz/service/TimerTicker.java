package com.gamequiz.service;

import com.gamequiz.domain.GameSession;
import com.gamequiz.domain.common.GamePhase;
import com.gamequiz.domain.common.Team;
import com.gamequiz.web.dto.GameDtos.TimerView;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Advances the stage-1 timers of all games once per second. Only a small timers message is sent (not the whole
 * board), and only for games where a timer actually changed.
 */
@Component
public class TimerTicker {

    private final GameRegistry registry;
    private final GameViews views;
    private final GameBroadcaster broadcaster;

    public TimerTicker(GameRegistry registry, GameViews views, GameBroadcaster broadcaster) {
        this.registry = registry;
        this.views = views;
        this.broadcaster = broadcaster;
    }

    @Scheduled(fixedRate = 1000)
    public void tick() {
        for (GameSession session : registry.all()) {
            Map<Team, TimerView> timers = session.locked(() -> {
                if (session.phase() != GamePhase.STAGE_1 || !session.stage1().tick()) {
                    return null;
                }
                return views.timers(session);
            });
            if (timers != null) {
                broadcaster.sendTimers(session.code(), timers);
            }
        }
    }
}
