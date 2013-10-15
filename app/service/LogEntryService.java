package service;

import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.YEAR;
import static java.util.TimeZone.getTimeZone;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import model.LoadedDate;
import model.LogEntry;

import com.google.inject.Inject;

import dao.LogEntryDao;

public class LogEntryService {
    private final LogEntryDao logEntryDao;

    @Inject
    public LogEntryService(LogEntryDao logEntryDao) {
        System.out.println("coming in LogEntryService");
        this.logEntryDao = logEntryDao;
    }

    public void addLogEntriesToMongo(Date date, String source) {
        date = getUTCDateAtMidnight(date);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setTimeZone(getTimeZone("UTC"));
        String dateString = dateFormat.format(date);

        logEntryDao.addLoadedDate(date, source);
        List<LogsProcessingThread> threads = new ArrayList<LogsProcessingThread>();
        for (int i = 1; i <= 2; i++) {
            LogsProcessingThread thread = new LogsProcessingThread(dateString, i, logEntryDao);
            thread.start();
            threads.add(thread);
        }

        int entriesInserted = 0;
        for (LogsProcessingThread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                // Should not happen...
            }
            entriesInserted += thread.getEntriesInserted();
        }
        logEntryDao.setCompletedOnLoadedDate(date, new Date(), entriesInserted);
    }

    public boolean logsLoadedForDate(Date date) {
        LoadedDate loadedDate = logEntryDao.getLoadedDate(getUTCDateAtMidnight(date));
        return loadedDate != null && loadedDate.getCompleted() != null;
    }

    public boolean logsLoadingForDate(Date date) {
        LoadedDate loadedDate = logEntryDao.getLoadedDate(getUTCDateAtMidnight(date));
        return loadedDate != null && loadedDate.getCompleted() == null;
    }

    /*public List<LogEntry> getLogEntriesForDay(Date dateObj) {
        Calendar date = Calendar.getInstance();
        date.setTime(dateObj);

        Calendar start = Calendar.getInstance();
        start.set(date.get(YEAR), date.get(MONTH), date.get(DAY_OF_MONTH), 0, 0, 0);
        start.set(MILLISECOND, 0);

        Calendar end = Calendar.getInstance();
        end.set(date.get(YEAR), date.get(MONTH), date.get(DAY_OF_MONTH), 23, 59, 59);
        end.set(MILLISECOND, 999);

        return logEntryDao.getLogEntriesByDateRange(start.getTime(), end.getTime());
    }*/

    public List<LoadedDate> getCompletedLoadedDates() {
        return logEntryDao.getCompletedLoadedDates();
    }

    Date getUTCDateAtMidnight(Date dateObj) {
        Calendar date = Calendar.getInstance();
        date.setTime(dateObj);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(getTimeZone("UTC"));
        calendar.set(date.get(YEAR), date.get(MONTH), date.get(DAY_OF_MONTH), 0, 0, 0);
        calendar.set(MILLISECOND, 0);
        return calendar.getTime();
    }
}
