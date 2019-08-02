package com.kevin.mqtt.Fucky.util.exceptions;

public class FuckyDateTime {
  public FuckyDateTime() {}
  
  public static long getCurrentTimestamp() { return System.currentTimeMillis() / 1000L; }
}
