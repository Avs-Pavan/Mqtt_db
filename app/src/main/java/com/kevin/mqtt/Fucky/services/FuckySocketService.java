package com.kevin.mqtt.Fucky.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;


import com.kevin.mqtt.Fucky.util.FuckyLogger;
import com.kevin.mqtt.Fucky.util.FuckyMqttConnection;
import com.kevin.mqtt.Fucky.util.FuckyNetworking;
import com.kevin.mqtt.Fucky.util.FuckyPreferences;
import com.kevin.mqtt.Fucky.util.exceptions.FuckyFatalException;
import com.kevin.mqtt.MainActivity;
import com.kevin.mqtt.model.FuckyMessage;
import com.kevin.mqtt.model.db.FuckyRepository;
import com.kevin.mqtt.roomdb.AppDatabase;

import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class FuckySocketService
  extends Service
{
  private WifiManager mWifiManager;
  private AlarmManager mAlarmManager;
  private ConnectivityManager mConnectivityManager;
  private boolean mIsDestroyed;
  private FuckyMqttConnection mSocket;
  private long mRetryInterval = 500L;

  public FuckySocketService() {}
  
  public void onCreate()
  {
    super.onCreate();
    

    FuckyLogger.d("Creating service");
    savetoDb("Creating service",true);



    mWifiManager = ((WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE));
    mAlarmManager = ((AlarmManager)getApplicationContext().getSystemService(ALARM_SERVICE));
    mConnectivityManager = ((ConnectivityManager)getApplicationContext().getSystemService(CONNECTIVITY_SERVICE));
    

    mSocket = new FuckyMqttConnection(this, mWifiManager, mConnectivityManager, FuckyMqttConnection.getKeepAliveInterval(this), new ConnectionLostRunnable());
    

    handleCrashedService();
    

    start();
    

    registerReceiver(mConnectivityListener, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
  }
  
  public class ConnectionLostRunnable implements Runnable {
    public ConnectionLostRunnable() {}
    
    public void run() {
      FuckySocketService.this.reconnectAsync();
    }
  }
  

  public void onTaskRemoved(Intent rootIntent)
  {
    FuckyLogger.d("Task removed, attempting restart in 3 seconds");
    savetoDb("Task removed, attempting restart in 3 seconds",true);



    Intent restartService = new Intent(getApplicationContext(), getClass());
    

    restartService.setPackage(getPackageName());
    

    PendingIntent restartServiceIntent = PendingIntent.getService(getApplicationContext(), 1, restartService, PendingIntent.FLAG_ONE_SHOT);
    

    mAlarmManager.set(  AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 3000L, restartServiceIntent);
  }
  
  private void handleCrashedService()
  {
    stopKeepAliveTimerAndWifiLock();
    

    cancelReconnect();
  }
  

  public void onDestroy()
  {
    FuckyLogger.d("Service destroyed");
    savetoDb("Service destroyed\"",true);



    mIsDestroyed = true;
    

    stop();
    

    super.onDestroy();
  }
  

  public int onStartCommand(Intent intent, int flags, int startId)
  {
    if ((intent == null) || (intent.getAction() == null))
    {
      start();

    }
    else if (intent.getAction().equals("Fucky.START"))
    {
      start();
    } else if (intent.getAction().equals("Fucky.KEEP_ALIVE"))
    {
      sendKeepAlive();
    } else if (intent.getAction().equals("Fucky.RECONNECT"))
    {
      reconnectAsync();
    }
    


    return START_STICKY;
  }
  
  private void start()
  {
    if (!FuckyNetworking.isNetworkAvailable(mConnectivityManager)) {
      return;
    }
    

    if ((mSocket.isConnecting()) || (mSocket.isConnected()))
    {
      return;
    }
    


    if (!FuckyPreferences.getBoolean("pushyNotificationsEnabled", true, this)) {
      FuckyLogger.d("Notifications have been disabled by the app");
      savetoDb("Notifications have been disabled by the app",true);

      stopSelf();
      return;
    }
    

    new ConnectAsync().execute(new Integer[0]);
  }
  
  private void stop()
  {
    cancelReconnect();
    

    stopKeepAliveTimerAndWifiLock();
    

    unregisterReceiver(mConnectivityListener);
    

    mSocket.disconnectExistingClient();
  }
  
  private void sendKeepAlive()
  {
    if (mSocket.isConnected())
    {
      new SendKeepAliveAsync().execute(new Integer[0]);
    }
  }
  
  private void startKeepAliveTimerAndWifiLock()
  {
    long interval = FuckyMqttConnection.getKeepAliveInterval(this) * 1000;
    

    PendingIntent pendingIntent = getAlarmPendingIntent("Fucky.KEEP_ALIVE");
    

    mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + interval, interval, pendingIntent);
    

    mSocket.acquireWifiLock();
  }
  
  PendingIntent getAlarmPendingIntent(String action)
  {
    Intent keepAliveIntent = new Intent();
    

    keepAliveIntent.setClass(this, FuckySocketService.class);
    keepAliveIntent.setAction(action);
    

    return PendingIntent.getService(this, 0, keepAliveIntent, 0);
  }
  
  private void stopKeepAliveTimerAndWifiLock()
  {
    PendingIntent keepAliveIntent = getAlarmPendingIntent("Fucky.KEEP_ALIVE");
    

    mAlarmManager.cancel(keepAliveIntent);
    

    mSocket.releaseWifiLock();
  }
  
  public void scheduleReconnect()
  {
    long now = System.currentTimeMillis();
    

    if (mRetryInterval < 60000L) {
      mRetryInterval = Math.min(mRetryInterval * 2L, 60000L);
    }
    

    FuckyLogger.d("Reconnecting in " + mRetryInterval + "ms.");
    savetoDb("Reconnecting in \" + mRetryInterval + \"ms.",true);



    PendingIntent keepAliveIntent = getAlarmPendingIntent("Fucky.RECONNECT");
    

    mAlarmManager.set(AlarmManager.RTC_WAKEUP, now + mRetryInterval, keepAliveIntent);

  }
  
  public void cancelReconnect()
  {
    PendingIntent reconnectIntent = getAlarmPendingIntent("Fucky.RECONNECT");
    

    mAlarmManager.cancel(reconnectIntent);
  }
  
  private void reconnectAsync()
  {
    stopKeepAliveTimerAndWifiLock();
    

    if (mIsDestroyed) {
      FuckyLogger.d("Not reconnecting (service destroyed)");
      savetoDb("Not reconnecting (service destroyed)",true);

      return;
    }
    

    if (!FuckyNetworking.isNetworkAvailable(mConnectivityManager)) {
      FuckyLogger.d("Not reconnecting (network not available)");
      savetoDb("Not reconnecting (network not available)",true);

      return;
    }
    

    if (mSocket.isConnecting()) {
      FuckyLogger.d("Already reconnecting");
      savetoDb("Already reconnecting",true);

      return;
    }
    

    FuckyLogger.d("Reconnecting...");
    savetoDb("Reconnecting.....",true);


    new ConnectAsync().execute(new Integer[0]);
  }
  
  private BroadcastReceiver mConnectivityListener = new BroadcastReceiver()
  {
    public void onReceive(Context context, Intent intent)
    {
      if (FuckyNetworking.isNetworkAvailable(mConnectivityManager))
      {
        FuckyLogger.d("Internet connected");
        savetoDb("Internet connected",true);


        if (!mSocket.isConnected())
        {
          FuckySocketService.this.reconnectAsync();


        }
        else if ((FuckyNetworking.getConnectedNetwork(mConnectivityManager) == 1) && (mSocket.getNetwork() == 0))
        {
          FuckySocketService.this.reconnectAsync();
        }
      }
      else
      {
        FuckyLogger.d("Internet disconnected");
        savetoDb("Internet disconnected",true);


        cancelReconnect();
      }
    }
  };
  

  public IBinder onBind(Intent intent)
  {
    return null;
  }
  
  public class ConnectAsync extends AsyncTask<Integer, String, Integer>
  {
    public ConnectAsync() {
      mSocket.setConnecting(true);
    }
    

    protected Integer doInBackground(Integer... parameter)
    {
      FuckyLogger.d("Connecting...");
      savetoDb("Connecting...",true);

      try
      {
        mSocket.connect();
        

        if (mIsDestroyed)
        {
          mSocket.disconnectExistingClient();
          

          FuckyLogger.d("Service destroyed, aborting connection");
          savetoDb("Service destroyed, aborting connection",true);


          return Integer.valueOf(0);
        }
        

        mRetryInterval = 500L;
        

        FuckySocketService.this.startKeepAliveTimerAndWifiLock();
        

        FuckyLogger.d("Connected successfully (sending keep alive every " + FuckyMqttConnection.getKeepAliveInterval(FuckySocketService.this) + " seconds)");
        savetoDb("Connected successfully (sending keep alive every "+ FuckyMqttConnection.getKeepAliveInterval(FuckySocketService.this) + " seconds)",true);

      }
      catch (Exception e)
      {
        FuckyLogger.d("Connect exception: " + e.toString());
        savetoDb("Connect exception: " + e.toString(),true);

        Integer localInteger2;
        if (e.getClass() == FuckyFatalException.class)
        {
          FuckyLogger.d("Fatal error encountered, stopping service");
          savetoDb("Fatal error encountered, stopping service",true);


          stopSelf();
          

          return Integer.valueOf(0);
        }
        

        if (e.getClass() == MqttSecurityException.class)
        {
          if (((MqttSecurityException)e).getReasonCode() == 5)
          {
            FuckyLogger.d("MQTT connect returned error code 5, clearing the device credentials");
            

          //  PushyAuthentication.clearDeviceCredentials(FuckySocketService.this);
            

            stopSelf();
            

            return Integer.valueOf(0);
          }
        }
        

        if (FuckyNetworking.isNetworkAvailable(mConnectivityManager))
        {
          scheduleReconnect();
        }
      }
      finally
      {
        mSocket.setConnecting(false);
      }
      

      return Integer.valueOf(0);
    }
  }
  
  public class SendKeepAliveAsync extends AsyncTask<Integer, String, Integer> {
    public SendKeepAliveAsync() {}
    
    protected Integer doInBackground(Integer... parameter) {
      if (!mSocket.isConnected()) {
        return Integer.valueOf(0);
      }
      

      FuckyLogger.d("Sending keep alive");
      savetoDb("Sending keep alive",true);


      try
      {
        mSocket.sendKeepAlive();
      }
      catch (Exception e) {
        FuckyLogger.d("Keep alive error: " + e.toString(), e);
        savetoDb("Keep alive error: " + e.toString(),true);

        mSocket.disconnectExistingClient();
        

        FuckySocketService.this.reconnectAsync();
      }
      

      return Integer.valueOf(0);
    }
  }

  public void savetoDb(final String Message, final boolean boool) {


    class GetTasks extends AsyncTask<Void, Void, Void> {

      @Override
      protected Void doInBackground(Void... voids) {
        FuckyRepository fuckyRepository = new FuckyRepository(AppDatabase.getDatabase(getApplicationContext()));
        DateFormat dateFormat = new SimpleDateFormat("hh:mm:ss a");
        String currentDateandTime = dateFormat.format(new Date());
        fuckyRepository.insert(new FuckyMessage(currentDateandTime, "FuckyJobService ..", Message, boool));
        return  null;
      }

      @Override
      protected void onPostExecute(Void voi) {
        super.onPostExecute(voi);

      }
    }

    GetTasks gt = new GetTasks();
    gt.execute();



  }
}
