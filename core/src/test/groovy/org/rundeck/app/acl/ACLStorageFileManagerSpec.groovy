package org.rundeck.app.acl

import com.dtolabs.rundeck.core.authorization.RuleSetValidation
import com.dtolabs.rundeck.core.authorization.Validation
import com.dtolabs.rundeck.core.authorization.providers.BaseValidator
import com.dtolabs.rundeck.core.authorization.providers.Validator
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageManager
import com.dtolabs.rundeck.core.storage.StorageUtil
import org.rundeck.storage.api.Resource
import spock.lang.Specification

class ACLStorageFileManagerSpec extends Specification {

    def "listStoredPolicyPaths"() {
        given:
            def storage = Mock(StorageManager)
            def validator = Mock(Validator)
            ACLStorageFileManager sut = new ACLStorageFileManager('aprefix/', storage, validator)
        when:
            def result = sut.listStoredPolicyFiles()
        then:
            1 * storage.listDirPaths('aprefix/', '.*\\.aclpolicy') >> [
                'aprefix/a.aclpolicy',
                'aprefix/b.aclpolicy',
            ]
            result == ['a.aclpolicy', 'b.aclpolicy']
    }

    def "existsPolicyFile"() {
        given:
            def storage = Mock(StorageManager)
            def validator = Mock(Validator)
            ACLStorageFileManager sut = new ACLStorageFileManager('aprefix/', storage, validator)
        when:
            def result = sut.existsPolicyFile('afilename')
        then:
            1 * storage.existsFileResource('aprefix/afilename') >> response
            result == response
        where:
            response << [true, false]
    }

    def "validatePolicyFile"() {
        given:
            def storage = Mock(StorageManager)
            def validator = Mock(BaseValidator)
            ACLStorageFileManager sut = new ACLStorageFileManager('aprefix/', storage, validator)
            def validation = Mock(RuleSetValidation)
        when:
            def result = sut.validatePolicyFile('afilename')
        then:
            1 * storage.existsFileResource('aprefix/afilename') >> true
            1 * storage.getFileResource('aprefix/afilename') >> Mock(Resource) {
                getContents() >> Mock(ResourceMeta) {
                    getInputStream() >> { new ByteArrayInputStream('asdf'.bytes) }
                }
            }
            1 * validator.validateYamlPolicy('afilename', 'asdf') >> validation
            result == validation
    }

    def "validatePolicyFile does not exist"() {
        given:
            def storage = Mock(StorageManager)
            def validator = Mock(BaseValidator)
            ACLStorageFileManager sut = new ACLStorageFileManager('aprefix/', storage, validator)
        when:
            def result = sut.validatePolicyFile('afilename')
        then:
            1 * storage.existsFileResource('aprefix/afilename') >> false
            result == null
    }

    def "getPolicyFileContents"() {
        given:
            def storage = Mock(StorageManager)
            def validator = Mock(BaseValidator)
            ACLStorageFileManager sut = new ACLStorageFileManager('aprefix/', storage, validator)
        when:
            def result = sut.getPolicyFileContents('afilename')
        then:
            1 * storage.existsFileResource('aprefix/afilename') >> true
            1 * storage.getFileResource('aprefix/afilename') >> Mock(Resource) {
                getContents() >> Mock(ResourceMeta) {
                    getInputStream() >> { new ByteArrayInputStream('asdf'.bytes) }
                }
            }
            result == 'asdf'
    }

    def "getPolicyFileContents does not exist"() {
        given:
            def storage = Mock(StorageManager)
            def validator = Mock(BaseValidator)
            ACLStorageFileManager sut = new ACLStorageFileManager('aprefix/', storage, validator)
        when:
            def result = sut.getPolicyFileContents('afilename')
        then:
            1 * storage.existsFileResource('aprefix/afilename') >> false
            result == null
    }

