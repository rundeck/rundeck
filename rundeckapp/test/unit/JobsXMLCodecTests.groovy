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
/*
 * JobsXMLCodecTests.java
 * 
 * User: greg
 * Created: Jun 10, 2009 11:27:54 AM
 * $Id$
 */
class JobsXMLCodecTests extends GroovyTestCase {

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
    /** should fail job 1*/
    def fail1 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description>a simple desc</description>
    <loglevel>INFO</loglevel>
    <!-- no context-->
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
  </job>
</joblist>
"""
    /** should fail job 2 */
    def fail2 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description>a simple desc</description>
    <loglevel>INFO</loglevel>
    <context>
      <!-- no project -->
      <type>MyService</type>
      <command>dowait</command>
      <options>
        <option name='delay' value='60' />
        <option name='monkey' value='bluefish' />
      </options>
    </context>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
  </job>
</joblist>
"""
    /** basic job */
    def okxml0 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description>a simple desc</description>
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

    void testDecodeBasic(){

        try{
            JobsXMLCodec.decode(badxml1)
            fail "Parsing shouldn't complete"
        }catch(JobXMLException e){
            assertNotNull e
        }
        try{
            JobsXMLCodec.decode(badxml2)
            fail "Parsing shouldn't complete"
        }catch(JobXMLException e){
            assertNotNull e
        }
        try{
            JobsXMLCodec.decode(fail1)
            fail "Parsing shouldn't complete"
        }catch(JobXMLException e){
            assertNotNull e
            e.printStackTrace(System.err)
            assertEquals "failed: ${e.getMessage()}","'context' element not found",e.getMessage()
        }
        try{
            JobsXMLCodec.decode(fail2)
            fail "Parsing shouldn't complete"
        }catch(JobXMLException e){
            assertNotNull e
            e.printStackTrace(System.err)
            assertEquals "failed: ${e.getMessage()}","'context/project' element not found",e.getMessage()
        }
            def jobs = JobsXMLCodec.decode(okxml0)
            assertNotNull jobs
            assertEquals "incorrect size",1,jobs.size()
            assertEquals "incorrect jobName","wait1",jobs[0].jobName
            assertEquals "incorrect description","a simple desc",jobs[0].description
            assertEquals "incorrect loglevel","INFO",jobs[0].loglevel
            assertEquals "incorrect project","test1",jobs[0].project
            assertNotNull jobs[0].options
            assertEquals 2,jobs[0].options.size()
            def iter = jobs[0].options.iterator()
            def opt1=iter.next()
            assertEquals 'delay',opt1.name
            assertEquals '60',opt1.defaultValue
            def opt2=iter.next()
            assertEquals 'monkey',opt2.name
            assertEquals 'bluefish',opt2.defaultValue
            assertFalse "incorrect doNodedispatch: ${jobs[0].doNodedispatch}",jobs[0].doNodedispatch
            assertEquals "incorrect nodeThreadcount",1,jobs[0].nodeThreadcount
            assertFalse "incorrect nodeKeepgoing",jobs[0].nodeKeepgoing
            assertNull "incorrect groupPath",jobs[0].groupPath

            assertEquals "incorrect scheduled","false",jobs[0].scheduled.toString()

            jobs = JobsXMLCodec.decode(okxml1)
            assertNotNull jobs
            assertEquals "incorrect size",1,jobs.size()
            assertEquals "incorrect jobName","wait1",jobs[0].jobName
            assertEquals "incorrect description","a simple desc",jobs[0].description
            assertEquals "incorrect loglevel","INFO",jobs[0].loglevel
            assertEquals "incorrect project","test1",jobs[0].project
        assertNotNull jobs[0].options
                    assertEquals 2,jobs[0].options.size()
                    def iter2 = jobs[0].options.iterator()
                    def opt1_1=iter2.next()
                    assertEquals 'delay',opt1_1.name
                    assertEquals '60',opt1_1.defaultValue
                    def opt2_2=iter2.next()
                    assertEquals 'monkey',opt2_2.name
                    assertEquals 'bluefish',opt2_2.defaultValue
            assertFalse "incorrect doNodedispatch: ${jobs[0].doNodedispatch}",jobs[0].doNodedispatch
            assertEquals "incorrect nodeThreadcount",1,jobs[0].nodeThreadcount
            assertFalse "incorrect nodeKeepgoing",jobs[0].nodeKeepgoing
            assertEquals "incorrect groupPath","some/group",jobs[0].groupPath

            assertFalse "incorrect scheduled",jobs[0].scheduled
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
            jobs = JobsXMLCodec.decode(basic2)
            assertNotNull jobs
            assertEquals "incorrect size",1,jobs.size()
            assertEquals "incorrect jobName","punch2",jobs[0].jobName
            assertEquals "incorrect description","dig it potato",jobs[0].description
            assertEquals "incorrect loglevel","WARN",jobs[0].loglevel
            assertEquals "incorrect project","zig",jobs[0].project
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
            jobs = JobsXMLCodec.decode(basic3)
            assertNotNull jobs
            assertEquals "incorrect groupPath","simple",jobs[0].groupPath
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
            jobs = JobsXMLCodec.decode(basic4)
            assertNotNull jobs
            assertEquals "incorrect groupPath","simple",jobs[0].groupPath

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
            jobs = JobsXMLCodec.decode(basic5)
            assertNotNull jobs
            assertEquals "incorrect groupPath","this/is/a/simple/path",jobs[0].groupPath

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
             jobs = JobsXMLCodec.decode(basic6)
            assertNotNull jobs
            assertNull "incorrect groupPath",jobs[0].groupPath

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
            jobs = JobsXMLCodec.decode(basic7)
            assertNotNull jobs
            assertNull "incorrect groupPath",jobs[0].groupPath
    }

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
        <type />
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
            assertEquals "incorrect nodeInclude","cypress.hill.com",jobs[0].nodeInclude
            assertEquals "incorrect project","demo",jobs[0].project
    }

    void testDecodeBackwardsCompatibility(){
        /**
         * Backwards compatibility test, using pre 3.5 format for input
         */

        def example1 = """<joblist>
  <job>
    <id>1</id>
    <name>XYZ Monthly WNP Report</name>
    <description />
    <loglevel>VERBOSE</loglevel>
    <context>
      <depot>demo</depot> <!-- "depot" should be interpreted as "project" -->
    </context>
    <sequence><command><exec>cd /home/test/nagios_sla_report/1.0.9 &amp;&amp;
    export ORACLE_HOME=/tools/oracle &amp;&amp; export
    LD_LIBRARY_PATH=/tools/oracle/lib &amp;&amp; /usr/bin/env
    python run_monthly.py test-prod</exec></command></sequence>
    <nodefilters excludeprecedence="true">
      <include>
        <hostname>cypress.hill.com</hostname>
        <type />
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
            assertEquals "incorrect nodeInclude","cypress.hill.com",jobs[0].nodeInclude
            assertEquals "incorrect project","demo",jobs[0].project
    }
    void testDecodeNodefilter(){
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
      <type>RoboDog</type>
      <object>myDog1</object>
      <command>Excel</command>
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
            assertEquals "incorrect size",1,jobs.size()
            assertEquals "incorrect nodefilter nodeInclude","centos5",jobs[0].nodeInclude
            assertEquals "incorrect nodefilter nodeIncludeType",null,jobs[0].nodeIncludeType
            assertEquals "incorrect nodefilter nodeIncludeTags",null,jobs[0].nodeIncludeTags
            assertEquals "incorrect nodefilter nodeIncludeOsName",null,jobs[0].nodeIncludeOsName
            assertEquals "incorrect nodefilter nodeIncludeOsFamily",null,jobs[0].nodeIncludeOsFamily
            assertEquals "incorrect nodefilter nodeIncludeOsArch",null,jobs[0].nodeIncludeOsArch
            assertEquals "incorrect nodefilter nodeIncludeOsVersion",null,jobs[0].nodeIncludeOsVersion
            assertEquals "incorrect nodefilter nodeIncludeName",null,jobs[0].nodeIncludeName

            assertEquals "incorrect nodefilter nodeExclude",null,jobs[0].nodeExclude
            assertEquals "incorrect nodefilter nodeExcludeType",null,jobs[0].nodeExcludeType
            assertEquals "incorrect nodefilter nodeExcludeTags",null,jobs[0].nodeExcludeTags
            assertEquals "incorrect nodefilter nodeExcludeOsName",null,jobs[0].nodeExcludeOsName
            assertEquals "incorrect nodefilter nodeExcludeOsFamily",null,jobs[0].nodeExcludeOsFamily
            assertEquals "incorrect nodefilter nodeExcludeOsArch",null,jobs[0].nodeExcludeOsArch
            assertEquals "incorrect nodefilter nodeExcludeOsVersion",null,jobs[0].nodeExcludeOsVersion
            assertEquals "incorrect nodefilter nodeExcludeName",null,jobs[0].nodeExcludeName
            assertTrue "incorrect nodefilter nodeExcludePrecedence ",jobs[0].nodeExcludePrecedence
            assertTrue "incorrect nodefilter doNodedispatch",jobs[0].doNodedispatch

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
      <type>RoboDog</type>
      <object>myDog1</object>
      <command>Excel</command>
      <options>
        <option name='clip' value='true' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <nodefilters>
        <include>
            <hostname>centos5</hostname>
            <type>MyNode</type>
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
            jobs = JobsXMLCodec.decode(filter2)
            assertNotNull jobs
            assertEquals "incorrect size",1,jobs.size()
            assertEquals "incorrect nodefilter nodeInclude","centos5",jobs[0].nodeInclude
            assertEquals "incorrect nodefilter nodeIncludeType","MyNode",jobs[0].nodeIncludeType
            assertEquals "incorrect nodefilter nodeIncludeTags","a+b,c",jobs[0].nodeIncludeTags
            assertEquals "incorrect nodefilter nodeIncludeOsName","Win.*",jobs[0].nodeIncludeOsName
            assertEquals "incorrect nodefilter nodeIncludeOsFamily","windows",jobs[0].nodeIncludeOsFamily
            assertEquals "incorrect nodefilter nodeIncludeOsArch","x86,sparc",jobs[0].nodeIncludeOsArch
            assertEquals "incorrect nodefilter nodeIncludeOsVersion","4\\..*",jobs[0].nodeIncludeOsVersion
            assertEquals "incorrect nodefilter nodeIncludeName","mynodename",jobs[0].nodeIncludeName

            assertEquals "incorrect nodefilter nodeExclude",null,jobs[0].nodeExclude
            assertEquals "incorrect nodefilter nodeExcludeType",null,jobs[0].nodeExcludeType
            assertEquals "incorrect nodefilter nodeExcludeTags",null,jobs[0].nodeExcludeTags
            assertEquals "incorrect nodefilter nodeExcludeOsName",null,jobs[0].nodeExcludeOsName
            assertEquals "incorrect nodefilter nodeExcludeOsFamily",null,jobs[0].nodeExcludeOsFamily
            assertEquals "incorrect nodefilter nodeExcludeOsArch",null,jobs[0].nodeExcludeOsArch
            assertEquals "incorrect nodefilter nodeExcludeOsVersion",null,jobs[0].nodeExcludeOsVersion
            assertEquals "incorrect nodefilter nodeExcludeName",null,jobs[0].nodeExcludeName
            assertTrue "incorrect nodefilter nodeExcludePrecedence ",jobs[0].nodeExcludePrecedence
            assertTrue "incorrect nodefilter doNodedispatch",jobs[0].doNodedispatch

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
      <type>RoboDog</type>
      <object>myDog1</object>
      <command>Excel</command>
      <options>
        <option name='clip' value='true' />
      </options>
    </context>
    <sequence><command><exec>test</exec></command></sequence>
    <nodefilters excludeprecedence="false">
        <exclude>
            <hostname>centos5</hostname>
            <type>MyNode</type>
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
            jobs = JobsXMLCodec.decode(filter3)
            assertNotNull jobs
            assertEquals "incorrect size",1,jobs.size()
            assertEquals "incorrect nodefilter nodeInclude",null,jobs[0].nodeInclude
            assertEquals "incorrect nodefilter nodeIncludeType",null,jobs[0].nodeIncludeType
            assertEquals "incorrect nodefilter nodeIncludeTags",null,jobs[0].nodeIncludeTags
            assertEquals "incorrect nodefilter nodeIncludeOsName",null,jobs[0].nodeIncludeOsName
            assertEquals "incorrect nodefilter nodeIncludeOsFamily",null,jobs[0].nodeIncludeOsFamily
            assertEquals "incorrect nodefilter nodeIncludeOsArch",null,jobs[0].nodeIncludeOsArch
            assertEquals "incorrect nodefilter nodeIncludeOsVersion",null,jobs[0].nodeIncludeOsVersion
            assertEquals "incorrect nodefilter nodeIncludeName",null,jobs[0].nodeIncludeName

            assertEquals "incorrect nodefilter nodeExclude","centos5",jobs[0].nodeExclude
            assertEquals "incorrect nodefilter nodeExcludeType","MyNode",jobs[0].nodeExcludeType
            assertEquals "incorrect nodefilter nodeExcludeTags","a+b,c",jobs[0].nodeExcludeTags
            assertEquals "incorrect nodefilter nodeExcludeOsName","Win.*",jobs[0].nodeExcludeOsName
            assertEquals "incorrect nodefilter nodeExcludeOsFamily","windows",jobs[0].nodeExcludeOsFamily
            assertEquals "incorrect nodefilter nodeExcludeOsArch","x86,sparc",jobs[0].nodeExcludeOsArch
            assertEquals "incorrect nodefilter nodeExcludeOsVersion","4\\..*",jobs[0].nodeExcludeOsVersion
            assertEquals "incorrect nodefilter nodeExcludeName","mynodename",jobs[0].nodeExcludeName
            assertFalse "incorrect nodefilter nodeExcludePrecedence",jobs[0].nodeExcludePrecedence
            assertTrue "incorrect nodefilter doNodedispatch",jobs[0].doNodedispatch
    }

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
      <type>MyService</type>
      <object>elfblister</object>
      <command>dowait</command>
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
      <type>MyService</type>
      <object>elfblister</object>
      <command>dowait</command>
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
        System.err.println("datemap: ${datemap}");
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
      <type>MyService</type>
      <object>elfblister</object>
      <command>dowait</command>
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
      <type>MyService</type>
      <object>elfblister</object>
      <command>dowait</command>
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
      <type>MyService</type>
      <object>elfblister</object>
      <command>dowait</command>
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
            final def cmd1 = jobs[0].workflow.commands[0]
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
            assertFalse "incorrect adhocExecution: ${cmd1.adhocExecution}", cmd1.adhocExecution
            assertNull "incorrect adhocLocalString", cmd1.adhocLocalString
            assertNull "incorrect adhocRemoteString", cmd1.adhocRemoteString
            assertNull "incorrect adhocRemoteString", cmd1.adhocFilepath
            assertNull "incorrect adhocRemoteString", cmd1.argString
            assertEquals "incorrect jobName", 'bob', cmd1.jobName
            assertNull "incorrect jobGroup", cmd1.jobGroup
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
            assertFalse "incorrect adhocExecution: ${cmd1.adhocExecution}", cmd1.adhocExecution
            assertNull "incorrect adhocLocalString", cmd1.adhocLocalString
            assertNull "incorrect adhocRemoteString", cmd1.adhocRemoteString
            assertNull "incorrect adhocRemoteString", cmd1.adhocFilepath
            assertNull "incorrect adhocRemoteString", cmd1.argString
            assertEquals "incorrect jobName", 'bob', cmd1.jobName
            assertEquals "incorrect jobGroup", '/some/path', cmd1.jobGroup
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
            assertFalse "incorrect adhocExecution: ${cmd1.adhocExecution}", cmd1.adhocExecution
            assertNull "incorrect adhocLocalString", cmd1.adhocLocalString
            assertNull "incorrect adhocRemoteString", cmd1.adhocRemoteString
            assertNull "incorrect adhocRemoteString", cmd1.adhocFilepath
            assertNull "incorrect adhocRemoteString", cmd1.argString
            assertEquals "incorrect jobName", 'bob', cmd1.jobName
            assertEquals "incorrect jobGroup", '/some/path', cmd1.jobGroup
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
            assertFalse "incorrect adhocExecution: ${cmd1.adhocExecution}", cmd1.adhocExecution
            assertNull "incorrect adhocLocalString", cmd1.adhocLocalString
            assertNull "incorrect adhocRemoteString", cmd1.adhocRemoteString
            assertNull "incorrect adhocRemoteString", cmd1.adhocFilepath
            assertNotNull "incorrect adhocRemoteString", cmd1.argString
            assertEquals "incorrect adhocRemoteString", "-test1 1 -test2 2",cmd1.argString
            assertEquals "incorrect jobName", 'bob', cmd1.jobName
            assertEquals "incorrect jobGroup", '/some/path', cmd1.jobGroup
    }
    
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
            assertFalse "incorrect adhocExecution: ${cmd1.adhocExecution}", cmd1.adhocExecution
            assertNull "incorrect adhocLocalString", cmd1.adhocLocalString
            assertNull "incorrect adhocRemoteString", cmd1.adhocRemoteString
            assertNull "incorrect adhocRemoteString", cmd1.adhocFilepath
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
            assertNull "incorrect values size", opt0.valuesUrl
            assertNotNull "incorrect values size", opt0.values
            assertEquals "incorrect values size", 3, opt0.values.size()
            def values=[]
            values.addAll(opt0.values as List)
            assertTrue "incorrect values content", values.contains("123")
            assertTrue "incorrect values content", values.contains("456")
            assertTrue "incorrect values content", values.contains("789")
    }

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
            assertNull "incorrect values size", opt0.valuesUrl
            assertNotNull "incorrect values size", opt0.values
            assertEquals "incorrect values size", 3, opt0.values.size()
            def values=[]
            values.addAll(opt0.values as List)
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
            assertNotNull "missing valuesUrl", opt1.valuesUrl
            assertTrue "missing valuesUrl", opt1.valuesUrl instanceof URL
            assertEquals "incorrect valuesUrl", "http://monkey/somewhere",opt1.valuesUrl.toExternalForm()

    }

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
            final def onsuccess = jobs[0].notifications.find{'onsuccess'==it.eventTrigger}
        assertNotNull "missing notifications onsuccess", onsuccess
            assertNotNull "missing notifications onsuccess email", onsuccess.content
            assertEquals "incorrect email content", "a@example.com,b@example.com", onsuccess.content
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
        final def onfailure = jobs[0].notifications.find{'onfailure'==it.eventTrigger}
            assertNotNull "missing notifications onfailure", onfailure
            assertNotNull "missing notifications onfailure email", onfailure.content
            assertEquals "incorrect email content", "c@example.com,d@example.com", onfailure.content
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
            <email recipients="z@example.com,x@example.com"/>
        </onsuccess>
        <onfailure>
            <email recipients="c@example.com,d@example.com"/>
        </onfailure>
    </notification>
  </job>
</joblist>
"""

            jobs = JobsXMLCodec.decode(xml3)
            assertNotNull jobs
            assertEquals "incorrect size", 1, jobs.size()
            assertNotNull "missing notifications", jobs[0].notifications
            assertEquals "incorrect notifications size", 2,jobs[0].notifications.size()
        onfailure = jobs[0].notifications.find{'onfailure'==it.eventTrigger}
            assertNotNull "missing notifications onfailure", onfailure
            assertNotNull "missing notifications onfailure email", onfailure.content
            assertEquals "incorrect email content", "c@example.com,d@example.com", onfailure.content
             onsuccess = jobs[0].notifications.find{'onsuccess'==it.eventTrigger}
        assertNotNull "missing notifications onsuccess", onsuccess
            assertNotNull "missing notifications onsuccess email", onsuccess.content
            assertEquals "incorrect email content", "z@example.com,x@example.com", onsuccess.content
    }

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
            assertEquals ("notification section had no onsuccess or onfailure element",e.message)
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
            assertEquals ("notification 'onsuccess' element had missing 'email' element",e.message)
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
            assertEquals ("onsuccess handler had blank or missing 'recipients' attribute",e.message)
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
            assertEquals ("onsuccess handler had blank or missing 'recipients' attribute",e.message)
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
            assertEquals ("notification 'onfailure' element had missing 'email' element",e.message)
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
            assertEquals ("onfailure handler had blank or missing 'recipients' attribute",e.message)
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
            assertEquals ("onfailure handler had blank or missing 'recipients' attribute",e.message)
        }
    }


    void testEncodeBasic(){
         def XmlSlurper parser = new XmlSlurper()
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
                        doNodedispatch:true
                )
        ]
        def  xmlstr = JobsXMLCodec.encode(jobs1)
            assertNotNull xmlstr
            assertTrue xmlstr instanceof String

            def doc = parser.parse(new StringReader(xmlstr))
            assertNotNull doc
            assertEquals "wrong root node name",'joblist',doc.name()
            assertEquals "wrong number of jobs",1,doc.job.size()
            assertEquals "wrong name","test job 1",doc.job[0].name[0].text()
            assertEquals "wrong description","test descrip",doc.job[0].description[0].text()
            assertEquals "wrong loglevel","INFO",doc.job[0].loglevel[0].text()
            assertNotNull "missing context",doc.job[0].context
            assertEquals "incorrect context size",1,doc.job[0].context.size()
            assertEquals "incorrect context project",'test1',doc.job[0].context[0].project[0].text()
            assertEquals "incorrect context options size",3,doc.job[0].context[0].options[0].option.size()
            assertEquals "incorrect context options option 1 name",'delay',doc.job[0].context[0].options[0].option[0]['@name'].text()
            assertEquals "incorrect context options option 1 value",'12',doc.job[0].context[0].options[0].option[0]['@value'].text()
            assertEquals "incorrect context options option 2 name",'monkey',doc.job[0].context[0].options[0].option[1]['@name'].text()
            assertEquals "incorrect context options option 2 value",'cheese',doc.job[0].context[0].options[0].option[1]['@value'].text()
            assertEquals "incorrect context options option 3 name",'particle',doc.job[0].context[0].options[0].option[2]['@name'].text()
            assertEquals "incorrect context options option 3 value",'true',doc.job[0].context[0].options[0].option[2]['@value'].text()

            assertEquals "incorrect dispatch threadcount",'1',doc.job[0].dispatch[0].threadcount[0].text()
            assertEquals "incorrect dispatch keepgoing",'true',doc.job[0].dispatch[0].keepgoing[0].text()


    }

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
            System.err.println("xmlstr: ${xmlstr}");

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

        System.err.println("xmlstr: ${xmlstr}");
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

    void testEncodeNodefilter(){
        def XmlParser parser = new XmlParser()

        //set node dispatch to false, and assert no nodefilters are generated
        def jobs1 = [
                new ScheduledExecution(
                        jobName:'test job 1',
                        description:'test descrip',
                        loglevel: 'INFO',
                        project:'test1',
                      workflow: new Workflow(keepgoing: true, commands: [new CommandExec(adhocExecution: true, adhocLocalString: 'test',)]),
                    nodeThreadcount:1,
                        nodeKeepgoing:true,
                        doNodedispatch:false,
                        nodeInclude:'myhostname',
                        nodeIncludeType:'Node1',
                        nodeIncludeTags:'a+b,c',
                        nodeIncludeOsName:'Windows.*',
                        nodeIncludeOsFamily:'windows',
                        nodeIncludeOsArch:'x86,sparc',
                        nodeIncludeOsVersion:'4\\..*',
                        nodeIncludeName:'mynode'
                )
        ]

            def xmlstr = JobsXMLCodec.encode(jobs1)
            assertNotNull xmlstr
            assertTrue xmlstr instanceof String


            def doc = parser.parse(new StringReader(xmlstr))
            assertNotNull doc
            assertEquals "missing nodefilters",0,doc.job[0].nodefilters.size()

        //set node dispatch to true, and assert 'include' nodefilters are generated
        def jobs2 = [
                new ScheduledExecution(
                        jobName:'test job 1',
                        description:'test descrip',
                        loglevel: 'INFO',
                        project:'test1',
                      workflow: new Workflow(keepgoing: true, commands: [new CommandExec(adhocExecution: true, adhocLocalString: 'test',)]),
                        nodeThreadcount:1,
                        nodeKeepgoing:true,
                        doNodedispatch:true,
                        nodeInclude:'myhostname',
                        nodeIncludeType:'Node1',
                        nodeIncludeTags:'a+b,c',
                        nodeIncludeOsName:'Windows.*',
                        nodeIncludeOsFamily:'windows',
                        nodeIncludeOsArch:'x86,sparc',
                        nodeIncludeOsVersion:'4\\..*',
                        nodeIncludeName:'mynode'
                )
        ]

            xmlstr = JobsXMLCodec.encode(jobs2)
            assertNotNull xmlstr
            assertTrue xmlstr instanceof String


            doc = parser.parse(new StringReader(xmlstr))
            assertNotNull doc
            assertEquals "missing nodefilters",1,doc.job[0].nodefilters.size()
            assertEquals "unexpected nodefilters exclude",0,doc.job[0].nodefilters[0].exclude.size()
            assertEquals "missing nodefilters include",1,doc.job[0].nodefilters[0].include.size()
            assertEquals "incorrect number of nodefilters include elements",8,doc.job[0].nodefilters[0].include[0].children().size()
            assertEquals "incorrect nodefilters include hostname",1,doc.job[0].nodefilters[0].include[0].hostname.size()
            assertEquals "incorrect nodefilters include hostname",'myhostname',doc.job[0].nodefilters[0].include[0].hostname[0].text()
            assertEquals "incorrect nodefilters include type",1,doc.job[0].nodefilters[0].include[0].type.size()
            assertEquals "incorrect nodefilters include type",'Node1',doc.job[0].nodefilters[0].include[0].type[0].text()
            assertEquals "incorrect nodefilters include tags",1,doc.job[0].nodefilters[0].include[0].tags.size()
            assertEquals "incorrect nodefilters include tags",'a+b,c',doc.job[0].nodefilters[0].include[0].tags[0].text()
            assertEquals "incorrect nodefilters include os-name",1,doc.job[0].nodefilters[0].include[0].'os-name'.size()
            assertEquals "incorrect nodefilters include os-name",'Windows.*',doc.job[0].nodefilters[0].include[0].'os-name'[0].text()
            assertEquals "incorrect nodefilters include os-family",1,doc.job[0].nodefilters[0].include[0].'os-family'.size()
            assertEquals "incorrect nodefilters include os-family",'windows',doc.job[0].nodefilters[0].include[0].'os-family'[0].text()
            assertEquals "incorrect nodefilters include os-arch",1,doc.job[0].nodefilters[0].include[0].'os-arch'.size()
            assertEquals "incorrect nodefilters include os-arch",'x86,sparc',doc.job[0].nodefilters[0].include[0].'os-arch'[0].text()
            assertEquals "incorrect nodefilters include os-version",1,doc.job[0].nodefilters[0].include[0].'os-version'.size()
            assertEquals "incorrect nodefilters include os-version",'4\\..*',doc.job[0].nodefilters[0].include[0].'os-version'[0].text()
            def tname=doc.job[0].nodefilters[0].include[0].find{it.name=='name'}.text()
            assertEquals "incorrect nodefilters include name",'mynode',tname
            assertEquals "incorrect nodefilters include name",1,doc.job[0].nodefilters[0].include[0].findAll{it.name=='name'}.size()


        //set node dispatch to true, and assert 'exclude' nodefilters are generated
        def jobs3 = [
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
                        nodeExcludeType:'Node1',
                        nodeExcludeTags:'a+b,c',
                        nodeExcludeOsName:'Windows.*',
                        nodeExcludeOsFamily:'windows',
                        nodeExcludeOsArch:'x86,sparc',
                        nodeExcludeOsVersion:'4\\..*',
                        nodeExcludeName:'mynode'
                )
        ]

            xmlstr = JobsXMLCodec.encode(jobs3)
            assertNotNull xmlstr
            assertTrue xmlstr instanceof String


            doc = parser.parse(new StringReader(xmlstr))
            assertNotNull doc
            assertEquals "missing nodefilters",1,doc.job[0].nodefilters.size()
            assertEquals "unexpected nodefilters include",0,doc.job[0].nodefilters[0].include.size()
            assertEquals "missing nodefilters exclude",1,doc.job[0].nodefilters[0].exclude.size()
            assertEquals "incorrect number of nodefilters exclude elements",8,doc.job[0].nodefilters[0].exclude[0].children().size()
            assertEquals "incorrect nodefilters exclude hostname",1,doc.job[0].nodefilters[0].exclude[0].hostname.size()
            assertEquals "incorrect nodefilters exclude hostname",'myhostname',doc.job[0].nodefilters[0].exclude[0].hostname[0].text()
            assertEquals "incorrect nodefilters exclude type",1,doc.job[0].nodefilters[0].exclude[0].type.size()
            assertEquals "incorrect nodefilters exclude type",'Node1',doc.job[0].nodefilters[0].exclude[0].type[0].text()
            assertEquals "incorrect nodefilters exclude tags",1,doc.job[0].nodefilters[0].exclude[0].tags.size()
            assertEquals "incorrect nodefilters exclude tags",'a+b,c',doc.job[0].nodefilters[0].exclude[0].tags[0].text()
            assertEquals "incorrect nodefilters exclude os-name",1,doc.job[0].nodefilters[0].exclude[0].'os-name'.size()
            assertEquals "incorrect nodefilters exclude os-name",'Windows.*',doc.job[0].nodefilters[0].exclude[0].'os-name'[0].text()
            assertEquals "incorrect nodefilters exclude os-family",1,doc.job[0].nodefilters[0].exclude[0].'os-family'.size()
            assertEquals "incorrect nodefilters exclude os-family",'windows',doc.job[0].nodefilters[0].exclude[0].'os-family'[0].text()
            assertEquals "incorrect nodefilters exclude os-arch",1,doc.job[0].nodefilters[0].exclude[0].'os-arch'.size()
            assertEquals "incorrect nodefilters exclude os-arch",'x86,sparc',doc.job[0].nodefilters[0].exclude[0].'os-arch'[0].text()
            assertEquals "incorrect nodefilters exclude os-version",1,doc.job[0].nodefilters[0].exclude[0].'os-version'.size()
            assertEquals "incorrect nodefilters exclude os-version",'4\\..*',doc.job[0].nodefilters[0].exclude[0].'os-version'[0].text()
            assertEquals "incorrect nodefilters exclude name",1,doc.job[0].nodefilters[0].exclude[0].findAll{it.name=='name'}.size()
            assertEquals "incorrect nodefilters exclude name",'mynode',doc.job[0].nodefilters[0].exclude[0].find{it.name=='name'}.text()


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
                        nodeExcludeType:'Node1',
                        nodeExcludeTags:'a+b,c',
                        nodeExcludeOsName:'Windows.*',
                        nodeExcludeOsFamily:'windows',
                        nodeExcludeOsArch:'x86,sparc',
                        nodeExcludeOsVersion:'4\\..*',
                        nodeExcludeName:'mynode',

                        nodeInclude:'anotherhost',
                        nodeIncludeType:'SomeNode',
                        nodeIncludeTags:'prod',
                        nodeIncludeOsName:'Mac.*',
                        nodeIncludeOsFamily:'unix',
                        nodeIncludeOsArch:'686',
                        nodeIncludeOsVersion:'10\\..*',
                        nodeIncludeName:'annode'
                )
        ]

            xmlstr = JobsXMLCodec.encode(jobs4)
            assertNotNull xmlstr
            assertTrue xmlstr instanceof String


            doc = parser.parse(new StringReader(xmlstr))
            assertNotNull doc
            assertEquals "missing nodefilters",1,doc.job[0].nodefilters.size()
            assertEquals "missing nodefilters exclude",1,doc.job[0].nodefilters[0].exclude.size()
            assertEquals "incorrect number of nodefilters exclude elements",8,doc.job[0].nodefilters[0].exclude[0].children().size()
            assertEquals "incorrect nodefilters exclude hostname",1,doc.job[0].nodefilters[0].exclude[0].hostname.size()
            assertEquals "incorrect nodefilters exclude hostname",'myhostname',doc.job[0].nodefilters[0].exclude[0].hostname[0].text()
            assertEquals "incorrect nodefilters exclude type",1,doc.job[0].nodefilters[0].exclude[0].type.size()
            assertEquals "incorrect nodefilters exclude type",'Node1',doc.job[0].nodefilters[0].exclude[0].type[0].text()
            assertEquals "incorrect nodefilters exclude tags",1,doc.job[0].nodefilters[0].exclude[0].tags.size()
            assertEquals "incorrect nodefilters exclude tags",'a+b,c',doc.job[0].nodefilters[0].exclude[0].tags[0].text()
            assertEquals "incorrect nodefilters exclude os-name",1,doc.job[0].nodefilters[0].exclude[0].'os-name'.size()
            assertEquals "incorrect nodefilters exclude os-name",'Windows.*',doc.job[0].nodefilters[0].exclude[0].'os-name'[0].text()
            assertEquals "incorrect nodefilters exclude os-family",1,doc.job[0].nodefilters[0].exclude[0].'os-family'.size()
            assertEquals "incorrect nodefilters exclude os-family",'windows',doc.job[0].nodefilters[0].exclude[0].'os-family'[0].text()
            assertEquals "incorrect nodefilters exclude os-arch",1,doc.job[0].nodefilters[0].exclude[0].'os-arch'.size()
            assertEquals "incorrect nodefilters exclude os-arch",'x86,sparc',doc.job[0].nodefilters[0].exclude[0].'os-arch'[0].text()
            assertEquals "incorrect nodefilters exclude os-version",1,doc.job[0].nodefilters[0].exclude[0].'os-version'.size()
            assertEquals "incorrect nodefilters exclude os-version",'4\\..*',doc.job[0].nodefilters[0].exclude[0].'os-version'[0].text()
            assertEquals "incorrect nodefilters exclude name",1,doc.job[0].nodefilters[0].exclude[0].findAll{it.name=='name'}.size()
            assertEquals "incorrect nodefilters exclude name",'mynode',doc.job[0].nodefilters[0].exclude[0].find{it.name=='name'}.text()

            assertEquals "missing nodefilters include",1,doc.job[0].nodefilters[0].include.size()
            assertEquals "incorrect number of nodefilters include elements",8,doc.job[0].nodefilters[0].include[0].children().size()
            assertEquals "incorrect nodefilters include hostname",1,doc.job[0].nodefilters[0].include[0].hostname.size()
            assertEquals "incorrect nodefilters include hostname",'anotherhost',doc.job[0].nodefilters[0].include[0].hostname[0].text()
            assertEquals "incorrect nodefilters include type",1,doc.job[0].nodefilters[0].include[0].type.size()
            assertEquals "incorrect nodefilters include type",'SomeNode',doc.job[0].nodefilters[0].include[0].type[0].text()
            assertEquals "incorrect nodefilters include tags",1,doc.job[0].nodefilters[0].include[0].tags.size()
            assertEquals "incorrect nodefilters include tags",'prod',doc.job[0].nodefilters[0].include[0].tags[0].text()
            assertEquals "incorrect nodefilters include os-name",1,doc.job[0].nodefilters[0].include[0].'os-name'.size()
            assertEquals "incorrect nodefilters include os-name",'Mac.*',doc.job[0].nodefilters[0].include[0].'os-name'[0].text()
            assertEquals "incorrect nodefilters include os-family",1,doc.job[0].nodefilters[0].include[0].'os-family'.size()
            assertEquals "incorrect nodefilters include os-family",'unix',doc.job[0].nodefilters[0].include[0].'os-family'[0].text()
            assertEquals "incorrect nodefilters include os-arch",1,doc.job[0].nodefilters[0].include[0].'os-arch'.size()
            assertEquals "incorrect nodefilters include os-arch",'686',doc.job[0].nodefilters[0].include[0].'os-arch'[0].text()
            assertEquals "incorrect nodefilters include os-version",1,doc.job[0].nodefilters[0].include[0].'os-version'.size()
            assertEquals "incorrect nodefilters include os-version",'10\\..*',doc.job[0].nodefilters[0].include[0].'os-version'[0].text()
            assertEquals "incorrect nodefilters include name",1,doc.job[0].nodefilters[0].include[0].findAll{it.name=='name'}.size()
            assertEquals "incorrect nodefilters include name",'annode',doc.job[0].nodefilters[0].include[0].find{it.name=='name'}.text()

    }

    void testEncodeWorkflow(){
        def XmlParser parser = new XmlParser()
        //encode basic workflow with one command call
        def jobs1 = [
                new ScheduledExecution(
                        jobName:'test job 1',
                        description:'test descrip',
                        loglevel: 'INFO',
                        project:'test1',
                        argString:'',
                        adhocExecution:false,
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

        if(true){
            def xmlstr
                xmlstr = JobsXMLCodec.encode(jobs1)
            assertNotNull xmlstr
            assertTrue xmlstr instanceof String


            def doc = parser.parse(new StringReader(xmlstr))
            assertNotNull doc
            assertEquals "missing job",1,doc.job.size()
            assertEquals "missing context",1,doc.job[0].context.size()
            assertEquals "missing context/project",1,doc.job[0].context[0].project.size()
            assertEquals "wrong project",'test1',doc.job[0].context[0].project[0].text()
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

        //encode basic workflow with one command call, change threadcount
        def jobs1b = [
                new ScheduledExecution(
                        jobName:'test job 1',
                        description:'test descrip',
                        loglevel: 'INFO',
                        project:'test1',
                        argString:'',
                        adhocExecution:false,
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

        if(true){
            def xmlstr
                xmlstr= JobsXMLCodec.encode(jobs1b)

            assertNotNull xmlstr
            assertTrue xmlstr instanceof String


            def doc = parser.parse(new StringReader(xmlstr))
            assertNotNull doc
            assertEquals "missing job",1,doc.job.size()
            assertEquals "missing context",1,doc.job[0].context.size()
            assertEquals "missing context/project",1,doc.job[0].context[0].project.size()
            assertEquals "wrong project",'test1',doc.job[0].context[0].project[0].text()
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
        //add conditional attributes
        def jobs2 = [
                new ScheduledExecution(
                        jobName:'test job 1',
                        description:'test descrip',
                        loglevel: 'INFO',
                        project:'test1',
                        argString:'',
                        adhocExecution:false,
                        nodeThreadcount:1,
                        nodeKeepgoing:true,
                        doNodedispatch:true,

                        workflow: new Workflow(keepgoing: true, commands: [new CommandExec(
                                    adhocExecution:true,
                                    adhocRemoteString:'aname',
                                    returnProperty:'returnproptest',
                                    ifString:'ifstringtest',
                                    unlessString:'unlessstringtest',
                                    equalsString:'equalsstringtest',
                            )]
                            )
                )
        ]

            def xmlstr = JobsXMLCodec.encode(jobs2)
            assertNotNull xmlstr
            assertTrue xmlstr instanceof String


            def doc = parser.parse(new StringReader(xmlstr))
            assertNotNull doc
            assertEquals "missing job",1,doc.job.size()
            assertEquals "missing context",1,doc.job[0].context.size()
            assertEquals "missing context/project",1,doc.job[0].context[0].project.size()
            assertEquals "wrong project",'test1',doc.job[0].context[0].project[0].text()
            assertEquals "missing sequence",1,doc.job.sequence.size()
            assertEquals "wrong keepgoing","node-first",doc.job[0].sequence[0]['@strategy']
            assertEquals "wrong command count",1,doc.job[0].sequence[0].command.size()
            assertEquals "wrong command/exec size",1, doc.job[0].sequence[0].command[0].exec.size()
            assertEquals "wrong command/exec","aname", doc.job[0].sequence[0].command[0].exec[0].text()

            //XXX: conditionals not yet supported, so expect null
            /*
            assertEquals "wrong command @return",'returnproptest',doc.job[0].sequence[0].command[0]['@return']
            assertEquals "wrong command @if",'ifstringtest',doc.job[0].sequence[0].command[0]['@if']
            assertEquals "wrong command @unless",'unlessstringtest',doc.job[0].sequence[0].command[0]['@unless']
            assertEquals "wrong command @equals",'equalsstringtest',doc.job[0].sequence[0].command[0]['@equals']
            */
            assertNull "wrong command @return",doc.job[0].sequence[0].command[0]['@return']
            assertNull "wrong command @if",doc.job[0].sequence[0].command[0]['@if']
            assertNull "wrong command @unless",doc.job[0].sequence[0].command[0]['@unless']
            assertNull "wrong command @equals",doc.job[0].sequence[0].command[0]['@equals']




        //test simple exec/script/scriptfile commands
        def jobs3 = [
                new ScheduledExecution(
                        jobName:'test job 1',
                        description:'test descrip',
                        loglevel: 'INFO',
                        project:'test1',
                        argString:'',
                        adhocExecution:false,
                        nodeThreadcount:1,
                        nodeKeepgoing:true,
                        doNodedispatch:true,

                        workflow: new Workflow(keepgoing: true, commands: [new CommandExec(
                                    adhocExecution:true,
                                    adhocRemoteString:'a remote command',
                                    argString:'test string'
                                )]
                            )
                )
        ]

            xmlstr = JobsXMLCodec.encode(jobs3)
            assertNotNull xmlstr
            assertTrue xmlstr instanceof String


            doc = parser.parse(new StringReader(xmlstr))
            assertNotNull doc
            assertEquals "missing job",1,doc.job.size()
            assertEquals "missing context",1,doc.job[0].context.size()
            assertEquals "missing context/project",1,doc.job[0].context[0].project.size()
            assertEquals "wrong project",'test1',doc.job[0].context[0].project[0].text()
            assertEquals "missing sequence",1,doc.job.sequence.size()
            assertEquals "wrong keepgoing","node-first",doc.job[0].sequence[0]['@strategy']
            assertEquals "wrong command count",1,doc.job[0].sequence[0].command.size()
            assertNull "wrong command @resource",doc.job[0].sequence[0].command[0]['@resource']
            assertNull "wrong command @name",doc.job[0].sequence[0].command[0]['@name']
            assertNull "wrong command @module",doc.job[0].sequence[0].command[0]['@module']
            assertEquals "missing command/exec",1,doc.job[0].sequence[0].command[0].exec.size()
            assertEquals "wrong command/exec",'a remote command',doc.job[0].sequence[0].command[0].exec[0].text()
            assertEquals "wrong command/exec",0,doc.job[0].sequence[0].command[0].script.size()
            assertEquals "wrong command/exec",0,doc.job[0].sequence[0].command[0].scriptfile.size()
            assertEquals "wrong command/exec",0,doc.job[0].sequence[0].command[0].scriptargs.size()



        //test simple exec/script/scriptfile commands
        def jobs4 = [
                new ScheduledExecution(
                        jobName:'test job 1',
                        description:'test descrip',
                        loglevel: 'INFO',
                        project:'test1',
                        argString:'',
                        adhocExecution:false,
                        nodeThreadcount:1,
                        nodeKeepgoing:true,
                        doNodedispatch:true,

                        workflow: new Workflow(keepgoing: true, commands: [new CommandExec(
                                    adhocExecution:true,
                                    adhocLocalString:'a local script command',
                                    argString:'test string'
                                )]
                            )
                )
        ]

            xmlstr = JobsXMLCodec.encode(jobs4)
            assertNotNull xmlstr
            assertTrue xmlstr instanceof String
                System.err.println("xmlstr: "+xmlstr);


            doc = parser.parse(new StringReader(xmlstr))
            assertNotNull doc
            assertEquals "missing job",1,doc.job.size()
            assertEquals "missing context",1,doc.job[0].context.size()
            assertEquals "missing context/project",1,doc.job[0].context[0].project.size()
            assertEquals "wrong project",'test1',doc.job[0].context[0].project[0].text()
            assertEquals "missing sequence",1,doc.job.sequence.size()
            assertEquals "wrong keepgoing","node-first",doc.job[0].sequence[0]['@strategy']
            assertEquals "wrong command count",1,doc.job[0].sequence[0].command.size()
            assertNull "wrong command @resource",doc.job[0].sequence[0].command[0]['@resource']
            assertNull "wrong command @name",doc.job[0].sequence[0].command[0]['@name']
            assertNull "wrong command @module",doc.job[0].sequence[0].command[0]['@module']
            assertEquals "missing command/script",1,doc.job[0].sequence[0].command[0].script.size()
            assertEquals "wrong command/script",'a local script command',doc.job[0].sequence[0].command[0].script[0].text()
            assertEquals "wrong command/exec",0,doc.job[0].sequence[0].command[0].exec.size()
            assertEquals "wrong command/scriptfile",0,doc.job[0].sequence[0].command[0].scriptfile.size()
            assertEquals "wrong command/exec",1,doc.job[0].sequence[0].command[0].scriptargs.size()
            assertEquals "wrong command/exec","test string",doc.job[0].sequence[0].command[0].scriptargs[0].text()



        //test simple exec/script/scriptfile commands
        def jobs5 = [
                new ScheduledExecution(
                        jobName:'test job 1',
                        description:'test descrip',
                        loglevel: 'INFO',
                        project:'test1',
                        argString:'',
                        adhocExecution:false,
                        nodeThreadcount:1,
                        nodeKeepgoing:true,
                        doNodedispatch:true,
                        workflow: new Workflow(keepgoing: true, commands: [new CommandExec(
                                    adhocExecution:true,
                                    adhocFilepath:'/path/to/a/file',
                                    argString:'test string'
                                )]
                        )
                )
        ]

            xmlstr = JobsXMLCodec.encode(jobs5)
            assertNotNull xmlstr
            assertTrue xmlstr instanceof String


            doc = parser.parse(new StringReader(xmlstr))
            assertNotNull doc
            assertEquals "missing job",1,doc.job.size()
            assertEquals "missing context",1,doc.job[0].context.size()
            assertEquals "missing context/project",1,doc.job[0].context[0].project.size()
            assertEquals "wrong project",'test1',doc.job[0].context[0].project[0].text()
            assertEquals "missing sequence",1,doc.job.sequence.size()
            assertEquals "wrong keepgoing","node-first",doc.job[0].sequence[0]['@strategy']
            assertEquals "wrong command count",1,doc.job[0].sequence[0].command.size()
            assertNull "wrong command @resource",doc.job[0].sequence[0].command[0]['@resource']
            assertNull "wrong command @name",doc.job[0].sequence[0].command[0]['@name']
            assertNull "wrong command @module",doc.job[0].sequence[0].command[0]['@module']
            assertEquals "missing command/scriptfile",1,doc.job[0].sequence[0].command[0].scriptfile.size()
            assertEquals "wrong command/scriptfile",'/path/to/a/file',doc.job[0].sequence[0].command[0].scriptfile[0].text()
            assertEquals "wrong command/exec",0,doc.job[0].sequence[0].command[0].exec.size()
            assertEquals "wrong command/script",0,doc.job[0].sequence[0].command[0].script.size()
            assertEquals "wrong command/exec",1,doc.job[0].sequence[0].command[0].scriptargs.size()
            assertEquals "wrong command/exec","test string",doc.job[0].sequence[0].command[0].scriptargs[0].text()



        //test simple exec/script/scriptfile commands
        def jobs6 = [
                new ScheduledExecution(
                        jobName:'test job 1',
                        description:'test descrip',
                        loglevel: 'INFO',
                        project:'test1',
                        argString:'',
                        adhocExecution:false,
                        nodeThreadcount:1,
                        nodeKeepgoing:true,
                        doNodedispatch:true,
                        workflow: new Workflow(keepgoing: true, commands: [new CommandExec(
                                    adhocExecution:true,
                                    adhocFilepath:'/path/to/a/file',
                                    argString:'-some script -args'
                                )]
                        )
                )
        ]

            xmlstr = JobsXMLCodec.encode(jobs6)
            assertNotNull xmlstr
            assertTrue xmlstr instanceof String


            doc = parser.parse(new StringReader(xmlstr))
            assertNotNull doc
            assertEquals "missing job",1,doc.job.size()
            assertEquals "missing context",1,doc.job[0].context.size()
            assertEquals "missing context/project",1,doc.job[0].context[0].project.size()
            assertEquals "wrong project",'test1',doc.job[0].context[0].project[0].text()
            assertEquals "missing sequence",1,doc.job.sequence.size()
            assertEquals "wrong keepgoing","node-first",doc.job[0].sequence[0]['@strategy']
            assertEquals "wrong command count",1,doc.job[0].sequence[0].command.size()
            assertNull "wrong command @resource",doc.job[0].sequence[0].command[0]['@resource']
            assertNull "wrong command @name",doc.job[0].sequence[0].command[0]['@name']
            assertNull "wrong command @module",doc.job[0].sequence[0].command[0]['@module']
            assertEquals "missing command/scriptfile",1,doc.job[0].sequence[0].command[0].scriptfile.size()
            assertEquals "missing command/scriptargs",1,doc.job[0].sequence[0].command[0].scriptargs.size()
            assertEquals "wrong command/scriptfile",'/path/to/a/file',doc.job[0].sequence[0].command[0].scriptfile[0].text()
            assertEquals "wrong command/scriptargs","-some script -args",doc.job[0].sequence[0].command[0].scriptargs[0].text()
            assertEquals "wrong command/exec",0,doc.job[0].sequence[0].command[0].exec.size()
            assertEquals "wrong command/script",0,doc.job[0].sequence[0].command[0].script.size()



        //test simple job ref workflow item
        def jobs7 = [
                new ScheduledExecution(
                        jobName:'test job 1',
                        description:'test descrip',
                        loglevel: 'INFO',
                        project:'test1',
                        argString:'',
                        adhocExecution:false,
                        nodeThreadcount:1,
                        nodeKeepgoing:true,
                        doNodedispatch:true,
                        workflow: new Workflow(keepgoing: true, commands: [new JobExec(
                                    jobName:'a Job'
                                )]
                        )
                )
        ]

            xmlstr = JobsXMLCodec.encode(jobs7)
            assertNotNull xmlstr
            assertTrue xmlstr instanceof String


            doc = parser.parse(new StringReader(xmlstr))
            assertNotNull doc
            assertEquals "missing job",1,doc.job.size()
            assertEquals "missing context",1,doc.job[0].context.size()
            assertEquals "missing context/project",1,doc.job[0].context[0].project.size()
            assertEquals "wrong project",'test1',doc.job[0].context[0].project[0].text()
            assertEquals "missing sequence",1,doc.job.sequence.size()
            assertEquals "wrong keepgoing","node-first",doc.job[0].sequence[0]['@strategy']
            assertEquals "wrong command count",1,doc.job[0].sequence[0].command.size()
            assertNull "wrong command @resource",doc.job[0].sequence[0].command[0]['@resource']
            assertNull "wrong command @name",doc.job[0].sequence[0].command[0]['@name']
            assertNull "wrong command @module",doc.job[0].sequence[0].command[0]['@module']
            assertEquals "missing command/jobref",1,doc.job[0].sequence[0].command[0].jobref.size()
            assertEquals "wrong command/jobref/@name",'a Job',doc.job[0].sequence[0].command[0].jobref[0]['@name']
            assertNull "wrong command/jobref/@group: "+doc.job[0].sequence[0].command[0].jobref[0]['@group'],doc.job[0].sequence[0].command[0].jobref[0]['@group']

        //test simple job ref workflow item, with a group
        def jobs8 = [
                new ScheduledExecution(
                        jobName:'test job 1',
                        description:'test descrip',
                        loglevel: 'INFO',
                        project:'test1',
                        argString:'',
                        adhocExecution:false,
                        nodeThreadcount:1,
                        nodeKeepgoing:true,
                        doNodedispatch:true,
                        workflow: new Workflow(keepgoing: true, commands: [new JobExec(
                                    jobName:'a Job',
                                    jobGroup:'/some/path'
                                )]
                        )
                )
        ]

            xmlstr = JobsXMLCodec.encode(jobs8)
            assertNotNull xmlstr
            assertTrue xmlstr instanceof String


            doc = parser.parse(new StringReader(xmlstr))
            assertNotNull doc
            assertEquals "missing job",1,doc.job.size()
            assertEquals "missing context",1,doc.job[0].context.size()
            assertEquals "missing context/project",1,doc.job[0].context[0].project.size()
            assertEquals "wrong project",'test1',doc.job[0].context[0].project[0].text()
            assertEquals "missing sequence",1,doc.job.sequence.size()
            assertEquals "wrong keepgoing","node-first",doc.job[0].sequence[0]['@strategy']
            assertEquals "wrong command count",1,doc.job[0].sequence[0].command.size()
            assertNull "wrong command @resource",doc.job[0].sequence[0].command[0]['@resource']
            assertNull "wrong command @name",doc.job[0].sequence[0].command[0]['@name']
            assertNull "wrong command @module",doc.job[0].sequence[0].command[0]['@module']
            assertEquals "missing command/jobref",1,doc.job[0].sequence[0].command[0].jobref.size()
            assertEquals "wrong command/jobref/@name",'a Job',doc.job[0].sequence[0].command[0].jobref[0]['@name']
            assertEquals "wrong command/jobref/@group",'/some/path',doc.job[0].sequence[0].command[0].jobref[0]['@group']

        //test step-first workflow strategy
        def jobs9 = [
                new ScheduledExecution(
                        jobName:'test job 1',
                        description:'test descrip',
                        loglevel: 'INFO',
                        project:'test1',
                        argString:'',
                        adhocExecution:false,
                        nodeThreadcount:1,
                        nodeKeepgoing:true,
                        doNodedispatch:true,
                        workflow: new Workflow(threadcount:1,strategy:'step-first',keepgoing: true, commands: [new JobExec(

                                    jobName:'a Job',
                                    jobGroup:'/some/path'
                                )]
                    )
                )
        ]

            xmlstr = JobsXMLCodec.encode(jobs9)
            assertNotNull xmlstr
            assertTrue xmlstr instanceof String


            doc = parser.parse(new StringReader(xmlstr))
            assertNotNull doc
            assertEquals "missing job",1,doc.job.size()
            assertEquals "missing context",1,doc.job[0].context.size()
            assertEquals "missing context/project",1,doc.job[0].context[0].project.size()
            assertEquals "wrong project",'test1',doc.job[0].context[0].project[0].text()
            assertEquals "missing sequence",1,doc.job.sequence.size()
            assertEquals "wrong keepgoing","step-first",doc.job[0].sequence[0]['@strategy']
            assertEquals "wrong command count",1,doc.job[0].sequence[0].command.size()
            assertNull "wrong command @resource",doc.job[0].sequence[0].command[0]['@resource']
            assertNull "wrong command @name",doc.job[0].sequence[0].command[0]['@name']
            assertNull "wrong command @module",doc.job[0].sequence[0].command[0]['@module']
            assertEquals "missing command/jobref",1,doc.job[0].sequence[0].command[0].jobref.size()
            assertEquals "wrong command/jobref/@name",'a Job',doc.job[0].sequence[0].command[0].jobref[0]['@name']
            assertEquals "wrong command/jobref/@group",'/some/path',doc.job[0].sequence[0].command[0].jobref[0]['@group']

        //test simple job ref workflow item, with a group
        def job10 = [
                new ScheduledExecution(
                        jobName:'test job 1',
                        description:'test descrip',
                        loglevel: 'INFO',
                        project:'test1',
                        argString:'',
                        adhocExecution:false,
                        nodeThreadcount:1,
                        nodeKeepgoing:true,
                        doNodedispatch:true,
                        workflow: new Workflow(keepgoing: true, commands: [new JobExec(
                                    jobName:'a Job',
                                    jobGroup:'/some/path',
                                    argString:'-test1 1 -test2 2'
                                )]
                        )
                )
        ]

            xmlstr = JobsXMLCodec.encode(job10)
            assertNotNull xmlstr
            assertTrue xmlstr instanceof String


            doc = parser.parse(new StringReader(xmlstr))
            assertNotNull doc
            assertEquals "missing job",1,doc.job.size()
            assertEquals "missing context",1,doc.job[0].context.size()
            assertEquals "missing context/project",1,doc.job[0].context[0].project.size()
            assertEquals "wrong project",'test1',doc.job[0].context[0].project[0].text()
            assertEquals "missing sequence",1,doc.job.sequence.size()
            assertEquals "wrong keepgoing","node-first",doc.job[0].sequence[0]['@strategy']
            assertEquals "wrong command count",1,doc.job[0].sequence[0].command.size()
            assertNull "wrong command @resource",doc.job[0].sequence[0].command[0]['@resource']
            assertNull "wrong command @name",doc.job[0].sequence[0].command[0]['@name']
            assertNull "wrong command @module",doc.job[0].sequence[0].command[0]['@module']
            assertEquals "missing command/jobref",1,doc.job[0].sequence[0].command[0].jobref.size()
            assertEquals "wrong command/jobref/@name",'a Job',doc.job[0].sequence[0].command[0].jobref[0]['@name']
            assertEquals "wrong command/jobref/@group",'/some/path',doc.job[0].sequence[0].command[0].jobref[0]['@group']
            assertEquals "wrong arg count",1,doc.job[0].sequence[0].command.jobref.arg.size()
            assertEquals "wrong arg @line",'-test1 1 -test2 2',doc.job[0].sequence[0].command[0].jobref[0].arg[0]['@line']

    }

    void testEncodeOptionValues(){
         def XmlSlurper parser = new XmlSlurper()
        def jobs1 = [
                new ScheduledExecution(
                        jobName:'test job 1',
                        description:'test descrip',
                        loglevel: 'INFO',
                        project:'test1',

                        workflow: new Workflow(keepgoing: true, commands: [new JobExec(
                                    jobName:'a Job',
                                    jobGroup:'/some/path',
                                )]
                            ),
                        nodeThreadcount:1,
                        nodeKeepgoing:true,
                        options:[
                            new Option(name:'test1',defaultValue:'monkey',values:['a','b','c'] as TreeSet,enforced:true,regex:'abcdefg',required:true),
                            new Option(name:'delay',defaultValue:'12')
                        ] as TreeSet,
                )
        ]

            def xmlstr = JobsXMLCodec.encode(jobs1)
            assertNotNull xmlstr
            assertTrue xmlstr instanceof String

            def doc = parser.parse(new StringReader(xmlstr))
            assertNotNull doc
            assertEquals "incorrect context options size",2,doc.job[0].context[0].options[0].option.size()
            assertEquals "incorrect context options option 1 name",'delay',doc.job[0].context[0].options[0].option[0]['@name'].text()
            assertEquals "incorrect context options option 1 value",'12',doc.job[0].context[0].options[0].option[0]['@value'].text()
            assertEquals 1,doc.job[0].context[0].options[0].option[0]['@value'].size()
            assertEquals 0,doc.job[0].context[0].options[0].option[0]['@enforcedvalues'].size()
            assertEquals 0,doc.job[0].context[0].options[0].option[0]['@values'].size()
            assertEquals 0,doc.job[0].context[0].options[0].option[0]['@valuesUrl'].size()
            assertEquals 0,doc.job[0].context[0].options[0].option[0]['@regex'].size()
            assertEquals 0,doc.job[0].context[0].options[0].option[0]['@required'].size()
            assertEquals 1,doc.job[0].context[0].options[0].option[0]['@value'].size()
            assertEquals 1,doc.job[0].context[0].options[0].option[1]['@enforcedvalues'].size()
            assertEquals 1,doc.job[0].context[0].options[0].option[1]['@values'].size()
            assertEquals 0,doc.job[0].context[0].options[0].option[1]['@valuesUrl'].size()
            assertEquals 1,doc.job[0].context[0].options[0].option[1]['@regex'].size()
            assertEquals 1,doc.job[0].context[0].options[0].option[1]['@required'].size()
            assertEquals "incorrect context options option 2 name",'test1',doc.job[0].context[0].options[0].option[1]['@name'].text()
            assertEquals "incorrect context options option 2 value",'monkey',doc.job[0].context[0].options[0].option[1]['@value'].text()
            assertEquals "incorrect context options option 2 enforcedvalues",'true',doc.job[0].context[0].options[0].option[1]['@enforcedvalues'].text()
            assertEquals "incorrect context options option 2 values",'a,b,c',doc.job[0].context[0].options[0].option[1]['@values'].text()
            assertEquals "incorrect context options option 2 regex",'abcdefg',doc.job[0].context[0].options[0].option[1]['@regex'].text()
            assertEquals "incorrect context options option 2 regex",'true',doc.job[0].context[0].options[0].option[1]['@required'].text()


        def jobs2 = [
                new ScheduledExecution(
                        jobName:'test job 1',
                        description:'test descrip',
                        loglevel: 'INFO',
                        project:'test1',


                        workflow: new Workflow(keepgoing: true, commands: [new JobExec(
                                    jobName:'a Job',
                                    jobGroup:'/some/path',
                                )]
                            ),
                        nodeThreadcount:1,
                        nodeKeepgoing:true,
                        options:[
                            new Option(name:'test1',defaultValue:'monkey',valuesUrl:new URL('http://monkey/somewhere'),enforced:false),
                            new Option(name:'delay',defaultValue:'12')
                        ] as TreeSet
                )
        ]

            xmlstr = JobsXMLCodec.encode(jobs2)
            assertNotNull xmlstr
            assertTrue xmlstr instanceof String

            doc = parser.parse(new StringReader(xmlstr))
            assertNotNull doc
            assertEquals "incorrect context options size",2,doc.job[0].context[0].options[0].option.size()
            assertEquals "incorrect context options option 1 name",'delay',doc.job[0].context[0].options[0].option[0]['@name'].text()
            assertEquals "incorrect context options option 1 value",'12',doc.job[0].context[0].options[0].option[0]['@value'].text()
            assertEquals 1,doc.job[0].context[0].options[0].option[0]['@value'].size()
            assertEquals 0,doc.job[0].context[0].options[0].option[0]['@enforcedvalues'].size()
            assertEquals 0,doc.job[0].context[0].options[0].option[0]['@values'].size()
            assertEquals 0,doc.job[0].context[0].options[0].option[0]['@valuesUrl'].size()
            assertEquals 0,doc.job[0].context[0].options[0].option[0]['@regex'].size()
            assertEquals 1,doc.job[0].context[0].options[0].option[0]['@value'].size()
            assertEquals 0,doc.job[0].context[0].options[0].option[1]['@enforcedvalues'].size()
            assertEquals 0,doc.job[0].context[0].options[0].option[1]['@values'].size()
            assertEquals 1,doc.job[0].context[0].options[0].option[1]['@valuesUrl'].size()
            assertEquals 0,doc.job[0].context[0].options[0].option[1]['@regex'].size()
            assertEquals "incorrect context options option 2 name",'test1',doc.job[0].context[0].options[0].option[1]['@name'].text()
            assertEquals "incorrect context options option 2 value",'monkey',doc.job[0].context[0].options[0].option[1]['@value'].text()
            assertEquals "incorrect context options option 2 valuesUrl",'http://monkey/somewhere',doc.job[0].context[0].options[0].option[1]['@valuesUrl'].text()


    }
    void testEncodeNotification(){
            def XmlSlurper parser = new XmlSlurper()
           def jobs1 = [
                   new ScheduledExecution(
                           jobName:'test job 1',
                           description:'test descrip',
                           loglevel: 'INFO',
                           project:'test1',


                        workflow: new Workflow(keepgoing: true, commands: [new JobExec(
                                    jobName:'a Job',
                                    jobGroup:'/some/path',
                                )]
                            ),
                           nodeThreadcount:1,
                           nodeKeepgoing:true,
                           notifications:[
                               new Notification(eventTrigger:'onsuccess',type:'email',content:'c@example.com,d@example.com')
                           ]
                   )
           ]

               def xmlstr = JobsXMLCodec.encode(jobs1)
               assertNotNull xmlstr
               assertTrue xmlstr instanceof String
        System.err.println("xmlstr: ${xmlstr}");

               def doc = parser.parse(new StringReader(xmlstr))
               assertNotNull doc
               assertEquals "incorrect notifications onsuccess email size",1,doc.job[0].notification[0].onsuccess[0].email.size()
               assertEquals "incorrect notifications onsuccess email size","c@example.com,d@example.com",doc.job[0].notification[0].onsuccess[0].email[0]['@recipients'].text()


           def jobs2 = [
                   new ScheduledExecution(
                           jobName:'test job 1',
                           description:'test descrip',
                           loglevel: 'INFO',
                           project:'test1',


                        workflow: new Workflow(keepgoing: true, commands: [new JobExec(
                                    jobName:'a Job',
                                    jobGroup:'/some/path',
                                )]
                            ),
                           nodeThreadcount:1,
                           nodeKeepgoing:true,
                           notifications:[
                               new Notification(eventTrigger:'onfailure',type:'email',content:'e@example.com,f@example.com')
                           ]
                   )
           ]

               xmlstr = JobsXMLCodec.encode(jobs2)
               assertNotNull xmlstr
               assertTrue xmlstr instanceof String

               doc = parser.parse(new StringReader(xmlstr))
               assertNotNull doc
               assertEquals "incorrect notifications onsuccess email size",1,doc.job[0].notification[0].onfailure[0].email.size()
               assertEquals "incorrect notifications onsuccess email size","e@example.com,f@example.com",doc.job[0].notification[0].onfailure[0].email[0]['@recipients'].text()


           def jobs3 = [
                   new ScheduledExecution(
                           jobName:'test job 1',
                           description:'test descrip',
                           loglevel: 'INFO',
                           project:'test1',


                        workflow: new Workflow(keepgoing: true, commands: [new JobExec(
                                    jobName:'a Job',
                                    jobGroup:'/some/path',
                                )]
                            ),
                           nodeThreadcount:1,
                           nodeKeepgoing:true,
                           notifications:[
                               new Notification(eventTrigger:'onsuccess',type:'email',content:'z@example.com,y@example.com'),
                               new Notification(eventTrigger:'onfailure',type:'email',content:'e@example.com,f@example.com')
                           ]
                   )
           ]

               xmlstr = JobsXMLCodec.encode(jobs3)
               assertNotNull xmlstr
               assertTrue xmlstr instanceof String

               doc = parser.parse(new StringReader(xmlstr))
               assertNotNull doc
               assertEquals "incorrect notifications onsuccess email size",1,doc.job[0].notification[0].onfailure[0].email.size()
               assertEquals "incorrect notifications onsuccess email size","e@example.com,f@example.com",doc.job[0].notification[0].onfailure[0].email[0]['@recipients'].text()
                assertEquals "incorrect notifications onsuccess email size",1,doc.job[0].notification[0].onsuccess[0].email.size()
               assertEquals "incorrect notifications onsuccess email size","z@example.com,y@example.com",doc.job[0].notification[0].onsuccess[0].email[0]['@recipients'].text()


    }
}
