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


import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.grails.plugins.codecs.URLCodec
import org.rundeck.app.data.providers.GormUserDataProvider
import rundeck.*
import rundeck.codecs.URIComponentCodec
import rundeck.services.FrameworkService
import rundeck.utils.OptionsUtil
import spock.lang.Specification
/**
 * Created by greg on 7/14/15.
 */
class OptionsParserUtilsSpec extends Specification implements ControllerUnitTest<ScheduledExecutionController>, DataTest{
    def setup() {
        mockCodec(URIComponentCodec)
        mockCodec(URLCodec)
        mockDomains(ScheduledExecution, Option, Workflow, CommandExec, Execution, JobExec, ReferencedExecution, ScheduledExecutionStats, User)
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
        GormUserDataProvider provider = new GormUserDataProvider()

        when:
        def result = OptionsUtil.expandUrl(option, url, job, provider, optsmap, ishttp)

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

    def "add user email to option context"() {
        setup:
        if(username) {
            new User(login: username, email: email).save()
            User.list()
        }

        Option option = new Option()
        ScheduledExecution job = new ScheduledExecution(createJobParams())
        def optsmap = [:]
        def ishttp = true
        def frameworkService = Mock(FrameworkService)
        OptionsUtil.metaClass.static.getFrameworkServiceInstance = { return frameworkService}
        OptionsUtil.metaClass.static.frameworkServiceInstance = { return frameworkService}
        OptionsUtil.metaClass.static.getHttpSessionInstance = { return null }
        GormUserDataProvider provider = new GormUserDataProvider()

        when:
        def result = OptionsUtil.expandUrl(option, url, job, provider, optsmap, ishttp, username)

        then:
        result == expected

        where:
        url                                                         | username  | email             | expected
        'http://host/user/${job.user.name}?email=${job.user.email}' | "bob"     | "bob@build.it"    | "http://host/user/bob?email=bob%40build.it"
        'http://host/user/${job.user.name}?email=${job.user.email}' | "bob1"    | null              | "http://host/user/bob1?email="
        'http://host/user/${job.user.name}?email=${job.user.email}' | null      | "something@somewhere.com"    | "http://host/user/anonymous?email="
    }
}