    def "loadPolicyFileContents"() {
        given:
            def storage = Mock(StorageManager)
            def validator = Mock(BaseValidator)
            ACLStorageFileManager sut = new ACLStorageFileManager('aprefix/', storage, validator)
            ByteArrayOutputStream out = new ByteArrayOutputStream()

        when:
            def result = sut.loadPolicyFileContents('afilename', out)
        then:
            1 * storage.existsFileResource('aprefix/afilename') >> true
            1 * storage.loadFileResource('aprefix/afilename', _) >> {
                it[1].write('asdf'.bytes)
                return 4L
            }
            result == 4
            new String(out.toByteArray()) == 'asdf'
    }

    def "loadPolicyFileContents does not exist"() {
        given:
            def storage = Mock(StorageManager)
            def validator = Mock(BaseValidator)
            ACLStorageFileManager sut = new ACLStorageFileManager('aprefix/', storage, validator)
            ByteArrayOutputStream out = new ByteArrayOutputStream()

        when:
            def result = sut.loadPolicyFileContents('afilename', out)
        then:
            1 * storage.existsFileResource('aprefix/afilename') >> false
            result == -1
    }


    def "getAclPolicy"() {
        given:
            def storage = Mock(StorageManager)
            def validator = Mock(BaseValidator)
            ACLStorageFileManager sut = new ACLStorageFileManager('aprefix/', storage, validator)
            def date1 = new Date(System.currentTimeMillis() - 1000L)
            def date2 = new Date(System.currentTimeMillis() - 2000L)
            //formatting the date loses precision so use parsed date from format to do equality comparison in then
            // clause
            Date modTime = StorageUtil.parseDate(StorageUtil.formatDate(date1), null)
            Date createTime = StorageUtil.parseDate(StorageUtil.formatDate(date2), null)
        when:
            def result = sut.getAclPolicy('afilename')
        then:
            1 * storage.existsFileResource('aprefix/afilename') >> true
            1 * storage.getFileResource('aprefix/afilename') >> Mock(Resource) {
                1 * getContents() >> StorageUtil.withStream(
                    new ByteArrayInputStream('asdf'.bytes),
                    [
                        (StorageUtil.RES_META_RUNDECK_CONTENT_MODIFY_TIME)  : StorageUtil.formatDate(modTime),
                        (StorageUtil.RES_META_RUNDECK_CONTENT_CREATION_TIME): StorageUtil.formatDate(createTime),
                    ]
                )
            }
            result != null
            result.modified == modTime
            result.created == createTime
            result.name == 'afilename'
            def ins = result.getInputStream()
            ins.text == 'asdf'
    }

    def "storePolicyFileContents"() {
        given:

            def storage = Mock(StorageManager)
            def validator = Mock(BaseValidator)
            ACLStorageFileManager sut = new ACLStorageFileManager('aprefix/', storage, validator)
        when:
            def result = sut.storePolicyFileContents('afilename', 'asdfasdf')
        then:
            1 * storage.writeFileResource('aprefix/afilename', { it.text == 'asdfasdf' }, [:]) >> Mock(Resource)
            result == 8
    }

    def "storePolicyFile"() {
        given:

            def storage = Mock(StorageManager)
            def validator = Mock(BaseValidator)
            ACLStorageFileManager sut = new ACLStorageFileManager('aprefix/', storage, validator)
            def is = new ByteArrayInputStream('asdfasdf'.bytes)
        when:
            def result = sut.storePolicyFile('afilename', is)
        then:
            1 * storage.writeFileResource('aprefix/afilename', { it.text == 'asdfasdf' }, [:]) >> Mock(Resource) {
                getContents() >> Mock(ResourceMeta) {
                    getContentLength() >> 8
                }
            }
            result == 8
    }

    def "deletePolicyFile"() {
        given:

            def storage = Mock(StorageManager)
            def validator = Mock(BaseValidator)
            ACLStorageFileManager sut = new ACLStorageFileManager('aprefix/', storage, validator)
        when:
            def result = sut.deletePolicyFile('afilename')
        then:
            1 * storage.deleteFileResource('aprefix/afilename') >> deleted
            result == deleted
        where:
            deleted << [true, false]
    }

}
