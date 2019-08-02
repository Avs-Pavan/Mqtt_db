package com.kevin.mqtt.Fucky.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.kevin.mqtt.Fucky.util.FuckyLogger;
import com.kevin.mqtt.Fucky.util.FuckyServiceManager;


public class FuckyUpdateReceiver extends BroadcastReceiver
{
  public FuckyUpdateReceiver() {}
  
  public void onReceive(Context context, Intent intent)
  {
   FuckyLogger.d("App updated");
    

    FuckyServiceManager.start(context);
  }
}
