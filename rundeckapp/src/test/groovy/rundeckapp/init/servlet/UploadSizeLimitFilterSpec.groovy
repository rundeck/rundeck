/*
 * Copyright 2026 Rundeck, Inc. (http://rundeck.com)
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
package rundeckapp.init.servlet

import jakarta.servlet.FilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import rundeck.services.UploadSizeLimitService
import spock.lang.Specification

class UploadSizeLimitFilterSpec extends Specification {

    UploadSizeLimitService limits = Stub(UploadSizeLimitService) {
        keyStorageMaxBytes() >> 10L
        aclMaxBytes() >> 10L
        keyStorageMaxSizeLabel() >> '10B'
        aclMaxSizeLabel() >> '10B'
    }
    UploadSizeLimitFilter filter = new UploadSizeLimitFilter(limits)

    def "targetFor matches key storage and access control uploads"() {
        expect:
        UploadSizeLimitFilter.targetFor(method, path) == target

        where:
        method | path                                              | target
        'POST' | '/api/41/storage/keys/project/app/secret.pem'    | UploadSizeLimitFilter.Target.KEY_STORAGE
        'PUT'  | '/api/41/storage/keys/keys/id_rsa'               | UploadSizeLimitFilter.Target.KEY_STORAGE
        'POST' | '/storage/upload/keys'                           | UploadSizeLimitFilter.Target.KEY_STORAGE
        'GET'  | '/api/41/storage/keys/project/app/secret.pem'    | null
        'POST' | '/menu/acls/save'                                | UploadSizeLimitFilter.Target.ACL
        'POST' | '/project/demo/admin/acls/save'                  | UploadSizeLimitFilter.Target.ACL
        'PUT'  | '/api/41/system/acl/policy.aclpolicy'            | UploadSizeLimitFilter.Target.ACL
        'POST' | '/api/41/project/demo/acl/policy.aclpolicy'      | UploadSizeLimitFilter.Target.ACL
        'POST' | '/api/41/project/demo/jobs/import'               | null
    }

    def "rejects an oversized key storage body before the chain runs"() {
        given:
        def request = new MockHttpServletRequest('POST', '/api/41/storage/keys/project/app/secret.pem')
        request.setContent(new byte[11])
        def response = new MockHttpServletResponse()
        def chain = Mock(FilterChain)

        when:
        filter.doFilter(request, response, chain)

        then:
        response.status == 413
        response.contentAsString.contains('10B')
        response.contentAsString.contains(UploadSizeLimitService.ERROR_CODE)
        0 * chain.doFilter(_, _)
    }

    def "allows a key storage body at the limit"() {
        given:
        def request = new MockHttpServletRequest('POST', '/api/41/storage/keys/project/app/secret.pem')
        request.setContent(new byte[10])
        def response = new MockHttpServletResponse()
        def chain = Mock(FilterChain)

        when:
        filter.doFilter(request, response, chain)

        then:
        response.status == 200
        1 * chain.doFilter(request, response)
    }

    def "multipart requests may include a small envelope over the file limit"() {
        given:
        def request = new MockHttpServletRequest('POST', '/menu/acls/save')
        request.contentType = 'multipart/form-data; boundary=----x'
        request.setContent(new byte[10 + 1024])
        def response = new MockHttpServletResponse()
        def chain = Mock(FilterChain)

        when:
        filter.doFilter(request, response, chain)

        then:
        1 * chain.doFilter(request, response)
    }

    def "multipart requests over the envelope allowance are rejected"() {
        given:
        def request = new MockHttpServletRequest('POST', '/project/demo/admin/acls/save')
        request.contextPath = '/rundeck'
        request.requestURI = '/rundeck/project/demo/admin/acls/save'
        request.contentType = 'multipart/form-data; boundary=----x'
        request.setContent(new byte[(int) (10 + UploadSizeLimitFilter.MULTIPART_ALLOWANCE + 1)])
        def response = new MockHttpServletResponse()
        def chain = Mock(FilterChain)

        when:
        filter.doFilter(request, response, chain)

        then:
        response.status == 413
        0 * chain.doFilter(_, _)
    }
}
