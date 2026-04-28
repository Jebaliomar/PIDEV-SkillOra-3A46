package tn.esprit.services;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import tn.esprit.entities.Course;
import tn.esprit.entities.CoursePayment;
import tn.esprit.entities.CoursePrice;
import tn.esprit.entities.Enrollment;
import tn.esprit.entities.User;
import tn.esprit.tools.AppConfig;
import tn.esprit.tools.MyConnection;
import tn.esprit.tools.PaymentCallbackServer;

import java.awt.Desktop;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class CoursePaymentService {

    private static final String PRICE_TABLE = "skillora_course_prices";
    private static final String PAYMENT_TABLE = "skillora_course_payments";

    private final Connection connection;
    private final EnrollmentService enrollmentService;

    public CoursePaymentService() {
        this.connection = MyConnection.getInstance().getConnection();
        this.enrollmentService = new EnrollmentService();
        initializeSchema();
    }

    public CoursePrice getOrCreatePrice(Course course) throws SQLException {
        if (course == null || course.getId() == null) {
            throw new IllegalStateException("No course is selected.");
        }
        CoursePrice existing = findPriceByCourse(course.getId());
        if (existing != null) {
            return existing;
        }
        CoursePrice price = new CoursePrice();
        price.setCourseId(course.getId());
        price.setAmountCents(Math.max(50, AppConfig.getInt("skillora.payment.defaultAmountCents", 1999)));
        price.setCurrency(AppConfig.get("skillora.payment.currency", "usd").toLowerCase(Locale.ROOT));
        price.setCreatedAt(LocalDateTime.now());
        insertPrice(price);
        return price;
    }

    public String formatPrice(CoursePrice price) {
        if (price == null || price.getAmountCents() == null) {
            return "$19.99";
        }
        String currency = price.getCurrency() == null || price.getCurrency().isBlank()
                ? "USD"
                : price.getCurrency().toUpperCase(Locale.ROOT);
        return currency + " " + String.format(Locale.US, "%.2f", price.getAmountCents() / 100.0);
    }

    public Enrollment checkoutAndEnroll(User user, Course course) throws Exception {
        validateUserAndCourse(user, course);
        Enrollment existing = enrollmentService.findOneByUserAndCourse(user.getId(), course.getId());
        if (existing != null) {
            return existing;
        }

        CoursePrice price = getOrCreatePrice(course);
        CoursePayment payment = createPendingPayment(user, course, price);

        try (PaymentCallbackServer callbackServer = new PaymentCallbackServer()) {
            Session session = createCheckoutSession(user, course, price, callbackServer.successUrl(), callbackServer.cancelUrl());
            markPaymentSession(payment, session.getId());
            openCheckout(session.getUrl());

            long timeoutSeconds = AppConfig.getLong("skillora.payment.checkoutTimeoutSeconds", 900);
            PaymentCallbackServer.Result result = callbackServer.resultFuture().get(timeoutSeconds, TimeUnit.SECONDS);
            if (result.status() == PaymentCallbackServer.Status.CANCELLED) {
                markPaymentStatus(payment.getId(), "cancelled", null, null);
                throw new IllegalStateException("Payment was cancelled.");
            }

            String sessionId = result.sessionId() == null || result.sessionId().isBlank()
                    ? session.getId()
                    : result.sessionId();
            Session verifiedSession = Session.retrieve(sessionId);
            if (!"paid".equalsIgnoreCase(verifiedSession.getPaymentStatus())) {
                markPaymentStatus(payment.getId(), "failed", sessionId, safePaymentIntentId(verifiedSession));
                throw new IllegalStateException("Stripe did not confirm the payment yet. Please try again.");
            }

            markPaymentStatus(payment.getId(), "paid", sessionId, safePaymentIntentId(verifiedSession));
            return enrollmentService.enrollIfMissing(user.getId(), course.getId());
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            markPaymentStatus(payment.getId(), "failed", payment.getProviderSessionId(), payment.getProviderPaymentIntentId());
            throw exception;
        }
    }

    private Session createCheckoutSession(User user, Course course, CoursePrice price, String successUrl, String cancelUrl) throws Exception {
        String secretKey = AppConfig.get("skillora.payment.stripe.secretKey", "");
        if (secretKey.isBlank() || !secretKey.startsWith("sk_test_")) {
            throw new IllegalStateException("Stripe test secret key is missing or invalid.");
        }

        Stripe.apiKey = secretKey;

        SessionCreateParams.Builder params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .putMetadata("user_id", String.valueOf(user.getId()))
                .putMetadata("course_id", String.valueOf(course.getId()))
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(price.getCurrency())
                                .setUnitAmount(Long.valueOf(price.getAmountCents()))
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(safeValue(course.getTitle(), "SkillORA Course"))
                                        .setDescription(truncate(safeValue(course.getDescription(), "Course access"), 420))
                                        .build())
                                .build())
                        .build());

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            params.setCustomerEmail(user.getEmail());
        }

        return Session.create(params.build());
    }

    private void openCheckout(String checkoutUrl) throws Exception {
        if (checkoutUrl == null || checkoutUrl.isBlank()) {
            throw new IllegalStateException("Stripe did not return a checkout URL.");
        }
        if (!Desktop.isDesktopSupported()) {
            throw new IllegalStateException("Opening the payment page is not supported on this device.");
        }
        Desktop.getDesktop().browse(URI.create(checkoutUrl));
    }

    private void validateUserAndCourse(User user, Course course) {
        if (user == null || user.getId() == null) {
            throw new IllegalStateException("Please sign in before buying a course.");
        }
        if (course == null || course.getId() == null) {
            throw new IllegalStateException("No course is selected.");
        }
    }

    private CoursePayment createPendingPayment(User user, Course course, CoursePrice price) throws SQLException {
        CoursePayment payment = new CoursePayment();
        payment.setUserId(user.getId());
        payment.setCourseId(course.getId());
        payment.setAmountCents(price.getAmountCents());
        payment.setCurrency(price.getCurrency());
        payment.setProvider("stripe");
        payment.setStatus("pending");
        payment.setCreatedAt(LocalDateTime.now());

        String sql = "INSERT INTO `" + PAYMENT_TABLE + "` (`user_id`, `course_id`, `amount_cents`, `currency`, `provider`, `status`, `created_at`) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, payment.getUserId());
            statement.setInt(2, payment.getCourseId());
            statement.setInt(3, payment.getAmountCents());
            statement.setString(4, payment.getCurrency());
            statement.setString(5, payment.getProvider());
            statement.setString(6, payment.getStatus());
            statement.setTimestamp(7, Timestamp.valueOf(payment.getCreatedAt()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    payment.setId(keys.getInt(1));
                }
            }
        }
        return payment;
    }

    private CoursePrice findPriceByCourse(int courseId) throws SQLException {
        String sql = "SELECT * FROM `" + PRICE_TABLE + "` WHERE `course_id` = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, courseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapPrice(resultSet);
                }
            }
        }
        return null;
    }

    private void insertPrice(CoursePrice price) throws SQLException {
        String sql = "INSERT INTO `" + PRICE_TABLE + "` (`course_id`, `amount_cents`, `currency`, `created_at`) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, price.getCourseId());
            statement.setInt(2, price.getAmountCents());
            statement.setString(3, price.getCurrency());
            statement.setTimestamp(4, Timestamp.valueOf(price.getCreatedAt()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    price.setId(keys.getInt(1));
                }
            }
        }
    }

    private void markPaymentSession(CoursePayment payment, String sessionId) throws SQLException {
        payment.setProviderSessionId(sessionId);
        String sql = "UPDATE `" + PAYMENT_TABLE + "` SET `provider_session_id` = ? WHERE `id` = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sessionId);
            statement.setInt(2, payment.getId());
            statement.executeUpdate();
        }
    }

    private void markPaymentStatus(Integer paymentId, String status, String sessionId, String paymentIntentId) throws SQLException {
        if (paymentId == null) {
            return;
        }
        String sql = "UPDATE `" + PAYMENT_TABLE + "` "
                + "SET `status` = ?, `provider_session_id` = COALESCE(?, `provider_session_id`), "
                + "`provider_payment_intent_id` = COALESCE(?, `provider_payment_intent_id`), `paid_at` = ? "
                + "WHERE `id` = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setString(2, sessionId);
            statement.setString(3, paymentIntentId);
            if ("paid".equalsIgnoreCase(status)) {
                statement.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            } else {
                statement.setNull(4, java.sql.Types.TIMESTAMP);
            }
            statement.setInt(5, paymentId);
            statement.executeUpdate();
        }
    }

    private CoursePrice mapPrice(ResultSet resultSet) throws SQLException {
        CoursePrice price = new CoursePrice();
        price.setId(resultSet.getInt("id"));
        price.setCourseId(resultSet.getInt("course_id"));
        price.setAmountCents(resultSet.getInt("amount_cents"));
        price.setCurrency(resultSet.getString("currency"));
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        price.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        price.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
        return price;
    }

    private void initializeSchema() {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + PRICE_TABLE + "` ("
                    + "`id` INT NOT NULL AUTO_INCREMENT,"
                    + "`course_id` INT NOT NULL,"
                    + "`amount_cents` INT NOT NULL,"
                    + "`currency` VARCHAR(8) NOT NULL DEFAULT 'usd',"
                    + "`created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "`updated_at` DATETIME NULL DEFAULT NULL,"
                    + "PRIMARY KEY (`id`),"
                    + "UNIQUE KEY `uq_skillora_course_prices_course` (`course_id`)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + PAYMENT_TABLE + "` ("
                    + "`id` INT NOT NULL AUTO_INCREMENT,"
                    + "`user_id` INT NOT NULL,"
                    + "`course_id` INT NOT NULL,"
                    + "`amount_cents` INT NOT NULL,"
                    + "`currency` VARCHAR(8) NOT NULL DEFAULT 'usd',"
                    + "`provider` VARCHAR(32) NOT NULL DEFAULT 'stripe',"
                    + "`provider_session_id` VARCHAR(255) NULL DEFAULT NULL,"
                    + "`provider_payment_intent_id` VARCHAR(255) NULL DEFAULT NULL,"
                    + "`status` VARCHAR(32) NOT NULL DEFAULT 'pending',"
                    + "`created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "`paid_at` DATETIME NULL DEFAULT NULL,"
                    + "PRIMARY KEY (`id`),"
                    + "KEY `idx_skillora_course_payments_user_course` (`user_id`, `course_id`),"
                    + "KEY `idx_skillora_course_payments_session` (`provider_session_id`)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to initialize desktop payment tables.", exception);
        }
    }

    private String safePaymentIntentId(Session session) {
        Object paymentIntent = session == null ? null : session.getPaymentIntentObject();
        if (paymentIntent != null) {
            return session.getPaymentIntentObject().getId();
        }
        return session == null ? null : session.getPaymentIntent();
    }

    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 1)).trim() + "...";
    }
}
