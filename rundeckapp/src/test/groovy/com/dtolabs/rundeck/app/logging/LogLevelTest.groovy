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

package com.dtolabs.rundeck.app.logging

import static org.junit.Assert.*

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin;

import com.dtolabs.rundeck.core.logging.LogLevel

/*
 * LogLevelTest.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 1/23/13 12:26 AM
 * 
 */

@TestMixin(GrailsUnitTestMixin)
class LogLevelTest {
    void testBelowThreshold() {
        assertTrue(LogLevel.DEBUG.belowThreshold(LogLevel.DEBUG))
        assertFalse(LogLevel.DEBUG.belowThreshold(LogLevel.VERBOSE))

        assertTrue(LogLevel.VERBOSE.belowThreshold(LogLevel.VERBOSE))
        assertTrue(LogLevel.ERROR.belowThreshold(LogLevel.VERBOSE))
    }
}
