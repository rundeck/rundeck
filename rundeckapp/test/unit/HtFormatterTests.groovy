import com.dtolabs.rundeck.app.internal.logging.LegacyLogOutFormatter

import grails.test.mixin.support.GrailsUnitTestMixin;

import java.text.SimpleDateFormat
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
 * HtFormatterTests.java
 * 
 * User: greg
 * Created: Jan 14, 2010 2:25:38 PM
 * $Id$
 */


@TestMixin(GrailsUnitTestMixin)
public class HtFormatterTests {
    def SimpleDateFormat fmt = new SimpleDateFormat("hh:mm:ss");
    static String lSep = System.getProperty("line.separator")
    /**
     * Test formatting simple log message output
     */
    void testFormat1() {
        LegacyLogOutFormatter format = new LegacyLogOutFormatter();
        String dformat = fmt.format(new Date())

        String format1 = format.reformat([level:'INFO',time:dformat],"test1")
        assertEquals "wrong format: ${format1}","^^^${dformat}|INFO||||||test1^^^",format1

        //dataset format
        def data = [user:'user1',module:'coax',command:'barge',node:'pilf',context:'a.b.c']
        //'user','module','command','node','context']
        String format2 = format.reformat(data+ [level: 'INFO', time: dformat],"test1")
        assertEquals "wrong format: ${format2}","^^^${dformat}|INFO|user1|coax|barge|pilf|a.b.c|test1^^^",format2
    }

    /**
     * test formatting multi-line records
     */
    void testFormat2() {
        LegacyLogOutFormatter format = new LegacyLogOutFormatter();
        def testMessage = "this is a multiline${lSep}record, for no reason${lSep} please"
        String dformat = fmt.format(new Date())

        String format1 = format.reformat([level: 'INFO', time: dformat],testMessage)
        String pref1="^^^${dformat}|INFO||||||"
        assertEquals "wrong format: ${format1}","${pref1}this is a multiline${lSep}record, for no reason${lSep} please^^^",format1

        //dataset format
        def data = [user:'user1',module:'coax',command:'barge',node:'pilf',context:'a.b.c']
        //'user','module','command','node','context']
        String format2 = format.reformat(data + [level: 'INFO', time: dformat],testMessage)
        String pref2 = "^^^${dformat}|INFO|user1|coax|barge|pilf|a.b.c|"
        assertEquals "wrong format: ${format2}","${pref2}this is a multiline${lSep}record, for no reason${lSep} please^^^",format2
    }


    void testHTFormatter() {
        def SimpleDateFormat fmt = new SimpleDateFormat("hh:mm:ss")
        def LegacyLogOutFormatter hf = new LegacyLogOutFormatter()
        assertNotNull hf
        def date= new Date()
        //
        def tstring = fmt.format(date)
        def String s = hf.reformat([time:tstring,level:'SEVERE'], "This is a test")

        assertEquals "formatting was incorrect: ${s}", "^^^${tstring}|SEVERE||||||This is a test^^^", s

        //add extra \r char
        s = hf.reformat([time: tstring, level: 'SEVERE'], "This is a test\r")
        assertEquals "formatting was incorrect: ${s}", "^^^${tstring}|SEVERE||||||This is a test^^^", s

        //add metadata
        def map = [user: 'user1', module: 'AModule', command: 'aCmd', node: 'someNode', context: 'Proj.AModule.something']
        s = hf.reformat(map+[time: tstring, level: 'SEVERE'], "This is a test\r")
        assertEquals "formatting was incorrect: ${s}", "^^^${tstring}|SEVERE|user1|AModule|aCmd|someNode|Proj.AModule.something|This is a test^^^", s

        //define some blank metadata
        map = [user: '', module: 'AModule', command: 'aCmd', node: '', context: 'Proj.AModule.something']
        s = hf.reformat(map + [time: tstring, level: 'SEVERE'], "This is a test\r")
        assertEquals "formatting was incorrect: ${s}", "^^^${tstring}|SEVERE||AModule|aCmd||Proj.AModule.something|This is a test^^^", s

        //define some null metadata
        map = [/* user:'',*/ module: 'AModule', command: 'aCmd',/* node:'', */ context: 'Proj.AModule.something']
        s = hf.reformat(map + [time: tstring, level: 'SEVERE'], "This is a test\r")
        assertEquals "formatting was incorrect: ${s}", "^^^${tstring}|SEVERE||AModule|aCmd||Proj.AModule.something|This is a test^^^", s

        //define all null metadata
        map = [:]
        s = hf.reformat(map + [time: tstring, level: 'SEVERE'], "This is a test\r")
        assertEquals "formatting was incorrect: ${s}", "^^^${tstring}|SEVERE||||||This is a test^^^", s


    }

}
