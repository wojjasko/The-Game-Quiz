package com.gamequiz.domain;

import com.gamequiz.domain.common.Team;

/**
 * A participant of a game. The {@code id} is a secret known only to the player and the host
 * (it works as a capability for leaving the game).
 */
public record Player(String id, String name, Team team) {

    public Player withTeam(Team newTeam) {
        return new Player(id, name, newTeam);
    }
}
