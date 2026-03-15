package org.pixelbays.rpg.mail;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MailActionResult {

    private final boolean success;
    private final String message;
    private final MailMessage mailMessage;

    private MailActionResult(boolean success, String message, @Nullable MailMessage mailMessage) {
        this.success = success;
        this.message = message == null ? "" : message;
        this.mailMessage = mailMessage;
    }

    @Nonnull
    public static MailActionResult success(String message) {
        return new MailActionResult(true, message, null);
    }

    @Nonnull
    public static MailActionResult success(String message, MailMessage mailMessage) {
        return new MailActionResult(true, message, mailMessage);
    }

    @Nonnull
    public static MailActionResult failure(String message) {
        return new MailActionResult(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    @Nullable
    public MailMessage getMailMessage() {
        return mailMessage;
    }
}
