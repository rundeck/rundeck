/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import grails.test.GrailsUnitTestCase
import grails.test.ControllerUnitTestCase


/*
* ScheduledExecutionControllerTests.java
*
* User: greg
* Created: Jun 11, 2008 5:12:47 PM
* $Id$
*/
class ScheduledExecutionControllerTests extends ControllerUnitTestCase {
    void setUp(){
        super.setUp()
        
        loadCodec(org.codehaus.groovy.grails.plugins.codecs.URLCodec)
    }
    void testEmpty(){

    }

    private void assertMap(key, map, value) {
        assertEquals "invalid ${key} ${map[key]}", value, map[key]
    }

    private void assertMap(expected, value) {
        expected.each {k, v ->
            assertMap(k, value, v)
        }
    }

    public void testExpandUrl() {
        mockDomain(ScheduledExecution)
        mockDomain(Option)
        ScheduledExecution se = new ScheduledExecution(jobName: 'blue', groupPath:'some/where',description:'a job',project:'AProject',argString:'-a b -c d',adhocExecution:false)

        final Option option = new Option(name: 'test1', enforced: false)
        se.addToOptions(option)
        se.save()
        assertNotNull(option.properties)
        System.err.println("properties: ${option.properties}");

        ScheduledExecutionController ctrl = new ScheduledExecutionController()
        assertEquals 'test1', ctrl.expandUrl(option, '${option.name}', se)
        assertEquals 'blue', ctrl.expandUrl(option, '${job.name}', se)
        assertEquals 'some%2Fwhere', ctrl.expandUrl(option, '${job.group}', se)
        assertEquals 'a+job', ctrl.expandUrl(option, '${job.description}', se)
        assertEquals 'AProject', ctrl.expandUrl(option, '${job.project}', se)
        assertEquals '-a+b+-c+d', ctrl.expandUrl(option, '${job.argString}', se)
        assertEquals 'false', ctrl.expandUrl(option, '${job.adhoc}', se)
        assertEquals '${job.noexist}', ctrl.expandUrl(option, '${job.noexist}', se)
        assertEquals 'http://test/action?name=blue&option=test1&project=AProject',
            ctrl.expandUrl(option, 'http://test/action?name=${job.name}&option=${option.name}&project=${job.project}', se)

    }

}