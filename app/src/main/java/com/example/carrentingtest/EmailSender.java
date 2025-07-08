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

    // Email configuration - WARNING: Hardcoding credentials is not secure!
    // In production, these should come from secure storage or backend service
    private static String SMTP_HOST = "smtp.gmail.com";  // Gmail SMTP server
    private static String SMTP_PORT = "587";            // Default TLS port for Gmail
    private static String EMAIL = "baghdadmohamed.me@gmail.com";  // Sender email
    private static String PASSWORD = "enxy piut ewpd zhvm";       // App password (not regular password)

    // Callback interface to handle email sending results
    public interface EmailCallback {
        void onSuccess();                   // Called when email sends successfully
        void onFailure(String error);       // Called when email fails to send
    }

    /**
     * Sends an email using SMTP protocol in a background thread
     * @param context   Android context (must be Activity for UI thread callbacks)
     * @param recipient Email address of recipient
     * @param subject   Email subject line
     * @param body      Email content
     * @param callback  Callback to handle success/failure
     */
    public static void sendEmail(Context context, String recipient, String subject, String body, EmailCallback callback) {
        // Start a new thread to avoid network operations on main thread
        new Thread(() -> {
            try {
                // Configure SMTP server properties
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");            // Enable authentication
                props.put("mail.smtp.starttls.enable", "true"); // Enable TLS encryption
                props.put("mail.smtp.host", SMTP_HOST);        // SMTP server address
                props.put("mail.smtp.port", SMTP_PORT);        // SMTP server port

                // Create mail session with authentication
                Session session = Session.getInstance(props,
                        new javax.mail.Authenticator() {
                            // Provides credentials for SMTP authentication
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(EMAIL, PASSWORD);
                            }
                        });

                // Create a new email message
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(EMAIL));   // Set sender address
                // Set recipient address(es) - can be multiple separated by commas
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
                message.setSubject(subject);    // Set email subject
                message.setText(body);          // Set email body (plain text)

                // Send the email
                Transport.send(message);

                // If callback provided, notify success on UI thread
                if (callback != null) {
                    ((Activity) context).runOnUiThread(callback::onSuccess);
                }
            } catch (Exception e) {
                // Log any errors that occur
                Log.e(TAG, "Email sending failed", e);
                // If callback provided, notify failure with error message
                if (callback != null) {
                    ((Activity) context).runOnUiThread(() -> callback.onFailure(e.getMessage()));
                }
            }
        }).start();  // Start the background thread
    }
}