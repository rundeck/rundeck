package com.dtolabs.rundeck.plugin.example;

import com.dtolabs.rundeck.core.logging.LogFileState;
import com.dtolabs.rundeck.core.logging.LogFileStorage;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.logging.LogFileStoragePlugin;
import com.dtolabs.utils.Streams;

import java.io.*;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Example plugin which copies log files to another directory
 */
@Plugin(service = "LogFileStorage", name = "example")
@PluginDescription(title = "Example Log File Storage Plugin", description = "An example Plugin for Log File Storage")
public class ExampleLogFileStoragePlugin implements LogFileStoragePlugin {
    static final Logger log = Logger.getLogger(ExampleLogFileStoragePlugin.class.getName());
    private Map<String, ? extends Object> context;
    @PluginProperty(required = true)
    private String destinationDirPath;

    public ExampleLogFileStoragePlugin() {
        this.destinationDirPath = "/tmp/example";
    }

    public void storeLogFile(InputStream stream) throws IOException {
        File storeFile = getDestinationFile();
        File tempFile = getDestinationTempFile();
        if (!storeFile.getParentFile().isDirectory() && !storeFile.getParentFile().mkdirs()) {
            log.log(Level.SEVERE, "Failed creating dirs {0}", storeFile.getParentFile());
        }
        if (!tempFile.getParentFile().isDirectory() && !tempFile.getParentFile().mkdirs()) {
            log.log(Level.SEVERE, "Failed creating dirs {0}", storeFile.getParentFile());
        }
        //introduce delay
        try {
            Thread.sleep(10000L);
        } catch (InterruptedException e) {

        }
        tempFile.deleteOnExit();
        OutputStream os = new FileOutputStream(tempFile);
        try {
            Streams.copyStream(stream, os);
            tempFile.renameTo(storeFile);
        } finally {
            os.close();
        }
        log.log(Level.SEVERE, "Stored output to file {0}", storeFile);
    }

    public void retrieveLogFile(OutputStream stream) throws IOException {
        File getFile = getDestinationFile();
        InputStream is = new FileInputStream(getFile);
        //introduce delay
        try {
            Thread.sleep(10000L);
        } catch (InterruptedException e) {

        }

        try {
            Streams.copyStream(is, stream);
        } finally {
            is.close();
        }
        log.log(Level.SEVERE, "Retrieved output from file {0}", getFile);
    }

    public void initialize(Map<String, ? extends Object> context) {
        this.context = context;
    }

    private File getDestinationFile() {
        return new File(destinationDirPath, "output-log-" + getIdentity() + ".log");
    }

    private File getDestinationTempFile() {
        return new File(destinationDirPath, "output-log-" + getIdentity() + ".log.temp");
    }

    private String getIdentity() {
        return (String) context.get("execid");
    }

    public LogFileState getState() {
        //introduce delay
        try {
            Thread.sleep(500L);
        } catch (InterruptedException e) {

        }
        File storeFile = getDestinationFile();
        File tempFile = getDestinationTempFile();
        LogFileState state = (storeFile != null && storeFile.exists()) ?
                LogFileState.AVAILABLE :
                (tempFile != null && tempFile.exists()) ? LogFileState.PENDING : LogFileState.NOT_FOUND;

        log.log(Level.SEVERE, "call getState {0}", state);
        return state;
    }

}
