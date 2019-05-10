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

package rundeck.controllers

import com.dtolabs.rundeck.app.support.ExtraCommand
import com.dtolabs.rundeck.app.support.RunJobCommand
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.common.NodesSelector
import com.dtolabs.rundeck.core.utils.NodeSet
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.grails.plugins.codecs.URLCodec
import org.grails.plugins.testing.GrailsMockMultipartFile
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import rundeck.*
import rundeck.codecs.URIComponentCodec
import rundeck.services.*
import rundeck.utils.OptionsUtil
import spock.lang.Specification
import spock.lang.Unroll

import javax.security.auth.Subject

/**
 * Created by greg on 7/14/15.
 */
@TestFor(ScheduledExecutionController)
@Mock([ScheduledExecution, Option, Workflow, CommandExec, Execution, JobExec, ReferencedExecution, ScheduledExecutionStats])
class OptionsUtilsSpec extends Specification {

    def setup() {
        mockCodec(URIComponentCodec)
        mockCodec(URLCodec)
    }

    private Map createJobParams(Map overrides=[:]){
        [
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy'])]),
                serverNodeUUID: null,
                scheduled: true
        ]+overrides
    }

    def "expandUrl with project globals"() {
        given:
        Option option = new Option()
        ScheduledExecution job = new ScheduledExecution(createJobParams())
        def optsmap = [:]
        def ishttp = true
        def frameworkService = Mock(FrameworkService)
        OptionsUtil.metaClass.static.getFrameworkServiceInstance = { return frameworkService}
        OptionsUtil.metaClass.static.frameworkServiceInstance = { return frameworkService}

        when:
        def result = OptionsUtil.expandUrl(option, url, job, optsmap, ishttp)

        then:
        expected == result
        1 * frameworkService.getFrameworkNodeName() >> 'anode'
        1 * frameworkService.getProjectGlobals('AProject') >> globals
        1 * frameworkService.getServerUUID()
        0 * frameworkService._(*_)

        where:
        url                                             | globals                           | expected
        ''                                              | [:]                               | ''
        'http://${globals.host}/a/path'                 | [host: 'myhost.com']              | 'http://myhost.com/a/path'
        'http://${globals.host}/a/path/${globals.path}' | [host: 'myhost.com', path: 'x y'] |
                'http://myhost.com/a/path/x%20y'
        'http://${globals.host}/a/path?q=${globals.q}'  | [host: 'myhost.com', q: 'a b']    |
                'http://myhost.com/a/path?q=a+b'


    }
}
