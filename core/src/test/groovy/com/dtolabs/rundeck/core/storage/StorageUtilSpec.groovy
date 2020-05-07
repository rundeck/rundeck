package com.dtolabs.rundeck.core.storage

import spock.lang.Specification

class StorageUtilSpec extends Specification {
    def "parse date null value should return null"() {
        given:
            def content = StorageUtil.withStream(new ByteArrayInputStream(''.bytes), [:])
        expect:
            content.modificationTime == null
            content.creationTime == null
    }

    def "parse date result"() {
        given:
            Date date = new Date(39393939000)
            Date date2 = new Date(12312312000)
            def content = StorageUtil.withStream(
                new ByteArrayInputStream(''.bytes), [
                (StorageUtil.RES_META_RUNDECK_CONTENT_MODIFY_TIME)  : StorageUtil.formatDate(date),
                (StorageUtil.RES_META_RUNDECK_CONTENT_CREATION_TIME): StorageUtil.formatDate(date2),
            ]
            )
        expect:
            content.modificationTime == date
            content.creationTime == date2
    }

    def "content length null value should return null"() {
        given:
            def content = StorageUtil.withStream(new ByteArrayInputStream(''.bytes), [:])
        expect:
            content.contentLength == -1
    }
}
