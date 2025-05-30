package com.example.carrentingtest;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSender {
    private static final String TAG = "EmailSender";

    // These should come from your app configuration (Firebase Remote Config or secure storage)
    private static String SMTP_HOST = "smtp.gmail.com";
    private static String SMTP_PORT = "587";
    private static String EMAIL = "baghdadmohamed.me@gmail.com";
    private static String PASSWORD = "enxy piut ewpd zhvm";

    public interface EmailCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public static void sendEmail(Context context, String recipient, String subject, String body, EmailCallback callback) {
        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", SMTP_HOST);
                props.put("mail.smtp.port", SMTP_PORT);

                Session session = Session.getInstance(props,
                        new javax.mail.Authenticator() {
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(EMAIL, PASSWORD);
                            }
                        });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(EMAIL));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
                message.setSubject(subject);
                message.setText(body);

                Transport.send(message);

                // Run callback on UI thread
                if (callback != null) {
                    ((Activity) context).runOnUiThread(callback::onSuccess);
                }
            } catch (Exception e) {
                Log.e(TAG, "Email sending failed", e);
                if (callback != null) {
                    ((Activity) context).runOnUiThread(() -> callback.onFailure(e.getMessage()));
                }
            }
        }).start();
    }
}