package com.litespeed.filter.actions;

import com.cloudflare.api.constants.SecurityLevel;
import com.cloudflare.api.requests.modify.ModifySecurityLevel;
import com.cloudflare.api.results.CloudflareError;
import com.litespeed.filter.Kernel;
import java.util.TimerTask;

/**
 *
 * @author LoadLow
 */
public class ResetSecurity extends TimerTask {

    @Override
    public synchronized void run() {
        StringBuilder disabled = new StringBuilder("{");
        for (String domain : Kernel.Config.CF_DOMAINS) {
            ModifySecurityLevel request = new ModifySecurityLevel(Kernel.RSEC_CloudflareAccess, domain, SecurityLevel.Medium);
            try {
                if (request.execute()) {
                    if (disabled.length() > 1) {
                        disabled.append(", ");
                    }
                    disabled.append(domain);
                }
            } catch (CloudflareError error) {
                Kernel.LiveStats.errorsLog.logErr(error);
            }
        }
        disabled.append("}");
        Kernel.LiveStats.attackLog.log("SECURITY_DISABLED : " + disabled.toString());
        Kernel.LiveStats.SecurityEnabled = false;
    }
}
