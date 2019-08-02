package com.kevin.mqtt.Fucky.services;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobInfo.Builder;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;

import com.kevin.mqtt.Fucky.config.FuckyConfig;
import com.kevin.mqtt.Fucky.util.FuckyLogger;
import com.kevin.mqtt.Fucky.util.FuckyMqttConnection;
import com.kevin.mqtt.Fucky.util.FuckyNetworking;
import com.kevin.mqtt.Fucky.util.FuckyPreferences;
import com.kevin.mqtt.Fucky.util.exceptions.FuckyFatalException;
import com.kevin.mqtt.model.FuckyMessage;
import com.kevin.mqtt.model.db.FuckyRepository;
import com.kevin.mqtt.roomdb.AppDatabase;

import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.kevin.mqtt.Fucky.util.exceptions.FuckyDateTime.getCurrentTimestamp;


@TargetApi(26)
public class FuckyJobService
  extends JobService
{
  private static WifiManager mWifiManager;
  private static ConnectivityManager mConnectivityManager;
  private JobParameters mParams;
  private static FuckyMqttConnection mSocket;
  private static long mLastKeepAlive;
  private static long mKeepAliveInterval;
  private static long mRetryInterval = 500L;
  
  public FuckyJobService() {}
  
  public boolean onStartJob(JobParameters params) {
    mParams = params;
    

    if (mWifiManager == null) {
      mWifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
    }
    

    if (mConnectivityManager == null) {
      mConnectivityManager = (ConnectivityManager)getApplicationContext().getSystemService(CONNECTIVITY_SERVICE);
    }
    

    if (mSocket == null) {
      mSocket = new FuckyMqttConnection(this, mWifiManager, mConnectivityManager, FuckyMqttConnection.getKeepAliveInterval(this), new ConnectionLostRunnable());
    }
    

    String command = params.getExtras().getString("command");
    

    if (command != null)
    {
      if (command.equals("stop"))
      {
        FuckyLogger.d("Stop requested");
        savetoDb("Stop requested..",true,getApplicationContext());
        

        mSocket.disconnectExistingClient();
        

        endJob();
        

        return false;
      }
    }
    

    if (!mSocket.isConnected())
    {
      connect();

    }
    else if ((mSocket.isConnected()) && (FuckyNetworking.getConnectedNetwork(mConnectivityManager) == 1) && (mSocket.getNetwork() == 0))
    {
      mSocket.disconnectExistingClient();
      

      connect();

    }
    else if (mLastKeepAlive + mKeepAliveInterval < getCurrentTimestamp() + FuckyConfig.MQTT_JOB_TASK_INTERVAL_PADDING)
    {
      sendKeepAlive();
    }
    else
    {
      scheduleJobAgain(getJobServiceInterval());
      return false;
    }
    

    return true;
  }
  
  void endJob()
  {
    jobFinished(mParams, false);
  }
  
  private void connect()
  {
    if (!FuckyNetworking.isNetworkAvailable(mConnectivityManager))
    {
      scheduleReconnect();
      return;
    }
    

    if (!FuckyPreferences.getBoolean("pushyNotificationsEnabled", true, this)) {
      FuckyLogger.d("Notifications have been disabled by the app");
      savetoDb("Notifications have been disabled by the app..",true,getApplicationContext());

      endJob();
      return;
    }
    

    if ((mSocket.isConnecting()) || (mSocket.isConnected())) {
      scheduleJobAgain(getJobServiceInterval());
      return;
    }
    

    new ConnectAsync().execute(new Integer[0]);
  }
  
  private int getJobServiceInterval()
  {
    return FuckyPreferences.getInt("pushyJobServiceInterval", FuckyConfig.MQTT_DEFAULT_JOB_SERVICE_INTERVAL, this) * 1000;
  }
  
  private void sendKeepAlive()
  {
    mLastKeepAlive = getCurrentTimestamp();
    

    if (mSocket.isConnected())
    {
      new SendKeepAliveAsync().execute(new Integer[0]);
    }
  }
  
  public class SendKeepAliveAsync extends AsyncTask<Integer, String, Integer> {
    public SendKeepAliveAsync() {}
    
    protected Integer doInBackground(Integer... parameter) {
      if (!FuckyJobService.mSocket.isConnected()) {
        FuckyJobService.this.connect();
        return 0;
      }
      

      FuckyLogger.d("FuckyJobService: Sending keep alive");
      savetoDb("FuckyJobService: Sending keep alive",true,getApplicationContext());


      try
      {
        FuckyJobService.mSocket.sendKeepAlive();
        

        scheduleJobAgain(FuckyJobService.this.getJobServiceInterval());
      }
      catch (Exception e) {
        FuckyLogger.d("Keep alive error: " + e.toString(), e);
        savetoDb("Keep alive error:",true,getApplicationContext());


        FuckyJobService.mSocket.disconnectExistingClient();
        

        FuckyJobService.this.connect();
      }
      

      return 0;
    }
  }
  
  public class ConnectAsync extends AsyncTask<Integer, String, Integer>
  {
    public ConnectAsync() {
      FuckyJobService.mSocket.setConnecting(true);
    }
    

    protected Integer doInBackground(Integer... parameter)
    {
      FuckyLogger.d("FuckyJobService: Connecting...");
      savetoDb("FuckyJobService: Connecting...",true,getApplicationContext());


      try
      {
        FuckyJobService.mSocket.connect();
        

        FuckyJobService.mLastKeepAlive= getCurrentTimestamp();
        

        FuckyJobService.mRetryInterval = 500L;
        //KevinsJobService.mSocket;
        FuckyJobService.mKeepAliveInterval = (long) FuckyMqttConnection.getKeepAliveInterval(FuckyJobService.this);


        FuckyLogger.d("Connected successfully (sending keep alive every " + FuckyMqttConnection.getKeepAliveInterval(FuckyJobService.this) + " seconds)");
        savetoDb("Connected successfully (sending keep alive every " + FuckyMqttConnection.getKeepAliveInterval(FuckyJobService.this) + " seconds)",true,getApplicationContext());


        scheduleJobAgain(FuckyJobService.this.getJobServiceInterval());
      }
      catch (Exception e)
      {
        FuckyLogger.d("Connect exception: " + e.toString());
        savetoDb("Connect exception: " + e.toString(),true,getApplicationContext());

        Integer localInteger;
        if (e.getClass() == FuckyFatalException.class)
        {
          FuckyLogger.d("Fatal error encountered, stopping service");
          savetoDb("Fatal error encountered, stopping service",true,getApplicationContext());



          endJob();
          

          return 0;
        }
        

        if (e.getClass() == MqttSecurityException.class)
        {
          if (((MqttSecurityException)e).getReasonCode() == 5)
          {
            FuckyLogger.d("MQTT connect returned error code 5, clearing the device credentials");
            savetoDb("MQTT connect returned error code 5, clearing the device credentials",true,getApplicationContext());


            // PushyAuthentication.clearDeviceCredentials(FuckyJobService.this);
            

            endJob();
            

            return Integer.valueOf(0);
          }
        }
        

        scheduleReconnect();
      }
      finally
      {
        FuckyJobService.mSocket.setConnecting(false);
      }
      

      return Integer.valueOf(0);
    }
  }
  
  public void scheduleReconnect()
  {
    if (mRetryInterval < 60000L) {
      mRetryInterval = Math.min(mRetryInterval * 2L, 60000L);
    }
    

    FuckyLogger.d("Reconnecting in " + mRetryInterval + "ms");
    savetoDb("Reconnecting in " + mRetryInterval + "ms",true,getApplicationContext());


    scheduleJobAgain(mRetryInterval);
  }
  
  public class ConnectionLostRunnable implements Runnable {
    public ConnectionLostRunnable() {}
    
    public void run() {
      FuckyJobService.this.connect();
    }
  }
  
  void scheduleJobAgain(long interval)
  {
    ComponentName serviceName = new ComponentName(getPackageName(), FuckyJobService.class.getName());

    JobInfo jobInfo = new Builder(FuckyConfig.MQTT_JOB_ID, serviceName).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setMinimumLatency(interval).setOverrideDeadline(interval).build();
    

    JobScheduler jobScheduler = (JobScheduler)getSystemService(JOB_SCHEDULER_SERVICE);
    

    jobScheduler.schedule(jobInfo);
    

    endJob();
  }
  

  public boolean onStopJob(JobParameters params)
  {
    return false;
  }


  public void savetoDb(final String Message, final boolean boool,Context context) {


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
