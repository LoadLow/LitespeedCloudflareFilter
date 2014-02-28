package com.litespeed.filter;

import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author LoadLow
 */
public class Processor {

    private Timer temporizer;

    public Processor() {
        this.temporizer = new Timer("Processor");
    }

    public void executeAfter(TimerTask task, long endCallBack) {
        temporizer.schedule(task, endCallBack);
    }

    public void executeLoop(TimerTask task, long period) {
        temporizer.schedule(task, period, period);
    }

    public void stop() {
        temporizer.cancel();
    }
}
