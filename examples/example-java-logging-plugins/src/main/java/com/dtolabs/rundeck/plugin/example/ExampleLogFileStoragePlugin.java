package com.dtolabs.rundeck.plugin.example;

import com.dtolabs.rundeck.core.logging.LogFileState;
import com.dtolabs.rundeck.core.logging.LogFileStorage;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.logging.LogFileStoragePlugin;
import com.dtolabs.utils.Streams;

import java.io.*;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Example plugin which copies log files to another directory
 */
@Plugin(service = ServiceNameConstants.LogFileStorage, name = "example")
@PluginDescription(title = "Example Log File Storage Plugin", description = "An example Plugin for Log File Storage")
public class ExampleLogFileStoragePlugin implements LogFileStoragePlugin {
    static final Logger log = Logger.getLogger(ExampleLogFileStoragePlugin.class.getName());
    private Map<String, ? extends Object> context;
    @PluginProperty(required = true)
    private String destinationDirPath;

    public ExampleLogFileStoragePlugin() {
        this.destinationDirPath = "/tmp/rundeck_cluster";
    }

    public boolean store(InputStream stream, long length, Date lastModified) throws IOException {
        File storeFile = getDestinationFile();
        File tempFile = getDestinationTempFile();
        if (!storeFile.getParentFile().isDirectory() && !storeFile.getParentFile().mkdirs()) {
            log.log(Level.SEVERE, "Failed creating dirs {0}", storeFile.getParentFile());
        }
        if (!tempFile.getParentFile().isDirectory() && !tempFile.getParentFile().mkdirs()) {
            log.log(Level.SEVERE, "Failed creating dirs {0}", storeFile.getParentFile());
        }
        tempFile.deleteOnExit();
        OutputStream os = new FileOutputStream(tempFile);
        boolean finished=false;
        try {
            Streams.copyStream(stream, os);
            finished=true;
        } finally {
            os.close();
            if(!finished) {
                tempFile.delete();
            }
        }

        finished=tempFile.renameTo(storeFile);
        if(!finished){
            log.log(Level.SEVERE, "Failed to rename output to file {0}", storeFile);
            tempFile.delete();
        }
        return finished;
    }

    public boolean retrieve(OutputStream stream) throws IOException {
        File getFile = getDestinationFile();
        InputStream is = new FileInputStream(getFile);
        //introduce delay
        boolean finished = false;
        try {
            Streams.copyStream(is, stream);
            finished=true;
        } finally {
            is.close();
        }
        log.log(Level.INFO, "Retrieved output from file {0}", getFile);
        return finished;
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
        File storeFile = getDestinationFile();
        File tempFile = getDestinationTempFile();
        LogFileState state = (storeFile != null && storeFile.exists()) ?
                LogFileState.AVAILABLE :
                (tempFile != null && tempFile.exists()) ? LogFileState.PENDING : LogFileState.NOT_FOUND;

        log.log(Level.SEVERE, "call getState {0}", state);
        return state;
    }

}
