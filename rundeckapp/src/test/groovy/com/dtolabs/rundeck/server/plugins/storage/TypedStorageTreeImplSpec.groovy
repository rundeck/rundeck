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

package com.dtolabs.rundeck.server.plugins.storage

import com.dtolabs.rundeck.core.storage.ResourceMeta
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.Resource
import org.rundeck.storage.api.StorageException
import org.rundeck.storage.api.Tree
import spock.lang.Specification

/**
 * Created by greg on 11/5/15.
 */
class TypedStorageTreeImplSpec extends Specification {
    def "getResourceWithType correct type"() {
        given:
        def tree = Mock(Tree)
        def test = new TypedStorageTreeImpl(tree)
        def path = PathUtil.asPath('a/path')


        when:
        def result = test.getResourceWithType(path, "correct-type")

        then:
        result != null
        tree.getResource(_) >> Mock(Resource) {
            getContents() >> Mock(ResourceMeta) {
                getContentType() >> 'correct-type'
            }
        }
    }

    def "hasResourceWithType"() {
        given:
        def tree = Mock(Tree)
        def test = new TypedStorageTreeImpl(tree)
        def path = PathUtil.asPath('a/path')


        when:
        def result = test.hasResourceWithType(path, "correct-type")

        then:
        result == expected
        tree.hasResource(_) >> hasResource
        tree.getResource(_) >> Mock(Resource) {
            getContents() >> Mock(ResourceMeta) {
                getContentType() >> resultType
            }
        }

        where:
        expected | hasResource | resultType
        true     | true        | 'correct-type'
        false    | false       | 'correct-type'
        false    | true        | 'wrong-type'
    }

    def "readResourceWithType reads content"() {
        given:
        def tree = Mock(Tree)
        def test = new TypedStorageTreeImpl(tree)
        def path = PathUtil.asPath('a/path')


        when:
        def result = test.readResourceWithType(path, "correct-type")

        then:
        result != null
        result == 'abcdef'.bytes
        tree.getResource(_) >> Mock(Resource) {
            getContents() >> Mock(ResourceMeta) {
                getContentType() >> 'correct-type'
                writeContent(_) >> { args ->
                    args[0].write('abcdef'.bytes)
                    return 6L
                }
            }
        }
    }

    def "getResourceWithType wrong type"() {
        given:
        def tree = Mock(Tree)
        def test = new TypedStorageTreeImpl(tree)
        def path = PathUtil.asPath('a/path')


        when:
        def result = test.getResourceWithType(path, "wrong-type")

        then:
        tree.getResource(_) >> Mock(Resource) {
            getContents() >> Mock(ResourceMeta) {
                getContentType() >> 'correct-type'
            }
        }
        WrongContentType exception = thrown()
        exception.path == path
        exception.event == StorageException.Event.READ
    }
}
