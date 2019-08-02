package com.kevin.mqtt;

import androidx.lifecycle.LiveData;

import com.kevin.mqtt.model.FuckyMessage;

import java.util.List;

public interface MainContrct {

    interface  MainView
    {
        void onFamiliyDataLoaded(LiveData<List<FuckyMessage>> listLiveData);
        void onError(String msg);
    }
    interface MainPresenter
    {
        public void getFamily();

    }
}
