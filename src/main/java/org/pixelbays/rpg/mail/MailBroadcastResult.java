package org.pixelbays.rpg.mail;

import javax.annotation.Nonnull;

public class MailBroadcastResult {

    private final int attemptedCount;
    private final int successCount;
    private final int failureCount;
    private final String lastFailureMessage;

    public MailBroadcastResult(int attemptedCount, int successCount, int failureCount, String lastFailureMessage) {
        this.attemptedCount = attemptedCount;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.lastFailureMessage = lastFailureMessage == null ? "" : lastFailureMessage;
    }

    public int getAttemptedCount() {
        return attemptedCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    @Nonnull
    public String getLastFailureMessage() {
        return lastFailureMessage == null ? "" : lastFailureMessage;
    }
}
