package tn.esprit.services;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import tn.esprit.entities.Reservation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

public class TwilioSmsService {

    private static final String DEFAULT_COUNTRY_CODE = "+216";
    private static final String ACCOUNT_SID_ENV = "TWILIO_ACCOUNT_SID";
    private static final String AUTH_TOKEN_ENV = "TWILIO_AUTH_TOKEN";
    private static final String PHONE_NUMBER_ENV = "TWILIO_PHONE_NUMBER";
    private static final String DEFAULT_COUNTRY_CODE_ENV = "TWILIO_DEFAULT_COUNTRY_CODE";
    private static final String PROPERTIES_PATH_ENV = "TWILIO_PROPERTIES_PATH";
    private static final String PROPERTIES_PATH_PROPERTY = "twilio.properties.path";
    private static final String ACCOUNT_SID_PROPERTY = "twilio.account.sid";
    private static final String AUTH_TOKEN_PROPERTY = "twilio.auth.token";
    private static final String PHONE_NUMBER_PROPERTY = "twilio.phone.number";
    private static final String FROM_NUMBER_PROPERTY = "twilio.from.number";
    private static final String DEFAULT_COUNTRY_CODE_PROPERTY = "twilio.default-country-code";
    private static final int STATUS_POLL_ATTEMPTS = 8;
    private static final long STATUS_POLL_DELAY_MILLIS = 2_500L;

    private final TwilioConfiguration configuration = loadConfiguration();

    public boolean isConfigured() {
        return configuration.isComplete();
    }

    public String describeConfiguration() {
        return configuration.describe();
    }

    public SmsSendResult sendSms(String toNumber, String body) {
        if (!isConfigured()) {
            throw new SmsException("Missing Twilio configuration: "
                    + String.join(", ", configuration.missingNames()) + ". " + configuration.describe());
        }

        String normalizedToNumber = normalizePhoneNumber(toNumber);
        String normalizedFromNumber = normalizePhoneNumber(configuration.fromNumber());
        if (isBlank(normalizedToNumber) || !normalizedToNumber.startsWith("+")) {
            throw new SmsException("Phone number is invalid. Use E.164 format, for example +216XXXXXXXX.");
        }
        if (isBlank(normalizedFromNumber) || !normalizedFromNumber.startsWith("+")) {
            throw new SmsException("Twilio phone number is invalid. Use E.164 format, for example +1234567890.");
        }
        if (isBlank(body)) {
            throw new SmsException("Message is required.");
        }

        try {
            Twilio.init(configuration.accountSid(), configuration.authToken());
            Message createdMessage = Message.creator(
                    new PhoneNumber(normalizedToNumber),
                    new PhoneNumber(normalizedFromNumber),
                    body.trim()
            ).create();
            Message finalMessage = waitForDeliveryStatus(createdMessage);
            return SmsSendResult.fromMessage(finalMessage, normalizedToNumber);
        } catch (ApiException exception) {
            throw new SmsException(twilioApiMessage(exception, normalizedToNumber, normalizedFromNumber), exception);
        } catch (RuntimeException exception) {
            throw new SmsException("Unable to send SMS with Twilio: " + safeMessage(exception)
                    + ". " + configuration.describe(), exception);
        }
    }

    public SmsSendResult sendReservationThankYou(Reservation reservation, String eventTitle, String salleName) {
        String body = buildReservationMessage(reservation, eventTitle, salleName);
        return sendSms(reservation == null ? null : reservation.getTelephone(), body);
    }

    private Message waitForDeliveryStatus(Message createdMessage) {
        if (createdMessage == null || isBlank(createdMessage.getSid())) {
            return createdMessage;
        }

        Message latestMessage = createdMessage;
        for (int attempt = 0; attempt < STATUS_POLL_ATTEMPTS; attempt++) {
            if (isDeliveryFailure(latestMessage)) {
                throw new SmsException(deliveryFailureMessage(latestMessage));
            }
            if (isDelivered(latestMessage)) {
                return latestMessage;
            }

            sleepBeforeStatusPoll();
            latestMessage = Message.fetcher(createdMessage.getSid()).fetch();
        }

        if (isDeliveryFailure(latestMessage)) {
            throw new SmsException(deliveryFailureMessage(latestMessage));
        }
        return latestMessage;
    }

    private boolean isDelivered(Message message) {
        return message != null && Message.Status.DELIVERED.equals(message.getStatus());
    }

    private boolean isDeliveryFailure(Message message) {
        if (message == null || message.getStatus() == null) {
            return false;
        }
        return Message.Status.FAILED.equals(message.getStatus())
                || Message.Status.UNDELIVERED.equals(message.getStatus())
                || Message.Status.CANCELED.equals(message.getStatus());
    }

