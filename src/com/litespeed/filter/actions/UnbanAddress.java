package com.litespeed.filter.actions;

import com.cloudflare.api.constants.IPAddrAction;
import com.cloudflare.api.requests.modify.ModifyIPList;
import com.cloudflare.api.results.CloudflareError;
import com.litespeed.filter.Kernel;
import java.util.TimerTask;

/**
 *
 * @author LoadLow
 */
public class UnbanAddress extends TimerTask {

    private String address;

    public UnbanAddress(String address) {
        this.address = address;
    }

    @Override
    public synchronized void run() {
        try {
            if (Kernel.LiveStats.bannedAddresses.contains(address)) {
                ModifyIPList request = new ModifyIPList(Kernel.UNBAN_CloudflareAccess, IPAddrAction.Unlist, address);
                try {
                    if (request.execute()) {
                        Kernel.LiveStats.bannedAddresses.remove(address);
                        Kernel.LiveStats.floodLog.log(address + ", UNBAN");
                    }
                } catch (CloudflareError error) {
                    Kernel.LiveStats.errorsLog.logErr(error);
                }
            }
        } finally {
            this.cancel();
        }
    }

    @Override
    public boolean cancel() {
        try {
            return super.cancel();
        } finally {
            address = null;
        }
    }
}
