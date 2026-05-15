package com.example.carrentingtest.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.carrentingtest.MainActivity;
import com.example.carrentingtest.R;

import java.util.concurrent.atomic.AtomicInteger;

public final class NotificationHelper {

    public static final String CHANNEL_BOOKING = "booking_updates";
    public static final String CHANNEL_VERIFICATION = "verification_updates";

    private static final AtomicInteger notifId = new AtomicInteger(1000);

    private NotificationHelper() {}

    public static void createChannels(Context context) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;

        nm.createNotificationChannel(new NotificationChannel(
                CHANNEL_BOOKING,
                "Booking Updates",
                NotificationManager.IMPORTANCE_DEFAULT));

        nm.createNotificationChannel(new NotificationChannel(
                CHANNEL_VERIFICATION,
                "Verification Updates",
                NotificationManager.IMPORTANCE_DEFAULT));
    }

    public static void showBookingNotification(Context context, String title, String body) {
        show(context, CHANNEL_BOOKING, title, body, MainActivity.class);
    }

    public static void showVerificationNotification(Context context, String title, String body) {
        show(context, CHANNEL_VERIFICATION, title, body, MainActivity.class);
    }

    private static void show(Context context, String channel, String title, String body,
                              Class<?> targetActivity) {
        PendingIntent pi = PendingIntent.getActivity(
                context,
                0,
                new Intent(context, targetActivity).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channel)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pi);

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        try {
            nm.notify(notifId.getAndIncrement(), builder.build());
        } catch (SecurityException ignored) {
            // POST_NOTIFICATIONS not granted — silently skip
        }
    }
}
