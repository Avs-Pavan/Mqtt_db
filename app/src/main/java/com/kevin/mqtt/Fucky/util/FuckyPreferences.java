package com.kevin.mqtt.Fucky.util;

import android.content.Context;

public class FuckyPreferences
{
  public FuckyPreferences() {}
  
  public static String getString(String key, String defaultValue, Context context) {
    return FuckySingleton.getSettings(context).getString(key, defaultValue);
  }
  
  public static boolean getBoolean(String key, boolean defaultValue, Context context)
  {
    return FuckySingleton.getSettings(context).getBoolean(key, defaultValue);
  }
  
  public static int getInt(String key, int defaultValue, Context context)
  {
    return FuckySingleton.getSettings(context).getInt(key, defaultValue);
  }
  
  public static void saveString(String key, String value, Context context)
  {
    FuckySingleton.getSettings(context).edit().putString(key, value).apply();
  }
  
  public static void remove(String key, Context context)
  {
    FuckySingleton.getSettings(context).edit().remove(key).apply();
  }
  
  public static void saveInt(String key, int value, Context context)
  {
    FuckySingleton.getSettings(context).edit().putInt(key, value).apply();
  }
  
  public static void saveBoolean(String key, boolean value, Context context)
  {
    FuckySingleton.getSettings(context).edit().putBoolean(key, value).apply();
  }
}
