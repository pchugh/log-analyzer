package dao;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import model.LoadedDate;
import model.LogEntry;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
public class LogEntryDao {

    private static final String DB_NAME = "server_foxtrot_logs_viewer";

    private static final String LOADED_DATES_COLLECTION_NAME = "loaded_dates";
    private static final String LOADED_DATES_DATE_FIELD = "date";
    private static final String LOADED_DATES_SOURCE_FIELD = "source";
    private static final String LOADED_DATES_COMPLETED_FIELD = "completed";
    private static final String LOADED_DATES_ENTRIES_INSERTED_FIELD = "entries_inserted";

    private static final String LOG_ENTRIES_COLLECTION_NAME = "log_entries";
    private static final String LOG_ENTRIES_VERSION_NUMBER = "versionNumber";
    private static final String LOG_ENTRIES_DATE_TIME_FIELD = "date_time";
    private static final String LOG_ENTRIES_STATUS_FIELD = "status";
    private static final String LOG_ENTRIES_URL_FIELD = "url";
    private static final String LOG_ENTRIES_EXCEPTION_MESSAGE = "exception_message";
    private static final String LOG_ENTRIES_PACKAGE = "package_name";
    private static final String LOG_ENTRIES_CLASSNAME_LINENUMBER = "classname_linenumber";
    private static final String LOG_ENTRIES_NODE_FIELD = "node";

    private final MongoClient mongoClient;

    @Inject
    public LogEntryDao(MongoClient mongoClient) {
        System.out.println("Coming in LogEntryDAO");
        this.mongoClient = mongoClient;
        getLogEntriesCollection().ensureIndex(LOG_ENTRIES_DATE_TIME_FIELD);
        getLoadedDatesCollection().ensureIndex(new BasicDBObject(LOADED_DATES_DATE_FIELD, 1), null, true);
    }

    public void save(LogEntry logEntry) {
        DBCollection collection = getLogEntriesCollection();
        BasicDBObject entry = new BasicDBObject(LOG_ENTRIES_STATUS_FIELD, logEntry.getStatus())
                .append(LOG_ENTRIES_DATE_TIME_FIELD, logEntry.getDateTime()) //
                .append(LOG_ENTRIES_URL_FIELD, logEntry.getUrl()) //
                .append(LOG_ENTRIES_NODE_FIELD, logEntry.getNode())
                .append(LOG_ENTRIES_EXCEPTION_MESSAGE, logEntry.getException_message())
                .append(LOG_ENTRIES_CLASSNAME_LINENUMBER, logEntry.getClassname_linenumber()) 
                .append(LOG_ENTRIES_PACKAGE, logEntry.getDateTime()) 
                .append(LOG_ENTRIES_VERSION_NUMBER, logEntry.getVersion()); //
        collection.insert(entry);
    }

    public void addLoadedDate(Date loaded, String source) {
        BasicDBObject entry = new BasicDBObject(LOADED_DATES_DATE_FIELD, loaded) //
                .append(LOADED_DATES_SOURCE_FIELD, source) //
                .append(LOADED_DATES_COMPLETED_FIELD, null); //
        getLoadedDatesCollection().insert(entry);
    }

    public void setCompletedOnLoadedDate(Date date, Date completed, int entriesInserted) {
        LoadedDate loadedDate = getLoadedDate(date);
        if (loadedDate == null) {
            throw new IllegalStateException("Loaded date " + date + " cannot be found when setting completion field");
        }

        BasicDBObject replacement = new BasicDBObject(LOADED_DATES_DATE_FIELD, date) //
                .append(LOADED_DATES_COMPLETED_FIELD, completed) //
                .append(LOADED_DATES_SOURCE_FIELD, loadedDate.getSource()) //
                .append(LOADED_DATES_ENTRIES_INSERTED_FIELD, entriesInserted);
        getLoadedDatesCollection().update(new BasicDBObject(LOADED_DATES_DATE_FIELD, date), replacement);
    }

    public LoadedDate getLoadedDate(Date date) {
        DBObject dbObj = getLoadedDatesCollection().findOne(new BasicDBObject(LOADED_DATES_DATE_FIELD, date));
        return dbObj == null ? null : new LoadedDate((Date) dbObj.get(LOADED_DATES_DATE_FIELD),
                (String) dbObj.get(LOADED_DATES_SOURCE_FIELD), (Date) dbObj.get(LOADED_DATES_COMPLETED_FIELD),
                (Integer) dbObj.get(LOADED_DATES_ENTRIES_INSERTED_FIELD));
    }

    public List<LoadedDate> getCompletedLoadedDates() {
        BasicDBObject query = new BasicDBObject(LOADED_DATES_COMPLETED_FIELD, new BasicDBObject("$exists", true));
        DBCursor cursor = getLoadedDatesCollection().find(query);
        if (cursor.count() == 0) {
            return emptyList();
        }

        List<LoadedDate> loadedDates = new ArrayList<LoadedDate>(cursor.count());
        for (Iterator<DBObject> it = cursor.iterator(); it.hasNext();) {
            DBObject loadedDateObj = it.next();
            Date date = (Date) loadedDateObj.get(LOADED_DATES_DATE_FIELD);
            String source = (String) loadedDateObj.get(LOADED_DATES_SOURCE_FIELD);
            Date completed = (Date) loadedDateObj.get(LOADED_DATES_COMPLETED_FIELD);
            Integer entriesInserted = (Integer) loadedDateObj.get(LOADED_DATES_ENTRIES_INSERTED_FIELD);

            loadedDates.add(new LoadedDate(date, source, completed, entriesInserted));
        }
        return loadedDates;
    }

    DBCollection getLogEntriesCollection() {
        return mongoClient.getDB(DB_NAME).getCollection(LOG_ENTRIES_COLLECTION_NAME);
    }

    DBCollection getLoadedDatesCollection() {
        return mongoClient.getDB(DB_NAME).getCollection(LOADED_DATES_COLLECTION_NAME);
    }

}
