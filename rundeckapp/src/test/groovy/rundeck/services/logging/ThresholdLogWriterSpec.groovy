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
import rundeck.services.execution.ValueHolder
import spock.lang.Specification

class ThresholdLogWriterSpec extends Specification  {

    class testLogWriter extends NoopLogWriter {
        List<String> messages = []

        @Override
        void addEvent(final LogEvent event) {
            messages << event.message
        }
    }

    class longValueHolder implements ValueHolder<Long> {
        Long value
    }

    void "test Log Output On Threshold Reached"() {
        given:
        def t = new LoggingThreshold()
        t.description = 'thresholddesc'
        t.maxValue = 1
        def valueHolder = new longValueHolder()
        valueHolder.value = 1
        t.valueHolder = valueHolder
        def logger = new testLogWriter()
        def w = new ThresholdLogWriter(logger, t)

        when:
        w.addEvent(LogUtil.logNormal("message1"))
        valueHolder.value = 2 //exceed threshold
        w.addEvent(LogUtil.logNormal("message2"))
        w.addEvent(LogUtil.logNormal("message3"))
        w.addEvent(LogUtil.logNormal("message4"))
        w.addEvent(LogUtil.logNormal("message5"))

        then:
        logger.messages.size() == 6
        logger.messages == ["message1", "message2", "Log output limit exceeded: thresholddesc, job execution: Unknown", "message3", "message4", "message5"]
    }

    void "test Truncate"() {
        given:
        def t = new LoggingThreshold()
        t.description = 'thresholddesc'
        t.maxValue = 1
        t.action = LoggingThreshold.ACTION_TRUNCATE
        def valueHolder = new longValueHolder()
        valueHolder.value = 1
        t.valueHolder = valueHolder
        def logger = new testLogWriter()
        def w = new ThresholdLogWriter(logger, t)

        when:
        w.addEvent(LogUtil.logNormal("message1"))
        valueHolder.value = 2 //exceed threshold
        w.addEvent(LogUtil.logNormal("message2"))
        w.addEvent(LogUtil.logNormal("message3"))
        w.addEvent(LogUtil.logNormal("message4"))
        w.addEvent(LogUtil.logNormal("message5"))

        then:
        logger.messages.size() == 3
        logger.messages == ["message1", "message2", "Log output limit exceeded: thresholddesc, job execution: Unknown"]
    }
}
