package com.kevin.mqtt.Fucky.util;

import android.net.ConnectivityManager;

public class FuckyNetworking {
  public FuckyNetworking() {}
  
  public static int getConnectedNetwork(ConnectivityManager manager) {
    android.net.NetworkInfo wifi = manager.getNetworkInfo(1);
    android.net.NetworkInfo mobile = manager.getNetworkInfo(0);
    

    if ((wifi != null) && (wifi.isConnected())) {
      return 1;
    }
    

    if ((mobile != null) && (mobile.isConnected())) {
      return 0;
    }
    

    return -1;
  }
  
  public static boolean isNetworkAvailable(ConnectivityManager connectivityManager)
  {
    android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    

    return activeNetworkInfo != null;
  }
}
