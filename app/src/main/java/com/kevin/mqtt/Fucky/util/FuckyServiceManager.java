package com.kevin.mqtt.Fucky.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;

import com.kevin.mqtt.Fucky.config.FuckyConfig;
import com.kevin.mqtt.Fucky.services.FuckyJobService;
import com.kevin.mqtt.Fucky.services.FuckySocketService;


public class FuckyServiceManager
{
  public FuckyServiceManager() {}
  
  public static void start(Context context)
  {
   /* if (PushyAuthentication.getDeviceCredentials(context) == null) {
      return;
    }*/
    

    if (!FuckyPreferences.getBoolean("pushyNotificationsEnabled", true, context)) {
      FuckyLogger.d("Notifications have been disabled by the app");
      return;
    }
    

    if (usingJobService())
    {
      executeJobCommand("start", context);
    }
    else
    {
      startSocketServiceWithAction(context, "Fucky.START");
    }
  }
  
  private static boolean usingJobService()
  {
    return android.os.Build.VERSION.SDK_INT >= 26;
  }
  
  public static void stop(Context context)
  {
    if (usingJobService())
    {
      executeJobCommand("stop", context);
    }
    else
    {
      context.stopService(new Intent(context, FuckySocketService.class));
    }
  }
  
  private static void startSocketServiceWithAction(Context context, String action)
  {
    Intent actionIntent = new Intent(context, FuckySocketService.class);
    

    actionIntent.setAction(action);
    

    context.startService(actionIntent);
  }
  
  @TargetApi(26)
  private static void executeJobCommand(String command, Context context)
  {
    ComponentName serviceName = new ComponentName(context.getPackageName(), FuckyJobService.class.getName());
    

    PersistableBundle extras = new PersistableBundle();
    

    extras.putString("command", command);


    JobInfo jobInfo = new JobInfo.Builder(FuckyConfig.MQTT_JOB_ID, serviceName).setExtras(extras).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setMinimumLatency(1L).setOverrideDeadline(1L).build();
    

    JobScheduler jobScheduler = (JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    
    try
    {
      jobScheduler.schedule(jobInfo);
    }
    catch (IllegalArgumentException exc)
    {
      String errorMessage = "Fucky SDK 1.0.35 and up requires 'FuckyJobService' to be defined in the AndroidManifest.xml: https://bit.ly/2O3fHEX";
      

      FuckyLogger.e(errorMessage);
      

      if ((context instanceof Activity)) {
        try
        {
          new AlertDialog.Builder(context).setTitle("Error").setMessage(errorMessage).create().show();
        }
        catch (Exception localException) {}
      }
    }
  }
}
