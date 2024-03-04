package com.taxi.easy.ua.utils.activ_push;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PushAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, MyService.class));
    }

}
