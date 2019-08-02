package com.kevin.mqtt.model.db;


import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import com.kevin.mqtt.model.FuckyMessage;
import com.kevin.mqtt.roomdb.AppDatabase;

import java.util.List;

public class FuckyRepository  {
    private FuckyDao familyDao;
    private LiveData<List<FuckyMessage>> familyLiveData;
    private List<FuckyMessage> fuckyMessages;
     private AppDatabase appDatabase;

    public FuckyRepository(AppDatabase appDatabase) {
        this.appDatabase=appDatabase;
        familyDao = appDatabase.getFamilyDao();
        familyLiveData = familyDao.getMessages();
        fuckyMessages=familyDao.getMessages1();
    }

    public LiveData<List<FuckyMessage>> getFamily() {
        return familyLiveData;
    }
    public List<FuckyMessage> getmessages() {
        return fuckyMessages;
    }

    public void insert (FuckyMessage family) {
        new insertAsyncTask(familyDao).execute(family);
    }

    private static class insertAsyncTask extends AsyncTask<FuckyMessage, Void, Void> {

        private FuckyDao mAsyncTaskDao;

        insertAsyncTask(FuckyDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final FuckyMessage... params) {
            mAsyncTaskDao.insetMessage(params[0]);
            return null;
        }
    }
}
