package com.bricopro.notification.service;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Tag(name = "Communication Service", description = "Email and WhatsApp delivery for all platform notifications")
@Service
@RequiredArgsConstructor
@Slf4j
public class CommunicationService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@bricopro.ma}")
    private String mailFrom;

    @Value("${app.greenapi.instance-id:}")
    private String greenApiInstanceId;

    @Value("${app.greenapi.token:}")
    private String greenApiToken;

    @Value("${app.whatsapp.enabled:false}")
    private boolean whatsappEnabled;

    private static final String GREENAPI_BASE      = "https://api.green-api.com";
    private static final String BRICOPRO_SIGNATURE = "\n\n— L'équipe BricoPro\nwww.bricopro.ma";


    // =========================================================================
    // WhatsApp — Green API (free tier: 1500 messages/month)
    // Sign up at https://green-api.com, connect your WhatsApp number,
    // then set GREENAPI_INSTANCE_ID and GREENAPI_TOKEN in your .env
    // =========================================================================

    @Async
    public void sendWhatsApp(String toPhone, String message) {
        if (!whatsappEnabled || greenApiInstanceId.isBlank() || greenApiToken.isBlank()) {
            log.info("[DEV] WhatsApp to {} — {}", toPhone, message);
            return;
        }
        try {
            String url = String.format("%s/waInstance%s/sendMessage/%s",
                    GREENAPI_BASE, greenApiInstanceId, greenApiToken);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = Map.of("chatId", formatPhoneForGreenApi(toPhone), "message", message);
            new RestTemplate().postForEntity(url, new HttpEntity<>(body, headers), String.class);
            log.info("WhatsApp sent to {}", toPhone);
        } catch (Exception e) {
            log.error("WhatsApp failed to {}: {}", toPhone, e.getMessage());
        }
    }

    private String formatPhoneForGreenApi(String phone) {
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("0") && digits.length() == 10) {
            digits = "212" + digits.substring(1);
        }
        return digits + "@c.us";
    }


    // =========================================================================
    // OTP delivery — WhatsApp first, email fallback
    // =========================================================================

    @Async
    public void sendOtp(String toPhone, String toEmail, String firstName, String otp) {
        String text = "Bonjour " + firstName + ",\n\n"
                + "Votre code de vérification BricoPro est : *" + otp + "*\n"
                + "Valable 10 minutes. Ne partagez jamais ce code."
                + BRICOPRO_SIGNATURE;

        if (whatsappEnabled && toPhone != null) {
            sendWhatsApp(toPhone, text);
        } else if (toEmail != null) {
            sendEmail(toEmail, "BricoPro — Code de vérification", text.replace("*" + otp + "*", otp));
        } else {
            log.warn("[OTP] No delivery channel — user has neither phone nor email");
        }
    }


    // =========================================================================
    // Email — Gmail / Zoho SMTP (free)
    // =========================================================================

    @Async
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(mailFrom);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Email failed to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendWelcomeEmail(String to, String firstName) {
        sendEmail(to, "Bienvenue sur BricoPro !",
                "Bonjour " + firstName + ",\n\n"
                        + "Bienvenue sur BricoPro — la plateforme de services à domicile au Maroc.\n\n"
                        + "Vous pouvez dès maintenant :\n"
                        + "  • Publier une demande de service\n"
                        + "  • Trouver un professionnel près de chez vous\n"
                        + "  • Suivre vos tâches en temps réel"
                        + BRICOPRO_SIGNATURE);
    }

    @Async
    public void sendAccountVerifiedEmail(String to, String firstName) {
        sendEmail(to, "BricoPro — Compte vérifié avec succès",
                "Bonjour " + firstName + ",\n\n"
                        + "Votre compte BricoPro a été vérifié avec succès.\n"
                        + "Vous pouvez maintenant vous connecter et utiliser toutes les fonctionnalités."
                        + BRICOPRO_SIGNATURE);
    }

    @Async
    public void sendPasswordResetEmail(String to, String firstName, String otp) {
        sendEmail(to, "BricoPro — Réinitialisation de mot de passe",
                "Bonjour " + firstName + ",\n\n"
                        + "Votre code de réinitialisation est : " + otp + "\n\n"
                        + "Ce code expire dans 10 minutes.\n"
                        + "Si vous n'avez pas demandé cette réinitialisation, ignorez cet email."
                        + BRICOPRO_SIGNATURE);
    }

    @Async
    public void sendPasswordChangedEmail(String to, String firstName) {
        sendEmail(to, "BricoPro — Mot de passe modifié",
                "Bonjour " + firstName + ",\n\n"
                        + "Votre mot de passe BricoPro a été modifié avec succès.\n"
                        + "Si vous n'êtes pas à l'origine de ce changement, contactez-nous immédiatement à support@bricopro.ma."
                        + BRICOPRO_SIGNATURE);
    }

    @Async
    public void sendTaskConfirmationEmail(String to, String firstName, String taskTitle, String workerName) {
        sendEmail(to, "BricoPro — Votre demande a été acceptée",
                "Bonjour " + firstName + ",\n\n"
                        + workerName + " a accepté votre demande : " + taskTitle + "\n\n"
                        + "Connectez-vous à BricoPro pour voir les détails et suivre l'avancement."
                        + BRICOPRO_SIGNATURE);
    }

    @Async
    public void sendTaskCancelledEmail(String to, String firstName, String taskTitle, String cancelledBy) {
        sendEmail(to, "BricoPro — Tâche annulée",
                "Bonjour " + firstName + ",\n\n"
                        + "La tâche « " + taskTitle + " » a été annulée par " + cancelledBy + ".\n"
                        + "Vous pouvez publier une nouvelle demande à tout moment."
                        + BRICOPRO_SIGNATURE);
    }

    @Async
    public void sendTaskCompletedEmail(String to, String firstName, String taskTitle) {
        sendEmail(to, "BricoPro — Tâche terminée",
                "Bonjour " + firstName + ",\n\n"
                        + "La tâche « " + taskTitle + " » est maintenant terminée.\n"
                        + "N'oubliez pas de laisser un avis pour aider les autres utilisateurs."
                        + BRICOPRO_SIGNATURE);
    }

    @Async
    public void sendPaymentConfirmationEmail(String to, String firstName, String amount) {
        sendEmail(to, "BricoPro — Paiement reçu",
                "Bonjour " + firstName + ",\n\n"
                        + "Vous avez reçu un paiement de " + amount + " MAD sur votre compte BricoPro.\n"
                        + "Consultez votre tableau de bord pour voir le détail."
                        + BRICOPRO_SIGNATURE);
    }

    @Async
    public void sendReviewReceivedEmail(String to, String firstName, int rating, String reviewerName) {
        String stars = "★".repeat(rating) + "☆".repeat(5 - rating);
        sendEmail(to, "BricoPro — Vous avez reçu un avis",
                "Bonjour " + firstName + ",\n\n"
                        + reviewerName + " vous a laissé un avis " + stars + " (" + rating + "/5).\n"
                        + "Consultez votre profil pour lire le commentaire complet."
                        + BRICOPRO_SIGNATURE);
    }

    @Async
    public void sendWorkerVerifiedEmail(String to, String firstName) {
        sendEmail(to, "BricoPro — Profil vérifié",
                "Bonjour " + firstName + ",\n\n"
                        + "Félicitations ! Votre profil professionnel BricoPro a été vérifié par notre équipe.\n"
                        + "Vous apparaissez maintenant dans les résultats de recherche et pouvez recevoir des missions."
                        + BRICOPRO_SIGNATURE);
    }

    @Async
    public void sendWorkerRejectedEmail(String to, String firstName, String reason) {
        sendEmail(to, "BricoPro — Document refusé",
                "Bonjour " + firstName + ",\n\n"
                        + "Votre CIN a été refusé pour la raison suivante : " + reason + "\n\n"
                        + "Veuillez soumettre un document valide depuis votre espace profil."
                        + BRICOPRO_SIGNATURE);
    }

    @Async
    public void sendAccountSuspendedEmail(String to, String firstName, String reason) {
        sendEmail(to, "BricoPro — Compte suspendu",
                "Bonjour " + firstName + ",\n\n"
                        + "Votre compte a été suspendu pour la raison suivante : " + reason + "\n\n"
                        + "Pour contester cette décision, contactez support@bricopro.ma."
                        + BRICOPRO_SIGNATURE);
    }

    @Async
    public void sendDisputeResolvedEmail(String to, String firstName, Long taskId, String outcome) {
        sendEmail(to, "BricoPro — Litige résolu",
                "Bonjour " + firstName + ",\n\n"
                        + "Le litige concernant la tâche #" + taskId + " a été résolu " + outcome + ".\n"
                        + "Consultez votre tableau de bord pour plus de détails."
                        + BRICOPRO_SIGNATURE);
    }

    public void sendPaymentDisputeResolvedEmail(String to, String firstName, Long paymentId, String outcome) {
        sendEmail(to, "BricoPro — Litige de paiement résolu",
                "Bonjour " + firstName + ",\n\n"
                        + "Le litige concernant le paiement #" + paymentId + " a été résolu " + outcome + ".\n"
                        + "Consultez votre tableau de bord pour plus de détails."
                        + BRICOPRO_SIGNATURE);
    }

    @Async
    public void sendAdminTaskAssignedEmail(String to, String firstName, String taskTitle,
                                           String scheduledDate, String address) {
        sendEmail(to, "BricoPro — Mission assignée par un administrateur",
                "Bonjour " + firstName + ",\n\n"
                        + "Un administrateur vous a assigné la mission : " + taskTitle + "\n"
                        + "Date : " + scheduledDate + "\n"
                        + "Adresse : " + address + "\n\n"
                        + "Connectez-vous pour voir les détails complets."
                        + BRICOPRO_SIGNATURE);
    }

    @Async
    public void sendNewMessageNotificationEmail(String to, String firstName, String senderName) {
        sendEmail(to, "BricoPro — Nouveau message de " + senderName,
                "Bonjour " + firstName + ",\n\n"
                        + senderName + " vous a envoyé un message sur BricoPro.\n"
                        + "Connectez-vous pour lire et répondre."
                        + BRICOPRO_SIGNATURE);
    }
}