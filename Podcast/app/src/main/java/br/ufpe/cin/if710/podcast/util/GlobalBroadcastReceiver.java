package br.ufpe.cin.if710.podcast.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import br.ufpe.cin.if710.podcast.ui.MainActivity;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by albert on 06/10/17.
 */

public class GlobalBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final Intent notificationIntent = new Intent(context, MainActivity.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        final Notification notification = new Notification.Builder(
                context)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setOngoing(true).setContentTitle("Download finalizado!")
                .setContentText("Clique para acessar a lista de episódios.")
                .setContentIntent(pendingIntent).build();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
        //TODO update database with "true" in downloaded column
    }
}
