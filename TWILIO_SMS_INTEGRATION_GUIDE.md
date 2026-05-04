# Twilio SMS Integration

The reservation dashboard now includes an SMS section with:

- `ComboBox<Reservation>` that displays each reservation phone number.
- `TextArea` for the SMS body.
- `Send SMS` button with validation and success/error status text.
- Twilio logic isolated in `tn.esprit.services.TwilioSmsService`.

## Reservation Page Controller

The implementation is in:

```text
src/main/java/tn/esprit/controlles/ReservationDashboardController.java
```

Main flow:

```java
@FXML
private void onSendSms() {
    Reservation selectedReservation = reservationComboBox.getValue();
    String phoneNumber = selectedReservation.getPhone();
    String message = smsMessageArea.getText().trim();

    twilioSmsService.sendSms(phoneNumber, message);
}
```

The real controller also validates:

- reservation selected
- phone number exists
- message is not empty

## TwilioSmsService

The service reads credentials from the first complete configuration source it can find:

1. explicit `twilio.properties.path` or `TWILIO_PROPERTIES_PATH`
2. local `src/main/resources/twilio.properties`
3. classpath `twilio.properties`
4. Java system properties
5. environment variables

```java
private static final String ACCOUNT_SID_ENV = "TWILIO_ACCOUNT_SID";
private static final String AUTH_TOKEN_ENV = "TWILIO_AUTH_TOKEN";
private static final String PHONE_NUMBER_ENV = "TWILIO_PHONE_NUMBER";
```

SMS sending is handled by:

```java
twilioSmsService.sendSms(phoneNumber, message);
```

## Maven Dependency

The project uses the official Twilio Java helper library:

```xml
<dependency>
    <groupId>com.twilio.sdk</groupId>
    <artifactId>twilio</artifactId>
    <version>11.4.0</version>
</dependency>
```

## Environment Variables

PowerShell:

```powershell
$env:TWILIO_ACCOUNT_SID="ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
$env:TWILIO_AUTH_TOKEN="your_auth_token"
$env:TWILIO_PHONE_NUMBER="+1234567890"
```

Linux/macOS:

```bash
export TWILIO_ACCOUNT_SID="ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
export TWILIO_AUTH_TOKEN="your_auth_token"
export TWILIO_PHONE_NUMBER="+1234567890"
```
