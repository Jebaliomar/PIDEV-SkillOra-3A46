package tn.esprit.services;

public record ExamEvaluationPayload(
        String answer,
        int aiPercent,
        int plagiarismPercent,
        String plagiarismStatus,
        boolean fraudAttempt,
        String fraudReason,
        String feedback
) {
    public static ExamEvaluationPayload parse(String payload) {
        int plagiarism = extractInt(payload, "PLAGIARISM_PERCENT:::");
        boolean fraud = extractBoolean(payload, "FRAUD_ATTEMPT:::");
        String fraudReason = extractLine(payload, "FRAUD_REASON:::", fraud ? "Suspicion détectée" : "Aucune fraude majeure détectée");
        String plagiarismStatus = extractLine(payload, "PLAGIARISM_STATUS:::", plagiarism >= 60 ? "PLAGIAT DETECTE" : "AUCUN PLAGIAT MAJEUR");

        return new ExamEvaluationPayload(
                extractBlock(payload, "ANSWER:::", ":::END_ANSWER"),
                extractInt(payload, "AI_PERCENT:::"),
                plagiarism,
                plagiarismStatus,
                fraud,
                fraudReason,
                extractBlock(payload, "FEEDBACK:::", ":::END_FEEDBACK")
        );
    }

    public String formatForAdmin(Integer score) {
        StringBuilder sb = new StringBuilder();
        sb.append("Réponse étudiant :\n").append(emptyFallback(answer, "Aucune réponse")).append("\n\n");
        sb.append("Score IA : ").append(score != null ? score : 0).append("/100\n");
        sb.append("Pourcentage IA : ").append(aiPercent).append("%\n");
        sb.append("Plagiat : ").append(plagiarismPercent).append("%\n");
        sb.append("Statut plagiat : ").append(emptyFallback(plagiarismStatus, "-")).append("\n");
        sb.append("Fraude : ").append(fraudAttempt ? "Oui" : "Non").append("\n");
        if (fraudAttempt && fraudReason != null && !fraudReason.isBlank()) {
            sb.append("Raison : ").append(fraudReason).append("\n");
        }
        sb.append("\nFeedback :\n").append(emptyFallback(feedback, "Aucun feedback."));
        return sb.toString();
    }

    private static String extractBlock(String payload, String startToken, String endToken) {
        if (payload == null || payload.isBlank()) {
            return "";
        }
        int start = payload.indexOf(startToken);
        int end = payload.indexOf(endToken);
        if (start == -1 || end == -1 || end <= start) {
            return "";
        }
        return payload.substring(start + startToken.length(), end).trim();
    }

    private static int extractInt(String payload, String prefix) {
        try {
            String value = extractLine(payload, prefix, "0");
            return Math.max(0, Math.min(100, Integer.parseInt(value)));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static boolean extractBoolean(String payload, String prefix) {
        return Boolean.parseBoolean(extractLine(payload, prefix, "false"));
    }

    private static String extractLine(String payload, String prefix, String fallback) {
        if (payload == null || payload.isBlank()) {
            return fallback;
        }
        String[] lines = payload.split("\\R");
        for (String line : lines) {
            if (line.startsWith(prefix)) {
                String value = line.substring(prefix.length()).trim();
                return value.isBlank() ? fallback : value;
            }
        }
        return fallback;
    }

    private static String emptyFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
