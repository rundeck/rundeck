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

package com.dtolabs.rundeck.core.authorization;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * TestNoAuthorization 
 */
public class TestNoAuthorization extends TestCase {
    static final String RESOURCE_PROJECT = "TestNoAuthorization";
    static final String RESOURCE_TYPE = "TypeA";
    static final String RESOURCE_OBJ = "aTypeA";

    public TestNoAuthorization(String name) {
          super(name);
      }

      public static void main(String[] args) {
          junit.textui.TestRunner.run(suite());
      }

      public static Test suite() {
          return new TestSuite(TestNoAuthorization.class);
      }

    public void testConstructor() {
        Authorization auth = new NoAuthorization(null, null);
        assertNotNull(auth);
    }
    public void testListMatchedRoles() {
        LegacyAuthorization auth = new NoAuthorization(null, null);
        assertEquals(auth.listMatchedRoles(),"");

    }
    public void testGetMatchedRoles() {
        LegacyAuthorization auth = new NoAuthorization(null, null);
        assertEquals(auth.getMatchedRoles().length, 0);

    }
    public void testAuthorize() {
        LegacyAuthorization auth = new NoAuthorization(null, null);
        assertTrue(auth.authorizeScript("bozo", RESOURCE_PROJECT, "test script"));

    }
}
