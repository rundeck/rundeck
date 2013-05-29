package com.dtolabs.rundeck.plugin.example;

import com.dtolabs.rundeck.plugins.logging.StreamingLogWriterPlugin;
import com.dtolabs.rundeck.core.logging.LogEvent;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.*;

@Plugin(service = "StreamingLogWriter", name = "example")
@PluginDescription(title = "Example Streaming Log Writer Plugin", description = "An example Plugin for Streaming Log " +
        "Writer")
public class ExampleStreamingLogWriterPlugin implements StreamingLogWriterPlugin {
    public static int PORT = 9091;
    private Socket socket;
    private Map<String, ? extends Object> context;
    private OutputStream socketStream;

    public ExampleStreamingLogWriterPlugin() {

    }

    public void initialize(Map<String, ? extends Object> context) {
        this.context = context;
    }

    /** Open a stream, called before addEntry is called */
    public void openStream() throws IOException {
        socket = new Socket((String) null, PORT);
        socketStream = socket.getOutputStream();
        if (null != context.get("name") && null != context.get("id")) {
            Object group = context.get("group");
            String desc = (null != group ? group + "/" : "") + context.get("name");
            write("Job started: " + desc + ": " + context.get("id"));
        }
        write("Execution: " + context.get("execid"));
    }

    /**
     * Add a new entry
     *
     * @param entry
     */
    public void addEntry(LogEvent entry) {
        try {
            write(getString(entry));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getString(LogEvent entry) {
        return (entry.getEventType()!=null?entry.getEventType():"")
                +" "+entry.getDatetime()
                +" "+entry.getLoglevel()
                +" "+entry.getMessage()
                +" "+entry.getMetadata()
                ;
    }

    private void write(String string) throws IOException {
        socketStream.write(string.getBytes("UTF-8"));
        socketStream.write("\n".getBytes("UTF-8"));
    }

    /** Close the stream. */
    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
