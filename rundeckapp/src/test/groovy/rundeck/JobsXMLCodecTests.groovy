package rundeck

import grails.converters.XML
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.web.ControllerUnitTestMixin
import groovy.xml.MarkupBuilder

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

import org.junit.Before
import org.junit.Test
import org.rundeck.app.components.jobs.JobDefinitionException
import rundeck.codecs.JobsXMLCodec
import rundeck.controllers.JobXMLException

import javax.xml.stream.XMLEventWriter

import static org.junit.Assert.*

/*
 * rundeck.JobsXMLCodecTests.java
 * 
 * User: greg
 * Created: Jun 10, 2009 11:27:54 AM
 * $Id$
 */

class JobsXMLCodecTests {
    
    @Before
    public void setup(){
        // hack for 2.3.9:  https://jira.grails.org/browse/GRAILS-11136
//        defineBeans(new DataBindingGrailsPlugin().doWithSpring)
    }

    /** no joblist */
    def badxml1 = """<wrong>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
      <project>test1</project>
      <type>MyService</type>
      <object>elfblister</object>
      <command>dowait</command>
      <options>
        <option name='delay' value='60' />
      </options>
    </context>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</wrong>
"""
    /** no job */
    def badxml2 = """<joblist>
  <wrong>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
      <project>test1</project>
      <type>MyService</type>
      <object>elfblister</object>
      <command>dowait</command>
      <options>
        <option name='delay' value='60' />
      </options>
    </context>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </wrong>
</joblist>
"""
    /** should fail job 2 missing: sequence */
    def fail2 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description>a simple desc</description>
    <loglevel>INFO</loglevel>
    <context>
      <options>
        <option name='delay' value='60' />
        <option name='monkey' value='bluefish' />
      </options>
    </context>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
      <!-- no sequence -->
  </job>
</joblist>
"""
    /** basic job */
    def okxml0 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
      <project>test1</project>
      <options>
        <option name='delay' value='60' />
        <option name='monkey' value='bluefish' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
  </job>
</joblist>
"""
    /** basic job */
    def okxml1 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description>a simple desc</description>
    <loglevel>INFO</loglevel>
    <scheduleEnabled>false</scheduleEnabled>
    <executionEnabled>false</executionEnabled>
    <group>some/group</group>
    <context>
      <project>test1</project>
      <options>
        <option name='delay' value='60' />
        <option name='monkey' value='bluefish' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
  </job>
</joblist>
"""

    @Test
    void testDecodeBasic_InvalidJoblist() {

        try {
            JobsXMLCodec.decode(badxml1)
            fail "Parsing shouldn't complete"
        } catch (JobDefinitionException e) {
            assertNotNull e
        }
    }
    @Test
    void testDecodeBasic_invalid_no_job() {
        try {
            JobsXMLCodec.decode(badxml2)
            fail "Parsing shouldn't complete"
        } catch (JobDefinitionException e) {
            assertNotNull e
        }
    }
    @Test
    void testDecodeBasic_invalid_job_no_sequence() {
        try {
            JobsXMLCodec.decode(fail2)
            fail "Parsing shouldn't complete"
        } catch (JobXMLException e) {
            assertNotNull e
            assertEquals "failed: ${e.getMessage()}", "'sequence' element not found", e.getMessage()
        }
    }
    @Test
    void testDecodeBasic() {
        def jobs = JobsXMLCodec.decode(okxml0)
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()
        assertEquals "incorrect jobName", "wait1", jobs[0].jobName
        assertEquals "incorrect description", "", jobs[0].description
        assertEquals "incorrect loglevel", "INFO", jobs[0].loglevel
        assertEquals "incorrect project", 'test1', jobs[0].project
        assertNotNull jobs[0].options
        assertEquals 2, jobs[0].options.size()
        def iter = jobs[0].options.iterator()
        def opt1 = iter.next()
        assertEquals 'delay', opt1.name
        assertEquals '60', opt1.defaultValue
        def opt2 = iter.next()
        assertEquals 'monkey', opt2.name
        assertEquals 'bluefish', opt2.defaultValue
        assertFalse "incorrect doNodedispatch: ${jobs[0].doNodedispatch}", jobs[0].doNodedispatch
        assertEquals "incorrect nodeThreadcount", 1, jobs[0].nodeThreadcount
        assertFalse "incorrect nodeKeepgoing", jobs[0].nodeKeepgoing
        assertNull "incorrect groupPath", jobs[0].groupPath

        assertEquals "incorrect scheduled", "false", jobs[0].scheduled.toString()

    }
    @Test
    void testDecodeBasic_group() {
        def jobs = JobsXMLCodec.decode(okxml1)
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()
        assertEquals "incorrect groupPath", "some/group", jobs[0].groupPath
    }
    void testDecodeBasicOk1(){
        /** basic job */
    def basic2 = """<joblist>
  <job>
    <id>8</id>
    <name>punch2</name>
    <description>dig it potato</description>
    <loglevel>WARN</loglevel>
    <group>simple</group>
    <context>
      <project>zig</project>
      <options>
        <option name='clip' value='true' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>2</threadcount>
      <keepgoing>true</keepgoing>
    </dispatch>
  </job>
</joblist>
"""
            def jobs = JobsXMLCodec.decode(basic2)
            assertNotNull jobs
            assertEquals "incorrect size",1,jobs.size()
            assertEquals "incorrect jobName","punch2",jobs[0].jobName
            assertEquals "incorrect description","dig it potato",jobs[0].description
            assertEquals "incorrect loglevel","WARN",jobs[0].loglevel
            assertEquals "incorrect project",'zig',jobs[0].project
            assertNotNull "incorrect command.option.clip",jobs[0].options
            assertEquals "incorrect command.option.clip",1,jobs[0].options.size()
            final def opt1opt3 = jobs[0].options.iterator().next()
            assertEquals "incorrect command.option.clip",'clip',opt1opt3.name
            assertEquals "incorrect command.option.clip",'true',opt1opt3.defaultValue
            assertFalse "incorrect doNodedispatch",jobs[0].doNodedispatch
            assertEquals "incorrect nodeThreadcount",2,jobs[0].nodeThreadcount
            assertTrue "incorrect nodeKeepgoing",jobs[0].nodeKeepgoing
            assertEquals "incorrect groupPath","simple",jobs[0].groupPath

            assertFalse "incorrect scheduled",jobs[0].scheduled

    }
    void testDecodeBasic_normalized_group(){

    /** basic job  - make group value have leading/trailing "/" characters, assert they are normalized. */
    def basic3 = """<joblist>
  <job>
    <id>8</id>
    <name>punch2</name>
    <description>dig it potato</description>
    <loglevel>WARN</loglevel>
    <group>simple/</group>
    <context>
      <project>zig</project>
      <options>
        <option name='clip' value='true' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>2</threadcount>
      <keepgoing>true</keepgoing>
    </dispatch>
  </job>
</joblist>
"""
            def jobs = JobsXMLCodec.decode(basic3)
            assertNotNull jobs
            assertEquals "incorrect groupPath","simple",jobs[0].groupPath

    }
    @Test
    void testDecodeBasic_normalized_group2(){
    /** basic job  - make group value have leading/trailing "/" characters, assert they are normalized. */
    def basic4 = """<joblist>
  <job>
    <id>8</id>
    <name>punch2</name>
    <description>dig it potato</description>
    <loglevel>WARN</loglevel>
    <group>/simple</group>
    <context>
      <project>zig</project>
      <options>
        <option name='clip' value='true' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>2</threadcount>
      <keepgoing>true</keepgoing>
    </dispatch>
  </job>
</joblist>
"""
            def jobs = JobsXMLCodec.decode(basic4)
            assertNotNull jobs
            assertEquals "incorrect groupPath","simple",jobs[0].groupPath

    }
    @Test
    void testDecodeBasic_normalized_group3() {
        /** basic job  - make group value have leading/trailing "/" characters, assert they are normalized. */
        def basic5 = """<joblist>
  <job>
    <id>8</id>
    <name>punch2</name>
    <description>dig it potato</description>
    <loglevel>WARN</loglevel>
    <group>this/is/a/simple/path/</group>
    <context>
      <project>zig</project>
      <options>
        <option name='clip' value='true' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>2</threadcount>
      <keepgoing>true</keepgoing>
    </dispatch>
  </job>
</joblist>
"""
        def jobs = JobsXMLCodec.decode(basic5)
        assertNotNull jobs
        assertEquals "incorrect groupPath", "this/is/a/simple/path", jobs[0].groupPath
    }
    @Test
    void testDecodeBasic_normalized_group4() {
        /** basic job  - make group value have leading/trailing "/" characters, assert they are normalized. */
        def basic6 = """<joblist>
  <job>
    <id>8</id>
    <name>punch2</name>
    <description>dig it potato</description>
    <loglevel>WARN</loglevel>
    <group>//</group>
    <context>
      <project>zig</project>
      <options>
        <option name='clip' value='true' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>2</threadcount>
      <keepgoing>true</keepgoing>
    </dispatch>
  </job>
</joblist>
"""
        def jobs = JobsXMLCodec.decode(basic6)
        assertNotNull jobs
        assertNull "incorrect groupPath", jobs[0].groupPath
    }
    @Test
void testDecodeBasic__no_group(){
        def basic7 = """<joblist>
  <job>
    <id>8</id>
    <name>punch2</name>
    <description>dig it potato</description>
    <loglevel>WARN</loglevel>
    <context>
      <project>zig</project>
      <options>
        <option name='clip' value='true' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>2</threadcount>
      <keepgoing>true</keepgoing>
    </dispatch>
  </job>
</joblist>
"""
            def jobs = JobsXMLCodec.decode(basic7)
            assertNotNull jobs
            assertNull "incorrect groupPath",jobs[0].groupPath
    }
    @Test
    public void testDecodeTimeout(){
        /** basic job */
        def jobs = JobsXMLCodec.decode("""<joblist>
  <job>
    <id>8</id>
    <name>punch2</name>
    <timeout>20m</timeout>
    <description>dig it potato</description>
    <loglevel>WARN</loglevel>
    <group>simple</group>
    <context>
      <project>zig</project>
      <options>
        <option name='clip' value='true' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>2</threadcount>
      <keepgoing>true</keepgoing>
    </dispatch>
  </job>
</joblist>
""")
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()
        assertEquals "incorrect jobName", "punch2", jobs[0].jobName
        assertEquals "incorrect jobName", "20m", jobs[0].timeout
    }
    @Test
    public void testDecodeBasic2(){

        def xml = """<joblist>
  <job>
    <id>8</id>
    <name>punch2</name>
    <description>         dig it potato

* list item
* list item2

&lt;b&gt;inline html&lt;/b&gt;
    </description>
    <loglevel>WARN</loglevel>
    <context>
      <project>zig</project>
      <options>
        <option name='clip' value='true' />
      </options>
    </context>
    <sequence>
        <command>
        <exec>true</exec>
        </command>
        <command>
        <exec>false</exec>
        </command>
        <command>
        <exec>0</exec>
        </command>
        <command>
        <script>true</script>
            <scriptargs>true</scriptargs>
        </command>
        <command>
        <script>false</script>
            <scriptargs>false</scriptargs>
        </command>
        <command>
        <script>0</script>
            <scriptargs>0</scriptargs>
        </command>
        <command>
            <scriptfile>false</scriptfile>
            <scriptargs>false</scriptargs>
            <errorhandler  keepgoingOnSuccess='false'>
                <scriptfile>false</scriptfile>
                <scriptargs>0</scriptargs>
            </errorhandler>
        </command>
        <command>
            <jobref>
            <name>false</name>
            <group>false</group>
            <arg line="123"/>
            </jobref>
        </command>
    </sequence>
    <dispatch>
      <threadcount>2</threadcount>
      <keepgoing>true</keepgoing>
    </dispatch>
  </job>
</joblist>
"""
        def jobs = JobsXMLCodec.decode(xml)
        assertNotNull jobs
        assertEquals  '         dig it potato\n' +
                              '\n' +
                              '* list item\n' +
                              '* list item2\n' +
                              '\n' +
                              '<b>inline html</b>' +
                '\n    ', jobs[0].description
        assertEquals  8, jobs[0].workflow.commands.size()
        assertEquals 'true', jobs[0].workflow.commands[0].adhocRemoteString
        assertEquals 'false', jobs[0].workflow.commands[1].adhocRemoteString
        assertEquals '0', jobs[0].workflow.commands[2].adhocRemoteString
        assertEquals 'true', jobs[0].workflow.commands[3].adhocLocalString
        assertEquals 'true', jobs[0].workflow.commands[3].argString
        assertEquals 'false', jobs[0].workflow.commands[4].adhocLocalString
        assertEquals 'false', jobs[0].workflow.commands[4].argString
        assertEquals '0', jobs[0].workflow.commands[5].adhocLocalString
        assertEquals '0', jobs[0].workflow.commands[5].argString

        assertEquals 'false', jobs[0].workflow.commands[6].adhocFilepath
        assertEquals 'false', jobs[0].workflow.commands[6].argString
        assertEquals 'false', jobs[0].workflow.commands[6].errorHandler.adhocFilepath
        assertEquals '0', jobs[0].workflow.commands[6].errorHandler.argString

        assertEquals 'false', jobs[0].workflow.commands[7].jobName
        assertEquals 'false', jobs[0].workflow.commands[7].jobGroup
        assertEquals '123', jobs[0].workflow.commands[7].argString
    }
    @Test
    public void testDecodeLoglimitNoStatus(){

        def xml = """<joblist>
  <job>
    <id>8</id>
    <name>punch2</name>
    <description>dig it potato</description>
    <loglevel>WARN</loglevel>

    <!-- test logging -->
    <logging limit="20MB" limitAction="halt"/>

    <context>
      <project>zig</project>
      <options>
        <option name='clip' value='true' />
      </options>
    </context>
    <sequence>
        <command>
        <exec>true</exec>
        </command>
    </sequence>
    <dispatch>
      <threadcount>2</threadcount>
      <keepgoing>true</keepgoing>
    </dispatch>
  </job>
</joblist>
"""
        def jobs = JobsXMLCodec.decode(xml)
        assertNotNull jobs
        assertEquals '20MB', jobs[0].logOutputThreshold
        assertEquals 'halt', jobs[0].logOutputThresholdAction
        assertEquals 'failed', jobs[0].logOutputThresholdStatus
    }
    @Test
    public void testDecodeLoglimitCustomStatus(){

        def xml = """<joblist>
  <job>
    <id>8</id>
    <name>punch2</name>
    <description>dig it potato</description>
    <loglevel>WARN</loglevel>

    <!-- test logging -->
    <logging limit="20MB" limitAction="halt" status="mystatus"/>

    <context>
      <project>zig</project>
      <options>
        <option name='clip' value='true' />
      </options>
    </context>
    <sequence>
        <command>
        <exec>true</exec>
        </command>
    </sequence>
    <dispatch>
      <threadcount>2</threadcount>
      <keepgoing>true</keepgoing>
    </dispatch>
  </job>
</joblist>
"""
        def jobs = JobsXMLCodec.decode(xml)
        assertNotNull jobs
        assertEquals '20MB', jobs[0].logOutputThreshold
        assertEquals 'halt', jobs[0].logOutputThresholdAction
        assertEquals 'mystatus', jobs[0].logOutputThresholdStatus
    }
    @Test
    public void testDecodeWithoutProject(){

        def xml = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
      <options>
        <option name='delay' value='60' />
        <option name='monkey' value='bluefish' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
  </job>
</joblist>
"""
        def jobs = JobsXMLCodec.decode(xml)
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()
        assertEquals "incorrect jobName", "wait1", jobs[0].jobName
        assertEquals "incorrect description", "", jobs[0].description
        assertEquals "incorrect loglevel", "INFO", jobs[0].loglevel
        assertEquals "incorrect project", null, jobs[0].project
        assertNotNull jobs[0].options
        assertEquals 2, jobs[0].options.size()
        def iter = jobs[0].options.iterator()
        def opt1 = iter.next()
        assertEquals 'delay', opt1.name
        assertEquals '60', opt1.defaultValue
        def opt2 = iter.next()
        assertEquals 'monkey', opt2.name
        assertEquals 'bluefish', opt2.defaultValue
        assertFalse "incorrect doNodedispatch: ${jobs[0].doNodedispatch}", jobs[0].doNodedispatch
        assertEquals "incorrect nodeThreadcount", 1, jobs[0].nodeThreadcount
        assertFalse "incorrect nodeKeepgoing", jobs[0].nodeKeepgoing
        assertNull "incorrect groupPath", jobs[0].groupPath

