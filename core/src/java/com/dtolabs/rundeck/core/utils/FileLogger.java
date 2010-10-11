/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.dtolabs.rundeck.core.utils;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildLogger;

import java.io.*;

/**
 * FileLogger
 */
public class FileLogger implements BuildLogger {
    private boolean initialized;
    protected static final String LINESEP = System.getProperty("line.separator");

    private String targetIndent = "   ";
    private String taskIndent = targetIndent + targetIndent;
    private FileWriter writer;

    /**
     * Constructor
     *
     * @throws IOException
     */
    public FileLogger() throws IOException {
       this(null);
    }
    public FileLogger(File tmpDir) throws IOException {
        if (null == tmpDir) {
           logfile = File.createTempFile("FileLogger", ".txt");
        } else {
           logfile = File.createTempFile("FileLogger", ".txt", tmpDir);
        }
        initialize();
    }

    private void initialize() throws IOException {
        writer = new FileWriter(logfile, true);
        initialized = true;
    }


    private int msgOutputLevel;

    public void setMessageOutputLevel(final int level) {
        msgOutputLevel = level;
    }

    /**
     * <strong>Does nothing!</strong> Makes API happy
     *
     * @param b boolean ignored
     */
    public void setEmacsMode(final boolean b) {
    }

    /**
     * <strong>Does nothing!</strong>
     *
     * @param outputStream ignored
     */
    public void setOutputPrintStream(final PrintStream outputStream) {
    }


    /**
     * <strong>Does nothing!</strong> Makes API happy
     *
     * @param errorStream ignored
     */
    public void setErrorPrintStream(final PrintStream errorStream) {
    }


    /**
     * reference to logfile
     */
    private File logfile;

    /**
     * Gets logfile
     *
     * @return
     */
    public File getLogfile() {
        return logfile;
    }

    public void buildStarted(final BuildEvent e) {
        writeToFile(e);
    }

    public void buildFinished(final BuildEvent e) {
        writeToFile(e);
        try {
            writer.flush();
            writer.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }finally{
            initialized=false;
        }

    }

    public void targetStarted(final BuildEvent e) {
        final String msg = targetIndent + "<" + e.getTarget().getName() + "> ";
        writeToFile(msg);
    }

    public void targetFinished(final BuildEvent e) {
        final String msg = targetIndent + "</" + e.getTarget().getName() + "> ";
        writeToFile(msg);
    }

    public void taskStarted(final BuildEvent e) {
        final String msg = taskIndent + "<" + e.getTask().getTaskName() + "> ";
        writeToFile(msg);
    }

    public void taskFinished(final BuildEvent e) {
        final String msg = taskIndent + "</" + e.getTask().getTaskName() + "> ";

        writeToFile(msg);
    }

    public void messageLogged(final BuildEvent e) {
        writeToFile(e);
    }

    /**
     * Writes message to logfile
     *
     * @param msg message string
     */
    public void writeToFile(final String msg) {
        if (!initialized) {
            return;
        }

        if (msg == null || msg.length() == 0) {
            return;
        }

        try {
            writer.write(msg);
            writer.write(LINESEP);
        } catch (IOException ignore) {
        }
    }

    /**
     * Writes BuildEvent.getMesssage() content to logfile
     *
     * @param e
     */
    public void writeToFile(final BuildEvent e) {
        writeToFile(e.getMessage());
    }

    /**
     * Information useful for debugging
     *
     * @return
     */
    public String toString() {
        return "FileLogger{" +
                "logfile=" + logfile.getAbsolutePath() +
                "}";
    }
}