    private void sleepBeforeStatusPoll() {
        try {
            Thread.sleep(STATUS_POLL_DELAY_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SmsException("SMS delivery status check was interrupted.", exception);
        }
    }

    private String deliveryFailureMessage(Message message) {
        String status = statusName(message);
        String errorCode = message == null || message.getErrorCode() == null
                ? ""
                : " Error code: " + message.getErrorCode() + ".";
        String errorMessage = message == null || isBlank(message.getErrorMessage())
                ? ""
                : " " + message.getErrorMessage();
        return "Twilio accepted the SMS but delivery failed with status " + status + "."
                + errorCode + errorMessage + " " + configuration.describe();
    }

    private String buildReservationMessage(Reservation reservation, String eventTitle, String salleName) {
        String firstName = safe(reservation == null ? null : reservation.getPrenom());
        String event = safe(eventTitle);
        String salle = safe(salleName);
        String places = safe(reservation == null ? null : reservation.getNombrePlaces());

        StringBuilder message = new StringBuilder("Merci");
        if (!firstName.isBlank()) {
            message.append(' ').append(firstName);
        }
        message.append(" ! Votre reservation Skillora est confirmee.");
        if (!event.isBlank()) {
            message.append(" Event: ").append(event).append('.');
        }
        if (!salle.isBlank()) {
            message.append(" Salle: ").append(salle).append('.');
        }
        if (!places.isBlank()) {
            message.append(" Places: ").append(places).append('.');
        }
        return message.toString();
    }

    private String normalizePhoneNumber(String rawNumber) {
        if (rawNumber == null) {
            return "";
        }
        String cleaned = rawNumber.trim()
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "");
        if (cleaned.startsWith("+")) {
            return "+" + cleaned.substring(1).replaceAll("[^0-9]", "");
        }
        if (cleaned.startsWith("00")) {
            return "+" + cleaned.substring(2).replaceAll("[^0-9]", "");
        }

        String digits = cleaned.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return "";
        }
        String defaultCountryCode = normalizeCountryCode(configuration.defaultCountryCode());
        String countryDigits = defaultCountryCode.replaceAll("[^0-9]", "");
        if (!countryDigits.isBlank() && digits.startsWith(countryDigits)) {
            return "+" + digits;
        }
        if (digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        return defaultCountryCode + digits;
    }

    private String normalizeCountryCode(String rawCountryCode) {
        String digits = rawCountryCode == null ? "" : rawCountryCode.replaceAll("[^0-9]", "");
        return digits.isBlank() ? DEFAULT_COUNTRY_CODE : "+" + digits;
    }

    private TwilioConfiguration loadConfiguration() {
        List<TwilioConfiguration> candidates = new ArrayList<>();

        addExplicitPropertyFile(candidates, System.getProperty(PROPERTIES_PATH_PROPERTY), PROPERTIES_PATH_PROPERTY);
        addExplicitPropertyFile(candidates, System.getenv(PROPERTIES_PATH_ENV), PROPERTIES_PATH_ENV);
        for (Path path : discoverLocalPropertyPaths()) {
            addPropertyFile(candidates, path);
        }
        addClasspathProperties(candidates);
        candidates.add(fromSystemProperties());
        candidates.add(fromEnvironment());

        TwilioConfiguration bestCandidate = TwilioConfiguration.empty("no Twilio configuration source found");
        for (TwilioConfiguration candidate : candidates) {
            if (candidate.isComplete()) {
                return candidate;
            }
            if (candidate.configuredValueCount() > bestCandidate.configuredValueCount()) {
                bestCandidate = candidate;
            }
        }
        return bestCandidate;
    }

    private void addExplicitPropertyFile(List<TwilioConfiguration> candidates, String rawPath, String sourceName) {
        if (isBlank(rawPath)) {
            return;
        }
        addPropertyFile(candidates, Path.of(rawPath.trim()), sourceName);
    }

    private void addPropertyFile(List<TwilioConfiguration> candidates, Path path) {
        addPropertyFile(candidates, path, "file");
    }

