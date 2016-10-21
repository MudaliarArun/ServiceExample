package com.safe.serviceexample;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.safe.serviceexample.broadcast.AutoServiceWatch;
import com.safe.serviceexample.event.NotificationContent;
import com.safe.serviceexample.event.SimpleEvent;
import com.safe.serviceexample.service.MyService;
import com.safe.serviceexample.util.Constants;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Calendar;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @Bind(R.id.timer_tv)
    TextView timer_tv;
    private Handler mHandler;
    private boolean isPaused;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        refreshService();
    }

    private void refreshService() {
        Intent i = new Intent(this, MyService.class);
        i.putExtra(Constants.EVENT_TYPE, Constants.REFRESH);
        i.putExtra(Constants.EVENT_START_TIME, MyService.REFRESH);
        startService(i);
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @OnClick(R.id.startButton)
    public void onStartClicked() {
        Intent i = new Intent(this, MyService.class);
        i.putExtra(Constants.EVENT_TYPE, Constants.NEW_EVENT);
        i.putExtra(Constants.EVENT_START_TIME, Calendar.getInstance().getTimeInMillis());
        startService(i);
    }

    @OnClick(R.id.stopButton)
    public void onStopClicked() {
        EventBus.getDefault().post(new NotificationContent(Constants.END_EVENT));
    }

    @OnClick(R.id.lapButton)
    public void onLapClicked() {
        EventBus.getDefault().post(new NotificationContent(Constants.ADD_LAP, Calendar.getInstance().getTimeInMillis()));
    }

    @Subscribe(threadMode = ThreadMode.ASYNC, sticky = true)
    public void onEvent(final SimpleEvent event) {
        if (mHandler != null && !isPaused) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    //event.getTimerText()
                    timer_tv.setText(event.getTimerText());
                }
            });
        }
    }

    /*@Subscribe(threadMode = ThreadMode.ASYNC, sticky = true)
    public void onEvent(final NotificationContent event) {  // for extra messages
        if(mHandler != null && !isPaused) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {

                }
            });
        }
    }*/
    @Override
    protected void onResume() {
        isPaused = false;
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        isPaused = true;
        super.onPause();
        EventBus.getDefault().unregister(this);
    }
}
