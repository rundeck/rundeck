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

package com.dtolabs.rundeck.core.execution.script;

import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import com.dtolabs.utils.Streams;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.*;

/**
 * ScriptfileUtilsTest is ...
 *
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-10-30
 */
@RunWith(JUnit4.class)
public class ScriptfileUtilsTest {

    @Test
    public void lineEndingStyleForNode_null() {
        NodeEntryImpl test = new NodeEntryImpl("test");
        testStyleForNode(test, ScriptfileUtils.LineEndingStyle.LOCAL);
    }


    @Test
    public void lineEndingStyleForNode_incorrect() {
        NodeEntryImpl test = new NodeEntryImpl("test");
        test.setOsFamily("not_an_os_family");
        testStyleForNode(test, ScriptfileUtils.LineEndingStyle.LOCAL);
    }

    @Test
    public void lineEndingStyleForNode_unix() {
        NodeEntryImpl test = new NodeEntryImpl("test");
        test.setOsFamily("unix");
        testStyleForNode(test, ScriptfileUtils.LineEndingStyle.UNIX);
    }

    @Test
    public void lineEndingStyleForNode_windows() {
        NodeEntryImpl test = new NodeEntryImpl("test");
        test.setOsFamily("windows");
        testStyleForNode(test, ScriptfileUtils.LineEndingStyle.WINDOWS);
    }

    @Test
    public void writeScriptFile_unix2unix() throws IOException {
        assertWriteScriptFileContents(
                "scriptString\nscriptString\n",
                ScriptfileUtils.LineEndingStyle.UNIX,
                "scriptString\nscriptString\n"
        );
    }
    @Test
    public void writeScriptFile_win2win() throws IOException {
        assertWriteScriptFileContents(
                "scriptString\r\nscriptString\r\n",
                ScriptfileUtils.LineEndingStyle.WINDOWS,
                "scriptString\r\nscriptString\r\n"
        );
    }
    @Test
    public void writeScriptFile_unix2win() throws IOException {
        assertWriteScriptFileContents(
                "scriptString\nscriptString\n",
                ScriptfileUtils.LineEndingStyle.WINDOWS,
                "scriptString\r\nscriptString\r\n"
        );
    }
    @Test
    public void writeScriptFile_win2unix() throws IOException {
        assertWriteScriptFileContents(
                "scriptString\r\nscriptString\r\n",
                ScriptfileUtils.LineEndingStyle.UNIX,
                "scriptString\nscriptString\n"
        );
    }

    private void assertWriteScriptFileContents(
            final String inputString,
            final ScriptfileUtils.LineEndingStyle style,
            final String expectedString
    ) throws IOException {
        File temp = File.createTempFile("writeScriptFileTest", "tmp");
        temp.deleteOnExit();
        ScriptfileUtils.writeScriptFile(
                null,
                inputString,
                null,
                style,
                temp
        );
        Assert.assertEquals(expectedString, readFileString(temp));
        temp.delete();
    }

    private String readFileString(final File temp) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(temp);
        try{
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Streams.copyStream(fileInputStream, output);
            return new String(output.toByteArray());
        }finally {
            fileInputStream.close();
        }
    }

    private void testStyleForNode(NodeEntryImpl test, ScriptfileUtils.LineEndingStyle expected) {
        ScriptfileUtils.LineEndingStyle lineEndingStyle = ScriptfileUtils.lineEndingStyleForNode(
                test
        );
        Assert.assertNotNull(lineEndingStyle);
        Assert.assertEquals(expected, lineEndingStyle);
    }
}
