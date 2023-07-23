package com.taxi.easy.ua.ui.home;

import java.util.Timer;
import java.util.TimerTask;

public class TimeOutTask extends TimerTask {
    private Thread thread;
    public Timer timer;

    public TimeOutTask(Thread thread, Timer timer) {
        this.thread = thread;
        this.timer = timer;
    }

    @Override
    public void run() {
        if(thread != null && thread.isAlive()) {
            thread.interrupt();
            timer.cancel();
        }
    }
}
