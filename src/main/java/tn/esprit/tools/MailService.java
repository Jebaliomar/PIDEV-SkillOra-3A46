package tn.esprit.tools;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.InputStream;
import java.util.Properties;

public class MailService {

    private static Properties config;

    private static synchronized Properties config() {
        if (config == null) {
            config = new Properties();
            try (InputStream in = MailService.class.getResourceAsStream("/mail.properties")) {
                if (in == null) {
                    throw new IllegalStateException("mail.properties not found on classpath");
                }
                config.load(in);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load mail.properties", e);
            }
        }
        return config;
    }

    public static void sendHtml(String toAddress, String subject, String htmlBody) throws Exception {
        Properties cfg = config();

        Properties props = new Properties();
        props.put("mail.smtp.host", cfg.getProperty("mail.smtp.host"));
        props.put("mail.smtp.port", cfg.getProperty("mail.smtp.port"));
        props.put("mail.smtp.auth", cfg.getProperty("mail.smtp.auth", "true"));
        props.put("mail.smtp.starttls.enable", cfg.getProperty("mail.smtp.starttls.enable", "true"));
        props.put("mail.smtp.ssl.trust", cfg.getProperty("mail.smtp.host"));

        final String user = cfg.getProperty("mail.username");
        final String pass = cfg.getProperty("mail.password");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        });

        MimeMessage msg = new MimeMessage(session);
        String fromName = cfg.getProperty("mail.from.name", "SkillORA");
        String fromAddress = cfg.getProperty("mail.from.address", user);
        msg.setFrom(new InternetAddress(fromAddress, fromName, "UTF-8"));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress));
        msg.setSubject(subject, "UTF-8");
        msg.setContent(htmlBody, "text/html; charset=UTF-8");

        Transport.send(msg);
    }
}
