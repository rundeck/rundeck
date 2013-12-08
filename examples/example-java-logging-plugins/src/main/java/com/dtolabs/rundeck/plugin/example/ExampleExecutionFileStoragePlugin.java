package com.dtolabs.rundeck.plugin.example;

import com.dtolabs.rundeck.core.logging.ExecutionFileStorageException;
import com.dtolabs.rundeck.core.logging.LogFileStorageException;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.logging.ExecutionFileStoragePlugin;
import com.dtolabs.utils.Streams;

import java.io.*;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Example plugin which copies log and state files to another directory
 */
@Plugin(service = ServiceNameConstants.ExecutionFileStorage, name = "example")
@PluginDescription(title = "Example Execution File Storage", description = "An example Plugin for Execution File Storage")
public class ExampleExecutionFileStoragePlugin implements ExecutionFileStoragePlugin {
    static final Logger log = Logger.getLogger(ExampleExecutionFileStoragePlugin.class.getName());
    private Map<String, ? extends Object> context;
    @PluginProperty(required = true, defaultValue = "/tmp/rundeck_cluster")
    private String destinationDirPath;

    public ExampleExecutionFileStoragePlugin() {
    }

    public boolean store(String filetype, InputStream stream, long length, Date lastModified) throws IOException,
            ExecutionFileStorageException {
        File storeFile = getDestinationFile(filetype);
        File tempFile = getDestinationTempFile(filetype);
        if (!storeFile.getParentFile().isDirectory() && !storeFile.getParentFile().mkdirs()) {
            throw new ExecutionFileStorageException(String.format("Failed creating dirs %s", storeFile.getParentFile()));
        }
        if (!tempFile.getParentFile().isDirectory() && !tempFile.getParentFile().mkdirs()) {
            throw new ExecutionFileStorageException(String.format("Failed creating dirs %s", storeFile.getParentFile()));
        }

        tempFile.deleteOnExit();
        OutputStream os = new FileOutputStream(tempFile);
        boolean finished = false;
        try {
            Streams.copyStream(stream, os);
            finished = true;
        } finally {
            os.close();
            if (!finished) {
                tempFile.delete();
            }
        }

        finished = tempFile.renameTo(storeFile);
        if (!finished) {
            tempFile.delete();
            throw new ExecutionFileStorageException(String.format("Failed to rename output to file %s ", storeFile));
        }
        return finished;
    }

    public boolean retrieve(String filetype, OutputStream stream) throws IOException {
        File getFile = getDestinationFile(filetype);
        InputStream is = new FileInputStream(getFile);
        //introduce delay
        boolean finished = false;
        try {
            Streams.copyStream(is, stream);
            finished = true;
        } finally {
            is.close();
        }
        log.log(Level.INFO, "Retrieved output from file {0}", getFile);
        return finished;
    }

    public void initialize(Map<String, ? extends Object> context) {
        this.context = context;
    }

    private File getDestinationFile(String filetype) {
        return new File(destinationDirPath, "execution-" + getIdentity(filetype) );
    }

    private File getDestinationTempFile(String filetype) {
        return new File(destinationDirPath, "execution-" + getIdentity(filetype) + ".temp");
    }

    private String getIdentity(String filetype) {
        return (String) context.get("execid") + "." + filetype;
    }

    public boolean isAvailable(String filetype) {
        //introduce delay
        File storeFile = getDestinationFile(filetype);
        File tempFile = getDestinationTempFile(filetype);
        boolean available = (storeFile != null && storeFile.exists());

        log.log(Level.SEVERE, "call isAvailable {0}", available);
        return available;
    }

}
