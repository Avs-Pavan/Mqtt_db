package com.kevin.mqtt.Fucky.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.kevin.mqtt.Fucky.util.FuckyLogger;
import com.kevin.mqtt.Fucky.util.FuckyServiceManager;
import com.kevin.mqtt.model.FuckyMessage;
import com.kevin.mqtt.model.db.FuckyRepository;
import com.kevin.mqtt.roomdb.AppDatabase;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class FuckyBootReceiver extends BroadcastReceiver {
    public FuckyBootReceiver() {
    }

    public void onReceive(Context context, Intent intent) {
        FuckyLogger.d("Device boot complete");
        savetoDb("Device boot complete", false, context);
        FuckyServiceManager.start(context);
    }

    public void savetoDb(String Message, boolean boool, Context mContext) {
        FuckyRepository fuckyRepository = new FuckyRepository(AppDatabase.getDatabase(mContext));
        DateFormat dateFormat = new SimpleDateFormat("hh:mm:ss a");
        String currentDateandTime = dateFormat.format(new Date());
        fuckyRepository.insert(new FuckyMessage(currentDateandTime, "FuckyBootReceiver ..", Message, boool));

    }

}
