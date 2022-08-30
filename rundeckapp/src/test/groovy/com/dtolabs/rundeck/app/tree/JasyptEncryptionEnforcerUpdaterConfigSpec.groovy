package com.dtolabs.rundeck.app.tree

import com.dtolabs.rundeck.core.storage.ResourceMeta
import org.rundeck.storage.api.Resource
import spock.lang.Specification

class JasyptEncryptionEnforcerUpdaterConfigSpec extends Specification {
    def "should perform with correct config"() {
        given:
            def sut = new JasyptEncryptionEnforcerUpdaterConfig()
            sut.treeCreator = Mock(TreeCreator) {
                getStorageConfigMap() >> config
            }
        when:
            def result = sut.shouldPerform()
        then:
            result == expected
        where:
            config                                     | expected
            [:]                                        | false
            ['converter.1.type': 'jasypt-encryption']  | true
            ['converter.23.type': 'jasypt-encryption'] | true
            ['converter.1.type': 'other']              | false
    }

    def "modifyContents value when metadata matches"() {
        given:
            def sut = new JasyptEncryptionEnforcerUpdaterConfig()
            def resourceMeta = Mock(ResourceMeta) {
                getMeta() >> meta
            }
            def resource = Mock(Resource) {
                getContents() >> resourceMeta
            }
        when: "modify contents called with resource"
            def result = sut.getUpdatedContents(resource)

        then: "result is the same as the input resource meta if the content should be modified, otherwise null"
            (result == resourceMeta) == modified
            (result == null) == !modified

        where: "jasypt-encryption metadata values"
            meta                                                                    | modified
            [:]                                                                     | true
            ['jasypt-encryption:encrypted': 'false']                                | true
            [blah: 'bloo']                                                          | true
    }

    def "modify Contents value when metadata indicates encrypted but the content size is the same as declared in metadata"() {
        given:
        def sut = new JasyptEncryptionEnforcerUpdaterConfig()
        def resourceMeta = Mock(ResourceMeta) {
            getMeta() >> meta
            getInputStream() >> new ByteArrayInputStream(contentValue.getBytes());
        }
        def resource = Mock(Resource) {
            getContents() >> resourceMeta
        }
        when: "modify contents called with resource"
        def result = sut.getUpdatedContents(resource)

        then: "result is the same as the input resource meta if the content should be modified, otherwise null"
        (result == resourceMeta) == modified
        (result == null) == !modified

        where: "jasypt-encryption metadata values"
        meta                                                                    | contentValue  | modified
        ['jasypt-encryption:encrypted': 'true']                                 | "Abcdef"      | false
        ['jasypt-encryption:encrypted': 'true', 'Rundeck-content-size': 42 ]    | "Abcdef"      | false
        ['jasypt-encryption:encrypted': 'true', 'Rundeck-content-size': 6 ]     | "Abcdef"      | true
    }

}
