import java.util.logging.LogRecord
import java.util.logging.Level
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

public class HtFormatterTests extends GroovyTestCase{
    def SimpleDateFormat fmt = new SimpleDateFormat("hh:mm:ss");
    static String lSep = System.getProperty("line.separator")
    /**
     * Test formatting simple log message output
     */
    void testFormat1() {
        HtFormatter format = new HtFormatter();
        LogRecord lr = new LogRecord(Level.INFO, "test1")
        String dformat = fmt.format(lr.getMillis())

        String format1 = format.format(lr)
        assertEquals "wrong format: ${format1}","^^^${dformat}|INFO|test1^^^",format1

        //dataset format
        def data = [user:'user1',module:'coax',command:'barge',node:'pilf',context:'a.b.c']
        //'user','module','command','node','context']
        String format2 = format.format(lr,data)
        assertEquals "wrong format: ${format2}","^^^${dformat}|INFO|user1|coax|barge|pilf|a.b.c|test1^^^",format2
    }

    /**
     * test formatting multi-line records
     */
    void testFormat2() {
        HtFormatter format = new HtFormatter();
        LogRecord lr = new LogRecord(Level.INFO, "this is a multiline${lSep}record, for no reason${lSep} please")
        String dformat = fmt.format(lr.getMillis())

        String format1 = format.format(lr)
        String pref1="^^^${dformat}|INFO|"
        assertEquals "wrong format: ${format1}","${pref1}this is a multiline${lSep}record, for no reason${lSep} please^^^",format1

        //dataset format
        def data = [user:'user1',module:'coax',command:'barge',node:'pilf',context:'a.b.c']
        //'user','module','command','node','context']
        String format2 = format.format(lr,data)
        String pref2 = "^^^${dformat}|INFO|user1|coax|barge|pilf|a.b.c|"
        assertEquals "wrong format: ${format2}","${pref2}this is a multiline${lSep}record, for no reason${lSep} please^^^",format2
    }
}