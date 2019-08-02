package com.kevin.mqtt;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.hypertrack.hyperlog.HyperLog;
import com.kevin.mqtt.Fucky.Fucky;
import com.kevin.mqtt.model.FuckyMessage;
import com.kevin.mqtt.model.db.FuckyRepository;
import com.kevin.mqtt.roomdb.AppDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements MainContrct.MainView {

    MainContrct.MainPresenter presenter;
    RecyclerView recyclerView;
    MyRecyclerViewAdapter adapter;
    List<FuckyMessage> fuckyMessages=new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = findViewById(R.id.rv);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        Fucky.listen(this);
        DoFuck gt = new DoFuck();
        gt.execute();


    }


    @Override
    public void onFamiliyDataLoaded(LiveData<List<FuckyMessage>> listLiveData) {
        listLiveData.observe(this, new Observer<List<FuckyMessage>>() {
            @Override
            public void onChanged(List<FuckyMessage> fuckyMessage) {
                Log.e("count observed", fuckyMessage.size() + "");
                 Collections.reverse(fuckyMessage);
                adapter = new MyRecyclerViewAdapter(MainActivity.this, fuckyMessage);
                recyclerView.setAdapter(adapter);

            }
        });
    }

    @Override
    public void onError(String msg) {

    }


    class DoFuck extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            presenter = new MainPresenter(MainActivity.this, new FuckyRepository(AppDatabase.getDatabase(MainActivity.this)));

            return null;
        }

        @Override
        protected void onPostExecute(Void voi) {
            super.onPostExecute(voi);
            presenter.getFamily();
        }
    }


    public class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyRecyclerViewAdapter.ViewHolder> {

        private List<FuckyMessage> mData;
        private LayoutInflater mInflater;


        // data is passed into the constructor
        MyRecyclerViewAdapter(Context context, List<FuckyMessage> data) {
            this.mInflater = LayoutInflater.from(context);
            this.mData = data;
        }

        // inflates the row layout from xml when needed
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.row, parent, false);
            return new ViewHolder(view);
        }

        // binds the data to the TextView in each row
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            String animal = mData.get(position).getMessage();
            String time = mData.get(position).getTime();
            holder.myTextView.setText(animal);
            holder.time.setText(time);
        }

        // total number of rows
        @Override
        public int getItemCount() {
            return mData.size();
        }


        // stores and recycles views as they are scrolled off screen
        public class ViewHolder extends RecyclerView.ViewHolder{
            TextView myTextView,time;


            ViewHolder(View itemView) {
                super(itemView);
                myTextView = itemView.findViewById(R.id.text);
                time = itemView.findViewById(R.id.time);
            }
        }


    }


}
