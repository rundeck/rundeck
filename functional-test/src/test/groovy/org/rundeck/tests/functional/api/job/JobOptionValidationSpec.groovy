package org.rundeck.tests.functional.api.job

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

@APITest
class JobOptionValidationSpec extends BaseContainer {

    def setupSpec() {
        startEnvironment()
        setupProject()
    }

    def "validate job option  fails for version<47"() {
        given:
            def client = getClient()
            client.apiVersion = version
        when:
            def response = client.doPost(
                "/project/${PROJECT_NAME}/jobs/validateOption",
                [
                    "name"    : "opt1",
                    "required": true
                ]
            )
        then:
            verifyAll {
                !response.successful
                response.code() == 400
                def json = client.jsonValue(response.body(), Map)
                json.errorCode == 'api.error.api-version.unsupported'
            }
        where:
            version << [14, 46]
    }

    def "validate job option ok for version>46"() {
        given:
            def client = getClient()
            client.apiVersion = 47
        when:
            def response = client.doPost(
                "/project/${PROJECT_NAME}/jobs/validateOption",
                [
                    "name"    : "opt1",
                    "required": true
                ]
            )
        then:
            verifyAll {
                response.successful
                response.code() == 200
                def json = client.jsonValue(response.body(), Map)
                json.valid == true
            }
    }
    def "validate job option invalid field for version>46"() {
        given:
            def client = getClient()
            client.apiVersion = 47
        when:
            def response = client.doPost(
                "/project/${PROJECT_NAME}/jobs/validateOption?jobWasScheduled=${sched}",
                [
                    "name"    : "opt1",
                    "required": true
                ]+extra
            )
        then:
            verifyAll {
                !response.successful
                response.code() == 400
                def json = client.jsonValue(response.body(), Map)
                json.valid == false
                json.messages!=null
                json.messages[fieldName]!=null
                json.messages[fieldName].size()>0
                json.messages[fieldName].any{it.contains(message)}
            }
        where:
          sched | extra                                        | fieldName                      | message
          false | [valuesUrl: 'notaurl']                       | 'valuesUrl'                    | 'Not a valid URL'
          false | [name: 'in valid']                           | 'name'                         | 'does not match the required pattern'
          false | [remoteUrlAuthenticationType: 'in valid']    | 'remoteUrlAuthenticationType'  | 'must be in the list'
          false | [defaultStoragePath: 'in valid']             | 'defaultStoragePath'           | 'Default key storage path must start with: keys/: in valid'
          false | [hidden: true]                               | 'hidden'                       | 'Hidden options must have a default value'
          false | [valuesType: 'url', enforced:true]           | 'valuesUrl'                    | 'Allowed values (list or remote URL) must be specified if values are enforced'
          false | [valuesType: 'list', enforced:true]          | 'values'                       | 'Allowed values (list or remote URL) must be specified if values are enforced'
          false | [value: 'b',values:['a'], enforced:true]     | 'value'                        | 'Default Value was not in the allowed values list, and values are enforced'
          false | [multivalued:true,delimiter:null]            | 'delimiter'                    | 'You must specify a delimiter for multivalued options'
          false | [value: 'a,b',values:['a'], enforced:true,multivalued:true,delimiter:',']  | 'value'                       | 'Default Value contains a string that was not in the allowed values list, and values are enforced'
          false | [regex:'asdf[']                              | 'regex'                        | 'Invalid Regular Expression:'
          false | [regex:'[a-f]+',value:'z']                   | 'value'                        | 'Default value "z" does not match the regex: [a-f]+'
          false | [regex:'[a-f]+',values:['z']]                | 'values'                       | 'Allowed value "z" does not match the regex: [a-f]+'
          false | [multivalued: true, secure:true]             | 'multivalued'                  | 'Secure input cannot be used with multi-valued input'
          true  | [required: true, optionType:'file']          | 'required'                     | 'File option type cannot be Required when the Job is scheduled'
          true  | [required: true]                             | 'value'                        | 'Specify a Default Value for Required options when the Job is scheduled'
          true  | [valuesUrl:'http://x.com',configRemoteUrl:[jsonFilter:'bad[.']]       | 'configRemoteUrl.jsonFilter'   | 'The Remote URL Json Path Filter has an invalid syntax'
    }
}