    private void addPropertyFile(List<TwilioConfiguration> candidates, Path path, String sourceName) {
        if (path == null) {
            return;
        }
        Path normalizedPath = path.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalizedPath)) {
            return;
        }

        try (InputStream inputStream = Files.newInputStream(normalizedPath)) {
            Properties loaded = loadProperties(inputStream);
            candidates.add(fromProperties(loaded, sourceName + ":" + normalizedPath));
        } catch (IOException ignored) {
        }
    }

    private List<Path> discoverLocalPropertyPaths() {
        Set<Path> paths = new LinkedHashSet<>();
        addPath(paths, Path.of("src", "main", "resources", "twilio.properties"));
        addPath(paths, Path.of("twilio.properties"));
        addAncestorPropertyPaths(paths, Path.of("").toAbsolutePath().normalize());
        addCodeSourcePropertyPaths(paths);
        return new ArrayList<>(paths);
    }

    private void addAncestorPropertyPaths(Set<Path> paths, Path startPath) {
        if (startPath == null) {
            return;
        }
        Path current = Files.isRegularFile(startPath) ? startPath.getParent() : startPath;
        while (current != null) {
            addPath(paths, current.resolve(Path.of("src", "main", "resources", "twilio.properties")));
            addPath(paths, current.resolve("twilio.properties"));
            current = current.getParent();
        }
    }

    private void addCodeSourcePropertyPaths(Set<Path> paths) {
        try {
            if (getClass().getProtectionDomain() == null
                    || getClass().getProtectionDomain().getCodeSource() == null
                    || getClass().getProtectionDomain().getCodeSource().getLocation() == null) {
                return;
            }
            Path codeSourcePath = Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            addAncestorPropertyPaths(paths, codeSourcePath.toAbsolutePath().normalize());
        } catch (Exception ignored) {
        }
    }

    private void addPath(Set<Path> paths, Path path) {
        if (path != null) {
            paths.add(path.toAbsolutePath().normalize());
        }
    }

    private void addClasspathProperties(List<TwilioConfiguration> candidates) {
        try (InputStream inputStream = getClass().getResourceAsStream("/twilio.properties")) {
            if (inputStream != null) {
                Properties loaded = loadProperties(inputStream);
                candidates.add(fromProperties(loaded, "classpath:/twilio.properties"));
            }
        } catch (IOException ignored) {
        }
    }

    private Properties loadProperties(InputStream inputStream) throws IOException {
        Properties loaded = new Properties();
        loaded.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        Properties normalized = new Properties();
        for (String propertyName : loaded.stringPropertyNames()) {
            normalized.setProperty(stripLeadingBom(propertyName), loaded.getProperty(propertyName));
        }
        return normalized;
    }

    private String stripLeadingBom(String value) {
        if (value == null) {
            return "";
        }
        String stripped = value;
        while (stripped.startsWith("\uFEFF") || stripped.startsWith("ï»¿")) {
            stripped = stripped.startsWith("\uFEFF")
                    ? stripped.substring(1)
                    : stripped.substring("ï»¿".length());
        }
        return stripped;
    }

    private TwilioConfiguration fromProperties(Properties properties, String source) {
        return new TwilioConfiguration(
                property(properties, ACCOUNT_SID_PROPERTY),
                property(properties, AUTH_TOKEN_PROPERTY),
                property(properties, PHONE_NUMBER_PROPERTY, FROM_NUMBER_PROPERTY),
                property(properties, DEFAULT_COUNTRY_CODE_PROPERTY),
                source
        );
    }

    private TwilioConfiguration fromSystemProperties() {
        return new TwilioConfiguration(
                System.getProperty(ACCOUNT_SID_PROPERTY),
                System.getProperty(AUTH_TOKEN_PROPERTY),
                firstNonBlank(System.getProperty(PHONE_NUMBER_PROPERTY), System.getProperty(FROM_NUMBER_PROPERTY)),
                System.getProperty(DEFAULT_COUNTRY_CODE_PROPERTY),
                "java system properties"
        );
    }

    private TwilioConfiguration fromEnvironment() {
        return new TwilioConfiguration(
                System.getenv(ACCOUNT_SID_ENV),
                System.getenv(AUTH_TOKEN_ENV),
                System.getenv(PHONE_NUMBER_ENV),
                System.getenv(DEFAULT_COUNTRY_CODE_ENV),
                "environment variables"
        );
    }

    private String property(Properties properties, String... propertyNames) {
        if (properties == null) {
            return "";
        }
        for (String propertyName : propertyNames) {
            String value = properties.getProperty(propertyName);
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String twilioApiMessage(ApiException exception, String toNumber, String fromNumber) {
        String message = safeMessage(exception);
        String normalizedMessage = message.toLowerCase(Locale.ROOT);
        Integer statusCode = exception.getStatusCode();
        Integer code = exception.getCode();

        if (Integer.valueOf(401).equals(statusCode) || normalizedMessage.contains("authenticate")) {
            return "Twilio authentication failed. The Account SID and Auth Token being used by the app do not match. "
                    + configuration.describe() + ". Update src/main/resources/twilio.properties and restart the app.";
        }
        if (Integer.valueOf(21608).equals(code) || normalizedMessage.contains("unverified")) {
            return "Twilio trial account cannot send SMS to " + maskPhone(toNumber)
                    + " until that destination number is verified in Twilio. Twilio message: " + message;
        }
        if (normalizedMessage.contains("mismatch between the 'from' number")
                || normalizedMessage.contains("from number") && normalizedMessage.contains("account")) {
            return message + " Check that " + maskPhone(fromNumber)
                    + " belongs to the same account SID. " + configuration.describe();
        }
        return "Twilio rejected the SMS: " + message + " " + configuration.describe();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeMessage(Exception exception) {
        return exception == null || exception.getMessage() == null || exception.getMessage().isBlank()
                ? "Unknown error."
                : exception.getMessage();
    }

    private static String statusName(Message message) {
        return message == null || message.getStatus() == null ? "unknown" : message.getStatus().toString();
    }

    private static String firstNonBlank(String firstValue, String secondValue) {
        return isBlank(firstValue) ? secondValue : firstValue;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String maskAccountSid(String accountSid) {
        if (isBlank(accountSid)) {
            return "missing";
        }
        String trimmed = accountSid.trim();
        if (trimmed.length() <= 10) {
            return "configured";
        }
        return trimmed.substring(0, 6) + "..." + trimmed.substring(trimmed.length() - 4);
    }

    private static String maskPhone(String phoneNumber) {
        if (isBlank(phoneNumber)) {
            return "missing";
        }
        String digits = phoneNumber.replaceAll("[^0-9]", "");
        if (digits.length() <= 4) {
            return "configured";
        }
        String prefix = phoneNumber.trim().startsWith("+") ? "+" : "";
        return prefix + "..." + digits.substring(digits.length() - 4);
    }

    public record SmsSendResult(String messageSid, String status, String toNumber) {
        static SmsSendResult fromMessage(Message message, String toNumber) {
            String sid = message == null ? "" : message.getSid();
            return new SmsSendResult(sid, statusName(message), toNumber);
        }

        public boolean delivered() {
            return "delivered".equalsIgnoreCase(status);
        }

        public String shortMessageSid() {
            if (isBlank(messageSid) || messageSid.length() <= 8) {
                return messageSid == null ? "" : messageSid;
            }
            return messageSid.substring(0, 4) + "..." + messageSid.substring(messageSid.length() - 4);
        }
    }

    private static final class TwilioConfiguration {
        private final String accountSid;
        private final String authToken;
        private final String fromNumber;
        private final String defaultCountryCode;
        private final String source;

        private TwilioConfiguration(String accountSid, String authToken, String fromNumber,
                                    String defaultCountryCode, String source) {
            this.accountSid = safeConfigValue(accountSid);
            this.authToken = safeConfigValue(authToken);
            this.fromNumber = safeConfigValue(fromNumber);
            this.defaultCountryCode = isBlank(defaultCountryCode) ? DEFAULT_COUNTRY_CODE : defaultCountryCode.trim();
            this.source = isBlank(source) ? "unknown source" : source.trim();
        }

        private static TwilioConfiguration empty(String source) {
            return new TwilioConfiguration("", "", "", DEFAULT_COUNTRY_CODE, source);
        }

        private String accountSid() {
            return accountSid;
        }

        private String authToken() {
            return authToken;
        }

        private String fromNumber() {
            return fromNumber;
        }

        private String defaultCountryCode() {
            return defaultCountryCode;
        }

        private boolean isComplete() {
            return !isBlank(accountSid) && !isBlank(authToken) && !isBlank(fromNumber);
        }

        private int configuredValueCount() {
            int count = 0;
            if (!isBlank(accountSid)) {
                count++;
            }
            if (!isBlank(authToken)) {
                count++;
            }
            if (!isBlank(fromNumber)) {
                count++;
            }
            return count;
        }

        private List<String> missingNames() {
            List<String> missing = new ArrayList<>();
            if (isBlank(accountSid)) {
                missing.add(ACCOUNT_SID_PROPERTY + " / " + ACCOUNT_SID_ENV);
            }
            if (isBlank(authToken)) {
                missing.add(AUTH_TOKEN_PROPERTY + " / " + AUTH_TOKEN_ENV);
            }
            if (isBlank(fromNumber)) {
                missing.add(FROM_NUMBER_PROPERTY + " / " + PHONE_NUMBER_ENV);
            }
            return missing;
        }

        private String describe() {
            return "Twilio config source: " + source
                    + ", account: " + maskAccountSid(accountSid)
                    + ", from: " + maskPhone(fromNumber);
        }

        private static String safeConfigValue(String value) {
            return value == null ? "" : value.trim();
        }
    }

    public static class SmsException extends RuntimeException {
        public SmsException(String message) {
            super(message);
        }

        public SmsException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
