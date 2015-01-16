/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
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
* ZipUtil.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Sep 30, 2010 3:36:38 PM
*/
package com.dtolabs.rundeck.core.utils;

import java.util.*;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.io.*;

/**
 * ZipUtil provides utility methods for extracting the contents of a zip file.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ZipUtil {

    /**
     * Extracts all contents of the file to the destination directory
     *
     * @param path zip file path
     * @param dest destination directory
     *
     * @throws IOException on io error
     */
    public static void extractZip(final String path, final File dest) throws IOException {
        extractZip(path, dest, null);
    }

    /**
     * Extracts all contents that match a certain prefix
     *
     * @param path   zip file path
     * @param dest   destination directory
     * @param prefix prefix of contents to extract
     *
     * @throws IOException on io error
     */
    public static void extractZip(final String path, final File dest, final String prefix) throws IOException {
        extractZip(path, dest, prefix, null);
    }
    /**
     * Extracts a single entry from the zip
     *
     * @param path   zip file path
     * @param dest   destination directory
     * @param fileName specific filepath to extract
     *
     * @throws IOException on io error
     */
    public static void extractZipFile(final String path, final File dest, final String fileName) throws IOException {
        FilenameFilter filter = null;
        if (null != fileName) {
            filter = new FilenameFilter() {
                public boolean accept(final File file, final String name) {
                    return fileName.equals(name) || fileName.startsWith(name);
                }
            };
        }
        extractZip(path, dest, filter, null, null);
    }

    /**
     * Extract the zip file to the destination, optionally only the matching files and renaming the files
     *
     * @param path        zip file path
     * @param dest        destination directory to contain files
     * @param prefix      match files within the zip if they have this prefix path, or null selects all files
     * @param stripPrefix rename files by removing this prefix if they have it, or null doesn't rename files
     *
     * @throws IOException on io error
     */
    public static void extractZip(final String path, final File dest, final String prefix,
                                  final String stripPrefix) throws IOException {
        extractZip(path, dest, prefix, stripPrefix, null);
    }

    /**
     * Extract the zip file to the destination, optionally only the matching files and renaming the files
     *
     * @param path        zip file path
     * @param dest        destination directory to contain files
     * @param prefix      match files within the zip if they have this prefix path, or null selects all files
     * @param stripPrefix rename files by removing this prefix if they have it, or null doesn't rename files
     * @param copier  copier
     *
     * @throws IOException on io error
     */
    public static void extractZip(final String path, final File dest, final String prefix, final String stripPrefix,
                                  final streamCopier copier) throws IOException {
        extractZip(path, dest, prefix, new PrefixStripper(stripPrefix), copier);
    }

    /**
     * Extract the zip file to the destination, optionally only the matching files and renaming the files
     *
     * @param path   zip file path
     * @param dest   destination directory to contain files
     * @param prefix match files within the zip if they have this prefix path, or null selects all files
     * @param rename renamer instance
     * @param copier streamCopier instance
     *
     * @throws IOException on io error
     */
    public static void extractZip(final String path, final File dest, final String prefix, final renamer rename,
                                  final streamCopier copier) throws IOException {
        FilenameFilter filter = null;
        if (null != prefix) {
            filter = new FilenameFilter() {
                public boolean accept(final File file, final String name) {
                    return name.startsWith(prefix);
                }
            };
        }
        extractZip(path, dest, filter, rename, copier);

    }

    /**
     * Extract the zip file to the destination, optionally only the matching files and renaming the files
     *
     * @param path   zip file path
     * @param dest   destination directory to contain files
     * @param filter filter to select matching files
     * @param rename renamer to use
     * @param copier streamCopier to use
     *
     * @throws IOException on io error
     */
    public static void extractZip(final String path, final File dest, final FilenameFilter filter, final renamer rename,
                                  final streamCopier copier) throws IOException {
        final ZipFile jar = new ZipFile(path);
        final Enumeration<? extends ZipEntry> enumeration = jar.entries();
        while (enumeration.hasMoreElements()) {
            final ZipEntry entry = enumeration.nextElement();
            if (null != filter && !filter.accept(dest, entry.getName())) {
                continue;
            }
            String name = entry.getName();
            if (null != rename) {
                name = rename.rename(name);
            }
            final File destFile = new File(dest, name);
            if (entry.isDirectory() && !destFile.isDirectory()) {
                if (!destFile.mkdirs()) {
                    throw new IOException("Unable to make directory: " + destFile);
                }
            } else if (!entry.isDirectory()) {
                if (!destFile.exists()) {
                    //create parent dirs if necessary
                    File parent = destFile.getParentFile();
                    if(!parent.exists() && !parent.mkdirs()){
                        throw new IOException("Unable to create parent dir for file: " + destFile.getAbsolutePath());
                    }
                    if (!destFile.createNewFile()) {
                        throw new IOException("Unable to create file: " + destFile.getAbsolutePath());
                    }
                }
                InputStream entryStream = jar.getInputStream(entry);
                FileOutputStream fileOut = new FileOutputStream(destFile);
                try{
                    if (null != copier) {
                        copier.copyStream(entryStream, fileOut);
                    } else {
                        copyStream(entryStream, fileOut);
                    }
                }finally{
                    fileOut.close();
                }
            }
        }
    }

    /**
     * Interface for renaming a file
     */
    public static interface renamer {
        public String rename(String input);
    }

    /**
     * Strips a prefix from input
     */
    public static class PrefixStripper implements renamer {
        String prefix;

        public PrefixStripper(final String prefix) {
            this.prefix = prefix;
        }

        public String rename(final String name) {
            if (null != prefix && name.startsWith(prefix)) {
                return name.substring(prefix.length());
            }
            return name;
        }
    }

    /**
     * Interface for copying a stream from input to output.
     */
    public static interface streamCopier {
        public void copyStream(InputStream in, OutputStream out) throws IOException;
    }

    /**
     * streamCopier that simply copies the stream without modification.
     */
    public static class CopyStreamCopier implements streamCopier {
        public void copyStream(final InputStream in, final OutputStream out) throws IOException {
            ZipUtil.copyStream(in, out);
        }
    }


    public static void copyStream(final InputStream inputStream, final OutputStream outputStream) throws IOException {
        final byte[] buffer = new byte[1024 * 4];
        int c;
        while (-1 != (c = inputStream.read(buffer))) {
            outputStream.write(buffer, 0, c);
        }
    }

}
