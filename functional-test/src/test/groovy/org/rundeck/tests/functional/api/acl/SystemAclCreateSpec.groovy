package org.rundeck.tests.functional.api.acl

import okhttp3.Headers
import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

/**
 * Regression coverage for RUN-4790: creating a system ACL with a YAML request body while
 * asking for a JSON response (as the CLI's API client does) must return a JSON-wrapped body,
 * not raw YAML.
 */
@APITest
class SystemAclCreateSpec extends BaseContainer {

    def "create system acl with yaml body and json accept returns json"() {
        given:
        def aclPath = "run4790-${UUID.randomUUID()}.aclpolicy"
        def yamlBody = '''by:
  group: Run4790Group
description: Allow [read] for project_acl
for:
  project_acl:
  - allow:
    - read
context:
  application: rundeck
'''

        when:
        Integer code
        String contentType
        Map json
        try (def resp = client.doPost(
                "/system/acl/${aclPath}",
                yamlBody,
                'application/yaml',
                Headers.of('Accept', 'application/json')
        )) {
            code = resp.code()
            contentType = resp.header('Content-Type')
            json = client.jsonValue(resp.body(), Map)
        }

        then:
        code == 201
        contentType != null
        contentType.contains('application/json')
        json.contents.contains('Run4790Group')

        cleanup:
        deleteSystemAcl(aclPath)
    }
}
