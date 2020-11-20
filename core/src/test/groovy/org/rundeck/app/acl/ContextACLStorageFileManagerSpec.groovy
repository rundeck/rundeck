package org.rundeck.app.acl

import com.dtolabs.rundeck.core.authorization.LoggingAuthorization
import com.dtolabs.rundeck.core.authorization.RuleEvaluator
import com.dtolabs.rundeck.core.authorization.providers.Validator
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageManager
import org.rundeck.storage.api.ContentMeta
import org.rundeck.storage.api.Resource
import spock.lang.Specification

class ContextACLStorageFileManagerSpec extends Specification {


    void "get sub context"(){
        given:
            def files=[
                'file1.aclpolicy'
            ]
            def paths=[
                "projects/$project/acls/file1.aclpolicy".toString()
            ]
            def service = ContextACLStorageFileManager
                .builder()
                .validator(Mock(Validator))
                .storageManager(Mock(StorageManager){
                    _ * listDirPaths("projects/$project/acls/",'.*\\.aclpolicy') >> paths
                    _ * existsFileResource("projects/$project/acls/file1.aclpolicy") >> true
                    _ * getFileResource("projects/$project/acls/file1.aclpolicy") >> Mock(Resource) {
                        getContents() >> Mock(ResourceMeta) {
                            getInputStream() >> new ByteArrayInputStream(
                                (
                                    '{ description: \'\', \n' +
                                    'by: { username: \'test\' }, \n' +
                                    'for: { resource: [ { equals: { kind: \'zambo\' }, allow: \'x\' } ] } }'
                                ).bytes
                            )
                            getModificationTime() >> new Date()
                        }
                    }
                })
                .prefixMapping(
                    { AppACLContext context ->
                        context.system ?
                        'acls/' :
                        "projects/$context.project/acls/".toString()
                    }
                )
                .build()



        when:
            def ctx = service.forContext(AppACLContext.project(project))
            def result = ctx.listStoredPolicyFiles()


        then:

            result == files
        where:
            project='test1'
    }
}