        assertEquals "incorrect scheduled", "false", jobs[0].scheduled.toString()
    }
    @Test
    public void testDecodeWithProject(){
        def jobs = JobsXMLCodec.decode(okxml1)
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()
        assertEquals "incorrect jobName", "wait1", jobs[0].jobName
        assertEquals "incorrect description", "a simple desc", jobs[0].description
        assertEquals "incorrect loglevel", "INFO", jobs[0].loglevel
        assertEquals "incorrect project", 'test1', jobs[0].project
        assertNotNull jobs[0].options
        assertEquals 2, jobs[0].options.size()
        def iter2 = jobs[0].options.iterator()
        def opt1_1 = iter2.next()
        assertEquals 'delay', opt1_1.name
        assertEquals '60', opt1_1.defaultValue
        def opt2_2 = iter2.next()
        assertEquals 'monkey', opt2_2.name
        assertEquals 'bluefish', opt2_2.defaultValue
        assertFalse "incorrect doNodedispatch: ${jobs[0].doNodedispatch}", jobs[0].doNodedispatch
        assertEquals "incorrect nodeThreadcount", 1, jobs[0].nodeThreadcount
        assertFalse "incorrect nodeKeepgoing", jobs[0].nodeKeepgoing
        assertEquals "incorrect groupPath", "some/group", jobs[0].groupPath

        assertFalse "incorrect scheduled", jobs[0].scheduled
    }
    @Test
    public void testDecodeStepDescription(){

        def xml = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
      <options>
        <option name='delay' value='60' />
        <option name='monkey' value='bluefish' />
      </options>
    </context>
    <sequence>
        <command>
            <description>a test1</description>
            <exec>test</exec>
        </command>
        <command>
            <jobref>
                <name>false</name>
                <group>false</group>
                <arg line="123"/>
            </jobref>
            <description>a test2</description>
        </command>
        <command>
            <step-plugin type="blah">
                <configuration>
                    <entry key="elf" value="cheese"/>
                </configuration>
            </step-plugin>
            <description>a test3</description>
        </command>
        <command>
            <node-step-plugin type="blah">
                <configuration>
                    <entry key="elf" value="cheese"/>
                </configuration>
            </node-step-plugin>
            <description>a test4</description>
        </command>
    </sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
  </job>
</joblist>
"""
        def jobs = JobsXMLCodec.decode(xml)
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()
        assertEquals "incorrect steps",4, jobs[0].workflow.commands.size()
        jobs[0].workflow.commands.eachWithIndex{v,i->
            assertEquals ("a test${i+1}".toString(),v.description)
        }
    }
    @Test
    public void testDecodeBasicScriptInterpreter(){

        def xml = """<joblist>
  <job>
    <id>8</id>
    <name>punch2</name>
    <description>dig it potato</description>
    <loglevel>WARN</loglevel>
    <context>
      <project>zig</project>
      <options>
        <option name='clip' value='true' />
      </options>
    </context>
    <sequence>
        <command>
            <script>true</script>
            <scriptargs>true</scriptargs>
        </command>
        <command>
            <script>true</script>
            <scriptinterpreter>bash -c</scriptinterpreter>
            <scriptargs>true</scriptargs>
        </command>
        <command>
            <script>false</script>
            <scriptinterpreter argsquoted="true">bash -c</scriptinterpreter>
            <scriptargs>false</scriptargs>
        </command>
        <command>
            <script>0</script>
            <scriptinterpreter argsquoted="false">bash -c</scriptinterpreter>
            <scriptargs>0</scriptargs>
        </command>
        <command>
            <scriptfile>false</scriptfile>
            <scriptargs>false</scriptargs>
            <scriptinterpreter argsquoted="false">bash -c</scriptinterpreter>
            <errorhandler  keepgoingOnSuccess='false'>
                <scriptfile>false</scriptfile>
                <scriptargs>0</scriptargs>
            </errorhandler>
        </command>
    </sequence>
    <dispatch>
      <threadcount>2</threadcount>
      <keepgoing>true</keepgoing>
    </dispatch>
  </job>
</joblist>
"""
        def jobs = JobsXMLCodec.decode(xml)
        assertNotNull jobs
        assertEquals  5, jobs[0].workflow.commands.size()

        assertEquals 'true', jobs[0].workflow.commands[0].adhocLocalString
        assertEquals 'true', jobs[0].workflow.commands[0].argString
        assertEquals null, jobs[0].workflow.commands[0].scriptInterpreter
        assertEquals false, !!jobs[0].workflow.commands[0].interpreterArgsQuoted

        assertEquals 'true', jobs[0].workflow.commands[1].adhocLocalString
        assertEquals 'true', jobs[0].workflow.commands[1].argString
        assertEquals 'bash -c', jobs[0].workflow.commands[1].scriptInterpreter
        assertEquals false, !!jobs[0].workflow.commands[1].interpreterArgsQuoted

        assertEquals 'false', jobs[0].workflow.commands[2].adhocLocalString
        assertEquals 'false', jobs[0].workflow.commands[2].argString
        assertEquals 'bash -c', jobs[0].workflow.commands[2].scriptInterpreter
        assertEquals true, !!jobs[0].workflow.commands[2].interpreterArgsQuoted

        assertEquals '0', jobs[0].workflow.commands[3].adhocLocalString
        assertEquals '0', jobs[0].workflow.commands[3].argString
        assertEquals 'bash -c', jobs[0].workflow.commands[3].scriptInterpreter
        assertEquals false, !!jobs[0].workflow.commands[3].interpreterArgsQuoted

        assertEquals 'false', jobs[0].workflow.commands[4].adhocFilepath
        assertEquals 'false', jobs[0].workflow.commands[4].argString
        assertEquals 'false', jobs[0].workflow.commands[4].errorHandler.adhocFilepath
        assertEquals 'bash -c', jobs[0].workflow.commands[4].scriptInterpreter
        assertEquals false, !!jobs[0].workflow.commands[4].interpreterArgsQuoted
        assertEquals '0', jobs[0].workflow.commands[4].errorHandler.argString

    }
    /**
     * Empty options declaration
     */
    @Test
    void testDecodeEmptyOptions(){
        def xml= """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description>a simple desc</description>
    <loglevel>INFO</loglevel>
    <group>some/group</group>
    <context>
      <project>test1</project>
      <options/>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
  </job>
</joblist>
"""

        def jobs = JobsXMLCodec.decode(xml)
        assertNotNull jobs
        assertNull "incorrect options", jobs[0].options
    }
    /**
     * Options contains text content
     */
    @Test
    void testDecodeInvalidOptions(){
        def xml= """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description>a simple desc</description>
    <loglevel>INFO</loglevel>
    <group>some/group</group>
    <context>
      <project>test1</project>
      <options>asdf</options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
  </job>
</joblist>
"""

        try {
            def jobs = JobsXMLCodec.decode(xml)
            fail "Should not succeed"
        } catch (JobXMLException e) {
            assertNotNull e
        }
    }
    @Test
    void testDecodeNodedispatchEmptyThreadcount(){
        def basic7 = """<joblist>
  <job>
    <id>8</id>
    <name>punch2</name>
    <description>dig it potato</description>
    <loglevel>WARN</loglevel>
    <context>
      <project>zig</project>
      <options>
        <option name='clip' value='true' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
     <dispatch>
      <threadcount/>
      <keepgoing>false</keepgoing>
      <excludePrecedence>false</excludePrecedence>
      <rankOrder>ascending</rankOrder>
    </dispatch>
  </job>
</joblist>
"""
        def jobs = JobsXMLCodec.decode(basic7)
        assertNotNull jobs
        assertNotNull "incorrect nodeThreadcount", jobs[0].nodeThreadcount
        assertEquals "incorrect nodeThreadcount", 1, jobs[0].nodeThreadcount
    }

    @Test
    void testDecodeErrorhandler(){
        def basic7 = """<joblist>
  <job>
    <id>8</id>
    <name>punch2</name>
    <description>dig it potato</description>
    <loglevel>WARN</loglevel>
    <context>
      <project>zig</project>
      <options>
        <option name='clip' value='true' />
      </options>
    </context>
    <sequence>
        <command>
            <exec>test</exec>
            <errorhandler>
                <exec>testerr</exec>
            </errorhandler>
        </command>
        <command>
            <script>test2</script>
            <scriptargs>blah blah</scriptargs>
            <errorhandler>
                <script>test2err</script>
                <scriptargs>blah blah err</scriptargs>
            </errorhandler>
        </command>
        <command>
            <scriptfile>test3</scriptfile>
            <scriptargs>blah3 blah3</scriptargs>
            <errorhandler  keepgoingOnSuccess='false'>
                <scriptfile>test3err</scriptfile>
                <scriptargs>blah3 blah3 err</scriptargs>
            </errorhandler>
        </command>
        <command>
            <jobref name="test" group="group"/>
            <errorhandler keepgoingOnSuccess='true'>
                <jobref name="testerr" group="grouperr">
                    <arg line="line err"/>
                </jobref>
            </errorhandler>
        </command>
        <command>
            <step-plugin type="blah">
                <configuration>
                    <entry key="elf" value="cheese"/>
                </configuration>
            </step-plugin>
            <errorhandler keepgoingOnSuccess='true'>
                <node-step-plugin type="blah2">
                    <configuration>
                        <entry key="rice" value="pilaf"/>
                    </configuration>
                </node-step-plugin>
            </errorhandler>
        </command>
    </sequence>
    <dispatch>
      <threadcount>2</threadcount>
      <keepgoing>true</keepgoing>
    </dispatch>
  </job>
</joblist>
"""
        def jobs = JobsXMLCodec.decode(basic7)
        assertNotNull jobs
        assertEquals 1, jobs.size()
        ScheduledExecution se=jobs[0]
        assertEquals(5,jobs[0].workflow.commands.size())
        jobs[0].workflow.commands.each{
            assertNotNull(it.errorHandler)
        }
        assertEquals('testerr', jobs[0].workflow.commands[0].errorHandler.adhocRemoteString)
        assertNull(jobs[0].workflow.commands[0].errorHandler.argString)
        assertFalse(jobs[0].workflow.commands[0].errorHandler.keepgoingOnSuccess)

        assertEquals('test2err', jobs[0].workflow.commands[1].errorHandler.adhocLocalString)
        assertEquals('blah blah err',jobs[0].workflow.commands[1].errorHandler.argString)
        assertFalse(jobs[0].workflow.commands[1].errorHandler.keepgoingOnSuccess)

        assertEquals('test3err', jobs[0].workflow.commands[2].errorHandler.adhocFilepath)
        assertEquals('blah3 blah3 err',jobs[0].workflow.commands[2].errorHandler.argString)
        assertFalse(jobs[0].workflow.commands[2].errorHandler.keepgoingOnSuccess)

        assertEquals('testerr', jobs[0].workflow.commands[3].errorHandler.jobName)
        assertEquals('grouperr', jobs[0].workflow.commands[3].errorHandler.jobGroup)
        assertEquals('line err',jobs[0].workflow.commands[3].errorHandler.argString)
        assertNotNull(jobs[0].workflow.commands[3].errorHandler.keepgoingOnSuccess)
        assertTrue(jobs[0].workflow.commands[3].errorHandler.keepgoingOnSuccess)

        assertTrue( jobs[0].workflow.commands[4].errorHandler.nodeStep)
        assertEquals('blah2', jobs[0].workflow.commands[4].errorHandler.type)
        assertEquals([rice:'pilaf'],jobs[0].workflow.commands[4].errorHandler.configuration)
        assertNotNull(jobs[0].workflow.commands[4].errorHandler.keepgoingOnSuccess)
        assertTrue(jobs[0].workflow.commands[4].errorHandler.keepgoingOnSuccess)
    }

    @Test
    void testDecodeExample(){
        def example1 = """<joblist>
  <job>
    <id>1</id>
    <name>XYZ Monthly WNP Report</name>
    <description />
    <loglevel>VERBOSE</loglevel>
    <context>
      <project>demo</project>
      <!--
      <type>shellcommands</type>
      <command>SLA_Report</command>
      -->
    </context>
    <sequence><command><exec>cd /home/test/nagios_sla_report/1.0.9 &amp;&amp;
    export ORACLE_HOME=/tools/oracle &amp;&amp; export
    LD_LIBRARY_PATH=/tools/oracle/lib &amp;&amp; /usr/bin/env
    python run_monthly.py test-prod</exec></command></sequence>
    <nodefilters excludeprecedence="true">
      <include>
        <hostname>cypress.hill.com</hostname>
        <tags />
        <os-name />
        <os-family />
        <os-arch />
        <os-version />
        <name />
      </include>
    </nodefilters>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
  </job>
</joblist>
"""
            def jobs = JobsXMLCodec.decode(example1)
            assertNotNull jobs
            assertNull "incorrect groupPath",jobs[0].groupPath
            assertEquals "incorrect nodeExcludePrecedence","true",jobs[0].nodeExcludePrecedence.toString()
            assertEquals "incorrect nodeInclude",null,jobs[0].nodeInclude
            assertEquals "incorrect nodeInclude","hostname: cypress.hill.com",jobs[0].filter
            assertEquals "incorrect project",'demo',jobs[0].project
    }

    @Test
    void testDecodeStringsShouldNotBeBoolean() {
        def example1 = """<joblist>
  <job>
    <id>1</id>
    <name>false</name>
    <description >false</description>
    <loglevel>VERBOSE</loglevel>
    <group>false</group>
    <context>
      <project>proj1</project>
      <options>
        <option name="false" enforcedvalues="false"/>
        <option required="false" name="x" value="9000636026"/>
      </options>
    </context>
    <sequence keepgoing="false">
        <command>
            <exec>false</exec>
            <errorhandler keepgoingOnSuccess="false">
                <script>false</script>
                <scriptargs>false</scriptargs>
            </errorhandler>
        </command>
    </sequence>
    <nodefilters excludeprecedence="false">
      <include>
        <hostname>false</hostname>
        <tags />
        <os-name />
        <os-family />
        <os-arch />
        <os-version />
        <name />
      </include>
    </nodefilters>
    <dispatch>
      <threadcount>2</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
  </job>
</joblist>
"""
        def jobs = JobsXMLCodec.decode(example1)
        assertNotNull jobs
        assertEquals "false",jobs[0].jobName
        assertEquals  "false",jobs[0].groupPath
        assertEquals  "false",jobs[0].description
        assertEquals  false, jobs[0].nodeExcludePrecedence
        assertEquals  false, jobs[0].nodeKeepgoing
        assertEquals  null, jobs[0].nodeInclude
        assertEquals  "hostname: false", jobs[0].filter
        assertEquals  'proj1',jobs[0].project
        assertEquals  1, jobs[0].workflow.commands.size()
        assertEquals  "false", jobs[0].workflow.commands[0].adhocRemoteString
        assertEquals  "false", jobs[0].workflow.commands[0].errorHandler.adhocLocalString
        assertEquals  "false", jobs[0].workflow.commands[0].errorHandler.argString
        assertEquals  false, jobs[0].workflow.commands[0].errorHandler.keepgoingOnSuccess
        assertEquals  2, jobs[0].nodeThreadcount
        assertEquals  false, jobs[0].workflow.keepgoing
        assertEquals 2, jobs[0].options.size()
        def opts=new ArrayList(jobs[0].options)
        assertEquals 'false', opts[0].name
        assertEquals false, opts[0].enforced
        assertEquals 'x', opts[1].name
        assertEquals false, opts[1].required
        assertEquals '9000636026', opts[1].defaultValue
    }
    @Test
    void testDecodeNodefilter() {
        /** node filter job */
        def filter1 = """<joblist>
  <job>
    <id>8</id>
    <name>punch2</name>
    <description>dig it potato</description>
    <loglevel>WARN</loglevel>
    <group>simple</group>
    <context>
      <project>zig</project>
      <options>
        <option name='clip' value='true' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <nodefilters>
        <include>
            <hostname>centos5</hostname>            
        </include>
    </nodefilters>
    <dispatch>
      <threadcount>2</threadcount>
      <keepgoing>true</keepgoing>
    </dispatch>
  </job>
</joblist>
"""
        def jobs = JobsXMLCodec.decode(filter1)
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()
        assertEquals "incorrect nodefilter nodeInclude", "hostname: centos5", jobs[0].filter
        assertEquals "incorrect nodefilter nodeInclude", null, jobs[0].nodeInclude
        assertEquals "incorrect nodefilter nodeIncludeTags", null, jobs[0].nodeIncludeTags
        assertEquals "incorrect nodefilter nodeIncludeOsName", null, jobs[0].nodeIncludeOsName
        assertEquals "incorrect nodefilter nodeIncludeOsFamily", null, jobs[0].nodeIncludeOsFamily
        assertEquals "incorrect nodefilter nodeIncludeOsArch", null, jobs[0].nodeIncludeOsArch
        assertEquals "incorrect nodefilter nodeIncludeOsVersion", null, jobs[0].nodeIncludeOsVersion
        assertEquals "incorrect nodefilter nodeIncludeName", null, jobs[0].nodeIncludeName

        assertEquals "incorrect nodefilter nodeExclude", null, jobs[0].nodeExclude
        assertEquals "incorrect nodefilter nodeExcludeTags", null, jobs[0].nodeExcludeTags
        assertEquals "incorrect nodefilter nodeExcludeOsName", null, jobs[0].nodeExcludeOsName
        assertEquals "incorrect nodefilter nodeExcludeOsFamily", null, jobs[0].nodeExcludeOsFamily
        assertEquals "incorrect nodefilter nodeExcludeOsArch", null, jobs[0].nodeExcludeOsArch
        assertEquals "incorrect nodefilter nodeExcludeOsVersion", null, jobs[0].nodeExcludeOsVersion
        assertEquals "incorrect nodefilter nodeExcludeName", null, jobs[0].nodeExcludeName
        assertTrue "incorrect nodefilter nodeExcludePrecedence ", jobs[0].nodeExcludePrecedence
        assertTrue "incorrect nodefilter doNodedispatch", jobs[0].doNodedispatch
        assertTrue "incorrect nodefilter nodesSelectedByDefault", jobs[0].nodesSelectedByDefault
    }

    @Test
    void testDecodeNodefilter2() {
        /** node filter job */
    def filter2 = """<joblist>
  <job>
    <id>8</id>
    <name>punch2</name>
    <description>dig it potato</description>
    <loglevel>WARN</loglevel>
    <group>simple</group>
    <context>
      <project>zig</project>
      <options>
        <option name='clip' value='true' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <nodefilters>
        <include>
            <hostname>centos5</hostname>
            <tags>a+b,c</tags>
            <os-name>Win.*</os-name>
            <os-family>windows</os-family>
            <os-arch>x86,sparc</os-arch>
            <os-version>4\\..*</os-version>
            <name>mynodename</name>
        </include>
    </nodefilters>
    <dispatch>
      <threadcount>2</threadcount>
      <keepgoing>true</keepgoing>
    </dispatch>
  </job>
</joblist>
"""
            def jobs = JobsXMLCodec.decode(filter2)
            assertNotNull jobs
            assertEquals "incorrect size",1,jobs.size()
            assertEquals "incorrect nodefilter nodeInclude","hostname: centos5 tags: a+b,c os-name: Win.* os-family: windows os-arch: x86,sparc os-version: 4\\..* name: mynodename",jobs[0].filter
            assertEquals "incorrect nodefilter nodeInclude",null,jobs[0].nodeInclude
            assertEquals "incorrect nodefilter nodeIncludeTags", null,jobs[0].nodeIncludeTags
            assertEquals "incorrect nodefilter nodeIncludeOsName", null,jobs[0].nodeIncludeOsName
            assertEquals "incorrect nodefilter nodeIncludeOsFamily", null,jobs[0].nodeIncludeOsFamily
            assertEquals "incorrect nodefilter nodeIncludeOsArch", null,jobs[0].nodeIncludeOsArch
            assertEquals "incorrect nodefilter nodeIncludeOsVersion", null,jobs[0].nodeIncludeOsVersion
            assertEquals "incorrect nodefilter nodeIncludeName", null,jobs[0].nodeIncludeName

            assertEquals "incorrect nodefilter nodeExclude",null,jobs[0].nodeExclude
            assertEquals "incorrect nodefilter nodeExcludeTags",null,jobs[0].nodeExcludeTags
            assertEquals "incorrect nodefilter nodeExcludeOsName",null,jobs[0].nodeExcludeOsName
            assertEquals "incorrect nodefilter nodeExcludeOsFamily",null,jobs[0].nodeExcludeOsFamily
            assertEquals "incorrect nodefilter nodeExcludeOsArch",null,jobs[0].nodeExcludeOsArch
            assertEquals "incorrect nodefilter nodeExcludeOsVersion",null,jobs[0].nodeExcludeOsVersion
            assertEquals "incorrect nodefilter nodeExcludeName",null,jobs[0].nodeExcludeName
            assertTrue "incorrect nodefilter nodeExcludePrecedence ",jobs[0].nodeExcludePrecedence
            assertTrue "incorrect nodefilter doNodedispatch",jobs[0].doNodedispatch
            assertTrue "incorrect nodefilter doNodedispatch",jobs[0].nodesSelectedByDefault

    }

    @Test
    void testDecodeNodefilter3() {
        /** node filter job */
    def filter3 = """<joblist>
  <job>
    <id>8</id>
    <name>punch2</name>
    <description>dig it potato</description>
    <loglevel>WARN</loglevel>
    <group>simple</group>
    <context>
      <project>zig</project>
      <options>
        <option name='clip' value='true' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <nodefilters excludeprecedence="false">
        <exclude>
            <hostname>centos5</hostname>
            <tags>a+b,c</tags>
            <os-name>Win.*</os-name>
            <os-family>windows</os-family>
            <os-arch>x86,sparc</os-arch>
            <os-version>4\\..*</os-version>
            <name>mynodename</name>
        </exclude>
    </nodefilters>
    <dispatch>
      <threadcount>2</threadcount>
      <keepgoing>true</keepgoing>
    </dispatch>
  </job>
</joblist>
"""
            def jobs = JobsXMLCodec.decode(filter3)
            assertNotNull jobs
            assertEquals "incorrect size",1,jobs.size()
        assertEquals "incorrect nodefilter nodeInclude", "!hostname: centos5 !tags: a+b,c !os-name: Win.* !os-family: windows !os-arch: x86,sparc !os-version: 4\\..* !name: mynodename", jobs[0].filter
            assertEquals "incorrect nodefilter nodeInclude",null,jobs[0].nodeInclude
            assertEquals "incorrect nodefilter nodeIncludeTags",null,jobs[0].nodeIncludeTags
            assertEquals "incorrect nodefilter nodeIncludeOsName",null,jobs[0].nodeIncludeOsName
            assertEquals "incorrect nodefilter nodeIncludeOsFamily",null,jobs[0].nodeIncludeOsFamily
            assertEquals "incorrect nodefilter nodeIncludeOsArch",null,jobs[0].nodeIncludeOsArch
            assertEquals "incorrect nodefilter nodeIncludeOsVersion",null,jobs[0].nodeIncludeOsVersion
            assertEquals "incorrect nodefilter nodeIncludeName",null,jobs[0].nodeIncludeName

            assertEquals "incorrect nodefilter nodeExclude", null,jobs[0].nodeExclude
            assertEquals "incorrect nodefilter nodeExcludeTags", null,jobs[0].nodeExcludeTags
            assertEquals "incorrect nodefilter nodeExcludeOsName", null,jobs[0].nodeExcludeOsName
            assertEquals "incorrect nodefilter nodeExcludeOsFamily", null,jobs[0].nodeExcludeOsFamily
            assertEquals "incorrect nodefilter nodeExcludeOsArch", null,jobs[0].nodeExcludeOsArch
            assertEquals "incorrect nodefilter nodeExcludeOsVersion", null,jobs[0].nodeExcludeOsVersion
            assertEquals "incorrect nodefilter nodeExcludeName", null,jobs[0].nodeExcludeName
            assertFalse "incorrect nodefilter nodeExcludePrecedence",jobs[0].nodeExcludePrecedence
            assertTrue "incorrect nodefilter doNodedispatch",jobs[0].doNodedispatch
        assertTrue "incorrect nodefilter doNodedispatch",jobs[0].nodesSelectedByDefault
    }

    @Test
    void testDecodeNodefilterNodesSelectedByDefaultTrue() {
        /** node filter job */
    def filter3 = """<joblist>
  <job>
    <id>8</id>
    <name>punch2</name>
    <description>dig it potato</description>
    <loglevel>WARN</loglevel>
    <group>simple</group>
    <nodesSelectedByDefault>true</nodesSelectedByDefault>
    <context>
      <project>zig</project>
      <options>
        <option name='clip' value='true' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <nodefilters excludeprecedence="false">
        <exclude>
            <hostname>centos5</hostname>
            <tags>a+b,c</tags>
            <os-name>Win.*</os-name>
            <os-family>windows</os-family>
            <os-arch>x86,sparc</os-arch>
            <os-version>4\\..*</os-version>
            <name>mynodename</name>
        </exclude>
    </nodefilters>
    <dispatch>
      <threadcount>2</threadcount>
      <keepgoing>true</keepgoing>
    </dispatch>
  </job>
</joblist>
"""
            def jobs = JobsXMLCodec.decode(filter3)
            assertNotNull jobs
            assertEquals "incorrect size",1,jobs.size()
        assertEquals "incorrect nodefilter nodeInclude", "!hostname: centos5 !tags: a+b,c !os-name: Win.* !os-family: windows !os-arch: x86,sparc !os-version: 4\\..* !name: mynodename", jobs[0].filter
            assertEquals "incorrect nodefilter nodeInclude",null,jobs[0].nodeInclude
            assertEquals "incorrect nodefilter nodeIncludeTags",null,jobs[0].nodeIncludeTags
            assertEquals "incorrect nodefilter nodeIncludeOsName",null,jobs[0].nodeIncludeOsName
            assertEquals "incorrect nodefilter nodeIncludeOsFamily",null,jobs[0].nodeIncludeOsFamily
            assertEquals "incorrect nodefilter nodeIncludeOsArch",null,jobs[0].nodeIncludeOsArch
            assertEquals "incorrect nodefilter nodeIncludeOsVersion",null,jobs[0].nodeIncludeOsVersion
            assertEquals "incorrect nodefilter nodeIncludeName",null,jobs[0].nodeIncludeName

            assertEquals "incorrect nodefilter nodeExclude", null,jobs[0].nodeExclude
            assertEquals "incorrect nodefilter nodeExcludeTags", null,jobs[0].nodeExcludeTags
            assertEquals "incorrect nodefilter nodeExcludeOsName", null,jobs[0].nodeExcludeOsName
            assertEquals "incorrect nodefilter nodeExcludeOsFamily", null,jobs[0].nodeExcludeOsFamily
            assertEquals "incorrect nodefilter nodeExcludeOsArch", null,jobs[0].nodeExcludeOsArch
            assertEquals "incorrect nodefilter nodeExcludeOsVersion", null,jobs[0].nodeExcludeOsVersion
            assertEquals "incorrect nodefilter nodeExcludeName", null,jobs[0].nodeExcludeName
            assertFalse "incorrect nodefilter nodeExcludePrecedence",jobs[0].nodeExcludePrecedence
            assertTrue "incorrect nodefilter doNodedispatch",jobs[0].doNodedispatch
            assertTrue "incorrect nodefilter nodesSelectedByDefault",jobs[0].nodesSelectedByDefault
    }

    @Test
    void testDecodeNodefilterNodesSelectedByDefaultFalse() {
        /** node filter job */
    def filter3 = """<joblist>
  <job>
    <id>8</id>
    <name>punch2</name>
    <description>dig it potato</description>
    <nodesSelectedByDefault>false</nodesSelectedByDefault>
    <loglevel>WARN</loglevel>
    <group>simple</group>
    <context>
      <project>zig</project>
      <options>
        <option name='clip' value='true' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <nodefilters excludeprecedence="false">
        <exclude>
            <hostname>centos5</hostname>
            <tags>a+b,c</tags>
            <os-name>Win.*</os-name>
            <os-family>windows</os-family>
            <os-arch>x86,sparc</os-arch>
            <os-version>4\\..*</os-version>
            <name>mynodename</name>
        </exclude>
    </nodefilters>
    <dispatch>
      <threadcount>2</threadcount>
      <keepgoing>true</keepgoing>
    </dispatch>
  </job>
</joblist>
"""
        def jobs = JobsXMLCodec.decode(filter3)
        assertNotNull jobs
        assertEquals "incorrect size",1,jobs.size()
        assertEquals "incorrect nodefilter nodeInclude", "!hostname: centos5 !tags: a+b,c !os-name: Win.* !os-family: windows !os-arch: x86,sparc !os-version: 4\\..* !name: mynodename", jobs[0].filter
        assertEquals "incorrect nodefilter nodeInclude",null,jobs[0].nodeInclude
        assertEquals "incorrect nodefilter nodeIncludeTags",null,jobs[0].nodeIncludeTags
        assertEquals "incorrect nodefilter nodeIncludeOsName",null,jobs[0].nodeIncludeOsName
        assertEquals "incorrect nodefilter nodeIncludeOsFamily",null,jobs[0].nodeIncludeOsFamily
        assertEquals "incorrect nodefilter nodeIncludeOsArch",null,jobs[0].nodeIncludeOsArch
        assertEquals "incorrect nodefilter nodeIncludeOsVersion",null,jobs[0].nodeIncludeOsVersion
        assertEquals "incorrect nodefilter nodeIncludeName",null,jobs[0].nodeIncludeName

        assertEquals "incorrect nodefilter nodeExclude", null,jobs[0].nodeExclude
        assertEquals "incorrect nodefilter nodeExcludeTags", null,jobs[0].nodeExcludeTags
        assertEquals "incorrect nodefilter nodeExcludeOsName", null,jobs[0].nodeExcludeOsName
        assertEquals "incorrect nodefilter nodeExcludeOsFamily", null,jobs[0].nodeExcludeOsFamily
        assertEquals "incorrect nodefilter nodeExcludeOsArch", null,jobs[0].nodeExcludeOsArch
        assertEquals "incorrect nodefilter nodeExcludeOsVersion", null,jobs[0].nodeExcludeOsVersion
        assertEquals "incorrect nodefilter nodeExcludeName", null,jobs[0].nodeExcludeName
        assertFalse "incorrect nodefilter nodeExcludePrecedence",jobs[0].nodeExcludePrecedence
        assertTrue "incorrect nodefilter doNodedispatch",jobs[0].doNodedispatch
        assertFalse "incorrect nodefilter nodesSelectedByDefault",jobs[0].nodesSelectedByDefault
    }

    @Test
    void testDecodeDispatch() {
        /** node filter job  */
        def filter1 = """<joblist>
  <job>
    <id>8</id>
    <name>punch2</name>
    <description>dig it potato</description>
    <loglevel>WARN</loglevel>
    <group>simple</group>
    <context>
      <project>zig</project>
      <options>
        <option name='clip' value='true' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <nodefilters>
        <include>
            <hostname>centos5</hostname>
        </include>
    </nodefilters>
    <dispatch>
      <threadcount>2</threadcount>
      <keepgoing>true</keepgoing>
    </dispatch>
  </job>
</joblist>
"""
        print filter1
        def jobs = JobsXMLCodec.decode(filter1)
        print jobs
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()
        assertTrue "incorrect nodefilter doNodedispatch", jobs[0].doNodedispatch
        assertEquals "incorrect nodefilter doNodedispatch",2, jobs[0].nodeThreadcount
        assertEquals "incorrect nodefilter doNodedispatch",true, jobs[0].nodeKeepgoing
        assertNull "incorrect nodefilter doNodedispatch", jobs[0].nodeRankAttribute
        assertTrue "incorrect nodefilter doNodedispatch", jobs[0].nodeRankOrderAscending

        /** node filter job  */
        def filter2 = """<joblist>
  <job>
    <id>8</id>
    <name>punch2</name>
    <description>dig it potato</description>
    <loglevel>WARN</loglevel>
    <group>simple</group>
    <context>
      <project>zig</project>
      <options>
        <option name='clip' value='true' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <nodefilters>
        <include>
            <hostname>centos5</hostname>
            <tags>a+b,c</tags>
            <os-name>Win.*</os-name>
            <os-family>windows</os-family>
            <os-arch>x86,sparc</os-arch>
            <os-version>4\\..*</os-version>
            <name>mynodename</name>
        </include>
    </nodefilters>
    <dispatch>
      <threadcount>4</threadcount>
      <keepgoing>false</keepgoing>
      <rankAttribute>testRank</rankAttribute>
      <rankOrder>ascending</rankOrder>
    </dispatch>
  </job>
</joblist>
"""
        jobs = JobsXMLCodec.decode(filter2)
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()
        assertTrue "incorrect nodefilter doNodedispatch", jobs[0].doNodedispatch
        assertEquals "incorrect nodefilter doNodedispatch", 4, jobs[0].nodeThreadcount
        assertEquals "incorrect nodefilter doNodedispatch", false, jobs[0].nodeKeepgoing
        assertEquals "incorrect nodefilter doNodedispatch", "testRank",jobs[0].nodeRankAttribute
        assertTrue "incorrect nodefilter doNodedispatch", jobs[0].nodeRankOrderAscending

        /** node filter job  */
        def filter3 = """<joblist>
  <job>
    <id>8</id>
    <name>punch2</name>
    <description>dig it potato</description>
    <loglevel>WARN</loglevel>
    <group>simple</group>
    <context>
      <project>zig</project>
      <options>
        <option name='clip' value='true' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <nodefilters excludeprecedence="false">
        <exclude>
            <hostname>centos5</hostname>
            <tags>a+b,c</tags>
            <os-name>Win.*</os-name>
            <os-family>windows</os-family>
            <os-arch>x86,sparc</os-arch>
            <os-version>4\\..*</os-version>
            <name>mynodename</name>
        </exclude>
    </nodefilters>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
      <rankAttribute>nodename</rankAttribute>
      <rankOrder>descending</rankOrder>
    </dispatch>
  </job>
</joblist>
"""
        jobs = JobsXMLCodec.decode(filter3)
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()
        assertTrue "incorrect nodefilter doNodedispatch", jobs[0].doNodedispatch
        assertEquals "incorrect nodefilter doNodedispatch", 1, jobs[0].nodeThreadcount
        assertEquals "incorrect nodefilter doNodedispatch", false, jobs[0].nodeKeepgoing
        assertEquals "incorrect nodefilter doNodedispatch", "nodename", jobs[0].nodeRankAttribute
        assertFalse "incorrect nodefilter doNodedispatch", jobs[0].nodeRankOrderAscending
    }

    @Test
    void testDecodeScheduled(){
   /** scheduled job */
    def sched1 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description>a simple desc</description>
    <loglevel>INFO</loglevel>
    <group>some/group</group>
    <context>
      <project>test1</project>
      <options>
        <option name='delay' value='60' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
"""
            def jobs = JobsXMLCodec.decode(sched1)
            assertNotNull jobs
            assertEquals "incorrect size",1,jobs.size()
            
            assertEquals "incorrect scheduled","true",jobs[0].scheduled.toString()
            assertEquals "incorrect hour","11",jobs[0].hour
            assertEquals "incorrect minute","21",jobs[0].minute
            assertEquals "incorrect everyDayOfWeek","*",jobs[0].dayOfWeek
            assertEquals "incorrect everyMonth","*",jobs[0].month
            assertEquals "incorrect groupPath","some/group",jobs[0].groupPath
   /** scheduled job */
    def sched2 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description>a simple desc</description>
    <loglevel>INFO</loglevel>
    <group>some/group</group>
    <context>
      <project>test1</project>
      <options>
        <option name='delay' value='60' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='1,3-5' />
      <month month='1-5,9,12' />
    </schedule>
  </job>
</joblist>
"""
            jobs = JobsXMLCodec.decode(sched2)
            assertNotNull jobs
            assertEquals "incorrect size",1,jobs.size()

            assertEquals "incorrect scheduled","true",jobs[0].scheduled.toString()
            assertEquals "incorrect hour","11",jobs[0].hour
            assertEquals "incorrect minute","21",jobs[0].minute
            assertEquals "incorrect minute","1,3-5",jobs[0].dayOfWeek
            assertEquals "incorrect minute","?",jobs[0].dayOfMonth
            assertEquals "incorrect minute","1-5,9,12",jobs[0].month
            def datemap=jobs[0].timeAndDateAsBooleanMap()
            assertEquals "incorrect crontab.dayOfWeek.MON","true",datemap.'dayOfWeek.SUN'
            assertEquals "incorrect crontab.dayOfWeek.TUE",null,datemap.'dayOfWeek.MON'
            assertEquals "incorrect crontab.dayOfWeek.WED","true",datemap.'dayOfWeek.TUE'
            assertEquals "incorrect crontab.dayOfWeek.THU","true",datemap.'dayOfWeek.WED'
            assertEquals "incorrect crontab.dayOfWeek.FRI","true",datemap.'dayOfWeek.THU'
            assertEquals "incorrect crontab.month.JAN","true",datemap.'month.JAN'
            assertEquals "incorrect crontab.month.FEB","true",datemap.'month.FEB'
            assertEquals "incorrect crontab.month.MAR","true",datemap.'month.MAR'
            assertEquals "incorrect crontab.month.APR","true",datemap.'month.APR'
            assertEquals "incorrect crontab.month.MAY","true",datemap.'month.MAY'
            assertEquals "incorrect crontab.month.JUN",null,datemap.'month.JUN'
            assertEquals "incorrect crontab.month.JUL",null,datemap.'month.JUL'
            assertEquals "incorrect crontab.month.AUG",null,datemap.'month.AUG'
            assertEquals "incorrect crontab.month.SEP","true",datemap.'month.SEP'
            assertEquals "incorrect crontab.month.OCT",null,datemap.'month.OCT'
            assertEquals "incorrect crontab.month.NOV",null,datemap.'month.NOV'
            assertEquals "incorrect crontab.month.DEC","true",datemap.'month.DEC'
   /** scheduled job */
    def sched3 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description>a simple desc</description>
    <loglevel>INFO</loglevel>
    <group>some/group</group>
    <context>
      <project>test1</project>
      <options>
        <option name='delay' value='60' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='Mon,Wed-Fri' />
      <month month='Jan-May,Sep,Dec' />
    </schedule>
  </job>
</joblist>
"""
            jobs = JobsXMLCodec.decode(sched3)
            assertNotNull jobs
            assertEquals "incorrect size",1,jobs.size()

            assertEquals "incorrect scheduled","true",jobs[0].scheduled.toString()
            assertEquals "incorrect hour","11",jobs[0].hour
            assertEquals "incorrect minute","21",jobs[0].minute
            assertEquals "incorrect minute","Mon,Wed-Fri",jobs[0].dayOfWeek
            assertEquals "incorrect minute","Jan-May,Sep,Dec",jobs[0].month
            datemap=jobs[0].timeAndDateAsBooleanMap()
            assertEquals "incorrect crontab.dayOfWeek.MON","true",datemap.'dayOfWeek.MON'
            assertEquals "incorrect crontab.dayOfWeek.TUE",null,datemap.'dayOfWeek.TUE'
            assertEquals "incorrect crontab.dayOfWeek.WED","true",datemap.'dayOfWeek.WED'
            assertEquals "incorrect crontab.dayOfWeek.THU","true",datemap.'dayOfWeek.THU'
            assertEquals "incorrect crontab.dayOfWeek.FRI","true",datemap.'dayOfWeek.FRI'
            assertEquals "incorrect crontab.month.JAN","true",datemap.'month.JAN'
            assertEquals "incorrect crontab.month.FEB","true",datemap.'month.FEB'
            assertEquals "incorrect crontab.month.MAR","true",datemap.'month.MAR'
            assertEquals "incorrect crontab.month.APR","true",datemap.'month.APR'
            assertEquals "incorrect crontab.month.MAY","true",datemap.'month.MAY'
            assertEquals "incorrect crontab.month.JUN",null,datemap.'month.JUN'
            assertEquals "incorrect crontab.month.JUL",null,datemap.'month.JUL'
            assertEquals "incorrect crontab.month.AUG",null,datemap.'month.AUG'
            assertEquals "incorrect crontab.month.SEP","true",datemap.'month.SEP'
            assertEquals "incorrect crontab.month.OCT",null,datemap.'month.OCT'
            assertEquals "incorrect crontab.month.NOV",null,datemap.'month.NOV'
            assertEquals "incorrect crontab.month.DEC","true",datemap.'month.DEC'


        /***** extended schedule attributes *******/


   /** scheduled job with extended schedule details */
    def schedX1 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description>a simple desc</description>
    <loglevel>INFO</loglevel>
    <group>some/group</group>
    <context>
      <project>test1</project>
      <options>
        <option name='delay' value='60' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='*/4' minute='21' seconds='0' />
      <weekday day='?' />
      <month month='*/6' day='*/4'/>
      <year year="2010-2040"/>
    </schedule>
  </job>
</joblist>
"""
            jobs = JobsXMLCodec.decode(schedX1)
            assertNotNull jobs
            assertEquals "incorrect size",1,jobs.size()

            assertEquals "incorrect scheduled","true",jobs[0].scheduled.toString()
            assertEquals "incorrect hour","*/4",jobs[0].hour
            assertEquals "incorrect minute","21",jobs[0].minute
            assertEquals "incorrect seconds","0",jobs[0].seconds
            assertEquals "incorrect dayOfWeek",'?',jobs[0].dayOfWeek
            assertEquals "incorrect dayOfMonth",'*/4',jobs[0].dayOfMonth
            assertEquals "incorrect month",'*/6',jobs[0].month
            assertEquals "incorrect year",'2010-2040',jobs[0].year
        datemap = jobs[0].timeAndDateAsBooleanMap()
            assertEquals "incorrect crontab.dayOfWeek.MON",null,datemap.'dayOfWeek.MON'
            assertEquals "incorrect crontab.dayOfWeek.TUE",null,datemap.'dayOfWeek.TUE'
            assertEquals "incorrect crontab.dayOfWeek.WED",null,datemap.'dayOfWeek.WED'
            assertEquals "incorrect crontab.dayOfWeek.THU",null,datemap.'dayOfWeek.THU'
            assertEquals "incorrect crontab.dayOfWeek.FRI",null,datemap.'dayOfWeek.FRI'
            assertEquals "incorrect crontab.month.JAN",null,datemap.'month.JAN'
            assertEquals "incorrect crontab.month.FEB",null,datemap.'month.FEB'
            assertEquals "incorrect crontab.month.MAR",null,datemap.'month.MAR'
            assertEquals "incorrect crontab.month.APR",null,datemap.'month.APR'
            assertEquals "incorrect crontab.month.MAY",null,datemap.'month.MAY'
            assertEquals "incorrect crontab.month.JUN",null,datemap.'month.JUN'
            assertEquals "incorrect crontab.month.JUL",null,datemap.'month.JUL'
            assertEquals "incorrect crontab.month.AUG",null,datemap.'month.AUG'
            assertEquals "incorrect crontab.month.SEP",null,datemap.'month.SEP'
            assertEquals "incorrect crontab.month.OCT",null,datemap.'month.OCT'
            assertEquals "incorrect crontab.month.NOV",null,datemap.'month.NOV'
            assertEquals "incorrect crontab.month.DEC",null,datemap.'month.DEC'

   /** scheduled job with crontab string */
    def schedX2 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description>a simple desc</description>
    <loglevel>INFO</loglevel>
    <group>some/group</group>
    <context>
      <project>test1</project>
      <options>
        <option name='delay' value='60' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule crontab='0 21 */4 */4 */6 ? 2010-2040'>
    </schedule>
  </job>
</joblist>
"""
            jobs = JobsXMLCodec.decode(schedX2)
            assertNotNull jobs
            assertEquals "incorrect size",1,jobs.size()

            assertEquals "incorrect scheduled","true",jobs[0].scheduled.toString()
            assertEquals "incorrect scheduled",'0 21 */4 */4 */6 ? 2010-2040',jobs[0].crontabString
        datemap=jobs[0].timeAndDateAsBooleanMap()
            assertEquals "incorrect crontab.dayOfWeek.MON",null,datemap.'dayOfWeek.MON'
            assertEquals "incorrect crontab.dayOfWeek.TUE",null,datemap.'dayOfWeek.TUE'
            assertEquals "incorrect crontab.dayOfWeek.WED",null,datemap.'dayOfWeek.WED'
            assertEquals "incorrect crontab.dayOfWeek.THU",null,datemap.'dayOfWeek.THU'
            assertEquals "incorrect crontab.dayOfWeek.FRI",null,datemap.'dayOfWeek.FRI'
            assertEquals "incorrect crontab.month.JAN",null,datemap.'month.JAN'
            assertEquals "incorrect crontab.month.FEB",null,datemap.'month.FEB'
            assertEquals "incorrect crontab.month.MAR",null,datemap.'month.MAR'
            assertEquals "incorrect crontab.month.APR",null,datemap.'month.APR'
            assertEquals "incorrect crontab.month.MAY",null,datemap.'month.MAY'
            assertEquals "incorrect crontab.month.JUN",null,datemap.'month.JUN'
            assertEquals "incorrect crontab.month.JUL",null,datemap.'month.JUL'
            assertEquals "incorrect crontab.month.AUG",null,datemap.'month.AUG'
            assertEquals "incorrect crontab.month.SEP",null,datemap.'month.SEP'
            assertEquals "incorrect crontab.month.OCT",null,datemap.'month.OCT'
            assertEquals "incorrect crontab.month.NOV",null,datemap.'month.NOV'
            assertEquals "incorrect crontab.month.DEC",null,datemap.'month.DEC'

    }
    /**
     * Empty year attribute
     */
    @Test
    void testDecodeScheduledEmptyYear(){
        def jobs = JobsXMLCodec.decode("""<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description>a simple desc</description>
    <loglevel>INFO</loglevel>
    <group>some/group</group>
    <context>
      <project>test1</project>
      <options>
        <option name='delay' value='60' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule >
      <time hour='*/4' minute='21' seconds='0' />
      <weekday day='?' />
      <month month='*/6' day='*/4'/>
      <year year=""/>
    </schedule>
  </job>
</joblist>
""")
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()

        assertEquals "incorrect scheduled", "true", jobs[0].scheduled.toString()
        assertNull "incorrect scheduled", jobs[0].crontabString
        assertEquals("*/4",jobs[0].hour)
        assertEquals("21",jobs[0].minute)
        assertEquals("0",jobs[0].seconds)
        assertEquals("?",jobs[0].dayOfWeek)
        assertEquals("*/4",jobs[0].dayOfMonth)
        assertEquals("*/6",jobs[0].month)
        assertEquals("*",jobs[0].year)

    }
    /**
     * Incomplete year element
     */
    @Test
    void testDecodeScheduledIncompleteYear(){
        def jobs = JobsXMLCodec.decode("""<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description>a simple desc</description>
    <loglevel>INFO</loglevel>
    <group>some/group</group>
    <context>
      <project>test1</project>
      <options>
        <option name='delay' value='60' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule >
      <time hour='*/4' minute='21' seconds='0' />
      <weekday day='?' />
      <month month='*/6' day='*/4'/>
      <year />
    </schedule>
  </job>
</joblist>
""")
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()

        assertEquals "incorrect scheduled", "true", jobs[0].scheduled.toString()
        assertNull "incorrect scheduled", jobs[0].crontabString
        assertEquals("*/4",jobs[0].hour)
        assertEquals("21",jobs[0].minute)
        assertEquals("0",jobs[0].seconds)
        assertEquals("?",jobs[0].dayOfWeek)
        assertEquals("*/4",jobs[0].dayOfMonth)
        assertEquals("*/6",jobs[0].month)
        assertEquals("*",jobs[0].year)

    }
    /**
     * Test  weekday
     */
    @Test
    void testDecodeScheduledWeekday(){
        def jobs = JobsXMLCodec.decode("""<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description>a simple desc</description>
    <loglevel>INFO</loglevel>
    <group>some/group</group>
    <context>
      <project>test1</project>
      <options>
        <option name='delay' value='60' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule >
      <time hour='*/4' minute='21' seconds='0' />
      <day day='?' />
      <weekday day='2-4' />
      <month month='*/6' />
      <year year=""/>
    </schedule>
  </job>
</joblist>
""")
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()

        assertEquals "incorrect scheduled", "true", jobs[0].scheduled.toString()
        assertNull "incorrect scheduled", jobs[0].crontabString
        assertEquals("*/4",jobs[0].hour)
        assertEquals("21",jobs[0].minute)
        assertEquals("0",jobs[0].seconds)
        assertEquals("2-4",jobs[0].dayOfWeek)
        assertEquals("?",jobs[0].dayOfMonth)
        assertEquals("*/6",jobs[0].month)
        assertEquals("*",jobs[0].year)

    }


    @Test
    void testShouldFailEmptyWorkflow() {

        //empty workflow
        def xml1 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence>
    </sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
"""

        try{
            def jobs = JobsXMLCodec.decode(xml1)
            fail("Should not parse empty workflow")
        }catch(JobXMLException e){

        }
    }
    @Test
    void testDecodeWorkflow() {
        //simple workflow with script command
        def xml6 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence>
        <command>
            <exec>a script</exec>
        </command>
    </sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
"""

            def jobs = JobsXMLCodec.decode(xml6)
            assertNotNull jobs
            assertEquals "incorrect size", 1, jobs.size()
            assertNotNull "incorrect workflow", jobs[0].workflow
            assertEquals "incorrect workflow strategy", "node-first", jobs[0].workflow.strategy
            assertNotNull "incorrect workflow strategy", jobs[0].workflow.commands
            assertEquals "incorrect workflow strategy", 1, jobs[0].workflow.commands.size()
            def cmd1 = jobs[0].workflow.commands[0]
            assertNotNull "incorrect workflow", cmd1
            assertTrue "incorrect adhocExecution: ${cmd1.adhocExecution}", cmd1.adhocExecution
            assertEquals "incorrect adhocRemoteString", 'a script', cmd1.adhocRemoteString
            assertNull "incorrect adhocLocalString", cmd1.adhocLocalString
            assertNull "incorrect adhocFilepath", cmd1.adhocFilepath
            assertNull "incorrect argString", cmd1.argString

        //simple workflow with script content
        def xml7 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence>
        <command>
            <script>a script 2</script>
        </command>
    </sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
"""

            jobs = JobsXMLCodec.decode(xml7)
            assertNotNull jobs
            assertEquals "incorrect size", 1, jobs.size()
            assertNotNull "incorrect workflow", jobs[0].workflow
            assertEquals "incorrect workflow strategy", "node-first", jobs[0].workflow.strategy
            assertNotNull "incorrect workflow strategy", jobs[0].workflow.commands
            assertEquals "incorrect workflow strategy", 1, jobs[0].workflow.commands.size()
            cmd1 = jobs[0].workflow.commands[0]
            assertNotNull "incorrect workflow", cmd1
            assertTrue "incorrect adhocExecution: ${cmd1.adhocExecution}", cmd1.adhocExecution
            assertEquals "incorrect adhocLocalString", 'a script 2', cmd1.adhocLocalString
            assertNull "incorrect adhocRemoteString", cmd1.adhocRemoteString
            assertNull "incorrect adhocFilepath", cmd1.adhocFilepath
            assertNull "incorrect argString", cmd1.argString
        //simple workflow with script content
        def xml8 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence>
        <command>
            <scriptfile>/a/path/to/a/script</scriptfile>
            <scriptargs>-some args -to the -script</scriptargs>
        </command>
    </sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
"""

            jobs = JobsXMLCodec.decode(xml8)
            assertNotNull jobs
            assertEquals "incorrect size", 1, jobs.size()
            assertNotNull "incorrect workflow", jobs[0].workflow
            assertEquals "incorrect workflow strategy", "node-first", jobs[0].workflow.strategy
            assertNotNull "incorrect workflow strategy", jobs[0].workflow.commands
            assertEquals "incorrect workflow strategy", 1, jobs[0].workflow.commands.size()
            cmd1 = jobs[0].workflow.commands[0]
            assertNotNull "incorrect workflow", cmd1
            assertTrue "incorrect adhocExecution: ${cmd1.adhocExecution}", cmd1.adhocExecution
            assertNull "incorrect adhocLocalString", cmd1.adhocLocalString
            assertNull "incorrect adhocRemoteString", cmd1.adhocRemoteString
            assertEquals "incorrect adhocFilepath", '/a/path/to/a/script', cmd1.adhocFilepath
            assertEquals "incorrect argString", '-some args -to the -script', cmd1.argString
        //simple workflow with jobref without jobGroup
            jobs = JobsXMLCodec.decode("""<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence>
        <command>
            <jobref name="bob" />
        </command>
    </sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
""")
            assertNotNull jobs
            assertEquals "incorrect size", 1, jobs.size()
            assertNotNull "incorrect workflow", jobs[0].workflow
            assertEquals "incorrect workflow strategy", "node-first", jobs[0].workflow.strategy
            assertNotNull "incorrect workflow strategy", jobs[0].workflow.commands
            assertEquals "incorrect workflow strategy", 1, jobs[0].workflow.commands.size()
            cmd1 = jobs[0].workflow.commands[0]
            assertNotNull "incorrect workflow", cmd1
            assertTrue "incorrect type: ${cmd1}", (cmd1 instanceof JobExec)
            assertNull "incorrect argString", cmd1.argString
            assertEquals "incorrect jobName", 'bob', cmd1.jobName
            assertNull "incorrect jobGroup", cmd1.jobGroup
            assertEquals "incorrect nodeStep", false, !!cmd1.nodeStep

        //simple workflow with jobref
            jobs = JobsXMLCodec.decode("""<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence>
        <command>
            <jobref name="bob" group="/some/path"/>
        </command>
    </sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
""")
            assertNotNull jobs
            assertEquals "incorrect size", 1, jobs.size()
            assertNotNull "incorrect workflow", jobs[0].workflow
            assertEquals "incorrect workflow strategy", "node-first", jobs[0].workflow.strategy
            assertNotNull "incorrect workflow strategy", jobs[0].workflow.commands
            assertEquals "incorrect workflow strategy", 1, jobs[0].workflow.commands.size()
            cmd1 = jobs[0].workflow.commands[0]
            assertNotNull "incorrect workflow", cmd1
            assertTrue "incorrect type: ${cmd1}", (cmd1 instanceof JobExec)
            assertNull "incorrect adhocRemoteString", cmd1.argString
            assertEquals "incorrect jobName", 'bob', cmd1.jobName
            assertEquals "incorrect jobGroup", '/some/path', cmd1.jobGroup
            assertEquals "incorrect nodeStep", false, !!cmd1.nodeStep

        //simple workflow with step-first strategy
            jobs = JobsXMLCodec.decode("""<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence strategy="step-first">
        <command>
            <jobref name="bob" group="/some/path"/>
        </command>
    </sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
""")
            assertNotNull jobs
            assertEquals "incorrect size", 1, jobs.size()
            assertNotNull "incorrect workflow", jobs[0].workflow
            assertEquals "incorrect workflow strategy", "step-first", jobs[0].workflow.strategy
            assertNotNull "incorrect workflow strategy", jobs[0].workflow.commands
            assertEquals "incorrect workflow strategy", 1, jobs[0].workflow.commands.size()
            cmd1 = jobs[0].workflow.commands[0]
            assertNotNull "incorrect workflow", cmd1
            assertTrue "incorrect type: ${cmd1}", (cmd1 instanceof JobExec)
            assertNull "incorrect adhocRemoteString", cmd1.argString
            assertEquals "incorrect jobName", 'bob', cmd1.jobName
            assertEquals "incorrect jobGroup", '/some/path', cmd1.jobGroup
            assertEquals "incorrect nodeStep", false, !!cmd1.nodeStep

        //jobref item with args
            jobs = JobsXMLCodec.decode("""<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence>
        <command>
            <jobref name="bob" group="/some/path">
                <arg line="-test1 1 -test2 2"/>
            </jobref>
        </command>
    </sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
""")
            assertNotNull jobs
            assertEquals "incorrect size", 1, jobs.size()
            assertNotNull "incorrect workflow", jobs[0].workflow
            assertEquals "incorrect workflow strategy", "node-first", jobs[0].workflow.strategy
            assertNotNull "incorrect workflow strategy", jobs[0].workflow.commands
            assertEquals "incorrect workflow strategy", 1, jobs[0].workflow.commands.size()
            cmd1 = jobs[0].workflow.commands[0]
            assertNotNull "incorrect workflow", cmd1
            assertTrue "incorrect type: ${cmd1}", (cmd1 instanceof JobExec)
            assertNotNull "incorrect adhocRemoteString", cmd1.argString
            assertEquals "incorrect adhocRemoteString", "-test1 1 -test2 2",cmd1.argString
            assertEquals "incorrect jobName", 'bob', cmd1.jobName
            assertEquals "incorrect jobGroup", '/some/path', cmd1.jobGroup
            assertEquals "incorrect nodeStep", false, !!cmd1.nodeStep

        //jobref item nodeStep=true
            jobs = JobsXMLCodec.decode("""<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence>
        <command>
            <jobref name="bob" group="/some/path" nodeStep="true">
                <arg line="-test1 1 -test2 2"/>
            </jobref>
        </command>
    </sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
""")
            assertNotNull jobs
            assertEquals "incorrect size", 1, jobs.size()
            assertNotNull "incorrect workflow", jobs[0].workflow
            assertEquals "incorrect workflow strategy", "node-first", jobs[0].workflow.strategy
            assertNotNull "incorrect workflow strategy", jobs[0].workflow.commands
            assertEquals "incorrect workflow strategy", 1, jobs[0].workflow.commands.size()
            cmd1 = jobs[0].workflow.commands[0]
            assertNotNull "incorrect workflow", cmd1
            assertTrue "incorrect type: ${cmd1}", (cmd1 instanceof JobExec)
            assertNotNull "incorrect adhocRemoteString", cmd1.argString
            assertEquals "incorrect adhocRemoteString", "-test1 1 -test2 2",cmd1.argString
            assertEquals "incorrect jobName", 'bob', cmd1.jobName
            assertEquals "incorrect jobGroup", '/some/path', cmd1.jobGroup
            assertEquals "incorrect nodeStep", true, !!cmd1.nodeStep

        //simple workflow with script content
        jobs = JobsXMLCodec.decode("""<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence>
        <command>
            <scripturl>http://example.com/a/path/to/a/script</scripturl>
            <scriptargs>-some args -to the -script</scriptargs>
        </command>
    </sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
""")
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()
        assertNotNull "incorrect workflow", jobs[0].workflow
        assertEquals "incorrect workflow strategy", "node-first", jobs[0].workflow.strategy
        assertNotNull "incorrect workflow strategy", jobs[0].workflow.commands
        assertEquals "incorrect workflow strategy", 1, jobs[0].workflow.commands.size()
        cmd1 = jobs[0].workflow.commands[0]
        assertNotNull "incorrect workflow", cmd1
        assertTrue "incorrect adhocExecution: ${cmd1.adhocExecution}", cmd1.adhocExecution
        assertNull "incorrect adhocLocalString", cmd1.adhocLocalString
        assertNull "incorrect adhocRemoteString", cmd1.adhocRemoteString
        assertEquals "incorrect adhocFilepath", 'http://example.com/a/path/to/a/script', cmd1.adhocFilepath
        assertEquals "incorrect argString", '-some args -to the -script', cmd1.argString
    }
    @Test
    void testDecodeWorkflowJobref(){
        def jobs = JobsXMLCodec.decode("""<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence>
        <command>
            <jobref name="bob" />
        </command>
        <command>
            <jobref name="blang" >
                <nodefilters>
                    <filter>abc def</filter>
                </nodefilters>
            </jobref>
        </command>
        <command>
            <jobref name="blang2">
                <dispatch>
                    <threadcount>2</threadcount>
                </dispatch>
                <nodefilters>
                    <filter>abc def2</filter>
                </nodefilters>
            </jobref>
        </command>
        <command>
            <jobref name="blang3">
                <dispatch>
                    <threadcount>3</threadcount>
                    <keepgoing>true</keepgoing>
                </dispatch>
                <nodefilters>
                    <filter>abc def3</filter>
                </nodefilters>
            </jobref>
        </command>
        <command>
            <jobref name="blang4">
                <dispatch>
                    <threadcount>2</threadcount>
                    <keepgoing>false</keepgoing>
                </dispatch>
                <nodefilters>
                    <filter>abc def4</filter>
                </nodefilters>
            </jobref>
        </command>
        <command>
            <jobref name="blang5">
                <dispatch>
                    <threadcount>2</threadcount>
                    <keepgoing>false</keepgoing>
                    <rankAttribute>rank</rankAttribute>
                </dispatch>
                <nodefilters>
                    <filter>abc def5</filter>
                </nodefilters>
            </jobref>
        </command>
        <command>
            <jobref name="blang6">
                <dispatch>
                    <threadcount>2</threadcount>
                    <keepgoing>false</keepgoing>
                    <rankAttribute>rank</rankAttribute>
                    <rankOrder>descending</rankOrder>
                </dispatch>
                <nodefilters>
                    <filter>abc def6</filter>
                </nodefilters>
            </jobref>
        </command>
    </sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
""")
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()
        assertNotNull "incorrect workflow", jobs[0].workflow
        assertEquals "incorrect workflow strategy", "node-first", jobs[0].workflow.strategy
        assertNotNull "incorrect workflow strategy", jobs[0].workflow.commands
        assertEquals "incorrect workflow strategy", 7, jobs[0].workflow.commands.size()

        assertJobExec(
                jobs[0].workflow.commands[0],
                [argString: null, jobName: 'bob', jobGroup: null, nodeStep: false, nodeKeepgoing: null, nodeFilter: null, nodeThreadcount: null, nodeRankAttribute: null, nodeRankOrder: null]
        )
        assertJobExec(
                jobs[0].workflow.commands[1],
                [argString: null, jobName: 'blang', jobGroup: null, nodeStep: false, nodeKeepgoing: null, nodeFilter: 'abc def', nodeThreadcount: null, nodeRankAttribute: null, nodeRankOrder: null]
        )
        assertJobExec(
                jobs[0].workflow.commands[2],
                [argString: null, jobName: 'blang2', jobGroup: null, nodeStep: false, nodeKeepgoing: null, nodeFilter: 'abc def2', nodeThreadcount: 2, nodeRankAttribute: null, nodeRankOrder: null]
        )
        assertJobExec(
                jobs[0].workflow.commands[3],
                [argString: null, jobName: 'blang3', jobGroup: null, nodeStep: false, nodeKeepgoing: true, nodeFilter: 'abc def3', nodeThreadcount: 3, nodeRankAttribute: null, nodeRankOrder: null]
        )
        assertJobExec(
                jobs[0].workflow.commands[4],
                [argString: null, jobName: 'blang4', jobGroup: null, nodeStep: false, nodeKeepgoing: false, nodeFilter: 'abc def4', nodeThreadcount: 2, nodeRankAttribute: null, nodeRankOrder: null]
        )
        assertJobExec(
                jobs[0].workflow.commands[5],
                [argString: null, jobName: 'blang5', jobGroup: null, nodeStep: false, nodeKeepgoing: false, nodeFilter: 'abc def5', nodeThreadcount: 2, nodeRankAttribute: 'rank', nodeRankOrder: null]
        )
        assertJobExec(
                jobs[0].workflow.commands[6],
                [argString: null, jobName: 'blang6', jobGroup: null, nodeStep: false, nodeKeepgoing: false, nodeFilter: 'abc def6', nodeThreadcount: 2, nodeRankAttribute: 'rank', nodeRankOrder: 'descending']
        )

    }

    protected void assertJobExec(command, Map expected) {
        assertNotNull "should not be null command", command
        assertTrue "incorrect type: ${command}", (command instanceof JobExec)
        assertEquals "incorrect argString", expected.argString,command.argString
        assertEquals "incorrect jobName", expected.jobName, command.jobName
        assertEquals "incorrect jobGroup", expected.jobGroup, command.jobGroup
        assertEquals "incorrect nodeStep", expected.nodeStep, command.nodeStep
        assertEquals "incorrect nodeKeepgoing", expected.nodeKeepgoing, command.nodeKeepgoing
        assertEquals "incorrect nodeFilter", expected.nodeFilter, command.nodeFilter
        assertEquals "incorrect nodeThreadcount", expected.nodeThreadcount, command.nodeThreadcount
    }

    @Test
    void testDecodeWorkflowOptions(){
        //simple workflow with options
            def jobs = JobsXMLCodec.decode("""<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
        <options>
          <option name="buildstamp" value="789" values="123,456,789" enforcedvalues="false" regex="abc"/>
        </options>
    </context>
    <sequence>
        <command>
            <jobref name="bob" group="/some/path"/>
        </command>
    </sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
""")
            assertNotNull jobs
            assertEquals "incorrect size", 1, jobs.size()
            assertNotNull "incorrect workflow", jobs[0].workflow
            assertNotNull "incorrect workflow", jobs[0].workflow.commands
            assertEquals "incorrect workflow size", 1, jobs[0].workflow.commands.size()
            def cmd1 = jobs[0].workflow.commands[0]
            assertNotNull "incorrect workflow", cmd1
            assertTrue "incorrect type: ${cmd1}", (cmd1 instanceof JobExec)
            assertNull "incorrect adhocRemoteString", cmd1.argString
            assertEquals "incorrect jobName", 'bob', cmd1.jobName
            assertEquals "incorrect jobGroup", '/some/path', cmd1.jobGroup
            assertNotNull "incorrect options", jobs[0].options
            assertEquals "incorrect options size", 1, jobs[0].options.size()
            def opt0=jobs[0].options.iterator().next()
            assertEquals "incorrect name", "buildstamp", opt0.name
            assertEquals "incorrect defaultValue", "789", opt0.defaultValue
            assertFalse "incorrect enforced", opt0.enforced
            assertFalse "incorrect enforced", opt0.required
            assertEquals "incorrect regex", "abc", opt0.regex
            assertNull "incorrect values size", opt0.realValuesUrl
            assertNotNull "incorrect values size", opt0.optionValues
            assertEquals "incorrect values size", 3, opt0.optionValues.size()
            def values=[]
            values.addAll(opt0.optionValues as List)
            assertTrue "incorrect values content", values.contains("123")
            assertTrue "incorrect values content", values.contains("456")
            assertTrue "incorrect values content", values.contains("789")
    }

    @Test
    void testDecodePluginNodeStep() {
        //simple workflow with options
        def jobs = JobsXMLCodec.decode("""<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
        <options>
          <option name="buildstamp" value="789" values="123,456,789" enforcedvalues="false" regex="abc"/>
        </options>
    </context>
    <sequence>
        <command>
            <node-step-plugin type="blah">
                <configuration>
                    <entry key="elf" value="monkey"/>
                    <entry key="ok" value="howdy"/>
                </configuration>
            </node-step-plugin>
        </command>
    </sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
""")
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()
        assertNotNull "incorrect workflow", jobs[0].workflow
        assertNotNull "incorrect workflow", jobs[0].workflow.commands
        assertEquals "incorrect workflow size", 1, jobs[0].workflow.commands.size()
        def cmd1 = jobs[0].workflow.commands[0]
        assertNotNull "incorrect workflow", cmd1
        assertTrue "incorrect type: ${cmd1}", (cmd1 instanceof PluginStep)
        assertNotNull("incorrect nodeStep", cmd1.nodeStep)
        assertTrue("incorrect nodeStep", cmd1.nodeStep)
        assertEquals "incorrect type", 'blah', cmd1.type
        assertEquals "incorrect configuration", [elf:'monkey',ok:'howdy'], cmd1.configuration

    }

    @Test
    void testDecodePluginNodeStepEmptyConfig() {
        //simple workflow with options
        def jobs = JobsXMLCodec.decode("""<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
        <options>
          <option name="buildstamp" value="789" values="123,456,789" enforcedvalues="false" regex="abc"/>
        </options>
    </context>
    <sequence>
        <command>
            <node-step-plugin type="blah">
                <configuration>
                </configuration>
            </node-step-plugin>
        </command>
    </sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
""")
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()
        assertNotNull "incorrect workflow", jobs[0].workflow
        assertNotNull "incorrect workflow", jobs[0].workflow.commands
        assertEquals "incorrect workflow size", 1, jobs[0].workflow.commands.size()
        def cmd1 = jobs[0].workflow.commands[0]
        assertNotNull "incorrect workflow", cmd1
        assertTrue "incorrect type: ${cmd1}", (cmd1 instanceof PluginStep)
        assertNotNull("incorrect nodeStep", cmd1.nodeStep)
        assertTrue("incorrect nodeStep", cmd1.nodeStep)
        assertEquals "incorrect type", 'blah', cmd1.type
        assertEquals "incorrect configuration", null, cmd1.configuration

    }

    @Test
    void testDecodePluginNodeStepMissingConfig() {
        //simple workflow with options
        def jobs = JobsXMLCodec.decode("""<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
        <options>
          <option name="buildstamp" value="789" values="123,456,789" enforcedvalues="false" regex="abc"/>
        </options>
    </context>
    <sequence>
        <command>
            <node-step-plugin type="blah">
            </node-step-plugin>
        </command>
    </sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
""")
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()
        assertNotNull "incorrect workflow", jobs[0].workflow
        assertNotNull "incorrect workflow", jobs[0].workflow.commands
        assertEquals "incorrect workflow size", 1, jobs[0].workflow.commands.size()
        def cmd1 = jobs[0].workflow.commands[0]
        assertNotNull "incorrect workflow", cmd1
        assertTrue "incorrect type: ${cmd1}", (cmd1 instanceof PluginStep)
        assertNotNull("incorrect nodeStep", cmd1.nodeStep)
        assertTrue("incorrect nodeStep", cmd1.nodeStep)
        assertEquals "incorrect type", 'blah', cmd1.type
        assertEquals "incorrect configuration", null, cmd1.configuration

    }

    @Test
    void testDecodePluginStep() {
        //simple workflow with options
        def jobs = JobsXMLCodec.decode("""<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
        <options>
          <option name="buildstamp" value="789" values="123,456,789" enforcedvalues="false" regex="abc"/>
        </options>
    </context>
    <sequence>
        <command>
            <step-plugin type="blah">
                <configuration>
                    <entry key="elf" value="monkey"/>
                    <entry key="ok" value="howdy"/>
                </configuration>
            </step-plugin>
        </command>
    </sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
""")
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()
        assertNotNull "incorrect workflow", jobs[0].workflow
        assertNotNull "incorrect workflow", jobs[0].workflow.commands
        assertEquals "incorrect workflow size", 1, jobs[0].workflow.commands.size()
        def cmd1 = jobs[0].workflow.commands[0]
        assertNotNull "incorrect workflow", cmd1
        assertTrue "incorrect type: ${cmd1}", (cmd1 instanceof PluginStep)
        assertFalse("incorrect nodeStep", cmd1.nodeStep)
        assertEquals "incorrect type", 'blah', cmd1.type
        assertEquals "incorrect configuration", [elf:'monkey',ok:'howdy'], cmd1.configuration
    }

    @Test
    void testDecodePluginStepEmptyConfig() {
        //simple workflow with options
        def jobs = JobsXMLCodec.decode("""<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
        <options>
          <option name="buildstamp" value="789" values="123,456,789" enforcedvalues="false" regex="abc"/>
        </options>
    </context>
    <sequence>
        <command>
            <step-plugin type="blah">
                <configuration>
                </configuration>
            </step-plugin>
        </command>
    </sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
""")
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()
        assertNotNull "incorrect workflow", jobs[0].workflow
        assertNotNull "incorrect workflow", jobs[0].workflow.commands
        assertEquals "incorrect workflow size", 1, jobs[0].workflow.commands.size()
        def cmd1 = jobs[0].workflow.commands[0]
        assertNotNull "incorrect workflow", cmd1
        assertTrue "incorrect type: ${cmd1}", (cmd1 instanceof PluginStep)
        assertFalse("incorrect nodeStep", cmd1.nodeStep)
        assertEquals "incorrect type", 'blah', cmd1.type
        assertEquals "incorrect configuration", null, cmd1.configuration
    }

    @Test
    void testDecodePluginStepMissingConfig() {
        //simple workflow with options
        def jobs = JobsXMLCodec.decode("""<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
        <options>
          <option name="buildstamp" value="789" values="123,456,789" enforcedvalues="false" regex="abc"/>
        </options>
    </context>
    <sequence>
        <command>
            <step-plugin type="blah">
            </step-plugin>
        </command>
    </sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
""")
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()
        assertNotNull "incorrect workflow", jobs[0].workflow
        assertNotNull "incorrect workflow", jobs[0].workflow.commands
        assertEquals "incorrect workflow size", 1, jobs[0].workflow.commands.size()
        def cmd1 = jobs[0].workflow.commands[0]
        assertNotNull "incorrect workflow", cmd1
        assertTrue "incorrect type: ${cmd1}", (cmd1 instanceof PluginStep)
        assertFalse("incorrect nodeStep", cmd1.nodeStep)
        assertEquals "incorrect type", 'blah', cmd1.type
        assertEquals "incorrect configuration", null, cmd1.configuration
    }

    @Test
    void testDecodeOptions(){

        //empty options should include a property in parse result
        def xml0 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
"""

            def jobs = JobsXMLCodec.decode(xml0)
            assertNotNull jobs
            assertEquals "incorrect size", 1, jobs.size()
            assertNull "incorrect options", jobs[0].options
        //simple options
        def xml1 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
        <options>
          <option name="buildstamp" value="789" values="123,456,789" enforcedvalues="false" regex="abc"/>
        </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
"""

            jobs = JobsXMLCodec.decode(xml1)
            assertNotNull jobs
            assertEquals "incorrect size", 1, jobs.size()
            assertNotNull "incorrect options", jobs[0].options
            assertEquals "incorrect options size", 1, jobs[0].options.size()
            def opt0=jobs[0].options.iterator().next()
            assertEquals "incorrect name", "buildstamp", opt0.name
            assertEquals "incorrect defaultValue", "789", opt0.defaultValue
            assertEquals "incorrect enforced", "false", opt0.enforced.toString()
            assertEquals "incorrect regex", "abc", opt0.regex
            assertNull "incorrect values size", opt0.realValuesUrl
            assertNotNull "incorrect values size", opt0.optionValues
            assertEquals "incorrect values size", 3, opt0.optionValues.size()
            def values=[]
            values.addAll(opt0.optionValues as List)
            assertTrue "incorrect values content", values.contains("123")
            assertTrue "incorrect values content", values.contains("456")
            assertTrue "incorrect values content", values.contains("789")
        //simple options using valuesUrl
        def xml2 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
        <options>
          <option name="buildstamp" value="789" valuesUrl="http://monkey/somewhere" enforcedvalues="false" regex="abc"/>
        </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
"""

            jobs = JobsXMLCodec.decode(xml2)
            assertNotNull jobs
            assertEquals "incorrect size", 1, jobs.size()
            assertNotNull "incorrect options", jobs[0].options
            assertEquals "incorrect options size", 1, jobs[0].options.size()
            assertNotNull "missing options data", jobs[0].options.iterator().next()
            def opt1=jobs[0].options.iterator().next()
            assertEquals "incorrect name", "buildstamp", opt1.name
            assertEquals "incorrect defaultValue", "789", opt1.defaultValue
            assertEquals "incorrect enforced", "false", opt1.enforced.toString()
            assertEquals "incorrect regex", "abc", opt1.regex
            assertNull "incorrect values size", opt1.values
            assertNotNull "missing valuesUrl", opt1.realValuesUrl
            assertTrue "missing valuesUrl", opt1.realValuesUrl instanceof URL
            assertEquals "incorrect valuesUrl", "http://monkey/somewhere",opt1.realValuesUrl.toExternalForm()

    }

    @Test
    void testDecodeOptionMultivalue() {
        //secure option
        def xml1 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
        <options>
          <option name="password" value="789" multivalued="true" delimiter=","/>
        </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
"""

        def jobs = JobsXMLCodec.decode(xml1)
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()
        assertNotNull "incorrect options", jobs[0].options
        assertEquals "incorrect options size", 1, jobs[0].options.size()
        assertNotNull "missing options data", jobs[0].options.iterator().next()
        final iterator = jobs[0].options.iterator()
        def opt1 = iterator.next()
        assertEquals "incorrect name", "password", opt1.name
        assertEquals "incorrect defaultValue", "789", opt1.defaultValue
        assertEquals "incorrect enforced", "false", opt1.enforced.toString()
        assertEquals "incorrect secure", "true", opt1.multivalued.toString()
        assertEquals "incorrect secure", ",", opt1.delimiter
        assertFalse "incorrect secureInput", opt1.secureInput
        assertFalse "incorrect secureExposed", opt1.secureExposed
        assertNull "incorrect regex", opt1.regex
        assertNull "incorrect values size", opt1.values
        assertNull "missing valuesUrl", opt1.realValuesUrl

        def xml2 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
        <options>
          <option name="blah" multivalued="true" delimiter=" "/>
        </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
"""

        def jobs2 = JobsXMLCodec.decode(xml2)
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs2.size()
        assertNotNull "incorrect options", jobs2[0].options
        assertEquals "incorrect options size", 1, jobs2[0].options.size()
        assertNotNull "missing options data", jobs2[0].options.iterator().next()
        final iterator2 = jobs2[0].options.iterator()
        def opt2 = iterator2.next()
        assertEquals "incorrect name", "blah", opt2.name
        assertEquals "incorrect enforced", "false", opt2.enforced.toString()
        assertEquals "incorrect secure", "true", opt2.multivalued.toString()
        assertEquals "incorrect secure", " ", opt2.delimiter

        def xml3 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
        <options>
          <option name="blah" multivalued="true" delimiter="&gt;"/>
        </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
"""

        def jobs3 = JobsXMLCodec.decode(xml3)
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs3.size()
        assertNotNull "incorrect options", jobs3[0].options
        assertEquals "incorrect options size", 1, jobs3[0].options.size()
        assertNotNull "missing options data", jobs3[0].options.iterator().next()
        final iterator3 = jobs3[0].options.iterator()
        def opt3 = iterator3.next()
        assertEquals "incorrect name", "blah", opt3.name
        assertEquals "incorrect enforced", "false", opt3.enforced.toString()
        assertEquals "incorrect secure", "true", opt3.multivalued.toString()
        assertEquals "incorrect secure", ">", opt3.delimiter
    }

    @Test
    void testDecodeOptionSecure() {
        //secure option
        def xml1 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
        <options>
          <option name="password" value="789" secure="true" />
        </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
"""

        def jobs = JobsXMLCodec.decode(xml1)
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()
        assertNotNull "incorrect options", jobs[0].options
        assertEquals "incorrect options size", 1, jobs[0].options.size()
        assertNotNull "missing options data", jobs[0].options.iterator().next()
        def opt1 = jobs[0].options.iterator().next()
        assertEquals "incorrect name", "password", opt1.name
        assertEquals "incorrect defaultValue", "789", opt1.defaultValue
        assertEquals "incorrect enforced", "false", opt1.enforced.toString()
        assertEquals "incorrect secure", "true", opt1.secureInput.toString()
        assertFalse "incorrect secureExposed", opt1.secureExposed
        assertNull "incorrect regex", opt1.regex
        assertNull "incorrect values size", opt1.values
        assertNull "missing valuesUrl", opt1.realValuesUrl
    }

    @Test
    void testDecodeOptionSecure2() {
        //secure option
        def xml2 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
        <options>
          <option name="password" value="789" secure="true" valueExposed="true"/>
        </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
"""

        def jobs2 = JobsXMLCodec.decode(xml2)
        assertNotNull jobs2
        assertEquals "incorrect size", 1, jobs2.size()
        assertNotNull "incorrect options", jobs2[0].options
        assertEquals "incorrect options size", 1, jobs2[0].options.size()
        assertNotNull "missing options data", jobs2[0].options.iterator().next()
        def opt2 = jobs2[0].options.iterator().next()
        assertEquals "incorrect name", "password", opt2.name
        assertEquals "incorrect defaultValue", "789", opt2.defaultValue
        assertEquals "incorrect enforced", "false", opt2.enforced.toString()
        assertEquals "incorrect secure", "true", opt2.secureInput.toString()
        assertEquals "incorrect secureExposed", 'true', opt2.secureExposed.toString()
        assertNull "incorrect regex", opt2.regex
        assertNull "incorrect values size", opt2.values
        assertNull "missing valuesUrl", opt2.realValuesUrl
    }

    @Test
    void testDecodeOptionSecure3() {
        //secure option
        def xml3 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
        <options>
          <option name="password" value="789" secure="true" valueExposed="false"/>
        </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
"""

        def jobs3 = JobsXMLCodec.decode(xml3)
        assertNotNull jobs3
        assertEquals "incorrect size", 1, jobs3.size()
        assertNotNull "incorrect options", jobs3[0].options
        assertEquals "incorrect options size", 1, jobs3[0].options.size()
        assertNotNull "missing options data", jobs3[0].options.iterator().next()
        def opt3 = jobs3[0].options.iterator().next()
        assertEquals "incorrect name", "password", opt3.name
        assertEquals "incorrect defaultValue", "789", opt3.defaultValue
        assertEquals "incorrect enforced", "false", opt3.enforced.toString()
        assertEquals "incorrect secure", "true", opt3.secureInput.toString()
        assertEquals "incorrect secureExposed", 'false', opt3.secureExposed.toString()
        assertNull "incorrect regex", opt3.regex
        assertNull "incorrect values size", opt3.values
        assertNull "missing valuesUrl", opt3.realValuesUrl
    }

    @Test
    void testDecodeOptionSecure4(){
        //secure option
        def xml4 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
        <options>
          <option name="password" value="789" secure="false" valueExposed="false"/>
        </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
"""

        def jobs4 = JobsXMLCodec.decode(xml4)
        assertNotNull jobs4
        assertEquals "incorrect size", 1, jobs4.size()
        assertNotNull "incorrect options", jobs4[0].options
        assertEquals "incorrect options size", 1, jobs4[0].options.size()
        assertNotNull "missing options data", jobs4[0].options.iterator().next()
        def opt4 = jobs4[0].options.iterator().next()
        assertEquals "incorrect name", "password", opt4.name
        assertEquals "incorrect defaultValue", "789", opt4.defaultValue
        assertEquals "incorrect enforced", "false", opt4.enforced.toString()
        assertEquals "incorrect secure", "false", opt4.secureInput.toString()
        assertEquals "incorrect secureExposed", 'false',opt4.secureExposed.toString()
        assertNull "incorrect regex", opt4.regex
        assertNull "incorrect values size", opt4.values
        assertNull "missing valuesUrl", opt4.realValuesUrl

    }

    @Test
    void testDecodeOptionSecureDefaultStoragePath(){
        //secure option
        def xml4 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
        <options>
          <option name="password" value="789" secure="true" valueExposed="false" storagePath="keys/abc"/>
        </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
"""

        def jobs4 = JobsXMLCodec.decode(xml4)
        assertNotNull jobs4
        assertEquals "incorrect size", 1, jobs4.size()
        assertNotNull "incorrect options", jobs4[0].options
        assertEquals "incorrect options size", 1, jobs4[0].options.size()
        assertNotNull "missing options data", jobs4[0].options.iterator().next()
        def opt4 = jobs4[0].options.iterator().next()
        assertEquals "incorrect name", "password", opt4.name
        assertEquals "incorrect secure", "true", opt4.secureInput.toString()
        assertEquals "incorrect storagepath", "keys/abc", opt4.defaultStoragePath

    }


    @Test
    void testDecodeOptionsPreserveOrder() {
        //secure option
        def xml1 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
        <options preserveOrder="true">
          <option name="zxy" value="789" multivalued="true" delimiter=","/>
          <option name="abc" value="789" multivalued="true" delimiter=","/>
          <option name="wxy" value="789" multivalued="true" delimiter=","/>
        </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
"""

        def jobs = JobsXMLCodec.decode(xml1)
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()
        assertNotNull "incorrect options", jobs[0].options
        assertEquals "incorrect options size", 3, jobs[0].options.size()
        assertEquals(['zxy','abc','wxy'], jobs[0].options*.name)

    }

    @Test
    void testDecodeNotification(){

        //onsuccess notification
        def xml1 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <notification>
        <onsuccess>
            <email recipients="a@example.com,b@example.com"/>
        </onsuccess>
    </notification>
  </job>
</joblist>
"""

            def jobs = JobsXMLCodec.decode(xml1)
            assertNotNull jobs
            assertEquals "incorrect size", 1, jobs.size()
            assertNotNull "missing notifications", jobs[0].notifications
            assertEquals "incorrect notifications size", 1,jobs[0].notifications.size()
            def onsuccess = jobs[0].notifications.find{'onsuccess'==it.eventTrigger}
        assertNotNull "missing notifications onsuccess", onsuccess
            assertNotNull "missing notifications onsuccess email", onsuccess.content
            assertEquals "incorrect email content", "a@example.com,b@example.com", onsuccess.mailConfiguration().recipients
        //onfailure notification
        def xml2 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <notification>
        <onfailure>
            <email recipients="c@example.com,d@example.com"/>
        </onfailure>
    </notification>
  </job>
</joblist>
"""

            jobs = JobsXMLCodec.decode(xml2)
            assertNotNull jobs
            assertEquals "incorrect size", 1, jobs.size()
            assertNotNull "missing notifications", jobs[0].notifications
            assertEquals "incorrect notifications size", 1,jobs[0].notifications.size()
            def onfailure = jobs[0].notifications.find{'onfailure'==it.eventTrigger}
            assertNotNull "missing notifications onfailure", onfailure
            assertNotNull "missing notifications onfailure email", onfailure.content
            assertEquals "incorrect email content", "c@example.com,d@example.com", onfailure.mailConfiguration()
                    .recipients
        //onfailure and onsuccess notification
        def xml3 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <notification>
        <onsuccess>
            <email recipients="z@example.com,x@example.com" subject="success1"/>
        </onsuccess>
        <onfailure>
            <email recipients="c@example.com,d@example.com" subject="fail1"/>
        </onfailure>
        <onstart>
            <email recipients="h@example.com,j@example.com" subject="start1"/>
        </onstart>
    </notification>
  </job>
</joblist>
"""

            jobs = JobsXMLCodec.decode(xml3)
            assertNotNull jobs
            assertEquals "incorrect size", 1, jobs.size()
            assertNotNull "missing notifications", jobs[0].notifications
            assertEquals "incorrect notifications size", 3,jobs[0].notifications.size()
        onfailure = jobs[0].notifications.find{'onfailure'==it.eventTrigger}
            assertNotNull "missing notifications onfailure", onfailure
            assertNotNull "missing notifications onfailure email", onfailure.content
            assertEquals "incorrect email content", "c@example.com,d@example.com", onfailure.mailConfiguration()
                    .recipients
        assertEquals "incorrect email content", "fail1", onfailure.mailConfiguration().subject
        onsuccess = jobs[0].notifications.find{'onsuccess'==it.eventTrigger}
        assertNotNull "missing notifications onsuccess", onsuccess
            assertNotNull "missing notifications onsuccess email", onsuccess.content
            assertEquals "incorrect email content", "z@example.com,x@example.com", onsuccess.mailConfiguration()
                    .recipients
            assertEquals "incorrect email content", "success1", onsuccess.mailConfiguration()
                    .subject
             def onstart = jobs[0].notifications.find{'onstart'==it.eventTrigger}
        assertNotNull "missing notifications onstart", onstart
            assertNotNull "missing notifications onstart email", onstart.content
            assertEquals "incorrect email content onstart", "h@example.com,j@example.com", onstart.mailConfiguration()
                    .recipients
            assertEquals "incorrect email content onstart", "start1", onstart.mailConfiguration().subject


        //onsuccess notification wit attached inline
        def xml4 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <notification>
        <onsuccess>
            <email recipients="a@example.com,b@example.com" attachLogInline="true"/>
        </onsuccess>
    </notification>
  </job>
</joblist>
"""

        jobs = JobsXMLCodec.decode(xml4)
        assertNotNull jobs
        onsuccess = jobs[0].notifications.find{'onsuccess'==it.eventTrigger}
        assertNotNull "missing notifications onsuccess", onsuccess
        assertNotNull "missing notifications onsuccess email", onsuccess.content
        assertEquals "incorrect email content", "a@example.com,b@example.com", onsuccess.mailConfiguration().recipients
        assertEquals "incorrect email attach settings", true, onsuccess.mailConfiguration().attachLogInline
    }

    @Test
    void testDecodeNotificationPlugin() {

        //onsuccess notification
        def xml1 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <notification>
        <onsuccess>
            <plugin type="test1">
                <configuration>
                    <entry key="name" value="test"/>
                    <entry key="key" value="value"/>
                </configuration>
            </plugin>
        </onsuccess>
    </notification>
  </job>
</joblist>
"""

        def jobs = JobsXMLCodec.decode(xml1)
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()
        assertNotNull "missing notifications", jobs[0].notifications
        assertEquals "incorrect notifications size", 1, jobs[0].notifications.size()
        final def onsuccess = jobs[0].notifications.find { 'onsuccess' == it.eventTrigger }
        assertNotNull "missing notifications onsuccess", onsuccess
        assertNotNull "missing content", onsuccess.content
        assertEquals "test1", onsuccess.type
        assertEquals([key:'value',name:'test'], onsuccess.configuration)

    }

    @Test
    void testDecodeNotificationPluginMulti() {

        //onsuccess notification
        def xml1 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <notification>
        <onsuccess>
            <plugin type="test1">
                <configuration>
                <entry key="name" value="test"/>
                <entry key="key" value="value"/>
                </configuration>
            </plugin>
            <plugin type="test2">
                <configuration>
                <entry key="name2" value="test2"/>
                </configuration>
            </plugin>
        </onsuccess>
        <onfailure>
            <plugin type="test3">
                <configuration>
                <entry key="name3" value="test3"/>
                <entry key="key3" value="value3"/>
                </configuration>
            </plugin>
            <plugin type="test4">
                <configuration>
                <entry key="name4" value="test4"/>
                </configuration>
            </plugin>
        </onfailure>
        <onstart>
            <plugin type="test5">
                <configuration>
                <entry key="name5" value="test5"/>
                <entry key="key5" value="value5"/>
                </configuration>
            </plugin>
            <plugin type="test6">
                <configuration>
                <entry key="name6" value="test6"/>
                </configuration>
            </plugin>
        </onstart>
    </notification>
  </job>
</joblist>
"""

        def jobs = JobsXMLCodec.decode(xml1)
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()
        assertNotNull "missing notifications", jobs[0].notifications
        assertEquals "incorrect notifications size", 6, jobs[0].notifications.size()

        final def onsuccess = jobs[0].notifications.findAll { 'onsuccess' == it.eventTrigger } as List
        assertNotNull "missing notifications onsuccess", onsuccess
        assertEquals (2, onsuccess.size())
        def on1=onsuccess.find{it.type=='test1'}
        assertNotNull "missing content", on1
        assertNotNull "missing content", on1.content
        assertEquals "test1", on1.type
        assertEquals([key:'value',name:'test'], on1.configuration)

        def on2 = onsuccess.find { it.type == 'test2' }
        assertNotNull "missing content", on2
        assertNotNull "missing content", on2.content
        assertEquals "test2", on2.type
        assertEquals([name2:'test2'], on2.configuration)


        final def onfailure = jobs[0].notifications.findAll { 'onfailure' == it.eventTrigger } as List
        assertNotNull "missing notifications onfailure", onfailure
        assertEquals(2, onfailure.size())
        def on3 = onfailure.find { it.type == 'test3' }
        assertNotNull "missing content", on3
        assertNotNull "missing content", on3.content
        assertEquals "test3", on3.type
        assertEquals([key3: 'value3', name3: 'test3'], on3.configuration)

        def on4 = onfailure.find { it.type == 'test4' }
        assertNotNull "missing content", on4
        assertNotNull "missing content", on4.content
        assertEquals "test4", on4.type
        assertEquals([name4: 'test4'], on4.configuration)

        final def onstart = jobs[0].notifications.findAll { 'onstart' == it.eventTrigger } as List
        assertNotNull "missing notifications onstart", onstart
        assertEquals(2, onstart.size())
        def on5 = onstart.find { it.type == 'test5' }
        assertNotNull "missing content", on5
        assertNotNull "missing content", on5.content
        assertEquals "test5", on5.type
        assertEquals([key5: 'value5', name5: 'test5'], on5.configuration)

        def on6 = onstart.find { it.type == 'test6' }
        assertNotNull "missing content", on6
        assertNotNull "missing content", on6.content
        assertEquals "test6", on6.type
        assertEquals([name6: 'test6'], on6.configuration)

    }

    @Test
    void testDecodeNotificationFailure(){

        //missing notification handler
        def xml0 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <notification>
    </notification>
  </job>
</joblist>
"""

        try {
            def jobs = JobsXMLCodec.decode(xml0)
            fail "parsing should have failed"
        } catch (Exception e) {
            assertEquals ("notification section had no trigger elements",e.message)
        }
        //missing email element
        def xml1 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <notification>
        <onsuccess>

        </onsuccess>
    </notification>
  </job>
</joblist>
"""

        try {
            def jobs = JobsXMLCodec.decode(xml1)
            fail "parsing should have failed"
        } catch (Exception e) {
            assertEquals ("notification 'onsuccess' element had missing 'email' or 'webhook' or 'plugin' element",e.message)
        }
        //missing email attribute
        def xml2 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <notification>
        <onsuccess>
            <email />
        </onsuccess>
    </notification>
  </job>
</joblist>
"""

        try {
            def jobs = JobsXMLCodec.decode(xml2)
            fail "parsing should have failed"
        } catch (Exception e) {
            assertEquals ("onsuccess email had blank or missing 'recipients' attribute",e.message)
        }
        //onfailure and onsuccess notification
        def xml3 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <notification>
        <onsuccess>
            <email recipients=""/>
        </onsuccess>
    </notification>
  </job>
</joblist>
"""


        try {
            def jobs = JobsXMLCodec.decode(xml3)
            fail "parsing should have failed"
        } catch (Exception e) {
            assertEquals ("onsuccess email had blank or missing 'recipients' attribute",e.message)
        }


        //missing email element
        def xml4 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <notification>
        <onfailure>

        </onfailure>
    </notification>
  </job>
</joblist>
"""

        try {
            def jobs = JobsXMLCodec.decode(xml4)
            fail "parsing should have failed"
        } catch (Exception e) {
            assertEquals ("notification 'onfailure' element had missing 'email' or 'webhook' or 'plugin' element",e.message)
        }
        //missing email attribute
        def xml5 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <notification>
        <onfailure>
            <email />
        </onfailure>
    </notification>
  </job>
</joblist>
"""

        try {
            def jobs = JobsXMLCodec.decode(xml5)
            fail "parsing should have failed"
        } catch (Exception e) {
            assertEquals ("onfailure email had blank or missing 'recipients' attribute",e.message)
        }
        //onfailure and onsuccess notification
        def xml6 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <notification>
        <onfailure>
            <email recipients=""/>
        </onfailure>
    </notification>
  </job>
</joblist>
"""


        try {
            def jobs = JobsXMLCodec.decode(xml6)
            fail "parsing should have failed"
        } catch (Exception e) {
            assertEquals ("onfailure email had blank or missing 'recipients' attribute",e.message)
        }
    }

    @Test
    void testEncodeScheduled(){
         def XmlParser parser = new XmlParser()
        def jobs1 = [
                new ScheduledExecution(
                        jobName:'test job 1',
                        description:'test descrip',
                        loglevel: 'INFO',
                        project:'test1',
                        workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                        options:[new Option([name:'delay',defaultValue:'12']), new Option([name:'monkey',defaultValue:'cheese']), new Option([name:'particle',defaultValue:'true'])] as TreeSet,
                        nodeThreadcount:1,
                        nodeKeepgoing:true,
                        doNodedispatch:true,
                        scheduled:true,
                        hour:'12',
                        minute:'42',
                        dayOfWeek:'*',
                        month:'*'
                        )
        ]

        if(true){
            def xmlstr = JobsXMLCodec.encode(jobs1)
            assertNotNull xmlstr
            assertTrue xmlstr instanceof String

            def doc = parser.parse(new StringReader(xmlstr))
            assertNotNull doc
            assertEquals "wrong root node name",'joblist',doc.name()
            assertEquals "wrong number of jobs",1,doc.job.size()

            assertEquals "missing schedule",1,doc.job[0].schedule.size()
            assertEquals "missing schedule/time",1,doc.job[0].schedule[0].time.size()
            assertEquals "incorrect schedule/time/@hour",'12',doc.job[0].schedule[0].time[0]['@hour']
            assertEquals "incorrect schedule/time/@minute",'42',doc.job[0].schedule[0].time[0]['@minute']
            assertEquals "missing schedule/weekday",1,doc.job[0].schedule[0].weekday.size()
            assertEquals "incorrect schedule/weekday/@day",'*',doc.job[0].schedule[0].weekday[0]['@day']
            assertEquals "missing schedule/month",1,doc.job[0].schedule[0].month.size()
            assertEquals "incorrect schedule/month/@month",'*',doc.job[0].schedule[0].month[0]['@month']
        }
        def jobs2 = [
                new ScheduledExecution(
                        jobName:'test job 1',
                        description:'test descrip',
                        loglevel: 'INFO',
                        project:'test1',
                        workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                        options:[new Option([name:'delay',defaultValue:'12']), new Option([name:'monkey',defaultValue:'cheese']), new Option([name:'particle',defaultValue:'true'])] as TreeSet,
                        nodeThreadcount:1,
                        nodeKeepgoing:true,
                        scheduled:true,
                        hour:'12',
                        minute:'42',
                        dayOfWeek:'Mon,Tue,Wed,Sat',
                        month:'Jan,Feb,Mar,Jun,Jul'
                )
        ]

        if(true){
            def xmlstr = JobsXMLCodec.encode(jobs2)
            assertNotNull xmlstr
            assertTrue xmlstr instanceof String


            def doc = parser.parse(new StringReader(xmlstr))
            assertNotNull doc
            assertEquals "wrong root node name",'joblist',doc.name()
            assertEquals "wrong number of jobs",1,doc.job.size()

            assertEquals "missing schedule",1,doc.job[0].schedule.size()
            assertEquals "missing schedule/time",1,doc.job[0].schedule[0].time.size()
            assertEquals "incorrect schedule/time/@hour",'12',doc.job[0].schedule[0].time[0]['@hour']
            assertEquals "incorrect schedule/time/@minute",'42',doc.job[0].schedule[0].time[0]['@minute']
            assertEquals "missing schedule/weekday",1,doc.job[0].schedule[0].weekday.size()
            assertEquals "incorrect schedule/weekday/@day",'Mon,Tue,Wed,Sat',doc.job[0].schedule[0].weekday[0]['@day']
            assertEquals "missing schedule/month",1,doc.job[0].schedule[0].month.size()
            assertEquals "incorrect schedule/month/@month",'Jan,Feb,Mar,Jun,Jul',doc.job[0].schedule[0].month[0]['@month']
        }

        //use extended schedule properties: year, seconds, dayOfMonth//

        //use dayOfMonth
        def jobs3 = [
                new ScheduledExecution(
                        jobName:'test job 1',
                        description:'test descrip',
                        loglevel: 'INFO',
                        project:'test1',
                        workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                        options:[new Option([name:'delay',defaultValue:'12']), new Option([name:'monkey',defaultValue:'cheese']), new Option([name:'particle',defaultValue:'true'])] as TreeSet,
                        nodeThreadcount:1,
                        nodeKeepgoing:true,
                        scheduled:true,
                        hour:'12',
                        minute:'42',
                        dayOfWeek:'?',
                        dayOfMonth:'20',
                        month:'Jan,Feb,Mar,Jun,Jul'
                )
        ]

        if(true){
            def xmlstr = JobsXMLCodec.encode(jobs3)
            assertNotNull xmlstr
            assertTrue xmlstr instanceof String


            def doc = parser.parse(new StringReader(xmlstr))
            assertNotNull doc
            assertEquals "wrong root node name",'joblist',doc.name()
            assertEquals "wrong number of jobs",1,doc.job.size()

            assertEquals "missing schedule",1,doc.job[0].schedule.size()
            assertEquals "missing schedule/time",1,doc.job[0].schedule[0].time.size()
            assertEquals "incorrect schedule/time/@hour",'12',doc.job[0].schedule[0].time[0]['@hour']
            assertEquals "incorrect schedule/time/@minute",'42',doc.job[0].schedule[0].time[0]['@minute']
            assertEquals "unexpected schedule/weekday",0,doc.job[0].schedule[0].weekday.size()
            assertEquals "missing schedule/month",1,doc.job[0].schedule[0].month.size()
            assertEquals "incorrect schedule/month/@month",'Jan,Feb,Mar,Jun,Jul',doc.job[0].schedule[0].month[0]['@month']
            assertEquals "incorrect schedule/month/@day",'20',doc.job[0].schedule[0].month[0]['@day']
        }
        //use year
        def jobs4 = [
                new ScheduledExecution(
                        jobName:'test job 1',
                        description:'test descrip',
                        loglevel: 'INFO',
                        project:'test1',
                        workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                        options:[new Option([name:'delay',defaultValue:'12']), new Option([name:'monkey',defaultValue:'cheese']), new Option([name:'particle',defaultValue:'true'])] as TreeSet,

                        nodeThreadcount:1,
                        nodeKeepgoing:true,
                        scheduled:true,
                        hour:'12',
                        minute:'42',
                        dayOfWeek:'?',
                        dayOfMonth:'20',
                        month:'Jan,Feb,Mar,Jun,Jul',
                        year:'2010'
                )
        ]

        if(true){
            def xmlstr = JobsXMLCodec.encode(jobs4)
            assertNotNull xmlstr
            assertTrue xmlstr instanceof String


            def doc = parser.parse(new StringReader(xmlstr))
            assertNotNull doc
            assertEquals "wrong root node name",'joblist',doc.name()
            assertEquals "wrong number of jobs",1,doc.job.size()

            assertEquals "missing schedule",1,doc.job[0].schedule.size()
            assertEquals "missing schedule/time",1,doc.job[0].schedule[0].time.size()
            assertEquals "incorrect schedule/time/@hour",'12',doc.job[0].schedule[0].time[0]['@hour']
            assertEquals "incorrect schedule/time/@minute",'42',doc.job[0].schedule[0].time[0]['@minute']
            assertEquals "unexpected schedule/weekday",0,doc.job[0].schedule[0].weekday.size()
            assertEquals "missing schedule/month",1,doc.job[0].schedule[0].month.size()
            assertEquals "incorrect schedule/month/@month",'Jan,Feb,Mar,Jun,Jul',doc.job[0].schedule[0].month[0]['@month']
            assertEquals "incorrect schedule/month/@day",'20',doc.job[0].schedule[0].month[0]['@day']
            assertEquals "missing schedule/year",1,doc.job[0].schedule[0].year.size()
            assertEquals "incorrect schedule/year/@year",'2010',doc.job[0].schedule[0].year[0]['@year']
        }
        //use seconds
        def jobs5 = [
                new ScheduledExecution(
                        jobName:'test job 1',
                        description:'test descrip',
                        loglevel: 'INFO',
                        project:'test1',
                        workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                        options:[new Option([name:'delay',defaultValue:'12']), new Option([name:'monkey',defaultValue:'cheese']), new Option([name:'particle',defaultValue:'true'])] as TreeSet,

                        nodeThreadcount:1,
                        nodeKeepgoing:true,
                        scheduled:true,
                        hour:'12',
                        minute:'42',
                        seconds:'*/5',
                        dayOfWeek:'?',
                        dayOfMonth:'20',
                        month:'Jan,Feb,Mar,Jun,Jul',
                        year:'2010'
                )
        ]

        if(true){
            def xmlstr = JobsXMLCodec.encode(jobs5)
            assertNotNull xmlstr
            assertTrue xmlstr instanceof String


            def doc = parser.parse(new StringReader(xmlstr))
            assertNotNull doc
            assertEquals "wrong root node name",'joblist',doc.name()
            assertEquals "wrong number of jobs",1,doc.job.size()

            assertEquals "missing schedule",1,doc.job[0].schedule.size()
            assertEquals "missing schedule/time",1,doc.job[0].schedule[0].time.size()
            assertEquals "incorrect schedule/time/@hour",'12',doc.job[0].schedule[0].time[0]['@hour']
            assertEquals "incorrect schedule/time/@minute",'42',doc.job[0].schedule[0].time[0]['@minute']
            assertEquals "incorrect schedule/time/@seconds",'*/5',doc.job[0].schedule[0].time[0]['@seconds']
            assertEquals "unexpected schedule/weekday",0,doc.job[0].schedule[0].weekday.size()
            assertEquals "missing schedule/month",1,doc.job[0].schedule[0].month.size()
            assertEquals "incorrect schedule/month/@month",'Jan,Feb,Mar,Jun,Jul',doc.job[0].schedule[0].month[0]['@month']
            assertEquals "incorrect schedule/month/@day",'20',doc.job[0].schedule[0].month[0]['@day']
            assertEquals "missing schedule/year",1,doc.job[0].schedule[0].year.size()
            assertEquals "incorrect schedule/year/@year",'2010',doc.job[0].schedule[0].year[0]['@year']
        }

    }








    @Test
    void testEncodeDecode(){
        def XmlParser parser = new XmlParser()

        //test multiline script content
        def jobs3 = [
                new ScheduledExecution(
                        jobName:'test job 1',
                        description:'test descrip',
                        loglevel: 'INFO',
                        project:'test1',
                        //type
                        //name
                        //command

                        workflow: new Workflow(keepgoing: true, commands: [new CommandExec(
                            argString:'elf biscuits',
                            adhocExecution:true,
                            adhocLocalString:'#!/bin/bash\n\necho what is this monkey < test.out\n\necho this is a test\n\nexit 0',
                                            
                            )]),
                        nodeThreadcount:1,
                        nodeKeepgoing:true,
                )
        ]

            def xmlstr = JobsXMLCodec.encode(jobs3)
            assertNotNull xmlstr
            assertTrue xmlstr instanceof String

            def jobs=JobsXMLCodec.decode(xmlstr)
            assertNotNull jobs
            assertEquals "incorrect size",1,jobs.size()
            def job1=jobs[0]
        assertNotNull(job1.workflow)
        assertNotNull(job1.workflow.commands)
        assertEquals(1,job1.workflow.commands.size())
        def wfi=job1.workflow.commands[0]
            assertEquals "incorrect adhocLocalString","#!/bin/bash\n\necho what is this monkey < test.out\n\necho this is a test\n\nexit 0",wfi.adhocLocalString
            assertEquals "incorrect argString","elf biscuits",wfi.argString
            assertEquals "incorrect adhocRemoteString",null,wfi.adhocRemoteString
            assertEquals "incorrect adhocFilepath",null,wfi.adhocFilepath
            
    }

    @Test
    void testEncodeLoglimit() {
        def XmlParser parser = new XmlParser()

        //set node dispatch to false, and assert no nodefilters are generated
        def jobs1 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        workflow: new Workflow(
                                keepgoing: true,
                                commands: [new CommandExec(adhocExecution: true, adhocLocalString: 'test',)]
                        ),
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        logOutputThreshold: '20MB',
                        logOutputThresholdAction: 'halt',
                        logOutputThresholdStatus: 'failed',
                )
        ]

        def xmlstr = JobsXMLCodec.encode(jobs1)
        assertNotNull xmlstr
        assertTrue xmlstr instanceof String


        def doc = parser.parse(new StringReader(xmlstr))
        assertNotNull doc
        assertEquals "missing logging", 1, doc.job[0].logging.size()
        assertEquals "missing logging/@limit", '20MB', doc.job[0].logging[0].'@limit'
        assertEquals "missing logging/@limitAction", 'halt', doc.job[0].logging[0].'@limitAction'
        assertEquals "missing logging/@status", 'failed', doc.job[0].logging[0].'@status'
    }

    @Test
    void testEncodeLoglimitCustomStatus() {
        def XmlParser parser = new XmlParser()

        //set node dispatch to false, and assert no nodefilters are generated
        def jobs1 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        workflow: new Workflow(
                                keepgoing: true,
                                commands: [new CommandExec(adhocExecution: true, adhocLocalString: 'test',)]
                        ),
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        logOutputThreshold: '20MB',
                        logOutputThresholdAction: 'halt',
                        logOutputThresholdStatus: 'mystatus',
                )
        ]

        def xmlstr = JobsXMLCodec.encode(jobs1)
        assertNotNull xmlstr
        assertTrue xmlstr instanceof String


        def doc = parser.parse(new StringReader(xmlstr))
        assertNotNull doc
        assertEquals "missing logging", 1, doc.job[0].logging.size()
        assertEquals "missing logging/@limit", '20MB', doc.job[0].logging[0].'@limit'
        assertEquals "missing logging/@limitAction", 'halt', doc.job[0].logging[0].'@limitAction'
        assertEquals "missing logging/@status", 'mystatus', doc.job[0].logging[0].'@status'
    }

    @Test
    void testEncodeNodefilter(){
        def XmlParser parser = new XmlParser()

        //set node dispatch to false, and assert no nodefilters are generated
        def jobs1 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        workflow: new Workflow(
                                keepgoing: true,
                                commands: [new CommandExec(adhocExecution: true, adhocLocalString: 'test',)]
                        ),
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: false,
                        nodeInclude: 'myhostname',
                        nodeIncludeTags: 'a+b,c',
                        nodeIncludeOsName: 'Windows.*',
                        nodeIncludeOsFamily: 'windows',
                        nodeIncludeOsArch: 'x86,sparc',
                        nodeIncludeOsVersion: '4\\..*',
                        nodeIncludeName: 'mynode'
                )
        ]

        def xmlstr = JobsXMLCodec.encode(jobs1)
        assertNotNull xmlstr
        assertTrue xmlstr instanceof String


        def doc = parser.parse(new StringReader(xmlstr))
        assertNotNull doc
        assertEquals "missing nodefilters", 0, doc.job[0].nodefilters.size()
    }

    @Test
    void testEncodeNodefilter2() {
        def XmlParser parser = new XmlParser()
        //set node dispatch to true, and assert 'include' nodefilters are generated
        def jobs2 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        workflow: new Workflow(
                                keepgoing: true,
                                commands: [new CommandExec(adhocExecution: true, adhocLocalString: 'test',)]
                        ),
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true,
                        nodeInclude: 'myhostname',
                        nodeIncludeTags: 'a+b,c',
                        nodeIncludeOsName: 'Windows.*',
                        nodeIncludeOsFamily: 'windows',
                        nodeIncludeOsArch: 'x86,sparc',
                        nodeIncludeOsVersion: '4\\..*',
                        nodeIncludeName: 'mynode'
                )
        ]

        def xmlstr = JobsXMLCodec.encode(jobs2)
        assertNotNull xmlstr
        assertTrue xmlstr instanceof String


        def doc = parser.parse(new StringReader(xmlstr))
        assertNotNull doc
        assertEquals "missing nodefilters", 1, doc.job[0].nodefilters.size()
        assertEquals "unexpected nodefilters exclude", 0, doc.job[0].nodefilters[0].exclude.size()
        assertEquals "missing nodefilters include", 0, doc.job[0].nodefilters[0].include.size()
        assertEquals "incorrect nodefilters include hostname",
                     'hostname: myhostname name: mynode tags: a+b,c os-name: Windows.* ' +
                             'os-family: windows os-arch: x86,sparc os-version: 4\\..*',
                     doc.job[0].nodefilters[0].filter[0].text()

    }

    @Test
    void testEncodeNodefilter3() {
        def XmlParser parser = new XmlParser()

        //set node dispatch to true, and assert 'exclude' nodefilters are generated
        def jobs3 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        workflow: new Workflow(
                                keepgoing: true,
                                commands: [new CommandExec(adhocExecution: true, adhocLocalString: 'test',)]
                        ),
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true,
                        nodeExclude: 'myhostname',
                        nodeExcludeTags: 'a+b,c',
                        nodeExcludeOsName: 'Windows.*',
                        nodeExcludeOsFamily: 'windows',
                        nodeExcludeOsArch: 'x86,sparc',
                        nodeExcludeOsVersion: '4\\..*',
                        nodeExcludeName: 'mynode'
                )
        ]

        def xmlstr = JobsXMLCodec.encode(jobs3)
        assertNotNull xmlstr
        assertTrue xmlstr instanceof String


        def doc = parser.parse(new StringReader(xmlstr))
        assertNotNull doc
        assertEquals "missing nodefilters", 1, doc.job[0].nodefilters.size()
        assertEquals "unexpected nodefilters include", 0, doc.job[0].nodefilters[0].include.size()
        assertEquals "missing nodefilters exclude", 0, doc.job[0].nodefilters[0].exclude.size()
        assertEquals "incorrect nodefilters string", '!hostname: myhostname !name: mynode !tags: a+b,' +
                'c !os-name: Windows.* ' +
                '!os-family: windows !os-arch: x86,sparc !os-version: 4\\..*',
                     doc.job[0].nodefilters[0].filter[0].text()

    }

    @Test
    void testEncodeNodefilter4(){
        def XmlParser parser = new XmlParser()

        //set node dispatch to true, and assert both 'include' and 'exclude' nodefilters are generated
        def jobs4 = [
                new ScheduledExecution(
                        jobName:'test job 1',
                        description:'test descrip',
                        loglevel: 'INFO',
                        project:'test1',
                      workflow: new Workflow(keepgoing: true, commands: [new CommandExec(adhocExecution: true, adhocLocalString: 'test',)]),
                        nodeThreadcount:1,
                        nodeKeepgoing:true,
                        doNodedispatch:true,
                        nodeExclude:'myhostname',
                            nodeExcludeTags:'a+b,c',
                        nodeExcludeOsName:'Windows.*',
                        nodeExcludeOsFamily:'windows',
                        nodeExcludeOsArch:'x86,sparc',
                        nodeExcludeOsVersion:'4\\..*',
                        nodeExcludeName:'mynode',

                        nodeInclude:'anotherhost',
                        nodeIncludeTags:'prod',
                        nodeIncludeOsName:'Mac.*',
                        nodeIncludeOsFamily:'unix',
                        nodeIncludeOsArch:'686',
                        nodeIncludeOsVersion:'10\\..*',
                        nodeIncludeName:'annode'
                )
        ]

        def xmlstr = JobsXMLCodec.encode(jobs4)
        assertNotNull xmlstr
        assertTrue xmlstr instanceof String


        def doc = parser.parse(new StringReader(xmlstr))
        assertNotNull doc
        assertEquals "missing nodefilters",1,doc.job[0].nodefilters.size()
        assertEquals "missing nodefilters exclude",0,doc.job[0].nodefilters[0].exclude.size()
        assertEquals "missing nodefilters exclude",0,doc.job[0].nodefilters[0].include.size()
        assertEquals "incorrect nodefilters include hostname", 'hostname: anotherhost name: annode tags: prod os-name: Mac.* os-family: unix os-arch: 686 os-version: 10\\..* ' +
                '!hostname: myhostname !name: mynode !tags: a+b,c !os-name: Windows.* !os-family: windows !os-arch: x86,' +
                'sparc !os-version: 4\\..*', doc.job[0].nodefilters[0].filter[0].text()


    }

    @Test
    void testEncodeNodefilter_filterstring(){
        def XmlParser parser = new XmlParser()

        //set node dispatch to true, and assert both 'include' and 'exclude' nodefilters are generated
        def jobs4 = [
                new ScheduledExecution(
                        jobName:'test job 1',
                        description:'test descrip',
                        loglevel: 'INFO',
                        project:'test1',
                      workflow: new Workflow(keepgoing: true, commands: [new CommandExec(adhocExecution: true, adhocLocalString: 'test',)]),
                        nodeThreadcount:1,
                        nodeKeepgoing:true,
                        doNodedispatch:true,
                        filter: 'hostname: anotherhost name: annode tags: prod os-name: Mac.* os-family: unix os-arch: 686 os-version: 10\\..* ' +
                                '!hostname: myhostname !name: mynode !tags: a+b,c !os-name: Windows.* !os-family: windows !os-arch: x86,' +
                                'sparc !os-version: 4\\..*'
                )
        ]

        def xmlstr = JobsXMLCodec.encode(jobs4)
        assertNotNull xmlstr
        assertTrue xmlstr instanceof String


        def doc = parser.parse(new StringReader(xmlstr))
        assertNotNull doc
        assertEquals "missing nodefilters",1,doc.job[0].nodefilters.size()
        assertEquals "missing nodefilters exclude",0,doc.job[0].nodefilters[0].exclude.size()
        assertEquals "missing nodefilters exclude",0,doc.job[0].nodefilters[0].include.size()
        assertEquals "incorrect nodefilters include hostname", 'hostname: anotherhost name: annode tags: prod os-name: Mac.* os-family: unix os-arch: 686 os-version: 10\\..* ' +
                '!hostname: myhostname !name: mynode !tags: a+b,c !os-name: Windows.* !os-family: windows !os-arch: x86,' +
                'sparc !os-version: 4\\..*', doc.job[0].nodefilters[0].filter[0].text()


    }

    @Test
    void testEncodeNodefilter_nodesSelectedByDefaultFalse(){
        def XmlParser parser = new XmlParser()

        //set node dispatch to true, and assert both 'include' and 'exclude' nodefilters are generated
        def jobs4 = [
                new ScheduledExecution(
                        jobName:'test job 1',
                        description:'test descrip',
                        loglevel: 'INFO',
                        project:'test1',
                      workflow: new Workflow(keepgoing: true, commands: [new CommandExec(adhocExecution: true, adhocLocalString: 'test',)]),
                        nodeThreadcount:1,
                        nodeKeepgoing:true,
                        doNodedispatch:true,
                        nodesSelectedByDefault:false,
                        filter: 'hostname: anotherhost'
                )
        ]

        def xmlstr = JobsXMLCodec.encode(jobs4)
        assertNotNull xmlstr
        assertTrue xmlstr instanceof String


        def doc = parser.parse(new StringReader(xmlstr))
        assertNotNull doc
        assertEquals "missing nodefilters",1,doc.job[0].nodefilters.size()
        assertEquals "incorrect nodesSelectedByDefault: ${xmlstr}", 'false', doc.job[0].nodesSelectedByDefault[0]?.text()
    }

    @Test
    void testEncodeNodefilter_nodesSelectedByDefaultTrue(){
        def XmlParser parser = new XmlParser()

        //set node dispatch to true, and assert both 'include' and 'exclude' nodefilters are generated
        def jobs4 = [
                new ScheduledExecution(
                        jobName:'test job 1',
                        description:'test descrip',
                        loglevel: 'INFO',
                        project:'test1',
                      workflow: new Workflow(keepgoing: true, commands: [new CommandExec(adhocExecution: true, adhocLocalString: 'test',)]),
                        nodeThreadcount:1,
                        nodeKeepgoing:true,
                        doNodedispatch:true,
                        nodesSelectedByDefault:true,
                        filter: 'hostname: anotherhost'
                )
        ]

        def xmlstr = JobsXMLCodec.encode(jobs4)
        assertNotNull xmlstr
        assertTrue xmlstr instanceof String


        def doc = parser.parse(new StringReader(xmlstr))
        assertNotNull doc
        assertEquals "missing nodefilters",1,doc.job[0].nodefilters.size()
        assertEquals "incorrect nodefilters include hostname", 'true', doc.job[0].nodesSelectedByDefault[0]?.text()
    }

    @Test
    void testEncodeStepDescription() {
        def XmlParser parser = new XmlParser()
        //encode basic workflow with one command call
        def jobs1 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        argString: '',
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true,
                        workflow: new Workflow(keepgoing: true, commands: [
                                new CommandExec(
                                adhocExecution: true,
                                adhocRemoteString: 'aname',
                                description: 'test1'),
                                new JobExec(
                                jobName: 'jobname',
                                jobGroup: 'agroup',
                                description: 'test2'),
                                new PluginStep(
                                type: 'atype',
                                configuration: [a:1,b:2],
                                        nodeStep: true,
                                description: 'test3'),
                        ]
                        ),
                )
        ]

            def xmlstr
            xmlstr = JobsXMLCodec.encode(jobs1)
            assertNotNull xmlstr
            assertTrue xmlstr instanceof String


            def doc = parser.parse(new StringReader(xmlstr))
            assertNotNull doc
            assertEquals "missing job", 1, doc.job.size()
            assertEquals "missing context", 0, doc.job[0].context.size()
            assertEquals "missing sequence", 1, doc.job.sequence.size()
            assertEquals "wrong command count", 3, doc.job[0].sequence[0].command.size()
            doc.job[0].sequence[0].command.eachWithIndex{cmd,i->
                assertEquals "wrong description", "test${i+1}".toString(), cmd.description.text()
            }
    }

    @Test
    void testEncodeWorkflowBasic_onecommand(){
        def XmlParser parser = new XmlParser()
        //encode basic workflow with one command call
        def jobs1 = [
                new ScheduledExecution(
                        jobName:'test job 1',
                        description:'test descrip',
                        loglevel: 'INFO',
                        project:'test1',
                        argString:'',
                        nodeThreadcount:1,
                        nodeKeepgoing:true,
                        doNodedispatch:true,
                        workflow: new Workflow(keepgoing: true, commands: [new CommandExec(
                                    adhocExecution:true,
                                    adhocRemoteString:'aname',
                                )]
                            ),
                )
        ]

            def xmlstr
                xmlstr = JobsXMLCodec.encode(jobs1)
            assertNotNull xmlstr
            assertTrue xmlstr instanceof String


            def doc = parser.parse(new StringReader(xmlstr))
            assertNotNull doc
            assertEquals "missing job",1,doc.job.size()
            assertEquals "missing context",0,doc.job[0].context.size()
            assertEquals "missing sequence",1,doc.job.sequence.size()
            assertEquals "wrong keepgoing","true",doc.job[0].sequence[0]['@keepgoing']
            assertEquals "wrong keepgoing","node-first",doc.job[0].sequence[0]['@strategy']
            assertEquals "wrong command count",1,doc.job[0].sequence[0].command.size()
            assertEquals "wrong command/exec size",1, doc.job[0].sequence[0].command[0].exec.size()
            assertEquals "wrong command/exec","aname", doc.job[0].sequence[0].command[0].exec[0].text()
            assertNull "wrong command @return",doc.job[0].sequence[0].command[0]['@return']
            assertNull "wrong command @if",doc.job[0].sequence[0].command[0]['@if']
            assertNull "wrong command @unless",doc.job[0].sequence[0].command[0]['@unless']
            assertNull "wrong command @equals",doc.job[0].sequence[0].command[0]['@equals']
        }

    @Test
    void testEncodeWorkflow_threadcount(){
        def XmlParser parser = new XmlParser()
        //encode basic workflow with one command call, change threadcount
        def jobs1b = [
                new ScheduledExecution(
                        jobName:'test job 1',
                        description:'test descrip',
                        loglevel: 'INFO',
                        project:'test1',
                        argString:'',
                        nodeThreadcount:1,
                        nodeKeepgoing:true,
                        doNodedispatch:true,
                        workflow: new Workflow(threadcount:2, commands: [new CommandExec(
                                    adhocExecution:true,
                                    adhocRemoteString:'aname',
                                )]
                            )
                        )

        ]

            def xmlstr
                xmlstr= JobsXMLCodec.encode(jobs1b)

            assertNotNull xmlstr
            assertTrue xmlstr instanceof String


            def doc = parser.parse(new StringReader(xmlstr))
            assertNotNull doc
            assertEquals "missing job",1,doc.job.size()
            assertEquals "missing context",0,doc.job[0].context.size()
            assertEquals "missing sequence",1,doc.job.sequence.size()
            assertEquals "wrong keepgoing","false",doc.job[0].sequence[0]['@keepgoing']
            assertEquals "wrong keepgoing","node-first",doc.job[0].sequence[0]['@strategy']
            assertEquals "wrong command count",1,doc.job[0].sequence[0].command.size()
            assertEquals "wrong command/exec size",1, doc.job[0].sequence[0].command[0].exec.size()
            assertEquals "wrong command/exec","aname", doc.job[0].sequence[0].command[0].exec[0].text()
            assertNull "wrong command @return",doc.job[0].sequence[0].command[0]['@return']
            assertNull "wrong command @if",doc.job[0].sequence[0].command[0]['@if']
            assertNull "wrong command @unless",doc.job[0].sequence[0].command[0]['@unless']
            assertNull "wrong command @equals",doc.job[0].sequence[0].command[0]['@equals']

        }

    @Test
    void testEncodeWorkflow_attributes() {
        def XmlParser parser = new XmlParser()
        //add conditional attributes
        def jobs2 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        argString: '',
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true,

                        workflow: new Workflow(keepgoing: true, commands: [new CommandExec(
                                adhocExecution: true,
                                adhocRemoteString: 'aname',
                                )]
                        )
                )
        ]

        def xmlstr = JobsXMLCodec.encode(jobs2)
        assertNotNull xmlstr
        assertTrue xmlstr instanceof String


        def doc = parser.parse(new StringReader(xmlstr))
        assertNotNull doc
        assertEquals "missing job", 1, doc.job.size()
        assertEquals "missing context", 0, doc.job[0].context.size()
        assertEquals "missing sequence", 1, doc.job.sequence.size()
        assertEquals "wrong keepgoing", "node-first", doc.job[0].sequence[0]['@strategy']
        assertEquals "wrong command count", 1, doc.job[0].sequence[0].command.size()
        assertEquals "wrong command/exec size", 1, doc.job[0].sequence[0].command[0].exec.size()
        assertEquals "wrong command/exec", "aname", doc.job[0].sequence[0].command[0].exec[0].text()

        //XXX: conditionals not yet supported, so expect null
        /*
            assertEquals "wrong command @return",'returnproptest',doc.job[0].sequence[0].command[0]['@return']
            assertEquals "wrong command @if",'ifstringtest',doc.job[0].sequence[0].command[0]['@if']
            assertEquals "wrong command @unless",'unlessstringtest',doc.job[0].sequence[0].command[0]['@unless']
            assertEquals "wrong command @equals",'equalsstringtest',doc.job[0].sequence[0].command[0]['@equals']
            */
        assertNull "wrong command @return", doc.job[0].sequence[0].command[0]['@return']
        assertNull "wrong command @if", doc.job[0].sequence[0].command[0]['@if']
        assertNull "wrong command @unless", doc.job[0].sequence[0].command[0]['@unless']
        assertNull "wrong command @equals", doc.job[0].sequence[0].command[0]['@equals']


    }

    @Test
    void testEncodeWorkflow_command() {
        def XmlParser parser = new XmlParser()

        //test simple exec/script/scriptfile commands
        def jobs3 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        argString: '',
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true,

                        workflow: new Workflow(keepgoing: true, commands: [new CommandExec(
                                adhocExecution: true,
                                adhocRemoteString: 'a remote command',
                                argString: 'test string'
                        )]
                        )
                )
        ]

        def xmlstr = JobsXMLCodec.encode(jobs3)
        assertNotNull xmlstr
        assertTrue xmlstr instanceof String


        def doc = parser.parse(new StringReader(xmlstr))
        assertNotNull doc
        assertEquals "missing job", 1, doc.job.size()
        assertEquals "missing context", 0, doc.job[0].context.size()
        assertEquals "missing sequence", 1, doc.job.sequence.size()
        assertEquals "wrong keepgoing", "node-first", doc.job[0].sequence[0]['@strategy']
        assertEquals "wrong command count", 1, doc.job[0].sequence[0].command.size()
        assertNull "wrong command @resource", doc.job[0].sequence[0].command[0]['@resource']
        assertNull "wrong command @name", doc.job[0].sequence[0].command[0]['@name']
        assertNull "wrong command @module", doc.job[0].sequence[0].command[0]['@module']
        assertEquals "missing command/exec", 1, doc.job[0].sequence[0].command[0].exec.size()
        assertEquals "wrong command/exec", 'a remote command', doc.job[0].sequence[0].command[0].exec[0].text()
        assertEquals "wrong command/exec", 0, doc.job[0].sequence[0].command[0].script.size()
        assertEquals "wrong command/exec", 0, doc.job[0].sequence[0].command[0].scriptfile.size()
        assertEquals "wrong command/exec", 0, doc.job[0].sequence[0].command[0].scriptargs.size()

    }

    @Test
    void testEncodeWorkflow_script() {
        def XmlParser parser = new XmlParser()

        //test simple exec/script/scriptfile commands
        def jobs4 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        argString: '',
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true,

                        workflow: new Workflow(keepgoing: true, commands: [new CommandExec(
                                adhocExecution: true,
                                adhocLocalString: 'a local script command',
                                argString: 'test string'
                        )]
                        )
                )
        ]

        def xmlstr = JobsXMLCodec.encode(jobs4)
        assertNotNull xmlstr
        assertTrue xmlstr instanceof String


        def doc = parser.parse(new StringReader(xmlstr))
        assertNotNull doc
        assertEquals "missing job", 1, doc.job.size()
        assertEquals "missing context", 0, doc.job[0].context.size()
        assertEquals "missing sequence", 1, doc.job.sequence.size()
        assertEquals "wrong keepgoing", "node-first", doc.job[0].sequence[0]['@strategy']
        assertEquals "wrong command count", 1, doc.job[0].sequence[0].command.size()
        assertNull "wrong command @resource", doc.job[0].sequence[0].command[0]['@resource']
        assertNull "wrong command @name", doc.job[0].sequence[0].command[0]['@name']
        assertNull "wrong command @module", doc.job[0].sequence[0].command[0]['@module']
        assertEquals "missing command/script", 1, doc.job[0].sequence[0].command[0].script.size()
        assertEquals "wrong command/script", 'a local script command',
                     doc.job[0].sequence[0].command[0].script[0].text()
        assertEquals "wrong command/exec", 0, doc.job[0].sequence[0].command[0].exec.size()
        assertEquals "wrong command/scriptfile", 0, doc.job[0].sequence[0].command[0].scriptfile.size()
        assertEquals "wrong command/exec", 1, doc.job[0].sequence[0].command[0].scriptargs.size()
        assertEquals "wrong command/exec", "test string", doc.job[0].sequence[0].command[0].scriptargs[0].text()
    }

    @Test
    void testEncodeWorkflow_scriptfile() {
        def XmlParser parser = new XmlParser()

        //test simple exec/script/scriptfile commands
        def jobs5 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        argString: '',
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true,
                        workflow: new Workflow(keepgoing: true, commands: [new CommandExec(
                                adhocExecution: true,
                                adhocFilepath: '/path/to/a/file',
                                argString: 'test string'
                        )]
                        )
                )
        ]

        def xmlstr = JobsXMLCodec.encode(jobs5)
        assertNotNull xmlstr
        assertTrue xmlstr instanceof String


        def doc = parser.parse(new StringReader(xmlstr))
        assertNotNull doc
        assertEquals "missing job", 1, doc.job.size()
        assertEquals "missing context", 0, doc.job[0].context.size()
        assertEquals "missing sequence", 1, doc.job.sequence.size()
        assertEquals "wrong keepgoing", "node-first", doc.job[0].sequence[0]['@strategy']
        assertEquals "wrong command count", 1, doc.job[0].sequence[0].command.size()
        assertNull "wrong command @resource", doc.job[0].sequence[0].command[0]['@resource']
        assertNull "wrong command @name", doc.job[0].sequence[0].command[0]['@name']
        assertNull "wrong command @module", doc.job[0].sequence[0].command[0]['@module']
        assertEquals "missing command/scriptfile", 1, doc.job[0].sequence[0].command[0].scriptfile.size()
        assertEquals "wrong command/scriptfile", '/path/to/a/file',
                     doc.job[0].sequence[0].command[0].scriptfile[0].text()
        assertEquals "wrong command/exec", 0, doc.job[0].sequence[0].command[0].exec.size()
        assertEquals "wrong command/script", 0, doc.job[0].sequence[0].command[0].script.size()
        assertEquals "wrong command/exec", 1, doc.job[0].sequence[0].command[0].scriptargs.size()
        assertEquals "wrong command/exec", "test string", doc.job[0].sequence[0].command[0].scriptargs[0].text()

    }

    @Test
    void testEncodeWorkflow_scriptfile_args() {
        def XmlParser parser = new XmlParser()

        //test simple exec/script/scriptfile commands
        def jobs6 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        argString: '',
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true,
                        workflow: new Workflow(keepgoing: true, commands: [new CommandExec(
                                adhocExecution: true,
                                adhocFilepath: '/path/to/a/file',
                                argString: '-some script -args'
                        )]
                        )
                )
        ]

        def xmlstr = JobsXMLCodec.encode(jobs6)
        assertNotNull xmlstr
        assertTrue xmlstr instanceof String


        def doc = parser.parse(new StringReader(xmlstr))
        assertNotNull doc
        assertEquals "missing job", 1, doc.job.size()
        assertEquals "missing context", 0, doc.job[0].context.size()
        assertEquals "missing sequence", 1, doc.job.sequence.size()
        assertEquals "wrong keepgoing", "node-first", doc.job[0].sequence[0]['@strategy']
        assertEquals "wrong command count", 1, doc.job[0].sequence[0].command.size()
        assertNull "wrong command @resource", doc.job[0].sequence[0].command[0]['@resource']
        assertNull "wrong command @name", doc.job[0].sequence[0].command[0]['@name']
        assertNull "wrong command @module", doc.job[0].sequence[0].command[0]['@module']
        assertEquals "missing command/scriptfile", 1, doc.job[0].sequence[0].command[0].scriptfile.size()
        assertEquals "missing command/scriptargs", 1, doc.job[0].sequence[0].command[0].scriptargs.size()
        assertEquals "wrong command/scriptfile", '/path/to/a/file',
                     doc.job[0].sequence[0].command[0].scriptfile[0].text()
        assertEquals "wrong command/scriptargs", "-some script -args",
                     doc.job[0].sequence[0].command[0].scriptargs[0].text()
        assertEquals "wrong command/exec", 0, doc.job[0].sequence[0].command[0].exec.size()
        assertEquals "wrong command/script", 0, doc.job[0].sequence[0].command[0].script.size()

    }

    @Test
    void testEncodeWorkflow_jobref() {
        def XmlParser parser = new XmlParser()

        //test simple job ref workflow item
        def jobs7 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        argString: '',
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true,
                        workflow: new Workflow(keepgoing: true, commands: [new JobExec(
                                jobName: 'a Job'
                        )]
                        )
                )
        ]

        def xmlstr = JobsXMLCodec.encode(jobs7)
        assertNotNull xmlstr
        assertTrue xmlstr instanceof String


        def doc = parser.parse(new StringReader(xmlstr))
        assertNotNull doc
        assertEquals "missing job", 1, doc.job.size()
        assertEquals "missing context", 0, doc.job[0].context.size()
        assertEquals "missing sequence", 1, doc.job.sequence.size()
        assertEquals "wrong keepgoing", "node-first", doc.job[0].sequence[0]['@strategy']
        assertEquals "wrong command count", 1, doc.job[0].sequence[0].command.size()
        assertNull "wrong command @resource", doc.job[0].sequence[0].command[0]['@resource']
        assertNull "wrong command @name", doc.job[0].sequence[0].command[0]['@name']
        assertNull "wrong command @module", doc.job[0].sequence[0].command[0]['@module']
        assertEquals "missing command/jobref", 1, doc.job[0].sequence[0].command[0].jobref.size()
        assertEquals "wrong command/jobref/@name", 'a Job', doc.job[0].sequence[0].command[0].jobref[0]['@name']
        assertNull "wrong command/jobref/@group: " + doc.job[0].sequence[0].command[0].jobref[0]['@group'], doc.job[0].sequence[0].command[0].jobref[0]['@group']
    }

    @Test
    void testEncodeWorkflow_jobref_group() {
        def XmlParser parser = new XmlParser()
        //test simple job ref workflow item, with a group
        def jobs8 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        argString: '',
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true,
                        workflow: new Workflow(keepgoing: true, commands: [new JobExec(
                                jobName: 'a Job',
                                jobGroup: '/some/path'
                        )]
                        )
                )
        ]

        def xmlstr = JobsXMLCodec.encode(jobs8)
        assertNotNull xmlstr
        assertTrue xmlstr instanceof String


        def doc = parser.parse(new StringReader(xmlstr))
        assertNotNull doc
        assertEquals "missing job", 1, doc.job.size()
        assertEquals "missing context", 0, doc.job[0].context.size()
        assertEquals "missing sequence", 1, doc.job.sequence.size()
        assertEquals "wrong keepgoing", "node-first", doc.job[0].sequence[0]['@strategy']
        assertEquals "wrong command count", 1, doc.job[0].sequence[0].command.size()
        assertNull "wrong command @resource", doc.job[0].sequence[0].command[0]['@resource']
        assertNull "wrong command @name", doc.job[0].sequence[0].command[0]['@name']
        assertNull "wrong command @module", doc.job[0].sequence[0].command[0]['@module']
        assertEquals "missing command/jobref", 1, doc.job[0].sequence[0].command[0].jobref.size()
        assertEquals "wrong command/jobref/@name", 'a Job', doc.job[0].sequence[0].command[0].jobref[0]['@name']
        assertEquals "wrong command/jobref/@group", '/some/path', doc.job[0].sequence[0].command[0].jobref[0]['@group']
    }

    @Test
    void testEncodeWorkflow_strategy() {
        def XmlParser parser = new XmlParser()
        //test step-first workflow strategy
        def jobs9 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        argString: '',
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true,
                        workflow: new Workflow(
                                threadcount: 1,
                                strategy: 'step-first',
                                keepgoing: true,
                                commands: [new JobExec(

                                        jobName: 'a Job',
                                        jobGroup: '/some/path'
                                )]
                        )
                )
        ]

        def xmlstr = JobsXMLCodec.encode(jobs9)
        assertNotNull xmlstr
        assertTrue xmlstr instanceof String


        def doc = parser.parse(new StringReader(xmlstr))
        assertNotNull doc
        assertEquals "missing job", 1, doc.job.size()
        assertEquals "missing context", 0, doc.job[0].context.size()
        assertEquals "missing sequence", 1, doc.job.sequence.size()
        assertEquals "wrong keepgoing", "step-first", doc.job[0].sequence[0]['@strategy']
        assertEquals "wrong command count", 1, doc.job[0].sequence[0].command.size()
        assertNull "wrong command @resource", doc.job[0].sequence[0].command[0]['@resource']
        assertNull "wrong command @name", doc.job[0].sequence[0].command[0]['@name']
        assertNull "wrong command @module", doc.job[0].sequence[0].command[0]['@module']
        assertEquals "missing command/jobref", 1, doc.job[0].sequence[0].command[0].jobref.size()
        assertEquals "wrong command/jobref/@name", 'a Job', doc.job[0].sequence[0].command[0].jobref[0]['@name']
        assertEquals "wrong command/jobref/@group", '/some/path', doc.job[0].sequence[0].command[0].jobref[0]['@group']
    }

    @Test
    void testEncodeWorkflow_jobref_argstring() {
        def XmlParser parser = new XmlParser()
        //test simple job ref workflow item, with a group, with argString
        def job10 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        argString: '',
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true,
                        workflow: new Workflow(keepgoing: true, commands: [new JobExec(
                                jobName: 'a Job',
                                jobGroup: '/some/path',
                                argString: '-test1 1 -test2 2'
                        )]
                        )
                )
        ]

        def xmlstr = JobsXMLCodec.encode(job10)
        assertNotNull xmlstr
        assertTrue xmlstr instanceof String


        def doc = parser.parse(new StringReader(xmlstr))
        assertNotNull doc
        assertEquals "missing job", 1, doc.job.size()
        assertEquals "missing context", 0, doc.job[0].context.size()
        assertEquals "missing sequence", 1, doc.job.sequence.size()
        assertEquals "wrong keepgoing", "node-first", doc.job[0].sequence[0]['@strategy']
        assertEquals "wrong command count", 1, doc.job[0].sequence[0].command.size()
        assertNull "wrong command @resource", doc.job[0].sequence[0].command[0]['@resource']
        assertNull "wrong command @name", doc.job[0].sequence[0].command[0]['@name']
        assertNull "wrong command @module", doc.job[0].sequence[0].command[0]['@module']
        assertEquals "missing command/jobref", 1, doc.job[0].sequence[0].command[0].jobref.size()
        assertEquals "wrong command/jobref/@name", 'a Job', doc.job[0].sequence[0].command[0].jobref[0]['@name']
        assertEquals "wrong command/jobref/@group", '/some/path', doc.job[0].sequence[0].command[0].jobref[0]['@group']
        assertEquals "wrong arg count", 1, doc.job[0].sequence[0].command.jobref.arg.size()
        assertEquals "wrong arg @line", '-test1 1 -test2 2', doc.job[0].sequence[0].command[0].jobref[0].arg[0]['@line']
    }

    @Test
    void testEncodeWorkflow_jobref_nodestep() {
        def XmlParser parser = new XmlParser()
        //test simple job ref workflow item, with a group, with argString, nodeStep=true
        def jobref10 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        argString: '',
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true,
                        workflow: new Workflow(keepgoing: true, commands: [new JobExec(
                                jobName: 'a Job',
                                jobGroup: '/some/path',
                                argString: '-test1 1 -test2 2',
                                nodeStep: true
                        )]
                        )
                )
        ]

        def xmlstr = JobsXMLCodec.encode(jobref10)
        assertNotNull xmlstr
        assertTrue xmlstr instanceof String


        def doc = parser.parse(new StringReader(xmlstr))
        assertNotNull doc
        assertEquals "missing job", 1, doc.job.size()
        assertEquals "missing context", 0, doc.job[0].context.size()
        assertEquals "missing sequence", 1, doc.job.sequence.size()
        assertEquals "wrong keepgoing", "node-first", doc.job[0].sequence[0]['@strategy']
        assertEquals "wrong command count", 1, doc.job[0].sequence[0].command.size()
        assertNull "wrong command @resource", doc.job[0].sequence[0].command[0]['@resource']
        assertNull "wrong command @name", doc.job[0].sequence[0].command[0]['@name']
        assertNull "wrong command @module", doc.job[0].sequence[0].command[0]['@module']
        assertEquals "missing command/jobref", 1, doc.job[0].sequence[0].command[0].jobref.size()
        assertEquals "wrong command/jobref/@name", 'a Job', doc.job[0].sequence[0].command[0].jobref[0]['@name']
        assertEquals "wrong command/jobref/@group", '/some/path', doc.job[0].sequence[0].command[0].jobref[0]['@group']
        assertEquals "wrong command/jobref/@nodeStep", 'true', doc.job[0].sequence[0].command[0].jobref[0]['@nodeStep']
        assertEquals "wrong arg count", 1, doc.job[0].sequence[0].command.jobref.arg.size()
        assertEquals "wrong arg @line", '-test1 1 -test2 2', doc.job[0].sequence[0].command[0].jobref[0].arg[0]['@line']
    }

    @Test
    void testEncodeWorkflow_scriptfile_url(){
        def XmlParser parser = new XmlParser()
        //test simple exec/script/scripturl commands
        def jobs11 = [
            new ScheduledExecution(
                jobName: 'test job 1',
                description: 'test descrip',
                loglevel: 'INFO',
                project: 'test1',
                argString: '',
                nodeThreadcount: 1,
                nodeKeepgoing: true,
                doNodedispatch: true,
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec(
                    adhocExecution: true,
                    adhocFilepath: 'http://example.com/path/to/a/file',
                    argString: 'test string'
                )]
                )
            )
        ]

        def xmlstr = JobsXMLCodec.encode(jobs11)
        assertNotNull xmlstr
        assertTrue xmlstr instanceof String


        def doc = parser.parse(new StringReader(xmlstr))
        assertNotNull doc
        assertEquals "missing job", 1, doc.job.size()
        assertEquals "missing context", 0, doc.job[0].context.size()
        assertEquals "missing sequence", 1, doc.job.sequence.size()
        assertEquals "wrong keepgoing", "node-first", doc.job[0].sequence[0]['@strategy']
        assertEquals "wrong command count", 1, doc.job[0].sequence[0].command.size()
        assertNull "wrong command @resource", doc.job[0].sequence[0].command[0]['@resource']
        assertNull "wrong command @name", doc.job[0].sequence[0].command[0]['@name']
        assertNull "wrong command @module", doc.job[0].sequence[0].command[0]['@module']
        assertEquals "missing command/scriptfile", 0, doc.job[0].sequence[0].command[0].scriptfile.size()
        assertEquals "missing command/scriptfile", 1, doc.job[0].sequence[0].command[0].scripturl.size()
        assertEquals "wrong command/scripturl", 'http://example.com/path/to/a/file', doc.job[0].sequence[0].command[0].scripturl[0].text()
        assertEquals "wrong command/exec", 0, doc.job[0].sequence[0].command[0].exec.size()
        assertEquals "wrong command/script", 0, doc.job[0].sequence[0].command[0].script.size()
        assertEquals "wrong command/exec", 1, doc.job[0].sequence[0].command[0].scriptargs.size()
        assertEquals "wrong command/exec", "test string", doc.job[0].sequence[0].command[0].scriptargs[0].text()

    }

    @Test
    void testEncodeWorkflowJobExec(){

        //test simple job ref workflow item
        def jobs7 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        argString: '',
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true,
                        workflow: new Workflow(keepgoing: true, commands:
                                [
                                        new JobExec(jobName: 'a Job'),
                                        new JobExec(jobName: 'a Job2', jobGroup: 'job group'),
                                        new JobExec(jobName: 'a Job3', nodeFilter: 'abc def'),
                                        new JobExec(jobName: 'a Job4', nodeFilter: 'abc def4', nodeThreadcount: 4),
                                        new JobExec(jobName: 'a Job5', nodeFilter: 'abc def5', nodeThreadcount: 5, nodeKeepgoing: true),
                                        new JobExec(jobName: 'a Job6', nodeFilter: 'abc def6', nodeThreadcount: 6, nodeKeepgoing: false),
                                        new JobExec(jobName: 'a Job7', nodeFilter: 'abc def7', nodeThreadcount: 7, nodeKeepgoing: false, nodeRankAttribute: 'rank'),
                                        new JobExec(jobName: 'a Job8', nodeFilter: 'abc def8', nodeThreadcount: 8, nodeKeepgoing: false, nodeRankAttribute: 'rank', nodeRankOrderAscending: false),
                                        new JobExec(jobName: 'a Job9', nodeFilter: 'abc def9', nodeThreadcount: 9, nodeKeepgoing: false, nodeRankAttribute: 'rank', nodeRankOrderAscending: true),
                                ]
                        )
                )
        ]

        def xmlstr = JobsXMLCodec.encode(jobs7)
        assertNotNull xmlstr
        assertTrue xmlstr instanceof String


        def XmlParser parser = new XmlParser()
        def doc = parser.parse(new StringReader(xmlstr))
        assertNotNull doc
        assertEquals "missing job", 1, doc.job.size()
        assertEquals "missing sequence", 1, doc.job.sequence.size()
        assertEquals "wrong command count", 9, doc.job[0].sequence[0].command.size()
        assertXmlJobRefCommand(doc.job[0].sequence[0].command[0],
                ['@name':'a Job', '@group':null,'nodefilters':null,'dispatch':null])
        assertXmlJobRefCommand(doc.job[0].sequence[0].command[1],
                ['@name':'a Job2', '@group':'job group', 'nodefilters': null, 'dispatch': null])
        assertXmlJobRefCommand(doc.job[0].sequence[0].command[2],
                ['@name':'a Job3', '@group':null,nodefilters:['filter':'abc def'],dispatch:null])
        assertXmlJobRefCommand(doc.job[0].sequence[0].command[3],
                ['@name':'a Job4', '@group':null,
                 nodefilters:['filter': 'abc def4'],
                 dispatch:['threadcount': '4', 'keepgoing': null]
                ])
        assertXmlJobRefCommand(doc.job[0].sequence[0].command[4],
                ['@name':'a Job5', '@group':null,
                 nodefilters: ['filter': 'abc def5'],
                 dispatch   : ['threadcount': '5', 'keepgoing': 'true']
                ])
        assertXmlJobRefCommand(doc.job[0].sequence[0].command[5],
                ['@name':'a Job6', '@group':null,
                 nodefilters: ['filter': 'abc def6'],
                 dispatch   : ['threadcount': '6', 'keepgoing': 'false']
                ])
        assertXmlJobRefCommand(doc.job[0].sequence[0].command[6],
                ['@name':'a Job7', '@group':null,
                 nodefilters: ['filter': 'abc def7'],
                 dispatch   : ['threadcount': '7', 'keepgoing': 'false', 'rankAttribute':'rank']
                ])
        assertXmlJobRefCommand(doc.job[0].sequence[0].command[7],
                ['@name':'a Job8', '@group':null,
                 nodefilters: ['filter': 'abc def8'],
                 dispatch   : ['threadcount': '8', 'keepgoing': 'false', 'rankAttribute':'rank', 'rankOrder':'descending']
                ])
        assertXmlJobRefCommand(doc.job[0].sequence[0].command[8],
                ['@name':'a Job9', '@group':null,
                 nodefilters: ['filter': 'abc def9'],
                 dispatch   : ['threadcount': '9', 'keepgoing': 'false', 'rankAttribute':'rank', 'rankOrder':'ascending']
                ])

    }

    protected void assertXmlJobRefCommand(testCommand, Map data) {
        assertEquals "missing command/jobref", 1, testCommand.jobref.size()
        def elem = testCommand.jobref[0]
        assertXmlElement("command/jobref",data, elem)
    }

    protected void assertXmlElement(String prefix,Map data, elem) {
        data.each { key, value ->
            if(null == value || value instanceof String){
                def test = elem[key]?.size() > 0 ? key.startsWith('@') ? elem[key] : elem[key].text() : null
                assertEquals "wrong ${prefix}/${key}: " + test, value, test
            }else if(value instanceof Map){
                assertXmlElement(prefix+"/"+key,value,elem[key])
            }else{
                fail('Not expected type: '+value)
            }
        }
    }
}
