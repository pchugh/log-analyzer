package controllers;

import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Calendar;
import java.util.Date;

import play.mvc.Result;
import service.LogEntryService;
import views.html.load;
import com.google.inject.*;

public class Load extends Server_Log_Controller{
    private final LogEntryService logEntryService;

    @Inject
    public Load(LogEntryService logEntryService) {
        this.logEntryService = logEntryService;
    }

    public Result load(String dateString) {
        Date date = convertToDate(dateString);
        if (date == null) {
            return ok("Invalid date '" + dateString + "'. Dates must be in the format yyMMdd.");
        } else if (logEntryService.logsLoadedForDate(date)) {
            return ok("Logs for date '" + dateString + "' has already been loaded.");
        } else if (logEntryService.logsLoadingForDate(date)) {
            return ok("Logs for date '" + dateString + "' are currently being loaded.");
        } else if (dateIsTodayOrInTheFuture(date)) {
            return ok("Logs for today or future dates cannot be loaded.");
        }

        logEntryService.addLogEntriesToMongo(date, "Manual (" + request().remoteAddress() + ")");

        String returnUrl = request().getQueryString("returnUrl");
        return isBlank(returnUrl) ? ok(load.render()) : redirect("/log-analyzer" + returnUrl);
    }

    boolean dateIsTodayOrInTheFuture(Date date) {
        Calendar today = Calendar.getInstance();
        today.set(HOUR_OF_DAY, 0);
        today.set(MINUTE, 0);
        today.set(SECOND, 0);
        today.set(MILLISECOND, 0);
        return date.after(today.getTime()) || date.equals(today.getTime());
    }
}
