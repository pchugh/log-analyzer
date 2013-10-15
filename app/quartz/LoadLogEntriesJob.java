package quartz;

import static java.util.Calendar.DAY_OF_YEAR;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;

import java.util.Calendar;
import java.util.Date;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import play.Logger;
import service.LogEntryService;

public class LoadLogEntriesJob implements Job {
    public static final String LOG_ENTRY_SERVICE_KEY = "logEntryService";

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Calendar calendar = Calendar.getInstance();
        calendar.add(DAY_OF_YEAR, -1);
        calendar.set(HOUR_OF_DAY, 0);
        calendar.set(MINUTE, 0);
        calendar.set(SECOND, 0);
        calendar.set(MILLISECOND, 0);
        Date yesterday = calendar.getTime();

        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        LogEntryService logEntryService = (LogEntryService) jobDataMap.get(LOG_ENTRY_SERVICE_KEY);
        if (logEntryService.logsLoadedForDate(yesterday) || logEntryService.logsLoadingForDate(yesterday)) {
            Logger.info("Skipping scheduled job. Log entries already loaded for '" + yesterday + "'");
            return;
        }
        logEntryService.addLogEntriesToMongo(yesterday, "Scheduled Job");
    }
}
