import static com.google.inject.Guice.createInjector;
import static com.google.inject.Stage.PRODUCTION;
import static org.quartz.CronScheduleBuilder.dailyAtHourAndMinute;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;
import static quartz.LoadLogEntriesJob.LOG_ENTRY_SERVICE_KEY;

import java.net.UnknownHostException;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import play.Application;
import play.Configuration;
import play.GlobalSettings;
import play.Logger;
import play.Play;
import quartz.LoadLogEntriesJob;
import service.LogEntryService;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.mongodb.MongoClient;

import controllers.Load;
import controllers.Report;
import controllers.Stats;
import controllers.Summary;
import dao.LogEntryDao;

public class Global extends GlobalSettings {
    private Injector injector;

    @Override
    public void onStart(Application app) {
        injector = createInjector(PRODUCTION, new MongoModule(), new DaoModule(), new ServiceModule(),
                new ControllerModule(), new QuartzModule());

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(LOG_ENTRY_SERVICE_KEY, injector.getInstance(LogEntryService.class));
        JobDetail job = newJob(LoadLogEntriesJob.class) //
                .withIdentity("load-log-entries-job", "log-analyzer") //
                .usingJobData(jobDataMap) //
                .build();
        Trigger trigger = newTrigger() //
                .withIdentity("0300-daily-trigger", "log-analyzer") //
                .withSchedule(dailyAtHourAndMinute(3, 23)) //
                .forJob(new JobKey("load-log-entries-job", "log-analyzer")).build();
        try {
            injector.getInstance(Scheduler.class).scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            Logger.error("Failed to start quartz scheduler", e);
        }
    }

    @Override
    public void onStop(Application app) {
        MongoClient mongoClient = injector.getInstance(MongoClient.class);
        mongoClient.close();

        Scheduler scheduler = injector.getInstance(Scheduler.class);
        try {
            scheduler.shutdown();
        } catch (SchedulerException e) {
            Logger.error("Failed to shutdown quartz scheduler", e);
        }
    }

    @Override
    public <T> T getControllerInstance(Class<T> controllerClass) throws Exception {
        return injector.getInstance(controllerClass);
    }

    static class MongoModule extends AbstractModule {
        @Provides
        @Singleton
        public MongoClient getMongoClient() throws UnknownHostException {
            Configuration config = Play.application().configuration();
            return new MongoClient(config.getString("mongo.host"), config.getInt("mongo.port"));
        }

        @Override
        public void configure() {
        }
    }

    static class ControllerModule extends AbstractModule {
        @Override
        public void configure() {
            bind(Report.class).in(Singleton.class);
            bind(Load.class).in(Singleton.class);
            bind(Stats.class).in(Singleton.class);
            bind(Summary.class).in(Singleton.class);
        }
    }

    static class DaoModule extends AbstractModule {
        @Override
        public void configure() {
            bind(LogEntryDao.class).in(Singleton.class);
        }
    }

    static class ServiceModule extends AbstractModule {
        @Override
        public void configure() {
            bind(LogEntryService.class).in(Singleton.class);
        }
    }

    static class QuartzModule extends AbstractModule {
        @Provides
        @Singleton
        public Scheduler getScheduler() throws SchedulerException {
            Scheduler scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.start();
            return scheduler;
        }

        @Override
        public void configure() {
        }
    }
}
