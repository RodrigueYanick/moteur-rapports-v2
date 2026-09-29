package com.rapports.moteur.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@moteur-rapports.com}")
    private String fromEmail = "noreply@moteur-rapports.com";

    public EmailNotificationService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envoie le rapport généré par email avec pièce jointe aux destinataires spécifiés.
     *
     * @param destinataires     Liste des adresses emails des destinataires
     * @param subject           Sujet du courriel
     * @param htmlBody          Corps de message HTML stylé
     * @param filename          Nom du fichier joint (ex: rapport.pdf, rapport.xlsx, etc.)
     * @param mimeType          Type MIME du fichier (ex: application/pdf)
     * @param attachmentContent Données binaires du rapport
     * @return Nombre de destinataires ayant reçu l'email avec succès
     */
    public int sendReportEmail(List<String> destinataires, String subject, String htmlBody,
                               String filename, String mimeType, byte[] attachmentContent) {
        if (destinataires == null || destinataires.isEmpty()) {
            log.info("Aucun destinataire configuré pour l'envoi d'email");
            return 0;
        }

        if (mailSender == null) {
            log.warn("JavaMailSender n'est pas configuré. Simulation de l'envoi de mail à {} destinataires : {}",
                    destinataires.size(), destinataires);
            return destinataires.size();
        }

        int sentCount = 0;
        for (String to : destinataires) {
            if (to == null || to.isBlank()) continue;
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

                helper.setFrom(fromEmail);
                helper.setTo(to.trim());
                helper.setSubject(subject);
                helper.setText(htmlBody, true);

                if (attachmentContent != null && attachmentContent.length > 0 && filename != null) {
                    helper.addAttachment(filename, new ByteArrayResource(attachmentContent), mimeType);
                }

                mailSender.send(message);
                sentCount++;
                log.info("Email de rapport envoyé avec succès à {}", to.trim());
            } catch (Exception e) {
                log.error("Échec de l'envoi de l'email de rapport à {}", to, e);
            }
        }

        return sentCount;
    }

    /**
     * Construit un template HTML responsive pour l'email de diffusion du rapport.
     */
    public String buildReportEmailBody(String reportNom, String cronExpression, String entrepriseCode, String dateStr) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; color: #1e293b; padding: 20px; }
                .card { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; border: 1px solid #e2e8f0; overflow: hidden; }
                .header { background: #2563eb; color: #ffffff; padding: 24px; text-align: center; }
                .header h1 { margin: 0; font-size: 20px; font-weight: 600; }
                .content { padding: 24px; line-height: 1.6; }
                .badge { display: inline-block; background: #eff6ff; color: #2563eb; padding: 4px 10px; border-radius: 9999px; font-size: 12px; font-weight: 600; }
                .footer { background: #f1f5f9; padding: 16px 24px; text-align: center; font-size: 12px; color: #64748b; }
              </style>
            </head>
            <body>
              <div class="card">
                <div class="header">
                  <h1>Moteur de Rapports Dynamiques</h1>
                </div>
                <div class="content">
                  <p>Bonjour,</p>
                  <p>Votre rapport récurrent <strong>%s</strong> a été généré automatiquement avec succès.</p>
                  <p>Vous trouverez le document en pièce jointe de ce courriel.</p>
                  <table style="width: 100%%; margin: 20px 0; border-collapse: collapse;">
                    <tr>
                      <td style="padding: 8px 0; color: #64748b;">Entreprise :</td>
                      <td style="padding: 8px 0; font-weight: 600;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding: 8px 0; color: #64748b;">Planification :</td>
                      <td style="padding: 8px 0; font-weight: 600;"><code>%s</code></td>
                    </tr>
                    <tr>
                      <td style="padding: 8px 0; color: #64748b;">Date d'exécution :</td>
                      <td style="padding: 8px 0; font-weight: 600;">%s</td>
                    </tr>
                  </table>
                  <p style="margin-top: 24px; font-size: 13px; color: #64748b;">Ce message a été généré de manière automatique. Merci de ne pas y répondre directement.</p>
                </div>
                <div class="footer">
                  &copy; %s RY TECH SOLUTIONS - Moteur de Rapports Automatisés
                </div>
              </div>
            </body>
            </html>
            """.formatted(reportNom, entrepriseCode, cronExpression, dateStr, String.valueOf(java.time.Year.now().getValue()));
    }
}
