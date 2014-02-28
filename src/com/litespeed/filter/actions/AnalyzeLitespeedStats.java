package com.litespeed.filter.actions;

import com.cloudflare.api.constants.IPAddrAction;
import com.cloudflare.api.constants.SecurityLevel;
import com.cloudflare.api.requests.modify.ModifyIPList;
import com.cloudflare.api.requests.modify.ModifySecurityLevel;
import com.cloudflare.api.results.CloudflareError;
import com.litespeed.filter.Kernel;
import com.litespeed.stats.ConnectionsStats;
import com.litespeed.stats.Stats;
import com.litespeed.stats.StatsFile;
import java.io.FileNotFoundException;
import java.util.TimerTask;

/**
 *
 * @author LoadLow
 */
public class AnalyzeLitespeedStats extends TimerTask {

    @Override
    public synchronized void run() {
        Stats stats = null;
        for (StatsFile listedFile : Kernel.StatsFiles) {
            try {
                if (stats == null) {
                    stats = listedFile.loadAll();
                } else {
                    stats.merge(listedFile.loadAll());
                }
            } catch (FileNotFoundException e) {
                //Le fichier n'a pas ete trouve, litespeed a du le remplacer
            } catch (Exception e) {
                Kernel.LiveStats.errorsLog.logErr(e);
            }
        }
        if (stats == null) {
            Kernel.LiveStats.errorsLog.log("No rtreport file found.");
            return;
        }
        try {

            String[] file_blockedList = stats.bannedIPs.array;
            for (String ip : file_blockedList) {
                if (!Kernel.LiveStats.bannedAddresses.contains(ip)) {
                    ModifyIPList request = new ModifyIPList(Kernel.BAN_CloudflareAccess, IPAddrAction.Blacklist, ip);
                    try {
                        if (request.execute()) {
                            Kernel.LiveStats.floodLog.log(ip + ", BAN, " + Kernel.Config.TIME_ADDRESS_BANNED + "ms");
                            Kernel.LiveStats.bannedAddresses.add(ip);
                            Kernel.Processor.executeAfter(new UnbanAddress(ip), (Kernel.Config.TIME_ADDRESS_BANNED + (15 * 1000)));
                        }
                    } catch (CloudflareError error) {
                        Kernel.LiveStats.errorsLog.logErr(error);
                    }
                }
            }
            ConnectionsStats conStats = stats.connections;
            if (!Kernel.LiveStats.SecurityEnabled && !Kernel.LiveStats.checkConnectionsStats(conStats)) {
                StringBuilder enabled = new StringBuilder("{");
                for (String domain : Kernel.Config.CF_DOMAINS) {
                    ModifySecurityLevel request = new ModifySecurityLevel(Kernel.RSEC_CloudflareAccess, domain, SecurityLevel.ImUnderAttack);
                    try {
                        if (request.execute()) {
                            if (request.execute()) {
                                if (enabled.length() > 1) {
                                    enabled.append(", ");
                                }
                                enabled.append(domain);
                            }
                        }
                    } catch (CloudflareError error) {
                        Kernel.LiveStats.errorsLog.logErr(error);
                    }
                }
                enabled.append("}");
                Kernel.LiveStats.attackLog.log("SECURITY_ENABLED, " + conStats.PlainConnections + ", " + Kernel.Config.TIME_UPGRADE_SECURITY + "ms : " + enabled.toString());
                Kernel.LiveStats.SecurityEnabled = true;
                Kernel.Processor.executeAfter(new ResetSecurity(), (Kernel.Config.TIME_UPGRADE_SECURITY + (15 * 1000)));
            }
            Kernel.LiveStats.logStats(stats.toString());
        } catch (Exception e) {
            Kernel.LiveStats.errorsLog.logErr(e);
        }
    }
}
