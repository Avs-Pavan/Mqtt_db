package com.kevin.mqtt.Fucky.config;



public class FuckyConfig
{

  public static final String DIRECT_ENDPOINT = "tcp://nallas.in:1883";
  public static int MQTT_QUALITY_OF_SERVICE = 1;
  public static short MQTT_DEFAULT_KEEP_ALIVE = 300;
  public static boolean MQTT_RETAINED_PUBLISH = false;
  public static short MQTT_ACK_TIMEOUT = 15000;
  public static short MQTT_CONNECT_TIMEOUT = 15;
  public static int MQTT_JOB_ID = 10000;
  public static int MQTT_DEFAULT_JOB_SERVICE_INTERVAL = 15;
  public static int MQTT_JOB_TASK_INTERVAL_PADDING = 2;
  public FuckyConfig() {}
}
