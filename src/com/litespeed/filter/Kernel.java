package com.litespeed.filter;

import com.cloudflare.api.CloudflareAccess;
import com.litespeed.filter.actions.AnalyzeLitespeedStats;
import com.litespeed.stats.StatsFile;
import java.io.File;
import java.util.ArrayList;

/**
 *
 * @author LoadLow
 */
public class Kernel {

    public static Processor Processor;
    public static Processor AnalyzeProcessor;
    public static CloudflareAccess UNBAN_CloudflareAccess;
    public static CloudflareAccess RSEC_CloudflareAccess;
    public static CloudflareAccess BAN_CloudflareAccess;
    public static CloudflareAccess CMD_CloudflareAccess;
    public static AnalyzeLitespeedStats AnalyzeLitespeedStats;
    public static LiveStats LiveStats;
    public static Config Config;
    public static CommandsConsole CommandsConsole;
    public static ArrayList<StatsFile> StatsFiles = new ArrayList<StatsFile>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                exit();
            }
        });
    }

    public synchronized static void exit() {
        System.out.println("Stopping Filter...");

        if (CommandsConsole != null) {
            CommandsConsole.stop();
            CommandsConsole = null;
        }

        if (AnalyzeLitespeedStats != null) {
            System.out.println("... stopping Analyze Task");
            AnalyzeLitespeedStats.cancel();
            AnalyzeLitespeedStats = null;
        }

        if (AnalyzeProcessor != null) {
            System.out.println("... stopping Analyze Processor");
            AnalyzeProcessor.stop();
            AnalyzeProcessor = null;
        }

        if (Processor != null) {
            System.out.println("... stopping Processor");
            Processor.stop();
            Processor = null;
        }

        if (LiveStats != null) {
            System.out.println("... flushing/closing Logs");
            LiveStats.statsLog.close();
            LiveStats.floodLog.close();
            LiveStats.errorsLog.close();
            LiveStats.attackLog.close();
            LiveStats = null;
        }

        if (StatsFiles != null) {
            StatsFiles.clear();
            StatsFiles = null;
        }
        UNBAN_CloudflareAccess = RSEC_CloudflareAccess = BAN_CloudflareAccess = CMD_CloudflareAccess = null;
        Config = null;


        System.out.println("Filter stopped!");
    }

    public static void main(String[] args) {
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println("==========================================");
        System.out.println("LitespeedFilter using Cloudflare by LoadLow");
        System.out.println("==========================================");
        System.out.println("Filter starting...");
        System.out.println();

        System.out.print("Loading Config...");
        String configPath = "config.xml";
        if (args.length > 0) {
            configPath = args[0];
        }
        try {
            Config = new Config(configPath);
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
            try {
                Thread.sleep(4000);
            } catch (InterruptedException ex1) {
            }
            System.exit(0);
            return;
        }
        System.out.println("Ok!");

        System.out.print("Checking LS-Stats report files...");
        File rtreportFolder = new File(Config.LITESPEED_REPORT_FOLDER);
        if (!rtreportFolder.exists()) {
            System.out.println("Error: LS-Stats report folder not found.");
            try {
                Thread.sleep(4000);
            } catch (InterruptedException ex1) {
            }
            System.exit(0);
            return;
        } else {
            for (File file : rtreportFolder.listFiles()) {
                if (file.getName().startsWith(".rtreport")) {
                    StatsFiles.add(new StatsFile(file.getPath()));
                }
            }
        }
        if (StatsFiles.isEmpty()) {
            System.out.println("Error: no LS-Stats report file found.");
            try {
                Thread.sleep(4000);
            } catch (InterruptedException ex1) {
            }
            System.exit(0);
            return;
        } else {
            System.out.println("Ok: " + StatsFiles.size() + " files found.");
        }

        System.out.println("Starting Processors...");
        Processor = new Processor();
        AnalyzeProcessor = new Processor();

        System.out.println("Creating Stats&Logs...");
        UNBAN_CloudflareAccess = new CloudflareAccess(Config.CF_ACC_MAIL, Config.CF_ACC_KEY);
        RSEC_CloudflareAccess = new CloudflareAccess(Config.CF_ACC_MAIL, Config.CF_ACC_KEY);
        BAN_CloudflareAccess = new CloudflareAccess(Config.CF_ACC_MAIL, Config.CF_ACC_KEY);
        CMD_CloudflareAccess = new CloudflareAccess(Config.CF_ACC_MAIL, Config.CF_ACC_KEY);
        LiveStats = new LiveStats();

        System.out.println("Dispatching Analyzer...");
        AnalyzeLitespeedStats = new AnalyzeLitespeedStats();
        AnalyzeProcessor.executeLoop(AnalyzeLitespeedStats, Config.INTERVAL_ANALYZE_LITESPEED);

        System.out.println("Filter started!");
        System.out.println();
        System.out.println("Filtering...");

        CommandsConsole = new CommandsConsole();
    }
}
