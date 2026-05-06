package tn.esprit.tools;

import tn.esprit.services.TwilioSmsService;

import java.util.Arrays;

public class TwilioTestCli {

    public static void main(String[] args) {
        TwilioSmsService service = new TwilioSmsService();
        System.out.println(service.describeConfiguration());

        if (args.length == 1 && "--check-config".equals(args[0])) {
            System.out.println(service.isConfigured() ? "CONFIG_OK" : "CONFIG_MISSING");
            return;
        }

        String toNumber = args.length > 0 ? args[0] : System.getenv("TWILIO_TEST_TO");
        if (toNumber == null || toNumber.trim().isEmpty()) {
            System.err.println("Usage: TwilioTestCli <to-phone-number> [message]");
            System.exit(2);
            return;
        }

        String message = args.length > 1
                ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                : "SkillOra Twilio test";

        try {
            TwilioSmsService.SmsSendResult result = service.sendSms(toNumber, message);
            System.out.println("SEND_OK status=" + result.status()
                    + " message_sid=" + result.shortMessageSid()
                    + " to=" + result.toNumber());
        } catch (TwilioSmsService.SmsException exception) {
            System.err.println("SEND_FAILED " + exception.getMessage());
            System.exit(1);
        }
    }
}
