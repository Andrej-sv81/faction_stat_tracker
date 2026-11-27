package com.dnd.simulator.service;

import com.dnd.simulator.dto.PasswordResetEmailRequested;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.validation.annotation.Validated;

@Validated
@Service
public class PasswordResetEmailService {
  private static final Logger logger =
      LoggerFactory.getLogger(PasswordResetEmailService.class);

  private final JavaMailSender mailSender;

  public PasswordResetEmailService(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendPasswordResetEmail(@Valid PasswordResetEmailRequested request) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(request.fromAddress());
    message.setTo(request.email());
    message.setSubject("Reset your password");
    message.setText(
        "Use this link to reset your password. The link expires in "
            + request.expirationMinutes()
            + " minutes:\n\n"
            + request.resetUrl()
            + "\n\nIf you did not request a password reset, you can ignore this email.");
    try {
      mailSender.send(message);
    } catch (MailException exception) {
      logger.error(
          "Unable to deliver password reset email for user id {}",
          request.userId(),
          exception);
    }
  }
}
