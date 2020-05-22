package org.rundeck.storage.data

import spock.lang.Specification

class DataUtilSpec extends Specification {
    def "with stream should have not-null metadata"() {
        given:
            def input = new ByteArrayInputStream('test'.bytes)
        when:
            def result = DataUtil.<DataContent> withStream(input, DataUtil.contentFactory())
        then:
            result.meta != null
    }
}
