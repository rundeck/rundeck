/*
 * Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>
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

package com.dtolabs.rundeck.core.execution.impl.local;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import com.dtolabs.rundeck.core.execution.service.FileCopierException;
import com.dtolabs.rundeck.core.utils.FileUtils;
import com.dtolabs.utils.Streams;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.*;

/**
 * LocalFileCopierTest is ...
 *
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-11-03
 */
@RunWith(JUnit4.class)
public class LocalFileCopierTest {

    private static final String PROJ_NAME = "LocalFileCopierTest";

    //
    // junit exported java properties (e.g. from maven's project.properties)
    //
    public static String RDECK_BASE = System.getProperty("rdeck.base", "target/rdeck_base");

    //
    // derived modules and projects base
    //
    private static String PROJECTS_BASE = RDECK_BASE + "/" + "projects";

    private Framework getFramework(){

        return Framework.getInstance(RDECK_BASE, PROJECTS_BASE);
    }

    @Test
    public void testCopyString() throws IOException, FileCopierException {
        LocalFileCopier localFileCopier = new LocalFileCopier(getFramework());
        File temp = File.createTempFile("string-copy", "tmp");
        String script = "my script\n";
        String s = localFileCopier.copyScriptContent(null, script, null, temp.getAbsolutePath());
        Assert.assertEquals(temp.getAbsolutePath(), s);
        Assert.assertEquals(script, getContentString(temp));
    }

    @Test
    public void testCopyInputStream() throws IOException, FileCopierException {
        LocalFileCopier localFileCopier = new LocalFileCopier(getFramework());
        File temp = File.createTempFile("string-copy", "tmp");
        String script = "my script\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(script.getBytes());
        String s = localFileCopier.copyFileStream(null, inputStream, null, temp.getAbsolutePath());
        Assert.assertEquals(temp.getAbsolutePath(), s);
        Assert.assertEquals(script, getContentString(temp));
    }

    @Test
    public void testCopyFile() throws IOException, FileCopierException {
        LocalFileCopier localFileCopier = new LocalFileCopier(getFramework());
        File temp = File.createTempFile("string-copy", "tmp");
        String script = "my script\n";
        File inputFile = File.createTempFile(
                "input",
                "tmp"
        );
        FileOutputStream fileOutputStream = new FileOutputStream( inputFile);
        fileOutputStream.write(script.getBytes());
        fileOutputStream.close();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(script.getBytes());
        String s = localFileCopier.copyFile(null, inputFile, null, temp.getAbsolutePath());
        Assert.assertEquals(temp.getAbsolutePath(), s);
        Assert.assertEquals(script, getContentString(temp));
    }


    private static String getContentString(File file) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileInputStream fileInputStream = new FileInputStream(file);
        Streams.copyStream(fileInputStream, baos);
        return new String(baos.toByteArray());
    }
}
