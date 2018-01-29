/*
 * Licence is provided in the jar as license.yml also here:
 * https://github.com/Rsl1122/Plan-PlayerAnalytics/blob/master/Plan/src/main/resources/license.yml
 */
package com.djrapitops.plan.system.tasks;

import com.djrapitops.plan.Plan;
import com.djrapitops.plan.system.settings.Settings;
import com.djrapitops.plan.system.settings.locale.Locale;
import com.djrapitops.plan.system.settings.locale.Msg;
import com.djrapitops.plan.system.tasks.bukkit.*;
import com.djrapitops.plan.utilities.file.export.HtmlExport;
import com.djrapitops.plugin.api.Benchmark;
import com.djrapitops.plugin.api.TimeAmount;
import com.djrapitops.plugin.api.utility.log.Log;
import com.djrapitops.plugin.task.ITask;
import com.djrapitops.plugin.task.RunnableFactory;

/**
 * TaskSystem responsible for registering tasks for Bukkit.
 *
 * @author Rsl1122
 */
public class BukkitTaskSystem extends TaskSystem {

    private ITask bootAnalysisTask;

    private final Plan plugin;

    public BukkitTaskSystem(Plan plugin) {
        String serverName = plugin.getServer().getServerName();
        boolean isPaper = serverName.equals("Paper") || serverName.equals("TacoSpigot");
        tpsCountTimer = isPaper
                ? new PaperTPSCountTimer(plugin)
                : new BukkitTPSCountTimer(plugin);

        this.plugin = plugin;
    }

    @Override
    public void enable() {
        registerTasks();
    }

    private void registerTasks() {
        Benchmark.start("Task Registration");

        // Analysis refresh settings
        int analysisRefreshMinutes = Settings.ANALYSIS_AUTO_REFRESH.getNumber();
        boolean analysisRefreshTaskIsEnabled = analysisRefreshMinutes > 0;
        long analysisPeriod = analysisRefreshMinutes * TimeAmount.MINUTE.ticks();

        Log.info(Locale.get(Msg.ENABLE_BOOT_ANALYSIS_INFO).toString());

        registerTask(tpsCountTimer).runTaskTimer(1000, TimeAmount.SECOND.ticks());
        registerTask(new NetworkPageRefreshTask()).runTaskTimerAsynchronously(20L, 5L * TimeAmount.MINUTE.ticks());
        bootAnalysisTask = registerTask(new BootAnalysisTask()).runTaskLaterAsynchronously(30L * TimeAmount.SECOND.ticks());

        if (analysisRefreshTaskIsEnabled) {
            registerTask(new PeriodicAnalysisTask()).runTaskTimerAsynchronously(analysisPeriod, analysisPeriod);
        }
        if (Settings.ANALYSIS_EXPORT.isTrue()) {
            RunnableFactory.createNew(new HtmlExport(plugin)).runTaskAsynchronously();
        }
        Benchmark.stop("Enable", "Task Registration");
    }

    public void cancelBootAnalysis() {
        try {
            if (bootAnalysisTask != null) {
                bootAnalysisTask.cancel();
                bootAnalysisTask = null;
            }
        } catch (Exception ignored) {
            /* Ignored */
        }
    }
}