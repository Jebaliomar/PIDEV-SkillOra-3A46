package tn.esprit.services;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import tn.esprit.entities.RendezVous;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ReminderService {

    private static final long CHECK_INTERVAL_MINUTES = 30;
    private static final long LOOKAHEAD_MINUTES = 60;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Connection connection;
    private final AIService aiService;
    private final Map<String, LocalDateTime> reminderRegistry = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledTask;

    public ReminderService() {
        this(new AIService());
    }

    public ReminderService(AIService aiService) {
        this.connection = MyConnection.getInstance().getConnection();
        this.aiService = aiService == null ? new AIService() : aiService;
        this.scheduler = buildScheduler();
    }

    public synchronized void start() {
        if (scheduledTask != null && !scheduledTask.isCancelled() && !scheduledTask.isDone()) {
            return;
        }
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = buildScheduler();
        }

        scheduledTask = scheduler.scheduleAtFixedRate(
                this::runReminderCheckSafe,
                0,
                CHECK_INTERVAL_MINUTES,
                TimeUnit.MINUTES
        );
    }

    public synchronized void stop() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
        }
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException interruptedException) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void runReminderCheckSafe() {
        try {
            processUpcomingAppointments();
            cleanupReminderRegistry();
        } catch (Exception ignored) {
            // Keep scheduler alive even if one run fails.
        }
    }

    private void processUpcomingAppointments() throws SQLException {
        List<ReminderContext> contexts = loadUpcomingAppointments();
        for (ReminderContext context : contexts) {
            String key = buildReminderKey(context.rendezVous(), context.startAt());
            if (reminderRegistry.putIfAbsent(key, context.startAt()) != null) {
                continue;
            }

            String message = buildReminderMessage(context);
            showReminderAlert(context, message);
        }
    }

    private List<ReminderContext> loadUpcomingAppointments() throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime horizon = now.plusMinutes(LOOKAHEAD_MINUTES);

        String sql = """
                SELECT
                    rv.id AS rendezvous_id,
                    rv.student_id,
                    rv.professor_id,
                    rv.course_id,
                    rv.statut,
                    s.start_at,
                    COALESCE(
                        NULLIF(TRIM(CONCAT(COALESCE(su.first_name, ''), ' ', COALESCE(su.last_name, ''))), ''),
                        su.username,
                        su.email,
                        CONCAT('Student #', rv.student_id)
                    ) AS student_name,
                    COALESCE(
                        NULLIF(TRIM(CONCAT(COALESCE(pu.first_name, ''), ' ', COALESCE(pu.last_name, ''))), ''),
                        pu.username,
                        pu.email,
                        CONCAT('Professor #', COALESCE(rv.professor_id, s.professor_id))
                    ) AS professor_name,
                    COALESCE(c.title, CONCAT('Course #', rv.course_id)) AS subject
                FROM rendez_vous rv
                INNER JOIN availability_slots s ON s.id = rv.slot_id
                LEFT JOIN users su ON su.id = rv.student_id
                LEFT JOIN users pu ON pu.id = COALESCE(rv.professor_id, s.professor_id)
                LEFT JOIN course c ON c.id = rv.course_id
                WHERE s.start_at >= ?
                  AND s.start_at < ?
                  AND LOWER(COALESCE(rv.statut, '')) LIKE '%confirm%'
                ORDER BY s.start_at ASC
                """;

        List<ReminderContext> contexts = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setTimestamp(1, Timestamp.valueOf(now));
            preparedStatement.setTimestamp(2, Timestamp.valueOf(horizon));

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    RendezVous rendezVous = new RendezVous();
                    rendezVous.setId(resultSet.getInt("rendezvous_id"));

                    int studentId = resultSet.getInt("student_id");
                    rendezVous.setStudentId(resultSet.wasNull() ? null : studentId);

                    int professorId = resultSet.getInt("professor_id");
                    rendezVous.setProfessorId(resultSet.wasNull() ? null : professorId);

                    int courseId = resultSet.getInt("course_id");
                    rendezVous.setCourseId(resultSet.wasNull() ? null : courseId);

                    rendezVous.setStatut(resultSet.getString("statut"));
                    Timestamp startTimestamp = resultSet.getTimestamp("start_at");
                    if (startTimestamp == null) {
                        continue;
                    }

                    LocalDateTime startAt = startTimestamp.toLocalDateTime();
                    String studentName = defaultValue(resultSet.getString("student_name"), "Student");
                    String professorName = defaultValue(resultSet.getString("professor_name"), "Professor");
                    String subject = defaultValue(resultSet.getString("subject"), "Rendez-vous");

                    contexts.add(new ReminderContext(rendezVous, studentName, professorName, subject, startAt));
                }
            }
        }

        return contexts;
    }

    private String buildReminderMessage(ReminderContext context) {
        String timeText = context.startAt().format(TIME_FORMATTER);
        try {
            String aiMessage = aiService.generateReminder(
                    context.studentName(),
                    context.professorName(),
                    timeText,
                    context.subject()
            );
            if (aiMessage == null || aiMessage.isBlank()) {
                return buildFallbackMessage(context, timeText);
            }
            return aiMessage;
        } catch (Exception exception) {
            return buildFallbackMessage(context, timeText);
        }
    }

    private String buildFallbackMessage(ReminderContext context, String timeText) {
        return "Reminder: " + context.studentName() + ", your rendez-vous for "
                + context.subject() + " with " + context.professorName()
                + " is scheduled at " + timeText + ".";
    }

    private void showReminderAlert(ReminderContext context, String message) {
        Runnable showTask = () -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Appointment Reminder");
            alert.setHeaderText("Upcoming rendez-vous in less than 60 minutes");
            alert.setContentText(message);
            alert.show();
        };

        try {
            Platform.runLater(showTask);
        } catch (IllegalStateException ignored) {
            // JavaFX runtime may not be initialized yet.
        }
    }

    private void cleanupReminderRegistry() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(LOOKAHEAD_MINUTES);
        reminderRegistry.entrySet().removeIf(entry -> entry.getValue().isBefore(threshold));
    }

    private String buildReminderKey(RendezVous rendezVous, LocalDateTime startAt) {
        Integer rendezVousId = rendezVous == null ? null : rendezVous.getId();
        String idPart = rendezVousId == null ? "unknown" : rendezVousId.toString();
        return idPart + "|" + startAt;
    }

    private String defaultValue(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private ScheduledExecutorService buildScheduler() {
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "skillora-reminder-scheduler");
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    private record ReminderContext(
            RendezVous rendezVous,
            String studentName,
            String professorName,
            String subject,
            LocalDateTime startAt
    ) {
    }
}
