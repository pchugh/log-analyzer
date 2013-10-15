package model;

import java.util.Date;

public class LoadedDate {
    private Date date;
    private String source;
    private Date completed;
    private Integer entriesInserted;

    public LoadedDate(Date date, String source, Date completed, Integer entriesInserted) {
        this.date = date;
        this.source = source;
        this.completed = completed;
        this.entriesInserted = entriesInserted;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Date getCompleted() {
        return completed;
    }

    public void setCompleted(Date completed) {
        this.completed = completed;
    }

    public Integer getEntriesInserted() {
        return entriesInserted;
    }

    public void setEntriesInserted(Integer entriesInserted) {
        this.entriesInserted = entriesInserted;
    }
}
