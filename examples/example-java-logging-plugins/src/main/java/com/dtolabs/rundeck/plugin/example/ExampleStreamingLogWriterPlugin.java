package com.dtolabs.rundeck.plugin.example;

import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.logging.StreamingLogWriterPlugin;
import com.dtolabs.rundeck.core.logging.LogEvent;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.*;

@Plugin(service = ServiceNameConstants.StreamingLogWriter, name = "example")
@PluginDescription(title = "Example Streaming Log Writer Plugin", description = "An example Plugin for Streaming Log " +
        "Writer")
public class ExampleStreamingLogWriterPlugin implements StreamingLogWriterPlugin {
    @PluginProperty(title = "Port", description = "TCP Port to send data to", required = true, defaultValue = "9091")
    public int port = 9091;
    @PluginProperty(title = "Host", description = "Host address", required = true)
    private String host;
    private Socket socket;
    private Map<String, ? extends Object> context;
    private OutputStream socketStream;


    public ExampleStreamingLogWriterPlugin() {

    }

    public void initialize(Map<String, ? extends Object> context) {
        this.context = context;
    }

    /**
     * Open a stream, called before addEntry is called
     */
    public void openStream() throws IOException {
        socket = new Socket(host, port);
        socketStream = socket.getOutputStream();
        if (null != context.get("name") && null != context.get("id")) {
            Object group = context.get("group");
            String desc = (null != group ? group + "/" : "") + context.get("name");
            write("Job started: " + desc + ": " + context.get("id"));
        }
        write("Execution: " + context.get("execid"));
    }

    /**
     * Add a new event
     *
     * @param event
     */
    public void addEvent(LogEvent event) {
        try {
            write(getString(event));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getString(LogEvent event) {
        return (event.getEventType() != null ? (event.getEventType()+": ") : "")
                + " " + event.getDatetime()
                + " " + event.getLoglevel()
                + " " + event.getMessage()
                + " " + event.getMetadata()
                ;
    }

    private void write(String string) throws IOException {
        socketStream.write(string.getBytes("UTF-8"));
        socketStream.write("\n".getBytes("UTF-8"));
    }

    /**
     * Close the stream.
     */
    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
