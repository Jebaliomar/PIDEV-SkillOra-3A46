package tn.esprit.services;

import tn.esprit.tools.MailService;

public class RendezVousEmailService {

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
        MailService.sendHtml(to, subject, toHtml(body));
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

    private String toHtml(String body) {
        String escaped = normalize(body, "").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<div style=\"font-family:Segoe UI,Arial,sans-serif;color:#0f172a;line-height:1.6;\">"
                + escaped.replace("\n", "<br>")
                + "</div>";
    }
}
