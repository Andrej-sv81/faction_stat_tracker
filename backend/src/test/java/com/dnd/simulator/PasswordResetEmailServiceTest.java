package com.dnd.simulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.dnd.simulator.dto.PasswordResetEmailRequested;
import com.dnd.simulator.service.PasswordResetEmailService;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class PasswordResetEmailServiceTest {
  @Test
  void sendsResetLinkToTheRequestedAccount() {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    PasswordResetEmailService service =
        new PasswordResetEmailService(mailSender);
    PasswordResetEmailRequested request =
        new PasswordResetEmailRequested(
            "user-1",
            "keeper@example.com",
            "https://example.com/reset-password?token=secure-token",
            15,
            "noreply@example.com");

    service.sendPasswordResetEmail(request);

    var messageCaptor =
        org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(messageCaptor.capture());
    SimpleMailMessage message = messageCaptor.getValue();
    assertEquals("noreply@example.com", message.getFrom());
    assertEquals("keeper@example.com", message.getTo()[0]);
    assertTrue(message.getText().contains(request.resetUrl()));
    assertTrue(message.getText().contains("15 minutes"));
  }
}
