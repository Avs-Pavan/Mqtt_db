package com.kevin.mqtt.roomdb;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.kevin.mqtt.model.FuckyMessage;
import com.kevin.mqtt.model.db.FuckyDao;


@Database(entities = {FuckyMessage.class}, version =6,exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase mInstance;
    public abstract FuckyDao getFamilyDao();


    public static AppDatabase getDatabase(final Context context) {
        if (mInstance == null) {
            synchronized (AppDatabase.class) {
                if (mInstance == null) {
                    mInstance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, DbConstants.DB_NAME)
                            // Wipes and rebuilds instead of migrating
                            // if no Migration object.
                            // Migration is not part of this practical.
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return mInstance;
    }
}
