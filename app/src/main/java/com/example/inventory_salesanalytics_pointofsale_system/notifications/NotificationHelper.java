package com.example.inventory_salesanalytics_pointofsale_system.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.inventory_salesanalytics_pointofsale_system.R;
import com.example.inventory_salesanalytics_pointofsale_system.activities.NotificationDetailActivity;

public class NotificationHelper {

    private static final String CHANNEL_ID = "BianosPizza_Alerts";
    private static final String CHANNEL_NAME = "Admin Alerts";
    private static final String CHANNEL_DESC = "Notifications for Inventory and Sales";

    public static void showNotification(Context context, String title, String message) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // 1. Create Intent to open the Detail Activity (similar to opening an SMS/Email)
        Intent intent = new Intent(context, NotificationDetailActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("message", message);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        // 2. Wrap it in a PendingIntent
        int flags = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) 
                ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT 
                : PendingIntent.FLAG_UPDATE_CURRENT;
        
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int)System.currentTimeMillis(), intent, flags);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(CHANNEL_DESC);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true) 
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
