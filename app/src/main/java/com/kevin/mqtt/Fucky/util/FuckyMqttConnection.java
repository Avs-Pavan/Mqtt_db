package com.kevin.mqtt.Fucky.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import androidx.lifecycle.Observer;

import com.kevin.mqtt.Fucky.config.FuckyConfig;
import com.kevin.mqtt.model.FuckyMessage;
import com.kevin.mqtt.model.db.FuckyRepository;
import com.kevin.mqtt.roomdb.AppDatabase;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FuckyMqttConnection
        implements MqttCallback {
    private int mNetwork;
    private boolean mIsConnecting;
    private int mKeepAliveInterval;
    private Context mContext;
    private MqttClient mClient;
    private WifiManager mWifiManager;
    private Runnable mConnectionLostRunnable;
    private WifiLock mWifiWakeLock;
    private ConnectivityManager mConnectivityManager;

    public FuckyMqttConnection(Context context, WifiManager wifiManager, ConnectivityManager connectivityManager, int keepAliveInterval, Runnable connectionLostRunnable) {
        mContext = context;
        mWifiManager = wifiManager;
        mKeepAliveInterval = keepAliveInterval;
        mConnectivityManager = connectivityManager;
        mConnectionLostRunnable = connectionLostRunnable;
    }

    public static int getKeepAliveInterval(Context context) {
        return FuckyPreferences.getInt("pushyKeepAliveInterval", FuckyConfig.MQTT_DEFAULT_KEEP_ALIVE, context);
    }

    public void releaseWifiLock() {
        if (mWifiWakeLock == null) {
            return;
        }

        try {
            mWifiWakeLock.release();
        } catch (Exception exc) {
            FuckyLogger.d("Wi-Fi lock release failed");
            savetoDb("Wi-Fi lock release failed...",false);

        }


        mWifiWakeLock = null;


        FuckyLogger.d("Wi-Fi lock released");
        savetoDb("CWi-Fi lock released...",false);

    }

    public void acquireWifiLock() {
        if (mWifiWakeLock != null) {
            return;
        }


        if (FuckyPreferences.getBoolean("pushyWifiPolicyCompliance", false, mContext)) {
            if (Settings.System.getInt(mContext.getContentResolver(), "wifi_sleep_policy", 2) != 2) {
                FuckyLogger.d("Complying with device Wi-Fi sleep policy");
                savetoDb("Complying with device Wi-Fi sleep policy...",false);


                return;
            }
        }


        mWifiWakeLock = mWifiManager.createWifiLock(1, "Fucky");


        mWifiWakeLock.acquire();


        FuckyLogger.d("Wi-Fi lock acquired");
        savetoDb("Wi-Fi lock acquired ...",false);

    }

    public void connect() throws Exception {
        savetoDb("disconnectExistingClient ...",false);

        disconnectExistingClient();

        savetoDb("trying to connect again ...",false);

        String brokerEndpoint = FuckyConfig.DIRECT_ENDPOINT;


        SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd:HH:mm:ss");
        String currentDateandTime = sdf.format(new Date());
        mClient = new MqttClient(brokerEndpoint, currentDateandTime, new MemoryPersistence());


        mClient.setCallback(this);


        mClient.setTimeToWait(FuckyConfig.MQTT_ACK_TIMEOUT);


        mNetwork = FuckyNetworking.getConnectedNetwork(mConnectivityManager);


        MqttConnectOptions connectOptions = new MqttConnectOptions();


        //connectOptions.setUserName(credentials.token);
        // connectOptions.setPassword(credentials.authKey.toCharArray());


        connectOptions.setAutomaticReconnect(false);


        connectOptions.setCleanSession(false);


        connectOptions.setConnectionTimeout(FuckyConfig.MQTT_CONNECT_TIMEOUT);


        connectOptions.setKeepAliveInterval(mKeepAliveInterval);


        mClient.connect(connectOptions);

        subscribeToTopic("demo" + "/+");

    }

    private void subscribeToTopic(String topic) throws Exception {
        mClient.subscribe(topic, FuckyConfig.MQTT_QUALITY_OF_SERVICE);
        savetoDb("subscribe to topic ...",false);

    }

    private void unsubscribeToTopic(String topic) throws Exception {
        mClient.subscribe(topic, FuckyConfig.MQTT_QUALITY_OF_SERVICE);
    }

    private void publish(String topic, String payload) throws Exception {
        if ((mClient == null) || (!mClient.isConnected())) {
            throw new Exception("Publish failed: not connected");
        }


        mClient.publish(topic, payload.getBytes(), FuckyConfig.MQTT_QUALITY_OF_SERVICE, FuckyConfig.MQTT_RETAINED_PUBLISH);
    }

    public void sendKeepAlive() throws Exception {
        publish("keepalive", mClient.getClientId());
        savetoDb("publish keep alive...",false);
    }

    public void disconnectExistingClient() {
        if ((mClient == null) || (!mClient.isConnected())) {
            return;
        }

        try {
            mClient.disconnectForcibly(2000L, 2000L);


            mClient.close();
        } catch (MqttException localMqttException) {
        }
    }


    public boolean isConnected() {
        return (mClient != null) && (mClient.isConnected());
    }


    public void connectionLost(Throwable cause) {
        FuckyLogger.d("Connection lost");
        savetoDb("Connection lost",true);


        if (mConnectionLostRunnable != null) {
            mConnectionLostRunnable.run();
        }
    }

    public void messageArrived(String topic, MqttMessage message) {
        savetoDb("Message : " + message, false);
    }

    public void deliveryComplete(IMqttDeliveryToken token) {
    }


    public int getNetwork() {
        return mNetwork;
    }

    public boolean isConnecting() {
        return mIsConnecting;
    }

    public void setConnecting(boolean value) {
        mIsConnecting = value;
    }


    public void publish(String topic) {
        String msg = "";
        MqttMessage message = new MqttMessage(msg.getBytes());
        message.setQos(1);
        message.setRetained(true);

        try {
            mClient.publish(topic, message);

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }





    public void savetoDb(final String Message, final boolean boool) {


        class GetTasks extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... voids) {
                FuckyRepository fuckyRepository = new FuckyRepository(AppDatabase.getDatabase(mContext));
                DateFormat dateFormat = new SimpleDateFormat("hh:mm:ss a");
                String currentDateandTime = dateFormat.format(new Date());
                fuckyRepository.insert(new FuckyMessage(currentDateandTime, "FuckyMqtt Connection", Message, boool));
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
