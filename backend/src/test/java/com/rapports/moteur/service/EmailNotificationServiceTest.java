package com.rapports.moteur.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailNotificationService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailNotificationService(mailSender);
    }

    @Test
    @DisplayName("Devrait envoyer un courriel avec pièce jointe lorsque mailSender est disponible")
    void testSendReportEmailSuccess() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        int sent = emailService.sendReportEmail(
                List.of("client1@alpha.com", "client2@alpha.com"),
                "[Rapport] Hebdomadaire",
                "<p>Voici votre rapport</p>",
                "rapport.pdf",
                "application/pdf",
                "%PDF-1.4 test content".getBytes()
        );

        assertThat(sent).isEqualTo(2);
        verify(mailSender, times(2)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Devrait gérer gracieusement une liste de destinataires vide")
    void testSendReportEmailEmptyDestinataires() {
        int sent = emailService.sendReportEmail(
                List.of(),
                "Subject",
                "Body",
                "test.pdf",
                "application/pdf",
                new byte[]{1, 2, 3}
        );

        assertThat(sent).isEqualTo(0);
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("Devrait simuler l'envoi quand mailSender est nul")
    void testFallbackWhenMailSenderNull() {
        EmailNotificationService serviceWithoutSender = new EmailNotificationService(null);
        int sent = serviceWithoutSender.sendReportEmail(
                List.of("test@example.com"),
                "Subject",
                "Body",
                null,
                null,
                null
        );

        assertThat(sent).isEqualTo(1);
    }

    @Test
    @DisplayName("Devrait construire un corps HTML bien formaté avec les métadonnées")
    void testBuildReportEmailBody() {
        String body = emailService.buildReportEmailBody(
                "Facturation Trimestrielle",
                "0 0 8 1 * ?",
                "ENT-001",
                "28/09/2026 22:00"
        );

        assertThat(body).contains("Facturation Trimestrielle");
        assertThat(body).contains("ENT-001");
        assertThat(body).contains("0 0 8 1 * ?");
        assertThat(body).contains("Moteur de Rapports Dynamiques");
    }
}
