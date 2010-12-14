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

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.*;


/**
 * Unit tests for the FileUtils class
 */
public class TestFileUtils extends TestCase {

    File tempdir;
    public TestFileUtils(final String name) throws IOException {
         super(name);
     }

     public static Test suite() {
         return new TestSuite(TestFileUtils.class);
     }

    @Override
    protected void setUp() throws Exception {
        tempdir = new File("build/test-target/TestFileUtilsdir");
        tempdir.mkdirs();
    }

    @Override
    protected void tearDown() throws Exception {
        FileUtils.deleteDir(tempdir);
    }

    private File createTextfile(final String path) throws Exception {
        File file = new File(tempdir, path);
        BufferedWriter out = new BufferedWriter(new FileWriter(file));
        out.write("a");
        out.write("b");
        out.write("c");
        out.close();
        return file;
    }

     public void testFileRename() throws Exception {
         final File infile = createTextfile("TestFileUtils.src");
         assertTrue("test setup error: failed to create text file", infile.exists());
         final long infileSize = infile.length();
         final long infileMtime = infile.lastModified();
         final String destpath = tempdir.getAbsolutePath() + "/TestFileUtils.dest";
         FileUtils.fileRename(infile, destpath);
         assertFalse("infile still exists", infile.exists());
         final File destfile = new File(destpath);
         assertTrue("failed to rename file", destfile.exists());
         assertEquals("file sizes did not match. "
                 + "infile size="+ infileSize
                 + ", destfile size="+destfile.length(), infileSize, destfile.length());
         assertEquals("file mtime did not match. "
                 + "infile mtime="+ infileMtime
                 + ", destfile mtime="+destfile.lastModified(), infileMtime, destfile.lastModified());

         infile.delete(); destfile.delete();
         final File infile2 = createTextfile("TestFileUtils2.src");
         FileUtils.fileRename(infile2, destpath, TestFileUtils.class);
         assertFalse(infile2.getAbsolutePath() + "still exists", infile2.exists());
         // clean up
         infile2.delete(); destfile.delete();         
     }
}
