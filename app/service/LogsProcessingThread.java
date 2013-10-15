package service;

import static java.nio.charset.Charset.defaultCharset;
import static java.util.regex.Pattern.compile;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.getTempDirectory;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.http.util.EntityUtils.consumeQuietly;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.zip.GZIPInputStream;

import model.LogEntry;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.netty.util.internal.StringUtil;

import play.Logger;
import dao.LogEntryDao;

public class LogsProcessingThread extends Thread {
    private final String date;
    private final int node;
    private final LogEntryDao logEntryDao;
    private final DefaultHttpClient httpClient;

    private int entriesInserted;

    public LogsProcessingThread(String date, int node, LogEntryDao logEntryDao) {
        this.date = date;
        this.node = node;
        this.logEntryDao = logEntryDao;
        this.httpClient = new DefaultHttpClient();
        this.httpClient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY),
                new UsernamePasswordCredentials("npgloguser", "Ij98#r5o"));
    }

    @Override
    public void run() {
        File zippedLogFile = downloadZippedLogFile();
        if (zippedLogFile == null || !zippedLogFile.canRead()) {
            return;
        }

        try {
            Logger.info("Inserting log entries for node " + node);
            unzipLogFileAndInsertEntriesIntoMongo(zippedLogFile);
            Logger.info("Finished inserting log entries for node " + node);
        } catch (IOException e) {
            Logger.info("Error occured while inserting log entries for node " + node, e);
        } finally {
            deleteQuietly(zippedLogFile);
        }
    }

    public int getEntriesInserted() {
        return entriesInserted;
    }

    File downloadZippedLogFile() {
       // String url = "http://logs.nature.com/npgj2ee" + node + "/apache-nature.com/ex" + date + ".log.gz";
        String url = "http://logs.nature.com/npgj2ee" + node + "/jetty-salt-logs/"+"server_foxtrot.log."+date+".gz";
        HttpGet get = new HttpGet(url);
        HttpEntity entity = null;
        try {
            HttpResponse response = httpClient.execute(get);
            entity = response.getEntity();
            if (response.getStatusLine().getStatusCode() != 200) {
                Logger.error("File not found '" + url + "'");
            }

            StringBuilder message = new StringBuilder("Downloading file '").append(url).append("' File size: ");
            if (entity.getContentLength() > 0) {
                message.append(byteCountToDisplaySize(entity.getContentLength()));
            } else {
                message.append("Unknown");
            }
            Logger.info(message.toString());

            File zippedLogFile = new File(getTempDirectory(), "ex" + date + "_" + node + ".log.gz");
            copyInputStreamToFile(entity.getContent(), zippedLogFile);
            Logger.info("Finished downloading: '" + url + "'");

            return zippedLogFile;
        } catch (ClientProtocolException e) {
            Logger.error("Failed to download file '" + url + "'", e);
            return null;
        } catch (IOException e) {
            Logger.error("Error occured when downloading and copying file '" + url + "'", e);
            return null;
        } finally {
            if (entity != null) {
                consumeQuietly(entity);
            }
            httpClient.getConnectionManager().shutdown();
        }
    }

    void unzipLogFileAndInsertEntriesIntoMongo(File zippedLogFile) throws IOException {
        InputStream inputStream = null;
        BufferedReader reader = null;

        try {
            inputStream = new GZIPInputStream(new FileInputStream(zippedLogFile));
            reader = new BufferedReader(new InputStreamReader(inputStream, defaultCharset()));
            String logEntry = reader.readLine();
            while (logEntry != null) {
                processLogEntry(logEntry);
                logEntry = reader.readLine();
            }
        } catch (IOException e) {
            throw e;
        } finally {
            closeQuietly(inputStream);
            closeQuietly(reader);
        }
    }

    void processLogEntry(String logEntry) {
        System.out.println(logEntry);
        int status = getStatus(logEntry);
        String[] token = StringUtils.split(logEntry, " ");
        if(StringUtils.equals(token[2], "ERROR")){
            Date dateTime = getDateTime(token[0]);
            String url = token[5];
            String versionInfo = token[5];
            
            String excMessage  = StringUtils.substring(logEntry, StringUtils.indexOf(logEntry, token[8]));
            String packageName = token[4];
            String lineNumber = token[6];
            logEntryDao.save(new LogEntry(status, dateTime, url, node,packageName,lineNumber,excMessage,versionInfo));
            entriesInserted++;
        }
        
    }

    int getStatus(String line) {
        String[] token = StringUtils.split(line, " ");
        if(token[2] == " ERROR "){
            
        }
        String status = getStringMatchingPattern(line, "\" (\\d{3}) ");
        try {
            return Integer.valueOf(status);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    Date getDateTime(String line) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return format.parse(line);
        } catch (ParseException e) {
            return null;
        }
    }

    String getUrl(String line) {
        String urlLine = getStringMatchingPattern(line, "\\] \"(.*?)\"");
        // Annoyingly, the url line can be of 3 forms:
        // "<METHOD> <URL> HTTP/<VERSION>", "<METHOD> <URL>" or "<URL>". Try and strip out the <METHOD> and HTTP/<VERSION> if they exist.
        if (!urlLine.contains(" ")) {
            return urlLine;
        }
        urlLine = getStringMatchingPattern(urlLine, " (.*)");
        if (!urlLine.contains(" ")) {
            return urlLine;
        }
        return getStringMatchingPattern(urlLine, "(.*) ");
    }

    String getStringMatchingPattern(String line, String pattern) {
        Matcher matcher = compile(pattern).matcher(line);
        return matcher.find() ? matcher.group(1) : "";
    }
}
