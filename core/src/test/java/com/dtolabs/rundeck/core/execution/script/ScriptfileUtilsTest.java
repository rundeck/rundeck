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
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * ScriptfileUtilsTest is ...
 *
 * @author Greg Schueler <greg@simplifyops.com>
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

    private void testStyleForNode(NodeEntryImpl test, ScriptfileUtils.LineEndingStyle expected) {
        ScriptfileUtils.LineEndingStyle lineEndingStyle = ScriptfileUtils.lineEndingStyleForNode(
                test
        );
        Assert.assertNotNull(lineEndingStyle);
        Assert.assertEquals(expected, lineEndingStyle);
    }
}
