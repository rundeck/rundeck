/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.app.support

import grails.test.mixin.TestFor
import rundeck.controllers.MenuController
import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author greg
 * @since 12/7/17
 */
@TestFor(MenuController)
class PluginResourceReqSpec extends Specification {
    @Unroll
    def "path validation rejection for #path"() {
        def resrequest = new PluginResourceReq(service: 'UI', name: 'a-plugin', path: path)

        when:
        resrequest.validate()
        then:
        resrequest.hasErrors()
        resrequest.errors.hasFieldErrors('path')

        where:
        path                  | _
        'test/not-valid file' | _
        'test/notvalid\''     | _
        'test/.notvalid'      | _
        '.test/notvalid'      | _
        '../test/notvalid'    | _
        'test/../notvalid'    | _
    }

    @Unroll
    def "name validation rejection for #name"() {
        def resrequest = new PluginResourceReq(service: 'UI', name: plugin, path: path)

        when:
        resrequest.validate()
        then:
        resrequest.hasErrors()
        resrequest.errors.hasFieldErrors('name')

        where:
        path              | plugin
        'test/valid-path' | 'invalid/plugin'
        'test/valid-path' | 'invalid plugin'
    }

    @Unroll
    def "service validation rejection for #service"() {
        def resrequest = new PluginResourceReq(service: service, name: 'valid', path: 'test/valid')

        when:
        resrequest.validate()
        then:
        resrequest.hasErrors()
        resrequest.errors.hasFieldErrors('service')

        where:
        service    | _
        'test.bad' | _
        'test bad' | _
        'test/bad' | _
    }

    @Unroll
    def "path validation pass for #path and #plugin"() {
        def resrequest = new PluginResourceReq(service: 'UI', name: plugin, path: path)

        when:
        resrequest.validate()
        then:
        !resrequest.hasErrors()

        where:
        path                                        | plugin
        'test/valid-file'                           | 'a-plugin'
        'test/valid-file'                           | 'a_plugin'
        'test/valid-file'                           | 'a:plugin'
        'test/valid-file.html'                      | 'a-plugin'
        'test/valid-file/012359-+_ASDFZEDasdf.html' | 'a-plugin'
    }
}
