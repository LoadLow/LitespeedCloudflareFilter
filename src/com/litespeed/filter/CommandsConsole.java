package com.litespeed.filter;

import com.cloudflare.api.constants.IPAddrAction;
import com.cloudflare.api.requests.modify.ModifyIPList;
import com.cloudflare.api.results.CloudflareError;
import java.io.Console;

/**
 *
 * @author LoadLow
 */
public class CommandsConsole implements Runnable {

    private Thread _t;
    boolean running = false;

    public CommandsConsole() {
        this._t = new Thread(this);
        _t.setDaemon(true);
        running = true;
        _t.start();
    }

    public void stop() {
        running = false;
        _t.interrupt();
    }

    @Override
    public void run() {
        while (running) {
            Console console = System.console();
            try {
                if (console != null) {
                    String command = console.readLine();
                    evalCommand(command);
                }
            } catch (Exception e) {
            } finally {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public void evalCommand(String command) {
        String[] args = command.split(" ");
        String fct = args[0].toUpperCase();
        if (fct.equals("CF_IP_BAN")) {
            String ip = args[1];
            ModifyIPList request = new ModifyIPList(Kernel.CMD_CloudflareAccess, IPAddrAction.Blacklist, ip);
            try {
                if (request.execute()) {
                    Kernel.LiveStats.bannedAddresses.add(ip);
                    println("Success!");
                }
            } catch (CloudflareError e) {
                println("Result::ERROR -> " + e.getMessage());
            }
        } else if (fct.equals("CF_IP_UNLIST")) {
            String ip = args[1];
            ModifyIPList request = new ModifyIPList(Kernel.CMD_CloudflareAccess, IPAddrAction.Unlist, ip);
            try {
                if (request.execute()) {
                    println("Success!");
                }
            } catch (CloudflareError e) {
                println("Result::ERROR -> " + e.getMessage());
            }
        } else if (fct.equals("CF_IP_TRUST")) {
            String ip = args[1];
            ModifyIPList request = new ModifyIPList(Kernel.CMD_CloudflareAccess, IPAddrAction.Whitelist, ip);
            try {
                if (request.execute()) {
                    println("Success!");
                }
            } catch (CloudflareError e) {
                println("Result::ERROR -> " + e.getMessage());
            }
        } else if (fct.equals("HELP")) {
            println("=======");
            println("Available commands :");
            println("- CF_IP_BAN [ip]    => Ban an IP in CF");
            println("- CF_IP_TRUST [ip]  => Trust an IP in CF");
            println("- CF_IP_UNLIST [ip] => Take out an IP in CF");
        } else {
            println("Commande non reconnue ou incomplete.");
        }
    }

    public void println(String msg) {
        System.out.println(msg);
    }
}
