package com.gamequiz.service;

import com.gamequiz.domain.GameSession;
import com.gamequiz.domain.Player;
import com.gamequiz.domain.common.Team;
import com.gamequiz.web.dto.GameDtos.GameSnapshot;
import com.gamequiz.web.dto.GameDtos.HostSnapshot;
import com.gamequiz.web.dto.GameDtos.JoinResponse;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.function.Consumer;

/**
 * The single entry point for everything that changes or reads a game. For each command it
 * <ol>
 *     <li>finds the game and (for host commands) checks the host token,</li>
 *     <li>runs the change under the session lock and builds the snapshots while still holding it,</li>
 *     <li>broadcasts the new public snapshot after the lock is released.</li>
 * </ol>
 * Controllers therefore only describe <em>what</em> changes.
 */
@Service
public class GameCommands {

    private final GameRegistry registry;
    private final GameViews views;
    private final GameBroadcaster broadcaster;

    public GameCommands(GameRegistry registry, GameViews views, GameBroadcaster broadcaster) {
        this.registry = registry;
        this.views = views;
        this.broadcaster = broadcaster;
    }

    /** Runs a host action, broadcasts the result and returns the host view. */
    public HostSnapshot host(String code, String hostToken, Consumer<GameSession> action) {
        GameSession session = requireHost(code, hostToken);
        Snapshots snapshots = session.locked(() -> {
            action.accept(session);
            session.touch();
            return new Snapshots(views.publicSnapshot(session), views.hostSnapshot(session));
        });
        broadcaster.sendState(session.code(), snapshots.publicView());
        return snapshots.hostView();
    }

    /** The host view without changing anything (initial load, re-sync). */
    public HostSnapshot hostView(String code, String hostToken) {
        GameSession session = requireHost(code, hostToken);
        return session.locked(() -> views.hostSnapshot(session));
    }

    /** Verifies the host token and returns the game. */
    public GameSession requireHost(String code, String hostToken) {
        GameSession session = registry.require(code);
        if (hostToken == null || !MessageDigest.isEqual(
                hostToken.getBytes(StandardCharsets.UTF_8), session.hostToken().getBytes(StandardCharsets.UTF_8))) {
            throw new ForbiddenException("Missing or wrong host token");
        }
        return session;
    }

    public GameSnapshot snapshot(String code) {
        GameSession session = registry.require(code);
        return session.locked(() -> views.publicSnapshot(session));
    }

    public JoinResponse join(String code, String name, Team team) {
        GameSession session = registry.require(code);
        Snapshots snapshots = session.locked(() -> {
            Player player = session.addPlayer(name, team);
            session.touch();
            return new Snapshots(views.publicSnapshot(session), player);
        });
        broadcaster.sendState(session.code(), snapshots.publicView());
        Player player = snapshots.player();
        return new JoinResponse(player.id(), player.name(), player.team());
    }

    /** A player leaves, or the host kicks somebody: knowing the secret player id is enough. */
    public void leave(String code, String playerId) {
        GameSession session = registry.require(code);
        GameSnapshot snapshot = session.locked(() -> {
            if (!session.removePlayer(playerId)) {
                throw new java.util.NoSuchElementException("Player not found");
            }
            session.touch();
            return views.publicSnapshot(session);
        });
        broadcaster.sendState(session.code(), snapshot);
    }

    private record Snapshots(GameSnapshot publicView, HostSnapshot hostView, Player player) {

        Snapshots(GameSnapshot publicView, HostSnapshot hostView) {
            this(publicView, hostView, null);
        }

        Snapshots(GameSnapshot publicView, Player player) {
            this(publicView, null, player);
        }
    }
}
