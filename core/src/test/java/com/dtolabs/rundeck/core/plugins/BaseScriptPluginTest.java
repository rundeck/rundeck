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

package com.dtolabs.rundeck.core.plugins;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import com.dtolabs.rundeck.core.execution.service.TestScriptPluginFileCopier;
import com.dtolabs.rundeck.core.plugins.metadata.PluginMeta;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.execution.ExecArgList;
import com.dtolabs.rundeck.core.plugins.metadata.ProviderDef;

@RunWith(JUnit4.class)
public class BaseScriptPluginTest {

    class test1 extends BaseScriptPlugin {

        protected test1(ScriptPluginProvider provider, Framework framework) {
            super(provider, framework);
            // TODO Auto-generated constructor stub
        }

        @Override
        public boolean isAllowCustomProperties() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public String[] createScriptArgs(
                Map<String, Map<String, String>> localDataContext) {
            // TODO Auto-generated method stub
            return super.createScriptArgs(localDataContext);
        }

        @Override
        public ExecArgList createScriptArgsList(
                Map<String, Map<String, String>> dataContext) {
            // TODO Auto-generated method stub
            return super.createScriptArgsList(dataContext);
        }
    }

    @Test
    public void testCreate() {
        test1 test = new test1(null, null);
        Assert.assertEquals(null, test.getProvider());
        Assert.assertEquals(null, test.getFramework());
    }

    @Test
    public void testCreateScriptArgsEmptyProvider() throws IOException {
        final Map<String, Object> data = new HashMap<String, Object>();
        data.put("script-file", "myfile.sh");
        data.put("plugin-type", "script");
        File archiveFile = File.createTempFile("test", "zip");
        archiveFile.deleteOnExit();
        File basedir = File.createTempFile("test", "dir");
        basedir.deleteOnExit();

        test1 test = new test1(
                scriptPluginProvider(new PluginMeta(),data, archiveFile, basedir), null);
        Map<String, Map<String, String>> datactx = new HashMap<String, Map<String, String>>();
        String[] result = test.createScriptArgs(datactx);
        File testfile = new File(basedir, "myfile.sh");
        Assert.assertArrayEquals(new String[] { testfile.getAbsolutePath() },
                result);
    }

    @Test
    public void testCreateScriptArgsListEmptyProvider() throws IOException {
        final Map<String, Object> data = new HashMap<String, Object>();
        data.put("script-file", "myfile.sh");
        data.put("plugin-type", "script");
        File archiveFile = File.createTempFile("test", "zip");
        archiveFile.deleteOnExit();
        File basedir = File.createTempFile("test", "dir");
        basedir.deleteOnExit();

        test1 test = new test1(
                scriptPluginProvider(new PluginMeta(),data, archiveFile, basedir), null);
        Map<String, Map<String, String>> datactx = new HashMap<String, Map<String, String>>();
        ExecArgList result = test.createScriptArgsList(datactx);
        File testfile = new File(basedir, "myfile.sh");
        Assert.assertArrayEquals(new String[] { testfile.getAbsolutePath() },
                result.asFlatStringArray());
    }

    static ProviderDef providerDef(final Map<String, Object> data) {
        return new ProviderDef(data);
    }

    static ScriptPluginProvider scriptPluginProvider(
            final PluginMeta pluginMeta,
            final Map<String, Object> data, final File archiveFile,
            final File basedir) {
        return new ScriptPluginProviderImpl(pluginMeta,providerDef(data), archiveFile,
                basedir);
    }
}
