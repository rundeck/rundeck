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

/*
* UpdateUtils.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Oct 14, 2010 12:15:28 PM
* 
*/
package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.common.impl.URLFileUpdater;
import com.dtolabs.rundeck.core.utils.FileUtils;
import com.dtolabs.utils.Streams;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * UpdateUtils provides {@link #updateFileFromUrl(String, String)} to GET a file from remote URL and store it to a file.
 * This utility will provide locks and synchronization to prevent two threads or jvms from overwriting the destination
 * file at the same time, and will use last modification time from the source URL to skip URL acquisition based on file
 * modification time.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class UpdateUtils {
    private static Logger logger = Logger.getLogger(UpdateUtils.class);

    /**
     * Get the source URL and store it to a destination file path
     *
     * @param sourceUrl url
     * @param destinationFilePath destination
     * @throws UpdateException on error
     */
    public static void updateFileFromUrl(final String sourceUrl, final String destinationFilePath) throws
        UpdateException {
        updateFileFromUrl(sourceUrl, destinationFilePath, null, null);
    }

    /**
     * Get the source File and store it to a destination file path
     *
     *
     * @param sourceFile source
     * @param destinationFilePath destination
     * @throws UpdateException on error
     */
    public static void updateFileFromFile(final File sourceFile, final String destinationFilePath) throws
        UpdateException {
        if (!sourceFile.exists()) {
            throw new UpdateException("Source file does not exist: " + sourceFile);
        }
        if (!sourceFile.isFile()) {
            throw new UpdateException("Not a file: " + sourceFile);
        }
        if (sourceFile.length() < 1) {
            throw new UpdateException("Source file is empty: " + sourceFile);
        }

        try {
            updateFileFromInputStream(new FileInputStream(sourceFile), destinationFilePath);
        } catch (IOException e) {
            throw new UpdateException("Unable to update file: " + e.getMessage(), e);
        }
    }

    /**
     * Get the source URL and store it to a destination file path
     *
     * @param input input stream
     * @param destinationFilePath destination
     * @throws UpdateException on error
     */
    public static void updateFileFromInputStream(final InputStream input, final String destinationFilePath) throws
        UpdateException {
        update(new FileStreamUpdater(input), new File(destinationFilePath));
    }

    /**
     * Rename the file. Handle possible OS specific issues
     */
    private static void moveFile(final File fromFile, final File toFile) throws UpdateException {
        if (!fromFile.renameTo(toFile)) {
            final String osName1 = System.getProperty("os.name");
            if (osName1.toLowerCase().indexOf("windows") > -1 && toFile.exists()) {
                //first remove the destFile
                if (!toFile.delete()) {
                    throw new UpdateException(
                        "Unable to remove dest file on windows: " + toFile);
                }
                if (!fromFile.renameTo(toFile)) {
                    throw new UpdateException(
                        "Unable to move temp file to dest file on windows: " + fromFile + ", "
                        + toFile);
                }
            } else {
                throw new UpdateException(
                    "Unable to move temp file to dest file: " + fromFile + ", " + toFile);
            }
        }
    }

    /**
     * Get the source URL and store it to a destination file path
     *
     *
     * @throws UpdateException on error
     * @param sourceUrl url
     * @param destinationFilePath destination
     * @param username username
     * @param password password
     */
    public static void updateFileFromUrl(final String sourceUrl, final String destinationFilePath,
                                         final String username, final String password) throws
        UpdateException {
        updateFileFromUrl(sourceUrl, destinationFilePath, username, password, URLFileUpdater.factory());
    }

    /**
     * Get the source URL and store it to a destination file path
     *
     * @param sourceUrl url
     * @param destinationFilePath destination
     * @param username username
     * @param password password
     * @param factory updater factory
     * @throws UpdateException on error
     */
    public static void updateFileFromUrl(final String sourceUrl, final String destinationFilePath,
                                         final String username, final String password,
                                         final URLFileUpdaterFactory factory) throws
        UpdateException {
        String tusername = username;
        String tpassword = password;
        URL url;
        try {
            url = new URL(sourceUrl);
            if (null == username && null == password && null != url.getUserInfo()) {
                //try to extract userinfo from URL
                final String userInfo = url.getUserInfo();
                final String[] split = userInfo.split(":", 2);
                if (2 == split.length) {
                    tusername = split[0];
                    tpassword = split[1];
                    url = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile());
                }
            }
        } catch (MalformedURLException e) {
            throw new UpdateException(e);
        }
        final File destinationFile = new File(destinationFilePath);
        final long mtime = destinationFile.exists() ? destinationFile.lastModified() : 0;

        update(factory.fileUpdaterFromURL(url, tusername, tpassword), destinationFile);
        if (destinationFile.lastModified() > mtime) {
            logger.info("updated file: " + destinationFile.getAbsolutePath());
        } else {
            logger.info("file already up to date: " + destinationFile.getAbsolutePath());
        }

    }

    /**
     * Update a destination file with an updater implementation, while maintaining appropriate
     * locks around the action and file
     * @param updater updater
     * @param destFile destination
     * @throws UpdateException on error
     */
    public static void update(final FileUpdater updater, final File destFile) throws UpdateException {

        final File lockFile = new File(destFile.getAbsolutePath() + ".lock");
        final File newDestFile = new File(destFile.getAbsolutePath() + ".new");

        try {

            //synchronize writing to file within this jvm
            synchronized (UpdateUtils.class) {
                final FileChannel channel = new RandomAccessFile(lockFile, "rw").getChannel();

                //acquire file lock to block external jvm (commandline) from writing to file
                final FileLock lock = channel.lock();
                try {
                    FileUtils.copyFileStreams(destFile, newDestFile);
                    if (!newDestFile.setLastModified(destFile.lastModified())) {
                        logger.warn("Unable to set modification time of temp file: " + newDestFile.getAbsolutePath());
                    }
                    updater.updateFile(newDestFile);
                    if(newDestFile.isFile() && newDestFile.length()>0){
                        moveFile(newDestFile, destFile);
                    }else {
                        throw new UpdateException("Result file was empty or not present: " + newDestFile);
                    }
                } catch (FileUpdaterException e) {
                    throw new UpdateException(e);
                } finally {
                    lock.release();
                    channel.close();
                }
            }
        } catch (IOException e) {
            throw new UpdateException("Unable to get and write file: " + e.getMessage(), e);
        }
    }

    /**
     * An exception caused by the UpdateUtils methods.
     */
    public static class UpdateException extends Exception {
        public UpdateException() {
            super();
        }

        public UpdateException(final String s) {
            super(s);
        }

        public UpdateException(final String s, final Throwable throwable) {
            super(s, throwable);
        }

        public UpdateException(final Throwable throwable) {
            super(throwable);
        }
    }

    private static class FileStreamUpdater implements FileUpdater {
        InputStream input;
        public FileStreamUpdater(InputStream input) {
            this.input=input;
        }

        public void updateFile(final File destinationFile) throws FileUpdaterException {
            try {
                Streams.copyStream(input, new FileOutputStream(destinationFile));
            } catch (IOException e) {
                throw new FileUpdaterException(e);
            }
        }
    }
}
