/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.core.utils;

import com.dtolabs.rundeck.core.CoreException;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * TODO: Consider wrapping {@link org.apache.tools.ant.util.FileUtils}
 */
public class FileUtils {

    /**
     * Copies file src to dest using nio.
     * @param src source file
     * @param dest destination file
     * @param overwrite true to overwrite if it already exists
     * @throws IOException on io error
     */
    public static void fileCopy(final File src, final File dest, final boolean overwrite) throws IOException {
        if (!dest.exists() || (dest.exists() && overwrite)) {
            // Create parent directory structure if necessary
            FileUtils.mkParentDirs(dest);

            if (overwrite) {
                Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.copy(src.toPath(), dest.toPath());
            }
        }
    }

    /**
     * Copy a file from one location to another, and set the modification time to match. (Uses java Streams).
     *
     * @param fromFile source file
     * @param toFile dest file
     *
     * @throws IOException on io error
     */
    public static void copyFileStreams(final File fromFile, final File toFile) throws IOException {
        if (!fromFile.exists()) {
            return;
        }

        Files.copy(fromFile.toPath(), toFile.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.COPY_ATTRIBUTES);
    }

    /**
     * Delete a directory recursively. This method will delete all files and subdirectories.
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
     * Delete a directory recursively. This method will delete all files and subdirectories.
     *
     * @param dir Directory to delete
     * @return If no error occurs, true is returned. false otherwise.
     */
    public static void deleteDirOnExit(final File dir) {
        if (dir.isDirectory()) {
            final String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                deleteDirOnExit(new File(dir, children[i]));
            }
        }

        // The directory is now empty so delete it
        dir.deleteOnExit();
    }

    /**
     * Rename a file.
     * Uses Java's nio library to use a lock.
     * @param file  File to rename
     * @param newPath  Path for new file name
     * @param clazz Class associated with lock
     * @throws com.dtolabs.rundeck.core.CoreException A CoreException is raised if any underlying I/O
     * operation fails.
     */
    public static void fileRename(final File file, final String newPath, final Class clazz) {
        File newDestFile = new File(newPath);
        File lockFile = new File(newDestFile.getAbsolutePath() + ".lock");
        try {
            synchronized (clazz) {
                FileChannel channel = new RandomAccessFile(lockFile, "rw").getChannel();
                FileLock lock = channel.lock();
                try {
                    try {
                        // Create parent directory structure if necessary
                        FileUtils.mkParentDirs(newDestFile);
                        Files.move(file.toPath(), newDestFile.toPath(),
                                StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException ioe) {
                        throw new CoreException("Unable to move file " + file +
                                " to destination " + newDestFile + ": " + ioe.getMessage());
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
     * @throws com.dtolabs.rundeck.core.CoreException An CoreException is raised if any underlying I/O
     * operation fails.  
     */
    public static void fileRename(final File file, final String newPath) {
        fileRename(file, newPath, FileUtils.class);
    }

    /**
     * Create parent directory structure of a given file, if it doesn't already
     * exist.
     * @param file  File to create directories for
     * @throws IOException if an I/O error occurs
     */
    public static void mkParentDirs(final File file) throws IOException {
        // Create parent directory structure if necessary
        File parentFile = file.getParentFile();
        if (parentFile == null) {
            // File was created with a relative path
            parentFile = file.getAbsoluteFile().getParentFile();
        }
        if (parentFile != null && !parentFile.exists()) {
            if (!parentFile.mkdirs()) {
                throw new IOException("Unable to create parent directory " +
                        "structure for file " + file.getAbsolutePath());
            }
        }
    }

    /**
     * Return common base directory of the given files
     *
     * @param files
     *
     * @return common base directory
     */
    public static File getBaseDir(final List<File> files) {
        String common = getCommonPrefix(files.stream().map(new Function<File, String>() {
            @Override
            public String apply(final File file) {
                return file.getAbsolutePath();
            }
        }).collect(Collectors.toList()));
        if (null != common) {
            return new File(common);
        }
        return null;
    }

    /**
     * Return the relative path between a parent directory and some child path
     *
     * @param parent
     * @param child
     *
     * @return the relative path for the child
     *
     * @throws IllegalArgumentException if child is not a subpath
     */
    public static String relativePath(File parent, File child) {
        Path superPath = parent.toPath();
        Path subPath = child.toPath();
        if (!subPath.startsWith(superPath)) {
            throw new IllegalArgumentException("Not a subpath: " + child);
        }
        return superPath.relativize(subPath).toString();
    }

    public static String getCommonPrefix(final List<String> files) {
        int minlength = -1;
        String common = null;
        for (String path : files) {
            if (path == null) {
                continue;
            }
            if (minlength < 0) {
                common = path;
                minlength = common.length();
            } else {
                int testlen = Math.min(path.length(), minlength);
                for (int i = 0; i < testlen; i++) {
                    if (path.charAt(i) != common.charAt(i)) {
                        minlength = i;
                        common = path.substring(0, minlength);
                        break;
                    }
                }
                if (testlen < minlength) {
                    minlength = testlen;
                    common = path.substring(0, minlength);
                }
            }
        }
        return common;
    }


    public static void copyDirectory(final File sourceDir, final File newDir)
            throws IOException {
        final File[] srcFiles = sourceDir.listFiles();
        if (srcFiles == null) {
           throw new IOException("Failed to list contents of " + sourceDir);
        }
        if (newDir.exists() && !newDir.isDirectory()) {
            throw new IOException("Destination '" + newDir + "' exists but is not a directory");
        } else if (!newDir.mkdirs() && !newDir.isDirectory()) {
            throw new IOException("Destination '" + newDir + "' directory cannot be created");
        }
        if (!newDir.canWrite()) {
            throw new IOException("Destination '" + newDir + "' cannot be written to");
        }
        for (final File srcFile : srcFiles) {
            final File dstFile = new File(newDir, srcFile.getName());
            if (srcFile.isDirectory()) {
                copyDirectory(srcFile, dstFile);
            } else {
                copyFileStreams(srcFile, dstFile);
            }
        }

    }

}
