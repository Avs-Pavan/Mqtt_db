package com.kevin.mqtt.Fucky.util;

import android.content.SharedPreferences;
import android.util.Log;

import com.hypertrack.hyperlog.HyperLog;

public class FuckyLogger {
  private static PushyLogListener mListener;
  
  public FuckyLogger() {}
  
  public static void d(String message) {
    Log.d("Fucky", message);



    if (mListener != null) {
      mListener.onDebugLog(message);
    }
  }
  
  public static void d(String message, Exception exc)
  {
    Log.d("Fucky", message, exc);



    if (mListener != null) {
      mListener.onDebugLog(message);
    }
  }
  
  public static void e(String message)
  {
    Log.e("Fucky", message);



    if (mListener != null) {
      mListener.onErrorLog(message);
    }
  }
  
  public static void e(String message, Exception exc)
  {
    Log.e("Fucky", message, exc);
    

    if (mListener != null) {
      mListener.onErrorLog(message);
    }
  }
  
  public static void setLogListener(PushyLogListener listener)
  {
    mListener = listener;
  }

  public static abstract interface PushyLogListener
  {
    public abstract void onDebugLog(String paramString);

    public abstract void onErrorLog(String paramString);
  }




}
