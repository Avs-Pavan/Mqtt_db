package com.kevin.mqtt;

import androidx.lifecycle.LiveData;


import com.kevin.mqtt.model.FuckyMessage;
import com.kevin.mqtt.model.db.FuckyRepository;

import java.util.List;

public class MainPresenter implements MainContrct.MainPresenter {



    private MainContrct.MainView familyMemberView;
    private FuckyRepository familyRepository;

    public MainPresenter(MainContrct.MainView familyMemberView, FuckyRepository familyRepository) {
        this.familyMemberView = familyMemberView;
        this.familyRepository = familyRepository;
    }


    @Override
    public void getFamily() {
          LiveData<List<FuckyMessage>> familyLiveData=familyRepository.getFamily();
          familyMemberView.onFamiliyDataLoaded(familyLiveData);
    }

}
