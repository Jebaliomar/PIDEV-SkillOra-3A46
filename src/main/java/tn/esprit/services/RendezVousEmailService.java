package tn.esprit.services;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class RendezVousEmailService {

    private static final String CONFIG_FILE = "config.properties";
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";

    public void sendAppointmentConfirmedEmail(
            String studentEmail,
            String professorFullName,
            String meetingDateTime,
            String meetingLocation
    ) throws Exception {
        String subject = "Your appointment has been confirmed ✅";
        String body = buildConfirmedBody(professorFullName, meetingDateTime, meetingLocation);
        send(studentEmail, subject, body);
    }

    public void sendAppointmentDeclinedEmail(
            String studentEmail,
            String professorFullName,
            String requestedDateTime,
            String refusalReason
    ) throws Exception {
        String subject = "Your appointment request was declined ❌";
        String body = buildDeclinedBody(professorFullName, requestedDateTime, refusalReason);
        send(studentEmail, subject, body);
    }

    private void send(String recipient, String subject, String body) throws Exception {
        String to = trimToNull(recipient);
        if (to == null) {
            throw new IllegalArgumentException("Student email is missing.");
        }

        MailConfig mailConfig = loadMailConfig();

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.trust", SMTP_HOST);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(mailConfig.sender(), mailConfig.password());
            }
        });

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(mailConfig.sender()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject, StandardCharsets.UTF_8.name());
        message.setText(body, StandardCharsets.UTF_8.name());

        Transport.send(message);
    }

    private MailConfig loadMailConfig() {
        Properties properties = new Properties();
        try (InputStream inputStream = RendezVousEmailService.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream == null) {
                throw new IllegalStateException(CONFIG_FILE + " not found on classpath.");
            }
            properties.load(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read " + CONFIG_FILE + ".", exception);
        }

        String sender = trimToNull(properties.getProperty("mail.sender"));
        String password = trimToNull(properties.getProperty("mail.password"));
        if (sender == null || password == null) {
            throw new IllegalStateException("Missing mail.sender or mail.password in " + CONFIG_FILE + ".");
        }
        return new MailConfig(sender, password);
    }

    private String buildConfirmedBody(String professorFullName, String meetingDateTime, String meetingLocation) {
        String professor = normalize(professorFullName, "your professor");
        String dateTime = normalize(meetingDateTime, "the scheduled date/time");
        String location = normalize(meetingLocation, "the meeting location");
        return "Hello,\n\n"
                + "Your rendez-vous request has been confirmed.\n\n"
                + "Professor: " + professor + "\n"
                + "Date & time: " + dateTime + "\n"
                + "Location: " + location + "\n\n"
                + "Please be ready a few minutes before the appointment.\n\n"
                + "Best regards,\nSkillOra Team";
    }

    private String buildDeclinedBody(String professorFullName, String requestedDateTime, String refusalReason) {
        String professor = normalize(professorFullName, "your professor");
        String dateTime = normalize(requestedDateTime, "the requested date/time");
        String reason = normalize(refusalReason, "No reason provided.");
        return "Hello,\n\n"
                + "Your rendez-vous request was declined.\n\n"
                + "Professor: " + professor + "\n"
                + "Requested date & time: " + dateTime + "\n"
                + "Reason: " + reason + "\n\n"
                + "Please book another available slot.\n\n"
                + "Best regards,\nSkillOra Team";
    }

    private String normalize(String value, String fallback) {
        String cleaned = trimToNull(value);
        return cleaned == null ? fallback : cleaned;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record MailConfig(String sender, String password) {
    }
}
