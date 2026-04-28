package tn.esprit.services;

import tn.esprit.entities.Reservation;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Properties;

public class TwilioSmsService {

    private static final String TWILIO_MESSAGES_URL = "https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json";
    private static final String DEFAULT_COUNTRY_CODE = "+216";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .build();
    private final Properties properties = loadProperties();

    public boolean isConfigured() {
        return !isBlank(accountSid()) && !isBlank(authToken()) && !isBlank(fromNumber());
    }

    public void sendReservationThankYou(Reservation reservation, String eventTitle, String salleName) {
        if (!isConfigured()) {
            throw new SmsException("Twilio n'est pas configure. Ajoutez les variables d'environnement ou twilio.properties.");
        }

        String toNumber = normalizePhoneNumber(reservation == null ? null : reservation.getTelephone());
        if (isBlank(toNumber) || !toNumber.startsWith("+")) {
            throw new SmsException("Numero telephone invalide pour Twilio. Utilisez le format +216XXXXXXXX.");
        }

        String body = buildReservationMessage(reservation, eventTitle, salleName);
        sendSms(toNumber, body);
    }

    private void sendSms(String toNumber, String body) {
        String sid = accountSid();
        String requestBody = formParam("From", fromNumber())
                + "&" + formParam("To", toNumber)
                + "&" + formParam("Body", body);
        String auth = Base64.getEncoder().encodeToString((sid + ":" + authToken()).getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format(TWILIO_MESSAGES_URL, sid)))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Basic " + auth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new SmsException("Twilio a refuse le SMS: " + extractErrorMessage(response.body()));
            }
        } catch (IOException e) {
            throw new SmsException("Impossible de contacter Twilio: " + safeMessage(e), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SmsException("Envoi SMS interrompu.", e);
        }
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
        String defaultCountryCode = config("TWILIO_DEFAULT_COUNTRY_CODE", "twilio.default-country-code", DEFAULT_COUNTRY_CODE);
        String countryDigits = defaultCountryCode.replaceAll("[^0-9]", "");
        if (!countryDigits.isBlank() && digits.startsWith(countryDigits)) {
            return "+" + digits;
        }
        if (digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        return defaultCountryCode + digits;
    }

    private String accountSid() {
        return config("TWILIO_ACCOUNT_SID", "twilio.account.sid", "");
    }

    private String authToken() {
        return config("TWILIO_AUTH_TOKEN", "twilio.auth.token", "");
    }

    private String fromNumber() {
        return config("TWILIO_FROM_NUMBER", "twilio.from.number", "");
    }

    private String config(String envName, String propertyName, String fallback) {
        String envValue = System.getenv(envName);
        if (!isBlank(envValue)) {
            return envValue.trim();
        }
        String systemValue = System.getProperty(propertyName);
        if (!isBlank(systemValue)) {
            return systemValue.trim();
        }
        String propertyValue = properties.getProperty(propertyName);
        if (!isBlank(propertyValue)) {
            return propertyValue.trim();
        }
        return fallback;
    }

    private Properties loadProperties() {
        Properties loaded = new Properties();
        try (InputStream inputStream = getClass().getResourceAsStream("/twilio.properties")) {
            if (inputStream != null) {
                loaded.load(inputStream);
            }
        } catch (IOException ignored) {
        }
        return loaded;
    }

    private String formParam(String key, String value) {
        return URLEncoder.encode(key, StandardCharsets.UTF_8)
                + "="
                + URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String extractErrorMessage(String body) {
        String message = extractJsonString(body, "message");
        if (!message.isBlank()) {
            return message;
        }
        return body == null || body.isBlank() ? "Erreur Twilio inconnue." : body;
    }

    private String extractJsonString(String json, String key) {
        if (json == null || json.isBlank()) {
            return "";
        }
        String marker = "\"" + key + "\"";
        int keyIndex = json.indexOf(marker);
        if (keyIndex < 0) {
            return "";
        }
        int colonIndex = json.indexOf(':', keyIndex + marker.length());
        if (colonIndex < 0) {
            return "";
        }
        int startQuote = json.indexOf('"', colonIndex + 1);
        if (startQuote < 0) {
            return "";
        }
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = startQuote + 1; i < json.length(); i++) {
            char current = json.charAt(i);
            if (escaped) {
                value.append(current);
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else if (current == '"') {
                break;
            } else {
                value.append(current);
            }
        }
        return value.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeMessage(Exception exception) {
        return exception == null || exception.getMessage() == null || exception.getMessage().isBlank()
                ? "Erreur inconnue."
                : exception.getMessage();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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
