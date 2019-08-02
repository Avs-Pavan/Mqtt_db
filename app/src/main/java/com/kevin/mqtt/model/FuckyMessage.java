package com.kevin.mqtt.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.kevin.mqtt.roomdb.DbConstants;


@Entity(tableName = DbConstants.Family_tbl)
public class FuckyMessage {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String time;
    public  String from;
    public String message;
    public boolean isError;

    public FuckyMessage(String time, String from, String message, boolean isError) {
        this.time = time;
        this.from = from;
        this.message = message;
        this.isError = isError;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isError() {
        return isError;
    }

    public void setError(boolean error) {
        isError = error;
    }

    @Override
    public String toString() {
        return message;
    }
}
