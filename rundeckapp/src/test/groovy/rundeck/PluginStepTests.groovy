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

package rundeck

import org.grails.orm.hibernate.HibernateDatastore
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.springframework.transaction.PlatformTransactionManager

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNull

class PluginStepTests  {

    static HibernateDatastore hibernateDatastore

    PlatformTransactionManager transactionManager

    @BeforeClass
    static void setupGorm() {
        hibernateDatastore = new HibernateDatastore(PluginStep)
    }

    @AfterClass
    static void shutdownGorm() {
        hibernateDatastore.close()
    }

    @Before
    void setup() {
        transactionManager = hibernateDatastore.getTransactionManager()
    }

    @Test
    void testClone() {
        PluginStep t = new PluginStep(type: 'blah',configuration: [elf:'hello'],nodeStep: true, keepgoingOnSuccess: true)
        PluginStep j1 = t.createClone()
        assertEquals('blah', j1.type)
        assertEquals([elf:'hello'], j1.configuration)
        assertEquals(true, j1.nodeStep)
        assertEquals(true, !!j1.keepgoingOnSuccess)
        assertNull(j1.errorHandler)
    }
}
