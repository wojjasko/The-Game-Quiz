package com.gamequiz.service;

import com.gamequiz.domain.GameSession;
import com.gamequiz.domain.common.GamePhase;
import com.gamequiz.domain.common.Team;
import com.gamequiz.web.dto.GameDtos.GameSnapshot;
import com.gamequiz.web.dto.GameDtos.TimerView;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Runs once per second for the running game:
 * <ul>
 *     <li>advances the start countdown (and starts stage 1 when it reaches zero),</li>
 *     <li>advances the stage-1 timers.</li>
 * </ul>
 * Only small messages are sent, and only when something visibly changed.
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
            Tick tick = session.locked(() -> {
                GameSnapshot state = session.tickCountdown(Instant.now()) ? views.publicSnapshot(session) : null;
                Map<Team, TimerView> timers = session.phase() == GamePhase.STAGE_1 && session.stage1().tick()
                        ? views.timers(session) : null;
                return new Tick(state, timers);
            });
            if (tick.state() != null) {
                broadcaster.sendState(session.code(), tick.state());
            }
            if (tick.timers() != null) {
                broadcaster.sendTimers(session.code(), tick.timers());
            }
        }
    }

    private record Tick(GameSnapshot state, Map<Team, TimerView> timers) {
    }
}
