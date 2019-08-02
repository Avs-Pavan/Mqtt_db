package com.kevin.mqtt.Fucky;

import android.content.Context;

import com.kevin.mqtt.Fucky.util.FuckyLogger;
import com.kevin.mqtt.Fucky.util.FuckyPreferences;
import com.kevin.mqtt.Fucky.util.FuckyServiceManager;
import com.kevin.mqtt.model.FuckyMessage;
import com.kevin.mqtt.model.db.FuckyRepository;
import com.kevin.mqtt.roomdb.AppDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;


public class Fucky {
    public Fucky() {
    }

    public static void listen(Context context) {
        FuckyServiceManager.start(context);
    }

    public  static void stop(Context context)
    {
        FuckyServiceManager.stop(context);
    }
   /* public static void subscribe(String[] topics, Context context) throws FuckyException {
        PushyPubSub.subscribe(topics, context);
    }*/

   /*
    public static void subscribe(String topic, Context context) throws FuckyException {
        subscribe(new String[]{topic}, context);


    public static void unsubscribe(String[] topics, Context context) throws FuckyException {
       // PushyPubSub.unsubscribe(topics, context);
    }
    public static void unsubscribe(String topic, Context context) throws FuckyException {
        unsubscribe(new String[]{topic}, context);
    }

}*/
    public static void togglePermissionVerification(boolean value, Context context) {
        FuckyPreferences.saveBoolean("pushyPermissionVerification", value, context);
    }

    public static void toggleDirectConnectivity(boolean value, Context context) {
        FuckyPreferences.saveBoolean("pushyDirectConnectivity", value, context);
    }

    public static void setProxyEndpoint(String value, Context context) {
        FuckyPreferences.saveString("pushyProxyEndpoint", value, context);
    }

    public static void toggleWifiPolicyCompliance(boolean value, Context context) {
        FuckyPreferences.saveBoolean("pushyWifiPolicyCompliance", value, context);
    }

    public static void toggleNotifications(boolean value, Context context) {
        FuckyPreferences.saveBoolean("pushyNotificationsEnabled", value, context);


        if (value) {
            FuckyServiceManager.start(context);
        } else {
            FuckyServiceManager.stop(context);
        }
    }

    public static void setHeartbeatInterval(int seconds, Context context) {
        if (seconds < 60) {
            seconds = 60;


            FuckyLogger.e("The minimum heartbeat interval is 60 seconds.");
        }


        FuckyPreferences.saveInt("pushyKeepAliveInterval", seconds, context);
    }

    public static void setJobServiceInterval(int seconds, Context context) {
        if (seconds < 5) {
            FuckyLogger.e("The minimum JobService interval is 5 seconds.");
            return;
        }


        FuckyPreferences.saveInt("pushyJobServiceInterval", seconds, context);
    }

    /*
    public static void setEnterpriseConfig(String apiEndpoint, String mqttEndpoint, Context context) {
        if ((apiEndpoint != null) && (apiEndpoint.endsWith("/"))) {
            apiEndpoint = apiEndpoint.substring(0, apiEndpoint.length() - 1);
        }


        if ((mqttEndpoint != null) && (mqttEndpoint.endsWith("/"))) {
            mqttEndpoint = mqttEndpoint.substring(0, mqttEndpoint.length() - 1);
        }


        if (!PushyStringUtils.equals(FuckyPreferences.getString("pushyEnterpriseMQTTEndpoint", null, context), mqttEndpoint)) {
            FuckyLogger.d("New enterprise MQTT endpoint, stopping socket service");


            FuckyServiceManager.stop(context);
        }


        FuckyPreferences.saveString("pushyEnterpriseAPIEndpoint", apiEndpoint, context);
        FuckyPreferences.saveString("pushyEnterpriseMQTTEndpoint", mqttEndpoint, context);
    }

    public static boolean isRegistered(Context context) {
        return PushyAuthentication.getDeviceCredentials(context) != null;
    }

    public static void unregister(Context context) {
        PushyAuthentication.clearDeviceCredentials(context);


        FuckyServiceManager.stop(context);
    }

     public static void setAppId(String appId, Context context) {
        String previousId = FuckyPreferences.getString("pushyAppId", "", context);


        if (!previousId.equals(appId)) {
            if (isRegistered(context)) {
                unregister(context);
            }
        }


        FuckyPreferences.saveString("pushyAppId", appId, context);
    }

  public static String register(Context context) throws FuckyException {
        PushyPermissionVerification.verifyManifestPermissions(context);


        PushyDeviceCredentials persistedCredentials = PushyAuthentication.getDeviceCredentials(context);

        if (persistedCredentials != null) {
            if (!PushyEnterprise.isConfigured(context)) {
                if (PushyStringUtils.stringIsNullOrEmpty(persistedCredentials.authKey)) {
                    try {
                        persistedCredentials.authKey = PushyAuthentication.obtainDeviceAuthKey(persistedCredentials.token, context);
                    } catch (FuckyFatalException localPushyFatalException) {
                    } catch (Exception exc) {
                        throw new PushyRegistrationException("Registration failed: " + exc.getMessage());
                    }
                }
            }


            if (PushyAuthentication.validateCredentials(persistedCredentials, context)) {
                listen(context);


                return persistedCredentials.token;
            }
        }


        String json;

        try {
            PushyRegistrationRequest register = new PushyRegistrationRequest();


            String appId = FuckyPreferences.getString("pushyAppId", null, context);


            if (appId != null) {
                appId = appId;
            } else {
                appId = context.getPackageName();
            }


            json = FuckySingleton.getJackson().writeValueAsString(register);
        } catch (Exception exc) {
            throw new PushyJsonParseException(exc.getMessage());
        }

        String register = PushyHTTP.post(PushyEndpoints.getAPIEndpoint(context) + "/register", json);


        PushyRegistrationResponse response;


        try {
            response = (PushyRegistrationResponse) FuckySingleton.getJackson().readValue(register, PushyRegistrationResponse.class);
        } catch (Exception exc) {
            throw new PushyJsonParseException(exc.getMessage());
        }

        if (!PushyStringUtils.stringIsNullOrEmpty(response.error)) {
            throw new PushyRegistrationException("Registration failed: " + response.error);
        }


        if ((response.token == null) || (response.auth == null)) {
            throw new PushyRegistrationException("Registration failed, please try again later.");
        }


        PushyAuthentication.saveDeviceCredentials(new PushyDeviceCredentials(response.token, response.auth), context);


        listen(context);


        return response.token;
    }

    public static PushyDeviceCredentials getDeviceCredentials(Context context) {
        return PushyAuthentication.getDeviceCredentials(context);
    }

    public static void setDeviceCredentials(PushyDeviceCredentials credentials, Context context) throws FuckyException {
        if ((PushyStringUtils.stringIsNullOrEmpty(credentials.token)) || (PushyStringUtils.stringIsNullOrEmpty(credentials.authKey))) {
            throw new FuckyException("Please provide both the device token and auth key.");
        }


        if (!PushyAuthentication.validateCredentials(credentials, context)) {
            throw new FuckyException("Authentication failed, please double-check the provided credentials.");
        }


        PushyAuthentication.saveDeviceCredentials(credentials, context);


        listen(context);
    }

    public static void setNotificationChannel(Object builder, Context context) {
        PushyNotifications.setNotificationChannel(builder, context);
    }*/
}
