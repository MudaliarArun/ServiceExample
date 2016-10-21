package com.safe.serviceexample.event;

import com.safe.serviceexample.util.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import io.paperdb.Paper;

/**
 * Created by gunhansancar on 06/04/16.
 */
public class SimpleEvent {
    private long date;
    private String id;
    private String timerText;
    private ArrayList<Long> mLaps;
    private long endTime = -1;
    public SimpleEvent() {
    }

    public SimpleEvent(String id) {
        this.id = id;
        this.date = Calendar.getInstance().getTimeInMillis();
        timerText = "";
        mLaps = new ArrayList<>();
    }

    public long getDate() {
        return date;
    }

    public String getId() {
        return id;
    }

    public String getTimerText() {
        return timerText;
    }

    public void setTimerText(String timerText) {
        this.timerText = timerText;
    }


    public void processText() {
        if(mLaps != null && mLaps.size() > 0){
            this.timerText = getHumanTimeFormatFromMilliseconds((Calendar.getInstance().getTimeInMillis() - mLaps.get(mLaps.size()-1)));
        }else {
            this.timerText = getHumanTimeFormatFromMilliseconds((Calendar.getInstance().getTimeInMillis() - this.date));
        }
    }
    public String getHumanTimeFormatFromMilliseconds(long milliseconds){
        String message = "";
        if (milliseconds >= 1000){
            int seconds = (int) (milliseconds / 1000) % 60;
            int minutes = (int) ((milliseconds / (1000 * 60)) % 60);
            int hours = (int) ((milliseconds / (1000 * 60 * 60)) % 24);
            int days = (int) (milliseconds / (1000 * 60 * 60 * 24));
            if((days == 0) && (hours != 0)){
                //message = String.format("%02d hours %02d minutes %02d seconds ago", hours, minutes, seconds);
                message = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            }else if((hours == 0) && (minutes != 0)){
                //message = String.format("%02d minutes %02d seconds ago", minutes, seconds);
                message = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            }else if((days == 0) && (hours == 0) && (minutes == 0)){
                message = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            }else{
                message = String.format("%02d day(s) %02d:%02d:%02d", days, hours, minutes, seconds);
            }
        } else{
            message = String.format("%02d:%02d:%02d", 0,0,0);
        }
        return message;
    }

    public SimpleEvent save() {
        synchronized (this) {
            LinkedHashMap<String, SimpleEvent> mEvents = Paper.book().read(Constants.DB_EVENTS, new LinkedHashMap<String, SimpleEvent>());
            mEvents.put(id, this);
            Paper.book().write(Constants.DB_EVENTS, mEvents);
        }
        return SimpleEvent.this;
    }
    public SimpleEvent addLap(long time){
        mLaps.add(time);
        return save();
    }

    public SimpleEvent endEvent() {
        endTime = Calendar.getInstance().getTimeInMillis();
        return save();
    }

    public static SimpleEvent readLastRunning() {
        LinkedHashMap<String, SimpleEvent> mEvents = Paper.book().read(Constants.DB_EVENTS, new LinkedHashMap<String, SimpleEvent>());
        for (Map.Entry<String,SimpleEvent> entry : mEvents.entrySet()) {
            // entry.getValue() is of type User now
            SimpleEvent event = entry.getValue();
            if(event.getEndTime() == -1){
                return event;
            }
        }
        return null;
    }

    public long getEndTime() {
        return endTime;
    }

    public ArrayList<Long> getmLaps() {
        return mLaps;
    }

    public static LinkedHashMap<String, SimpleEvent> endSkipped() {
        LinkedHashMap<String, SimpleEvent> mEvents = Paper.book().read(Constants.DB_EVENTS, new LinkedHashMap<String, SimpleEvent>());
        for (Map.Entry<String,SimpleEvent> entry : mEvents.entrySet()) {
            // entry.getValue() is of type User now
            SimpleEvent event = entry.getValue();
            if(event.getEndTime() == -1){
                event.setEndTime();
                mEvents.put(event.getId(),event);
            }
        }
        Paper.book().write(Constants.DB_EVENTS, mEvents);
        return mEvents;
    }

    private void setEndTime() {
        if(mLaps != null && mLaps.size() > 0){
            endTime = mLaps.get(mLaps.size()-1);
        }else{
            endTime = date;
        }
    }

}
