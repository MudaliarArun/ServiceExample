package com.safe.serviceexample.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.safe.serviceexample.service.MyService;

public class AutoServiceWatch extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Intent myIntent = new Intent(context, MyService.class);
            context.startService(myIntent);
        }
    }
}