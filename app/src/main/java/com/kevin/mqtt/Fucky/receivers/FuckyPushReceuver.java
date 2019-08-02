package com.kevin.mqtt.Fucky.receivers;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class FuckyPushReceuver extends android.content.BroadcastReceiver
{
  public FuckyPushReceuver() {}
  
  @SuppressLint({"NewApi"})
  public void onReceive(Context context, Intent intent)
  {
    String notificationTitle = getAppName(context);
    String notificationText = "";


    if (intent.getStringExtra("message") != null) {
      notificationText = intent.getStringExtra("message");
    }

    Notification.Builder builder = new Notification.Builder(context).setAutoCancel(true).setContentTitle(notificationTitle).setContentText(notificationText).setVibrate(new long[] { 0L, 400L, 250L, 400L }).setSound(android.media.RingtoneManager.getDefaultUri(2)).setContentIntent(getMainActivityPendingIntent(context));
    

    @SuppressLint("WrongConstant") NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
    Log.e("fucky fucky",notificationText
    );

    //notificationManager.notify(1, builder.build());
  }
  
  private static String getAppName(Context context)
  {
    return context.getPackageManager().getApplicationLabel(context.getApplicationInfo()).toString();
  }
  
  private PendingIntent getMainActivityPendingIntent(Context context)
  {
    Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getApplicationContext().getPackageName());
    

    launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    

    return PendingIntent.getActivity(context, 0, launchIntent, Intent.FILL_IN_ACTION);
  }
}
