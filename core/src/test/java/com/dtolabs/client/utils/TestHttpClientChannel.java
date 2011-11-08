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
* TestHttpClientChannel.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 8/3/11 7:54 PM
* 
*/
package com.dtolabs.client.utils;

import junit.framework.TestCase;

import java.util.*;

/**
 * TestHttpClientChannel is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestHttpClientChannel extends TestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }
    public void testconstructURLQuery() throws Exception {
        assertEquals("base", HttpClientChannel.constructURLQuery("base", null));
        TreeMap<String, String> query = new TreeMap<String, String>();
        assertEquals("base?", HttpClientChannel.constructURLQuery("base", query));

        query.put("a", "b");
        assertEquals("base?a=b", HttpClientChannel.constructURLQuery("base", query));
        query.put("c", "d");
        assertEquals("base?a=b&c=d", HttpClientChannel.constructURLQuery("base", query));
        query.put("e ", "f ");
        assertEquals("base?a=b&c=d&e+=f+", HttpClientChannel.constructURLQuery("base", query));
    }
}
