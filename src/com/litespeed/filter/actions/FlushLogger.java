package com.litespeed.filter.actions;

import com.litespeed.filter.Logger;
import java.util.TimerTask;

/**
 *
 * @author LoadLow
 */
public class FlushLogger extends TimerTask {

    private Logger logger;

    public FlushLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void run() {
        if (logger != null) {
            logger.flush();
        }
    }

    @Override
    public boolean cancel() {
        try {
            return super.cancel();
        } finally {
            if (logger != null) {
                logger.flush();
            }
            logger = null;
        }
    }
}
