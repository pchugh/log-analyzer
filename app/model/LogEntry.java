package model;

import java.util.Date;

public class LogEntry {
    private int status;
    private Date dateTime;
    private String url;
    private int node;
    private String package_name;
    private String version;
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPackage_name() {
        return package_name;
    }

    public void setPackage_name(String package_name) {
        this.package_name = package_name;
    }

    public String getClassname_linenumber() {
        return classname_linenumber;
    }

    public void setClassname_linenumber(String classname_linenumber) {
        this.classname_linenumber = classname_linenumber;
    }

    public String getException_message() {
        return exception_message;
    }

    public void setException_message(String exception_message) {
        this.exception_message = exception_message;
    }

    private String classname_linenumber;
    private String exception_message;
    

    public LogEntry(int status, Date dateTime, String url, int node,String package_name,
            String classname_linenumber,String exception_message,String version) {
        this.status = status;
        this.dateTime = dateTime;
        this.url = url;
        this.node = node;
        this.package_name = package_name;
        this.exception_message = exception_message;
        this.classname_linenumber = classname_linenumber;
        this.version = version;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Date getDateTime() {
        return dateTime;
    }

    public void setDateTime(Date dateTime) {
        this.dateTime = dateTime;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getNode() {
        return node;
    }

    public void setNode(int node) {
        this.node = node;
    }
}
