package rundeck

import grails.testing.gorm.DataTest
import rundeck.CommandExec
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

import spock.lang.Specification

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue

/*
 * rundeck.CommandExecTests.java
 *
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 5/14/12 11:31 AM
 *
 */
class CommandExecTests extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [CommandExec]
    }

    def testAdhocRemoteStringToMap(){
        when:
        CommandExec t=new CommandExec(adhocRemoteString:'test1')
        then:
        assertEquals([exec:'test1', enabled: true],t.toMap())
    }

    def testAdhocRemoteStringNoArgsToMap(){
        when:
        CommandExec t=new CommandExec(adhocRemoteString:'test1',argString: 'blah')
        then:
        assertEquals([exec:'test1', enabled: true],t.toMap())
    }

    def testAdhocLocalStringToMap(){
        when:
        CommandExec t=new CommandExec(adhocLocalString:'test2')
        then:
        assertEquals([script:'test2', enabled: true],t.toMap())
    }

    def testAdhocLocalStringWithArgsToMap(){
        when:
        CommandExec t=new CommandExec(adhocLocalString:'test2',argString: 'test args')
        then:
        assertEquals([script:'test2',args:'test args', enabled: true],t.toMap())
    }

    def testAdhocFileStringToMap(){
        when:
        CommandExec t=new CommandExec(adhocFilepath:'test3')
        then:
        assertEquals([scriptfile:'test3', enabled: true],t.toMap())
    }

    def testAdhocFileStringExpandTokenInScriptFileToMap(){
        when:
        CommandExec t=new CommandExec(adhocFilepath:'test3', expandTokenInScriptFile: true)
        then:
        assertEquals([scriptfile:'test3', expandTokenInScriptFile: true, enabled: true],t.toMap())
    }

    def testAdhocFileStringWithArgsToMap(){
        when:
        CommandExec t = new CommandExec(adhocFilepath: 'test3',argString: 'test args3')
        then:
        assertEquals([scriptfile: 'test3',args:'test args3', enabled: true], t.toMap())
    }

    def testErrorHandlerExecToMap(){
        when:
        CommandExec h=new CommandExec(adhocRemoteString: 'testerr')
        CommandExec t=new CommandExec(adhocFilepath:'test3',errorHandler: h)
        then:
        assertEquals([scriptfile:'test3',errorhandler:[exec: 'testerr', enabled: true], enabled: true],t.toMap())
    }

    def testErrorHandlerScriptToMap(){
        when:
        CommandExec h = new CommandExec(adhocLocalString: 'testerr',argString: 'err args')
        CommandExec t = new CommandExec(adhocFilepath: 'test3', errorHandler: h)
        then:
        assertEquals([scriptfile: 'test3', errorhandler: [script: 'testerr',args: 'err args', enabled: true], enabled: true], t.toMap())
    }

    def testFileExtensionToMap() {
        when:
        CommandExec t = new CommandExec(adhocLocalString: 'test1', fileExtension: '.ext')
        then:
        assertEquals([script: 'test1',fileExtension:'.ext', enabled: true], t.toMap())
    }

    //test fromMap

    def testExecFromMap(){
        when:
        CommandExec t=CommandExec.fromMap([exec: 'commandstring'])
        assertEquals('commandstring',t.adhocRemoteString)
        then:
        assertNull(t.argString)
        assertNull(t.errorHandler)

        CommandExec t2=CommandExec.fromMap([exec: 'commandstring',args: 'arg string'])
        assertEquals('commandstring',t2.adhocRemoteString)
        assertNull(t.argString)
        assertNull(t2.errorHandler)
    }

    def testScriptFromMap(){
        when:
        CommandExec t=CommandExec.fromMap([script: 'scriptstring'])
        assertEquals('scriptstring',t.adhocLocalString)
        then:
        assertNull(t.argString)
        assertNull(t.errorHandler)

        CommandExec t2 = CommandExec.fromMap([script: 'scriptstring', args: 'arg string'])
        assertEquals('scriptstring', t2.adhocLocalString)
        assertEquals('arg string', t2.argString)
        assertNull(t2.errorHandler)
    }

    def testScriptFileExtensionFromMap(){
        when:
        CommandExec t=CommandExec.fromMap([script: 'scriptstring',fileExtension: 'boogy'])
        assertEquals('scriptstring',t.adhocLocalString)
        then:
        assertEquals('boogy', t.fileExtension)
        assertNull(t.argString)
        assertNull(t.errorHandler)
    }

    def testFileFromMap(){
        when:
        CommandExec t=CommandExec.fromMap([scriptfile: 'scriptfile'])
        assertEquals('scriptfile',t.adhocFilepath)
        then:
        assertNull(t.argString)
        assertNull(t.errorHandler)

        CommandExec t2 = CommandExec.fromMap([scriptfile: 'scriptfile', args: 'arg string'])
        assertEquals('scriptfile', t.adhocFilepath)
        assertEquals('arg string', t2.argString)
        assertNull(t2.errorHandler)

        CommandExec t3 = CommandExec.fromMap([scriptfile: 'scriptfile', args: 'arg string', expandTokenInScriptFile: true])
        assertEquals('scriptfile', t3.adhocFilepath)
        assertEquals('arg string', t3.argString)
        assertTrue(t3.expandTokenInScriptFile)
        assertNull(t3.errorHandler)
    }

    //test createClone

    def testCreateCloneExec(){
        when:
        CommandExec t = new CommandExec(adhocRemoteString: 'test1')
        CommandExec t1=t.createClone()
        then:
        assertEquals('test1',t1.adhocRemoteString)
        assertNull(t1.argString)
    }

    def testCreateCloneExecArgs(){
        when:
        CommandExec t = new CommandExec(adhocRemoteString: 'test1',argString: 'arg string')
        CommandExec t1=t.createClone()
        then:
        assertEquals('test1',t1.adhocRemoteString)
        assertEquals('arg string',t1.argString)
    }

    def testCreateCloneScript() {
        when:
        CommandExec t = new CommandExec(adhocLocalString: 'test1')
        CommandExec t1 = t.createClone()
        then:
        assertEquals('test1', t1.adhocLocalString)
        assertNull(t1.argString)
    }

    def testCreateCloneScriptFileExtension() {
        when:
        CommandExec t = new CommandExec(adhocLocalString: 'test1',fileExtension: 'ext')
        CommandExec t1 = t.createClone()
        then:
        assertEquals('test1', t1.adhocLocalString)
        assertEquals('ext', t1.fileExtension)
        assertNull(t1.argString)
    }

    def testCreateCloneFile() {
        when:
        CommandExec t = new CommandExec(adhocFilepath: 'test1')
        CommandExec t1 = t.createClone()
        then:
        assertEquals('test1', t1.adhocFilepath)
        assertNull(t1.argString)
    }

    def testCreateCloneNoHandler() {
        when:
        CommandExec h = new CommandExec(adhocRemoteString: 'testerr')
        CommandExec t = new CommandExec(adhocFilepath: 'test1',errorHandler: h)
        then:
        CommandExec t1 = t.createClone()
        assertEquals('test1', t1.adhocFilepath)
        assertNull(t1.errorHandler)
    }

    def testCreateCloneKeepgoing() {
        when:
        CommandExec h = new CommandExec(adhocRemoteString: 'testerr',keepgoingOnSuccess: true)
        CommandExec t1 = h.createClone()
        then:
        assertEquals('testerr', t1.adhocRemoteString)
        assertEquals(true, !!t1.keepgoingOnSuccess)
        assertNull(t1.errorHandler)
    }

    def testCreateCloneKeepgoingFalse() {
        when:
        CommandExec h = new CommandExec(adhocRemoteString: 'testerr',keepgoingOnSuccess: false)
        CommandExec t1 = h.createClone()
        then:
        assertEquals('testerr', t1.adhocRemoteString)
        assertEquals(true, !t1.keepgoingOnSuccess)
        assertNull(t1.errorHandler)
    }
}
