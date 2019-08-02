package com.kevin.mqtt.Fucky.util;

import android.content.Context;
import android.content.SharedPreferences;


public class FuckySingleton
{
  static SharedPreferences mSettings;
  
  public FuckySingleton() {}
  

  public static SharedPreferences getSettings(Context context) {
    if (mSettings == null)
    {
      mSettings = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
    }
    
    return mSettings;
  }
}
