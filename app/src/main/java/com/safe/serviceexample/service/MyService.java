package com.safe.serviceexample.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.safe.serviceexample.event.NotificationContent;
import com.safe.serviceexample.event.SimpleEvent;
import com.safe.serviceexample.util.Constants;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import io.paperdb.Paper;

public class MyService extends Service {

    private Timer timer;
    private AtomicInteger counter = new AtomicInteger();
    private LinkedHashMap<String,SimpleEvent> mEvents = new LinkedHashMap<>();
    private ArrayList<String> mRegIds = new ArrayList<>();
    public final static long REFRESH = -2;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null){
            String key = intent.getStringExtra(Constants.EVENT_TYPE);
            if(key != null){
                long time = intent.getLongExtra(Constants.EVENT_START_TIME, Calendar.getInstance().getTimeInMillis());
                processEventKey(key,time);
            }
        }else{
            startTimer(REFRESH);
        }



        return START_STICKY_COMPATIBILITY;
    }

    private void processEventKey(String key, long time) {
        switch (key){
            case Constants.REFRESH:
                startTimer(time);
                break;
            case Constants.NEW_EVENT:
                mEvents = SimpleEvent.endSkipped();
                startTimer(time);
                break;
            case Constants.ADD_LAP:
                addLap(time);
                break;
            case Constants.END_EVENT:
                /*if(mRegIds.contains(time)){
                    mRegIds.remove(time+"");
                }*/

                for (int i = 0; i < mRegIds.size(); i++) {
                    mEvents.put(mRegIds.get(i),endEvent(Long.parseLong(mRegIds.get(i))));
                }
                mRegIds.clear();
                mEvents = SimpleEvent.endSkipped();
                stopTimer();
                stopSelf();
                break;
            default:
                startTimer(Calendar.getInstance().getTimeInMillis());
                break;
        }
    }

    private SimpleEvent endEvent(long time) {
        SimpleEvent event = mEvents.get(time+"");
        if(event != null){
            event.endEvent();
        }
        stopTimer();
        return event;
    }

    private void addLap(long time) {
        SimpleEvent event = SimpleEvent.readLastRunning();
        if(event != null){
            mEvents.put(event.getId(),event.addLap(time));
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Paper.init(this);
        EventBus.getDefault().register(this);

    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        stopTimer();
        super.onDestroy();
    }

    @Subscribe
    public void onEvent(NotificationContent event) {
        if(event != null){
            long time = -1;
            if(event.getmObjects() != null && event.getmObjects().length > 0 &&
                    event.getmObjects()[0] != null){
                try {
                    time = Long.parseLong(String.valueOf(event.getmObjects()[0]));
                }catch (NumberFormatException e){

                }

            }
            processEventKey(event.getKey(),time);
        }

    }

   /* @Subscribe
    public void onEvent(NotificationContent mNotificationContent) {
        if(mNotificationContent !=null){
            switch (mNotificationContent.getKey()){
                case NotificationKey.UPDATE:
                    break;
            }
        }
    }*/

    private void startTimer(long time) {
        stopTimer();
        loadIds(time);
        SimpleEvent mSimpleEvent = SimpleEvent.readLastRunning();
        if(mSimpleEvent != null){
            if(!mRegIds.contains(mSimpleEvent.getId()+""))
                mRegIds.add(mSimpleEvent.getId()+"");
        }else if(time != -1 && time != REFRESH){
            if(!mRegIds.contains(time))
                mRegIds.add(time+"");
        }
        timer = new Timer();
        timer.scheduleAtFixedRate(new SimpleTimerTask(mEvents,mRegIds), 0, 1000);
    }
    private void refreshTimer(long time){
        stopTimer();
        loadIds(time);
        SimpleEvent mSimpleEvent = SimpleEvent.readLastRunning();
        if(mSimpleEvent != null){
            mRegIds.add(mSimpleEvent.getId()+"");
        }
        timer = new Timer();
        timer.scheduleAtFixedRate(new SimpleTimerTask(mEvents,mRegIds), 0, 1000);
    }

    private void loadIds(long time) {
        mEvents = Paper.book().read(Constants.DB_EVENTS,new LinkedHashMap<String, SimpleEvent>());
        if(mEvents.size() == 0 && time != REFRESH){
            SimpleEvent event = new SimpleEvent(time != -1 ? time+"" : Calendar.getInstance().getTimeInMillis()+"");
            event.save();
            mEvents.put(event.getId(),event);
        }else if(time != REFRESH){
            SimpleEvent event = new SimpleEvent(time != -1 ? time+"" : Calendar.getInstance().getTimeInMillis()+"");
            event.save();
            mEvents.put(event.getId(),event);
        }
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private static class SimpleTimerTask extends TimerTask {

        private LinkedHashMap<String, SimpleEvent> mEvents;
        private ArrayList<String> mRegIds;
        public SimpleTimerTask(LinkedHashMap<String, SimpleEvent> mEvents,ArrayList<String> mRegIds) {
            this.mEvents = mEvents;
            this.mRegIds =mRegIds;
        }

        @Override
        public void run() {
            if(EventBus.getDefault().hasSubscriberForEvent(SimpleEvent.class)) {
                for (String key:mRegIds) {
                    SimpleEvent event = mEvents.get(key);
                    if(event != null) {
                        event.processText();
                        EventBus.getDefault().postSticky(event);
                    }
                }
                /*for (Map.Entry<String,SimpleEvent> entry : mEvents.entrySet()) {
                    // entry.getValue() is of type User now
                    SimpleEvent event = entry.getValue();
                    event.processText(DATE_FORMAT);
                    EventBus.getDefault().postSticky(event);
                }*/
            }
        }
    }
}
