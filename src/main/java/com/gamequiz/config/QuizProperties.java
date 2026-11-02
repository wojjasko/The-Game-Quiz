package com.gamequiz.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Application settings, bound from {@code quiz.*} properties (see {@code application.properties}). */
@Component
@ConfigurationProperties(prefix = "quiz")
public class QuizProperties {

    /** When not blank, creating a game requires this code. Use it to restrict the server to chosen people. */
    private String accessCode = "";

    /** Directory with {@code images/stage1}, {@code images/stage2} and {@code sounds}. */
    private String mediaDir = "./media";

    private int maxGames = 50;
    private int gameIdleTimeoutMinutes = 360;
    private int maxPlayersPerTeam = 8;

    /** Allowed origins of WebSocket handshakes. */
    private List<String> allowedOrigins = new ArrayList<>(List.of("*"));

    public String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }

    public String getMediaDir() {
        return mediaDir;
    }

    public void setMediaDir(String mediaDir) {
        this.mediaDir = mediaDir;
    }

    public int getMaxGames() {
        return maxGames;
    }

    public void setMaxGames(int maxGames) {
        this.maxGames = maxGames;
    }

    public int getGameIdleTimeoutMinutes() {
        return gameIdleTimeoutMinutes;
    }

    public void setGameIdleTimeoutMinutes(int gameIdleTimeoutMinutes) {
        this.gameIdleTimeoutMinutes = gameIdleTimeoutMinutes;
    }

    public int getMaxPlayersPerTeam() {
        return maxPlayersPerTeam;
    }

    public void setMaxPlayersPerTeam(int maxPlayersPerTeam) {
        this.maxPlayersPerTeam = maxPlayersPerTeam;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
