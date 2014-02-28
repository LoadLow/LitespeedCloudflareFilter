package com.litespeed.filter;

import com.litespeed.stats.ConnectionsStats;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author LoadLow
 */
public class LiveStats {

    public final CopyOnWriteArrayList<String> bannedAddresses;
    public final Logger floodLog;
    public final Logger attackLog;
    public final Logger errorsLog;
    public final Logger statsLog;
    public volatile ConnectionsStats lastReport = null;
    public volatile long lastVerif = 0;
    public volatile boolean SecurityEnabled = false;
    public volatile long lastStatsLog = 0;

    public LiveStats() {
        bannedAddresses = new CopyOnWriteArrayList<String>();
        floodLog = new Logger("flood.log");
        attackLog = new Logger("attack.log");
        errorsLog = new Logger("errors.log");
        statsLog = new Logger("stats.log");
    }

    public boolean checkConnectionsStats(ConnectionsStats stats) {
        long actualTime = System.currentTimeMillis();
        if (lastReport != null && ((actualTime - lastVerif) >= Kernel.Config.INTERVAL_ANALYZE_REPORT) && lastReport.PlainConnections > 0) {
            float MOY = (lastReport.PlainConnections + stats.PlainConnections) / 2;
            if (MOY >= Kernel.Config.MINIMAL_AVERAGE_FOR_ATTACK) {
                double augmentationPercent = (MOY * 100) / lastReport.PlainConnections;
                if (augmentationPercent >= Kernel.Config.MINIMAL_AUGMENTATION_FOR_ATTACK) {
                    lastVerif = System.currentTimeMillis();
                    lastReport = stats;
                    return false;
                }
            }
            lastVerif = System.currentTimeMillis();
            lastReport = stats;
            return true;
        } else if (lastReport == null) {
            lastVerif = System.currentTimeMillis();
            lastReport = stats;
        }
        return true;
    }

    public void logStats(String stats) {
        if ((System.currentTimeMillis() - lastStatsLog) > Kernel.Config.INTERVAL_LOG_STATS) {
            statsLog.log(stats);
            lastStatsLog = System.currentTimeMillis();
        }
    }
}
