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

import com.dtolabs.rundeck.core.CoreException;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * TODO: Consider wrapping {@link org.apache.tools.ant.util.FileUtils}
 */
public class FileUtils {

    /**
     * Copies file src to dest using nio.
     * @param src
     * @param dest
     * @param overwrite
     * @throws IOException
     */
    public static void fileCopy(final File src, final File dest, final boolean overwrite) throws IOException {
        if (!dest.exists() || (dest.exists() && overwrite)) {
            // Create channel on the source
            final FileChannel srcChannel = new FileInputStream(src).getChannel();
            // Create channel on the destination
            final FileChannel dstChannel = new FileOutputStream(dest).getChannel();
            // Copy file contents from source to destination
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
            // Close the channels
            srcChannel.close();
            dstChannel.close();
        }
    }

    /**
     * Copy a file from one location to another, and set the modification time to match. (Uses java Streams).
     *
     * @param fromFile
     * @param toFile
     *
     * @throws IOException
     */
    public static void copyFileStreams(File fromFile, File toFile) throws IOException {
        if (!fromFile.exists()) {
            return;
        }

        FileInputStream fis = new FileInputStream(fromFile);
        FileOutputStream fos = new FileOutputStream(toFile);
        int read = 0;
        byte[] buf = new byte[1024];
        while (-1 != read) {
            read = fis.read(buf);
            if (read >= 0) {
                fos.write(buf, 0, read);
            }
        }
        fos.close();
        fis.close();

    }

    /**
     * Delete a directory recursivley. This method will delete all files and subdirectories.
     *
     * @param dir Directory to delete
     * @return If no error occurs, true is returned. false otherwise.
     */
    public static boolean deleteDir(final File dir) {
        if (dir.isDirectory()) {
            final String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                final boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

    /**
     * Rename a file.
     * Uses Java's nio library to use a lock and also works around
     * a windows specific bug in File#renameTo method.
     * @param file  File to rename
     * @param newPath  Path for new file name
     * @param clazz Class associated with lock
     * @throws com.dtolabs.rundeck.core.CoreException An CoreException is raised if any underly I/O
     * operation fails.
     */
    public static void fileRename(final File file, final String newPath, final Class clazz) {
        File newDestFile = new File(newPath);
        File lockFile = new File(newDestFile.getAbsolutePath() + ".lock");
        try {
            synchronized (clazz) {
                FileChannel channel = new RandomAccessFile(lockFile, "rw").getChannel();
                FileLock lock = channel.lock();
                try{
                        FileUtils.copyFileStreams(file, newDestFile);
                        newDestFile.setLastModified(file.lastModified());
                        String osName = System.getProperty("os.name");
                        if (!file.renameTo(newDestFile)) {
                            if (osName.toLowerCase().indexOf("windows") > -1 && newDestFile.exists()) {
                                //first remove the destFile
                                if (! newDestFile.delete()) {
                                    throw new CoreException(
                                        "Unable to remove dest file on windows: " +  newDestFile.getAbsolutePath());
                                }
                                if (!file.renameTo(newDestFile)) {
                                    throw new CoreException(
                                        "Unable to move file to dest file on windows: " + file + ", "
                                        + newDestFile.getAbsolutePath());
                                }
                            } else {
                                throw new CoreException(
                                    "Unable to move file to dest file: " + file + ", " + newDestFile.getAbsolutePath());
                            }
                        }
                } finally {
                    lock.release();
                    channel.close();
                }
            }
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new CoreException("Unable to rename file: " + e.getMessage(), e);
        }
    }

    /**
     * Rename a file
     * @param file  File to rename
     * @param newPath  Path for new file name
     * @throws com.dtolabs.rundeck.core.CoreException An CoreException is raised if any underly I/O
     * operation fails.  
     */
    public static void fileRename(final File file, final String newPath) {
        fileRename(file, newPath, FileUtils.class);
    }


    /**
     * Copy one directory to another location (recursively)
     * @param sourceLocation Source directory
     * @param targetLocation Target directory
     * @throws IOException Exception raised if any underlying I/O operation fails
     */
    public static void copyDirectory(File sourceLocation, File targetLocation)
            throws IOException {

        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }

            String[] children = sourceLocation.list();
            for (int i = 0; i < children.length; i++) {
                copyDirectory(new File(sourceLocation, children[i]),
                        new File(targetLocation, children[i]));
            }
        } else {

            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);

            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }
}
