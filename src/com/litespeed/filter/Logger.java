package com.litespeed.filter;

import com.litespeed.filter.actions.FlushLogger;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang.exception.ExceptionUtils;

/**
 *
 * @author LoadLow
 */
public class Logger {

    public static String LINE_SEPARATOR = System.getProperty("line.separator");

    public static Calendar Calendar() {
        return Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris"));
    }
    private final static String LOGS_PATH = "logs";
    private StringBuilder cache;
    private BufferedWriter out;
    private FlushLogger task;
    private final Lock mySync = new ReentrantLock();
    /*
     * infos
     */
    private String logName;
    private String logDate;

    public Logger(String name) {
        this.logName = name;
        this.cache = new StringBuilder();
        setOut(getDate());
        this.task = new FlushLogger(this);
        Kernel.Processor.executeLoop(task, Kernel.Config.INTERVAL_FLUSH_LOGS);
    }

    public synchronized void close() {
        if (task != null) {
            task.cancel();
        }
        task = null;
        mySync.lock();
        try {
            if (out != null) {
                try {
                    if (cache != null && cache.length() > 0) {
                        out.write(cache.toString());
                        out.flush();
                    }
                    out.close();
                    cache = null;
                    out = null;
                } catch (IOException ex) {
                }
            }
        } finally {
            mySync.unlock();
        }
    }

    public Logger appendLine(String str) {
        return append(str + LINE_SEPARATOR);
    }

    public Logger append(String str) {
        if (cache == null) {
            cache = new StringBuilder();
        }
        cache.append(str);
        return this;
    }

    public Logger logErr(Throwable cause) {
        return log(ExceptionUtils.getStackTrace(cause));
    }

    public Logger log(String message) {
        String date = (Calendar().get(Calendar.HOUR_OF_DAY) < 10 ? ("0" + Calendar().get(Calendar.HOUR_OF_DAY)) : (Calendar().get(Calendar.HOUR_OF_DAY)))
                + ":"
                + (Calendar().get(+Calendar.MINUTE) < 10 ? ("0" + Calendar().get(+Calendar.MINUTE)) : (Calendar().get(+Calendar.MINUTE)))
                + ":"
                + (Calendar().get(Calendar.SECOND) < 10 ? ("0" + Calendar().get(Calendar.SECOND)) : (Calendar().get(Calendar.SECOND)));
        return appendLine("[" + date + "] " + message);
    }

    public void flush() {
        if (checkDate()) {
            if (cache != null && out != null && cache.length() > 0) {
                mySync.lock();
                try {
                    try {
                        out.write(cache.toString());
                        cache = new StringBuilder();
                        out.flush();
                    } catch (IOException ex) {
                    }
                } finally {
                    mySync.unlock();
                }
            }
        }
    }

    private boolean checkDate() {
        String newDate = getDate();
        if (!logDate.equals(newDate)) {
            mySync.lock();
            try {
                setOut(newDate);
                return false;
            } finally {
                mySync.unlock();
            }
        }
        return true;
    }

    private void setOut(String Date) {
        /*
         * Flush&Close
         */
        if (out != null) {
            try {
                if (cache != null && cache.length() > 0) {
                    out.write(cache.toString());
                    cache = new StringBuilder();
                    out.flush();
                }
                out.close();
            } catch (IOException ex) {
            }
        }
        /*
         * Set
         */
        logDate = Date;
        checkFolder(LOGS_PATH + "/" + logDate + "/");
        File fichier = new File(LOGS_PATH + "/" + logDate + "/" + logName);
        try {
            out = new BufferedWriter(new FileWriter(fichier, true));
        } catch (IOException e) {
        }
    }

    private static void checkFolder(String name) {
        File New = new File(LOGS_PATH);
        if (!New.exists()) {
            New.mkdir();
        }

        New = new File(name);
        if (!New.exists()) {
            New.mkdir();
        }
    }

    private static String getDate() {
        return Calendar().get(Calendar.DAY_OF_MONTH) + "-" + (Calendar().get(Calendar.MONTH) + 1) + "-" + Calendar().get(Calendar.YEAR);
    }
}
