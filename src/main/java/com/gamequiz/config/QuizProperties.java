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

    /** Directory with the editable question bank: {@code stage1-categories.json}, {@code stage2-questions.json}, {@code stage3-cards.json}. */
    private String dataDir = "./data";

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

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
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
