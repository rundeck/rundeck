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

package com.dtolabs.rundeck.core.authorization.providers;
/*
* TestPoliciesDocument.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Nov 16, 2010 1:31:59 PM
* 
*/

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;

public class TestPoliciesDocument extends TestCase {
    PoliciesDocument policiesDocument;
    File testdir;
    File test1;
    File test2;
    private DocumentBuilder builder;

    public TestPoliciesDocument(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestPoliciesDocument.class);
    }

    public void setUp() throws Exception {
        testdir = getPath("com/dtolabs/rundeck/core/authorization");
        test1 = new File(testdir, "test1.aclpolicy");
        test2 = new File(testdir, "admintest.aclpolicy");
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);
        builder = domFactory.newDocumentBuilder();
    }

    /**
     * @return
     */
    public static File getPath(String name) {

        URL url = ClassLoader.getSystemResource(name);
        File f;
        try {
            f = new File(url.toURI());
        } catch (URISyntaxException e) {
            f = new File(url.getPath());
        }
        return f;
    }

    protected void tearDown() throws Exception {
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }


    public void testPoliciesDocument() throws Exception {
        final Document document = builder.parse(test1);
        PoliciesDocument doc = new PoliciesDocument(document, test1);
        assertEquals(5.0, doc.countPolicies());
        final Collection<String> groupNames = doc.groupNames();
        assertEquals(6, groupNames.size());
    }
}