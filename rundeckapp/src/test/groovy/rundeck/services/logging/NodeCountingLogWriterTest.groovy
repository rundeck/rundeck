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

package rundeck.services.logging

import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogUtil
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Created by greg on 9/21/15.
 */
@RunWith(JUnit4)
class NodeCountingLogWriterTest {

    @Test
    void testCounter() {
        def counter = new NodeCountingLogWriter(new NoopLogWriter())
        Assert.assertEquals(0, counter.value)
        counter.addEvent(LogUtil.logError("",[node:'a']))
        Assert.assertEquals(1, counter.value)

        counter.addEvent(LogUtil.logError("monkey",[node:'b']))
        Assert.assertEquals(1, counter.value)

        counter.addEvent(LogUtil.logError("alpha\nbeta",[node:'a']))
        Assert.assertEquals(3, counter.value)
    }
}
