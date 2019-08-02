package com.kevin.mqtt.model.db;


import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.kevin.mqtt.model.FuckyMessage;
import com.kevin.mqtt.roomdb.DbConstants;

import java.util.List;


@Dao
public interface FuckyDao {

    @Query("SELECT * FROM " + DbConstants.Family_tbl + " ORDER BY id ASC")
    LiveData<List<FuckyMessage>> getMessages();

    @Query("SELECT * FROM " + DbConstants.Family_tbl)
    List<FuckyMessage> getMessages1();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insetMessage(FuckyMessage family);

}
