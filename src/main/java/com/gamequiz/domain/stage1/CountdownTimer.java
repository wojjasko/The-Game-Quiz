package com.gamequiz.domain.stage1;

/** A simple second-based countdown driven by an external ticker. Not thread-safe on its own. */
public final class CountdownTimer {

    private final int initialSeconds;
    private int remainingSeconds;
    private boolean running;

    public CountdownTimer(int initialSeconds) {
        this.initialSeconds = initialSeconds;
        this.remainingSeconds = initialSeconds;
    }

    public void start() {
        if (remainingSeconds > 0) {
            running = true;
        }
    }

    public void stop() {
        running = false;
    }

    public void reset() {
        remainingSeconds = initialSeconds;
        running = false;
    }

    /**
     * Advances the timer by one second. The timer stops by itself when it reaches zero.
     *
     * @return {@code true} when something visible changed and clients should be notified
     */
    public boolean tick() {
        if (!running) {
            return false;
        }
        if (remainingSeconds > 0) {
            remainingSeconds--;
        }
        if (remainingSeconds == 0) {
            running = false;
        }
        return true;
    }

    public int remainingSeconds() {
        return remainingSeconds;
    }

    public boolean isRunning() {
        return running;
    }
}
