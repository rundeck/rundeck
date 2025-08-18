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

package rundeck

import org.junit.Ignore
import org.junit.Test

import static org.junit.Assert.*
//import grails.test.GrailsUnitTestCase
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.quartz.CronExpression

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 4/19/13
 * Time: 6:06 PM
 */
class ScheduledExecutionTest  {

    @Test
    void testToMapOptions() {
        ScheduledExecution se = createBasicScheduledExecution()
        se.options.addAll([
                new Option(name: 'abc-4', defaultValue: '12', sortIndex: 4),
                new Option(name: 'bcd-2', defaultValue: '12', sortIndex: 2),
                new Option(name: 'cde-3', defaultValue: '12', sortIndex: 3),
                new Option(name: 'def-1', defaultValue: '12', sortIndex: 1),
        ])
        def jobMap = se.toMap()
        assertNotNull(jobMap)
        assertNotNull(jobMap.options)
        assertEquals(4, jobMap.options.size())
    }
    @Test
    void testToMapRetry() {
        ScheduledExecution se = createBasicScheduledExecution()
        se.retry='${option.retry}'
        def jobMap = se.toMap()
        assertNotNull(jobMap)
        assertNotNull(jobMap.retry)
        assertEquals('${option.retry}',jobMap.retry)
    }
    @Test
    void testToMapLoglimit() {
        ScheduledExecution se = createBasicScheduledExecution()
        se.logOutputThreshold = '20MB'
        se.logOutputThresholdAction = 'fail'

        def jobMap = se.toMap()
        assertNotNull(jobMap)
        assertNotNull(jobMap.loglimit)
        assertEquals('20MB',jobMap.loglimit)
        assertNotNull(jobMap.loglimitAction)
        assertEquals('fail',jobMap.loglimitAction)
    }
    @Test
    void testFromMapLoglimit() {
        ScheduledExecution se = ScheduledExecution.fromMap(
                [
                        jobName: 'abc',
                        loglimit:'20MB',
                        loglimitAction:'fail'
                ]
        )
        assertNotNull(se)
        assertNotNull(se.logOutputThreshold)
        assertEquals('20MB', se.logOutputThreshold)
        assertNotNull(se.logOutputThresholdAction)
        assertEquals('fail', se.logOutputThresholdAction)
    }
    @Test
    void testToMapNodesSelectedByDefault_default() {
        ScheduledExecution se = createBasicScheduledExecution()
        se.doNodedispatch=true
        def jobMap = se.toMap()
        assertNotNull(jobMap)
        assertEquals(true,jobMap.nodesSelectedByDefault)
    }
    @Test
    void testToMapNodesSelectedByDefault_false() {
        ScheduledExecution se = createBasicScheduledExecution()
        se.doNodedispatch=true
        se.nodesSelectedByDefault=false
        def jobMap = se.toMap()
        assertNotNull(jobMap)
        assertEquals(false,jobMap.nodesSelectedByDefault)
    }
    @Test void testToMapNodesSelectedByDefault_true() {
        ScheduledExecution se = createBasicScheduledExecution()
        se.doNodedispatch=true
        se.nodesSelectedByDefault=true
        def jobMap = se.toMap()
        assertNotNull(jobMap)
        assertEquals(true,jobMap.nodesSelectedByDefault)
    }
    @Test void testToMapNodeFilterEditable() {
        ScheduledExecution se = createBasicScheduledExecution()
        se.nodeFilterEditable=true
        def jobMap = se.toMap()
        assertNotNull(jobMap)
        assertNotNull(jobMap.nodeFilterEditable)
        assertEquals(true,jobMap.nodeFilterEditable)
    }

    @Test void testToMapNodeFilterNotEditable() {
        ScheduledExecution se = createBasicScheduledExecution()
        se.nodeFilterEditable=false
        def jobMap = se.toMap()
        assertNotNull(jobMap)
        assertNotNull(jobMap.nodeFilterEditable)
        assertEquals(false,jobMap.nodeFilterEditable)
    }

    @Test void testFromMapScheduleCrontabString() {
        ScheduledExecution se = ScheduledExecution.fromMap(
                [
                        jobName: 'abc',
                        schedule: [
                                crontab: '* * * * ? *'
                        ]
                ]
        )
        assertNotNull(se)
        assertEquals('* * * * ? *', se.crontabString)
    }

    @Test void testFromMapRetry() {
        ScheduledExecution se = ScheduledExecution.fromMap(
                [
                        jobName: 'abc',
                        retry: '${option.retry}'
                ]
        )
        assertNotNull(se)
        assertNotNull(se.retry)
        assertEquals('${option.retry}', se.retry)
    }
    @Test void testFromMapnodesSelectedByDefault_default() {
        ScheduledExecution se = ScheduledExecution.fromMap(
                [
                        jobName: 'abc',
                        nodefilters: [
                                filter:'abc'
                        ]
                ]
        )
        assertNotNull(se)
        assertTrue(se.nodesSelectedByDefault)
    }
    @Test void testFromMapnodesSelectedByDefault_true() {
        ScheduledExecution se = ScheduledExecution.fromMap(
                [
                        jobName: 'abc',
                        nodesSelectedByDefault:true,
                        nodefilters: [
                                filter:'abc'
                        ]
                ]
        )
        assertNotNull(se)
        assertTrue(se.nodesSelectedByDefault)
    }
    @Test void testFromMapnodesSelectedByDefault_false() {
        ScheduledExecution se = ScheduledExecution.fromMap(
                [
                        jobName: 'abc',
                        nodesSelectedByDefault:false,
                        nodefilters: [
                                filter:'abc'
                        ]
                ]
        )
        assertNotNull(se)
        assertFalse(se.nodesSelectedByDefault)
    }
    @Test void testFromMapSchedule() {
        ScheduledExecution se = ScheduledExecution.fromMap(
                [
                        jobName: 'abc',
                        schedule: [
                                time:[
                                        seconds:'1',
                                        minute: '2',
                                        hour: '3'
                                ]
                        ]
                ]
        )
        assertNotNull(se)
        assertNull(se.crontabString)
        assertEquals('1',se.seconds)
        assertEquals('2',se.minute)
        assertEquals('3',se.hour)
        assertEquals('*',se.month)
        assertEquals('*',se.year)
        assertEquals('?', se.dayOfMonth)
        assertEquals('*', se.dayOfWeek)
    }
    @Test void testFromMapScheduleMonth() {
        ScheduledExecution se = ScheduledExecution.fromMap(
                [
                        jobName: 'abc',
                        schedule: [
                                time: [
                                    seconds: '1',
                                    minute: '2',
                                    hour: '3'
                                ],
                                month: '4',
                        ]
                ]
        )
        assertNotNull(se)
        assertNull(se.crontabString)
        assertEquals('1',se.seconds)
        assertEquals('2',se.minute)
        assertEquals('3',se.hour)
        assertEquals('4',se.month)
        assertEquals('*',se.year)
        assertEquals('?', se.dayOfMonth)
        assertEquals('*', se.dayOfWeek)
    }
    @Test void testFromMapScheduleYear() {
        ScheduledExecution se = ScheduledExecution.fromMap(
                [
                        jobName: 'abc',
                        schedule: [
                                time: [
                                        seconds: '1',
                                        minute: '2',
                                        hour: '3'
                                ],
                                year: '4',
                        ]
                ]
        )
        assertNotNull(se)
        assertNull(se.crontabString)
        assertEquals('1',se.seconds)
        assertEquals('2',se.minute)
        assertEquals('3',se.hour)
        assertEquals('*',se.month)
        assertEquals('4',se.year)
        assertEquals('?',se.dayOfMonth)
        assertEquals('*',se.dayOfWeek)
    }
    @Test void testFromMapScheduleDayOfMonth() {
        ScheduledExecution se = ScheduledExecution.fromMap(
                [
                        jobName: 'abc',
                        schedule: [
                                time: [
                                        seconds: '1',
                                        minute: '2',
                                        hour: '3'
                                ],
                                dayofmonth: [
                                        day:'4'
                                ],
                        ]
                ]
        )
        assertNotNull(se)
        assertNull(se.crontabString)
        assertEquals('1',se.seconds)
        assertEquals('2',se.minute)
        assertEquals('3',se.hour)
        assertEquals('*',se.month)
        assertEquals('*',se.year)
        assertEquals('4',se.dayOfMonth)
        assertEquals('?',se.dayOfWeek)
    }
    @Test void testFromMapScheduleDayOfMonthInvalid() {
        ScheduledExecution se = ScheduledExecution.fromMap(
                [
                        jobName: 'abc',
                        schedule: [
                                time: [
                                        seconds: '1',
                                        minute: '2',
                                        hour: '3'
                                ],
                                dayofmonth:'4',
                        ]
                ]
        )
        assertNotNull(se)
        assertNull(se.crontabString)
        assertEquals('1',se.seconds)
        assertEquals('2',se.minute)
        assertEquals('3',se.hour)
        assertEquals('*',se.month)
        assertEquals('*',se.year)
        assertEquals('?',se.dayOfMonth)
        assertEquals('*',se.dayOfWeek)
    }
    @Test void testFromMapScheduleDayOfWeek() {
        ScheduledExecution se = ScheduledExecution.fromMap(
                [
                        jobName: 'abc',
                        schedule: [
                                time: [
                                        seconds: '1',
                                        minute: '2',
                                        hour: '3'
                                ],
                                weekday: [
                                        day:'4'
                                ],
                        ]
                ]
        )
        assertNotNull(se)
        assertNull(se.crontabString)
        assertEquals('1',se.seconds)
        assertEquals('2',se.minute)
        assertEquals('3',se.hour)
        assertEquals('*',se.month)
        assertEquals('*',se.year)
        assertEquals('?',se.dayOfMonth)
        assertEquals('4',se.dayOfWeek)
    }
    @Test void testFromMapScheduleDayOfWeekInvalid() {
        ScheduledExecution se = ScheduledExecution.fromMap(
                [
                        jobName: 'abc',
                        schedule: [
                                time: [
                                        seconds: '1',
                                        minute: '2',
                                        hour: '3'
                                ],
                                weekday: '4',
                        ]
                ]
        )
        assertNotNull(se)
        assertNull(se.crontabString)
        assertEquals('1',se.seconds)
        assertEquals('2',se.minute)
        assertEquals('3',se.hour)
        assertEquals('*',se.month)
        assertEquals('*',se.year)
        assertEquals('?',se.dayOfMonth)
        assertEquals('*',se.dayOfWeek)
    }

    @Test void testFromMapNodeFilterEditable() {
        ScheduledExecution se = ScheduledExecution.fromMap(
                [
                        jobName: 'abc',
                        nodeFilterEditable: true
                ]
        )
        assertNotNull(se)
        assertNotNull(se.nodeFilterEditable)
        assertEquals(true, se.nodeFilterEditable)
    }

    @Test void testFromMapNodeFilterNotEditable() {
        ScheduledExecution se = ScheduledExecution.fromMap(
                [
                        jobName: 'abc',
                        nodeFilterEditable: false
                ]
        )
        assertNotNull(se)
        assertNotNull(se.nodeFilterEditable)
        assertEquals(false, se.nodeFilterEditable)
    }

    private ScheduledExecution createBasicScheduledExecution() {
        new ScheduledExecution(
                jobName: "test",
                groupPath: "",
                description: "",
                project: "test",
                workflow: new Workflow(
                        commands: [
                                new CommandExec(adhocRemoteString: "exec")
                        ]
                ),
                options: [],
        )
    }

    @Test void testUserRoles() {
        def ScheduledExecution se = new ScheduledExecution()
        assertNull "should be null", se.userRoleList
        se.setUserRoles(["a", "b", "c"])
        assertEquals "User roles not set correctly", "[\"a\",\"b\",\"c\"]", se.userRoleList
        def x = se.getUserRoles()
        assertEquals "incorrect number of roles found", 3, x.size()
        assertEquals "invalid role item", "a", x[0]
        assertEquals "invalid role item", "b", x[1]
        assertEquals "invalid role item", "c", x[2]

        se.userRoleList = null
        x = se.getUserRoles()
        assertEquals "incorrect number of roles found", 0, x.size()
    }

    @Test void testUserRolesWithCommas() {
        def ScheduledExecution se = new ScheduledExecution()
        assertNull "should be null", se.userRoleList
        se.setUserRoles(["a,with,commas", "b, with commas", "c without commas"])
        assertEquals "User roles not set correctly", "[\"a,with,commas\",\"b, with commas\",\"c without commas\"]", se.userRoleList
        def x = se.getUserRoles()
        assertEquals "incorrect number of roles found", 3, x.size()
        assertEquals "invalid role item", "a,with,commas", x[0]
        assertEquals "invalid role item", "b, with commas", x[1]
        assertEquals "invalid role item", "c without commas", x[2]

        se.userRoleList = null
        x = se.getUserRoles()
        assertEquals "incorrect number of roles found", 0, x.size()
    }

    @Test void testUserRolesAsString() {
        def ScheduledExecution se = new ScheduledExecution()
        assertNull "should be null", se.userRoleList
//        se.setUserRoles(["a,with,commas", "b, with commas", "c without commas"])
        se.userRoleList = "a,b,c"
        assertEquals "User roles not set correctly", "a,b,c", se.userRoleList
        def x = se.getUserRoles()
        assertEquals "incorrect number of roles found", 3, x.size()
        assertEquals "invalid role item", "a", x[0]
        assertEquals "invalid role item", "b", x[1]
        assertEquals "invalid role item", "c", x[2]

        se.userRoleList = null
        x = se.getUserRoles()
        assertEquals "incorrect number of roles found", 0, x.size()
    }

    @Test void testUserRolesADWithDN() {
        def ScheduledExecution se = new ScheduledExecution()
        assertNull "should be null", se.userRoleList
        se.userRoleList='["CN=Admin,CN=Roles,DC=local","CN=Users,CN=Roles,DC=local","user"]'
        def x = se.getUserRoles()
        assertEquals "incorrect number of roles found", 3, x.size()
        assertEquals "invalid role item", "CN=Admin,CN=Roles,DC=local", x[0]
        assertEquals "invalid role item", "CN=Users,CN=Roles,DC=local", x[1]
        assertEquals "invalid role item", "user", x[2]

        se.userRoleList = null
        x = se.getUserRoles()
        assertEquals "incorrect number of roles found", 0, x.size()
    }

    @Test void testUserRolesListComma() {
        def ScheduledExecution se = new ScheduledExecution()
        assertNull "should be null", se.userRoleList
        se.userRoleList='admin,user,test'
        def x = se.getUserRoles()
        assertEquals "incorrect number of roles found", 3, x.size()
        assertEquals "invalid role item", "admin", x[0]
        assertEquals "invalid role item", "user", x[1]
        assertEquals "invalid role item", "test", x[2]

        se.userRoleList = null
        x = se.getUserRoles()
        assertEquals "incorrect number of roles found", 0, x.size()
    }

    @Test void testGenerateJobGroupName() {
        def ScheduledExecution se = new ScheduledExecution()
        se.properties = [uuid: "a1", jobName: 'TestName', project: "AFrameworkProject"]
        assertEquals "incorrect group name: ${se.generateJobGroupName()}", "AFrameworkProject", se.generateJobGroupName()

        se = new ScheduledExecution()
        se.properties = [uuid: "a1", jobName: 'TestName', project: "AFrameworkProject", groupPath: 'The Group']
        assertEquals "incorrect group name: ${se.generateJobGroupName()}", "AFrameworkProject", se.generateJobGroupName()
    }

    @Test void testGenerateCrontabExpression() {
        def ScheduledExecution se = new ScheduledExecution()

        //use default values

        assertEquals "incorrect expression: ${se.generateCrontabExression()}", "0 0 0 ? * * *", se.generateCrontabExression()

        se = new ScheduledExecution()
        se.properties = [minute: "5"]
        assertEquals "incorrect expression: ${se.generateCrontabExression()}", "0 5 0 ? * * *", se.generateCrontabExression()
        try {
            def CronExpression ce = new CronExpression(se.generateCrontabExression())
            assertNotNull(ce)
        } catch (Exception e) {
            fail("Unexpected exception: ${e}")
        }

        se = new ScheduledExecution()
        se.properties = [minute: "5", hour: "4"]
        assertEquals "incorrect expression: ${se.generateCrontabExression()}", "0 5 4 ? * * *", se.generateCrontabExression()
        try {
            def CronExpression ce = new CronExpression(se.generateCrontabExression())
            assertNotNull(ce)
        } catch (Exception e) {
            fail("Unexpected exception: ${e}")
        }

        se = new ScheduledExecution()
        se.properties = [minute: "5", hour: "4", dayOfMonth: "3"]
        String expr1 = se.generateCrontabExression()
        assertEquals "incorrect expression: ${se.generateCrontabExression()}", "0 5 4 3 * ? *", expr1
        try {
            def CronExpression ce = new CronExpression(expr1)
            assertNotNull(ce)
        } catch (Exception e) {
            e.printStackTrace()
            fail("Unexpected exception for expression '${expr1}': ${e}")
        }

        se = new ScheduledExecution()
        se.properties = [minute: "5", hour: "4", dayOfMonth: "3", month: "2"]
        assertEquals "incorrect expression: ${se.generateCrontabExression()}", "0 5 4 3 2 ? *", se.generateCrontabExression()
        try {
            def CronExpression ce = new CronExpression(se.generateCrontabExression())
            assertNotNull(ce)
        } catch (Exception e) {
            fail("Unexpected exception: ${e}")
        }

        se = new ScheduledExecution()
        //dayOfMonth and dayOfWeek should produce ? for dayOfWeek
        se.properties = [minute: "5", hour: "4", dayOfMonth: "3", month: "2", dayOfWeek: "1"]
        assertEquals "incorrect expression: ${se.generateCrontabExression()}", "0 5 4 3 2 ? *", se.generateCrontabExression()
        try {
            def CronExpression ce = new CronExpression(se.generateCrontabExression())
            assertNotNull(ce)
        } catch (Exception e) {
            fail("Unexpected exception: ${e}")
        }

        se = new ScheduledExecution()
        //dayOfMonth set to ? will allow dayOfWeek
        se.properties = [minute: "5", hour: "4", dayOfMonth: "?", month: "2", dayOfWeek: "1"]
        assertEquals "incorrect expression: ${se.generateCrontabExression()}", "0 5 4 ? 2 1 *", se.generateCrontabExression()
        try {
            def CronExpression ce = new CronExpression(se.generateCrontabExression())
            assertNotNull(ce)
        } catch (Exception e) {
            fail("Unexpected exception: ${e}")
        }

        se = new ScheduledExecution()
        se.properties = [minute: "5", hour: "4,3,2", dayOfMonth: "3", month: "2", dayOfWeek: "1"]
        assertEquals "incorrect expression: ${se.generateCrontabExression()}", "0 5 4,3,2 3 2 ? *", se.generateCrontabExression()
        try {
            def CronExpression ce = new CronExpression(se.generateCrontabExression())
            assertNotNull(ce)
        } catch (Exception e) {
            fail("Unexpected exception: ${e}")
        }

        se = new ScheduledExecution()
        se.properties = [minute: "5", hour: "4,3,2", dayOfMonth: "3", month: "2-6", dayOfWeek: "1"]
        assertEquals "incorrect expression: ${se.generateCrontabExression()}", "0 5 4,3,2 3 2-6 ? *", se.generateCrontabExression()
        try {
            def CronExpression ce = new CronExpression(se.generateCrontabExression())
            assertNotNull(ce)
        } catch (Exception e) {
            fail("Unexpected exception: ${e}")
        }
        se = new ScheduledExecution()

        se.properties = [minute: "5", hour: "4,3,2", dayOfMonth: "?", month: "Feb-Jun", dayOfWeek: "Mon"]
        assertEquals "incorrect expression: ${se.generateCrontabExression()}", "0 5 4,3,2 ? FEB-JUN MON *", se.generateCrontabExression()
        try {
            def CronExpression ce = new CronExpression(se.generateCrontabExression())
            assertNotNull(ce)
        } catch (Exception e) {
            fail("Unexpected exception: ${e}")
        }

        //test invalid cron expression

        se = new ScheduledExecution()

        se.properties = [minute: "5", hour: "4,3,2", dayOfMonth: "3", month: "shibby", dayOfWeek: "Mon"]
        assertEquals "incorrect expression: ${se.generateCrontabExression()}", "0 5 4,3,2 3 SHIBBY ? *", se.generateCrontabExression()
        try {
            def CronExpression ce = new CronExpression(se.generateCrontabExression())
            fail("should fail: ${ce}")
        } catch (Exception e) {
            assertNotNull(e)
        }
    }

    @Test void testParseCrontabString() {
        t: {
            //too few input parts
            def ScheduledExecution se = new ScheduledExecution()
            assertFalse(se.parseCrontabString('0 5 4,3,2 3 *'))
            assertFalse(se.parseCrontabString('0 5 4,3,2 3'))
            assertFalse(se.parseCrontabString('0 5 4,3,2'))
            assertFalse(se.parseCrontabString('0 5'))
            assertFalse(se.parseCrontabString('0'))
            assertFalse(se.parseCrontabString(''))
        }
        t: { //without optional year
            def ScheduledExecution se = new ScheduledExecution()
            assertTrue(se.parseCrontabString('0 5 4,3,2 3 SHIBBY ?'))
            assertEquals '0', se.seconds
            assertEquals '5', se.minute
            assertEquals '4,3,2', se.hour
            assertEquals '3', se.dayOfMonth
            assertEquals 'SHIBBY', se.month
            assertEquals '?', se.dayOfWeek
            assertEquals '*', se.year
        }
        t: {
            def ScheduledExecution se = new ScheduledExecution()
            assertTrue(se.parseCrontabString('0 5 4,3,2 3 SHIBBY ? *'))
            assertEquals '0', se.seconds
            assertEquals '5', se.minute
            assertEquals '4,3,2', se.hour
            assertEquals '3', se.dayOfMonth
            assertEquals 'SHIBBY', se.month
            assertEquals '?', se.dayOfWeek
            assertEquals '*', se.year
        }
        t: {
            //ignores extra content at the end
            def ScheduledExecution se = new ScheduledExecution()
            assertTrue(se.parseCrontabString('0 5 4,3,2 3 SHIBBY ? * test blah test'))
            assertEquals '0', se.seconds
            assertEquals '5', se.minute
            assertEquals '4,3,2', se.hour
            assertEquals '3', se.dayOfMonth
            assertEquals 'SHIBBY', se.month
            assertEquals '?', se.dayOfWeek
            assertEquals '*', se.year
        }
    }

    @Test void testCronExpressionIsValid() {
        assertFalse(CronExpression.isValidExpression('0 21 */4 */4 */6 3 2010-2040'))

    }

    @Test void testCrontabSpecialValue() {
        assertFalse(ScheduledExecution.crontabSpecialValue('0'))
        assertFalse(ScheduledExecution.crontabSpecialValue('0,2,3,4'))
        assertFalse(ScheduledExecution.crontabSpecialValue('*'))
        assertFalse(ScheduledExecution.crontabSpecialValue('?'))
        assertFalse(ScheduledExecution.crontabSpecialValue('MON,TUE,WED,THU,FRI,SAT'))

        assertTrue(ScheduledExecution.crontabSpecialValue('*/2'))
        assertTrue(ScheduledExecution.crontabSpecialValue('0-2'))
        assertTrue(ScheduledExecution.crontabSpecialValue('3#3'))
        assertTrue(ScheduledExecution.crontabSpecialValue('3W'))
        assertTrue(ScheduledExecution.crontabSpecialValue('3L'))
        assertTrue(ScheduledExecution.crontabSpecialValue('LW'))
        assertTrue(ScheduledExecution.crontabSpecialValue('JUL'))

        //month value allows JUL
        assertFalse(ScheduledExecution.crontabSpecialMonthValue('*'))
        assertFalse(ScheduledExecution.crontabSpecialMonthValue('?'))
        assertFalse(ScheduledExecution.crontabSpecialMonthValue('JUL'))

        assertTrue(ScheduledExecution.crontabSpecialMonthValue('*/2'))
        assertTrue(ScheduledExecution.crontabSpecialMonthValue('1-3'))


    }
    @Test void testShouldUseCrontabString(){
        assertTrue(new ScheduledExecution(seconds: '1').shouldUseCrontabString())
        assertFalse(new ScheduledExecution(seconds: '0').shouldUseCrontabString())
        assertTrue(new ScheduledExecution(year: '123').shouldUseCrontabString())
        assertFalse(new ScheduledExecution(year: '*').shouldUseCrontabString())
        assertTrue(new ScheduledExecution(hour: '*').shouldUseCrontabString())
        assertTrue(new ScheduledExecution(hour: '*/2').shouldUseCrontabString())
        assertFalse(new ScheduledExecution(hour: '12').shouldUseCrontabString())
        assertTrue(new ScheduledExecution(hour: '12,6').shouldUseCrontabString())
        assertTrue(new ScheduledExecution(minute: '*').shouldUseCrontabString())
        assertTrue(new ScheduledExecution(minute: '*/2').shouldUseCrontabString())
        assertFalse(new ScheduledExecution(minute: '12').shouldUseCrontabString())
        assertTrue(new ScheduledExecution(minute: '12,14').shouldUseCrontabString())
        assertTrue(new ScheduledExecution(hour: '06,08,10,12,14,16,18').shouldUseCrontabString())
        assertTrue(new ScheduledExecution(minute: '06,08,10,12,14,16,18').shouldUseCrontabString())
        assertTrue(new ScheduledExecution(dayOfMonth: '06').shouldUseCrontabString())
        assertFalse(new ScheduledExecution(dayOfMonth: '?').shouldUseCrontabString())
        //example: 0 00 06,08,10,12,14,16,18 ? * * *
    }

    @Test void testZeroPaddedString() {
        assertEquals("00", ScheduledExecution.zeroPaddedString(2, "0"))
        assertEquals("01", ScheduledExecution.zeroPaddedString(2, "1"))
        assertEquals("10", ScheduledExecution.zeroPaddedString(2, "10"))
        assertEquals("100", ScheduledExecution.zeroPaddedString(2, "100"))
        assertEquals("", ScheduledExecution.zeroPaddedString(2, ""))
        assertEquals("asdf", ScheduledExecution.zeroPaddedString(2, "asdf"))
        assertEquals(null, ScheduledExecution.zeroPaddedString(2, null))
    }
    @Test void testFilterCrontabParams() {
        def ScheduledExecution se = new ScheduledExecution()

        def params = [
                'crontab.month.Jan': "true",
                'crontab.month.Feb': "false",
                'crontab.month.Monkey': "true",
                'crontab.month.Elfkin': "false",
                "something.else": "b",
                "crontab.second.1": "two"]
        def map = se.filterCrontabParams("month", params)
        assertEquals "map is wrong size: ${map.size()}", 4, map.size()
        assertNotNull "map missing element Jan", map.Jan
        assertNotNull "map missing element Feb", map.Feb
        assertNotNull "map missing element Monkey", map.Monkey
        assertNotNull "map missing element Elfkin", map.Elfkin
        assertEquals "map element Jan had incorrect value", "true", map.Jan
        assertEquals "map element Feb had incorrect value", "false", map.Feb
        assertEquals "map element Monkey had incorrect value", "true", map.Monkey
        assertEquals "map element Elfkin had incorrect value", "false", map.Elfkin
    }

    @Test void testConstants() {
        assertEquals "Incorrect months", 12, ScheduledExecution.monthsofyearlist.size()
        assertEquals "Incorrect weekdays", 7, ScheduledExecution.daysofweeklist.size()
    }
    @Test void testparseLogOutputThreshold(){
        assertEquals(null,ScheduledExecution.parseLogOutputThreshold(null))
        assertEquals([maxLines:123L,perNode:false],ScheduledExecution.parseLogOutputThreshold("123"))
        assertEquals([maxLines:123L,perNode:true],ScheduledExecution.parseLogOutputThreshold("123/node"))
        assertEquals([maxSizeBytes:123L],ScheduledExecution.parseLogOutputThreshold("123b"))
        assertEquals([maxSizeBytes:123L],ScheduledExecution.parseLogOutputThreshold("123B"))
        assertEquals([maxSizeBytes:123L],ScheduledExecution.parseLogOutputThreshold("123B/node"))
        assertEquals([maxSizeBytes:123L*1024],ScheduledExecution.parseLogOutputThreshold("123kb"))
        assertEquals([maxSizeBytes:123L*1024],ScheduledExecution.parseLogOutputThreshold("123k"))
        assertEquals([maxSizeBytes:123L*1024],ScheduledExecution.parseLogOutputThreshold("123Kb"))
        assertEquals([maxSizeBytes:123L*1024*1024],ScheduledExecution.parseLogOutputThreshold("123mb"))
        assertEquals([maxSizeBytes:123L*1024*1024],ScheduledExecution.parseLogOutputThreshold("123m"))
        assertEquals([maxSizeBytes:123L*1024*1024],ScheduledExecution.parseLogOutputThreshold("123MB"))
        assertEquals([maxSizeBytes:123L*1024*1024*1024],ScheduledExecution.parseLogOutputThreshold("123gb"))
        assertEquals([maxSizeBytes:123L*1024*1024*1024],ScheduledExecution.parseLogOutputThreshold("123g"))
        assertEquals([maxSizeBytes:123L*1024*1024*1024],ScheduledExecution.parseLogOutputThreshold("123GB"))
    }

    @Test void testParseCheckboxFieldFromParams() {
        def ScheduledExecution se = new ScheduledExecution()
        def str

        //check defaultsToAsterix parameter
        str = se.parseCheckboxFieldFromParams("month", [:], true, ScheduledExecution.monthsofyearlist)
        assertEquals "value was incorrect: ${str}", "*", str
        str = se.parseCheckboxFieldFromParams("month", [:], false, ScheduledExecution.monthsofyearlist)
        assertNull "value should be null: ${str}", str

        def params = [
                'crontab.month.Jan': "true",
                'crontab.month.Feb': "false",
                'crontab.month.Monkey': "true",
                'crontab.month.Elfkin': "false",
                "something.else": "b",
                "crontab.second.1": "two"]
        str = se.parseCheckboxFieldFromParams("month", params, true, ScheduledExecution.monthsofyearlist)
        assertEquals "value was incorrect: ${str}", "JAN", str

        params = [
                'crontab.month.Jan': "true",
                'crontab.month.Feb': "true",
                'crontab.month.Monkey': "true",
                'crontab.month.Elfkin': "false",
                "something.else": "b",
                "crontab.second.1": "two"]
        str = se.parseCheckboxFieldFromParams("month", params, true, ScheduledExecution.monthsofyearlist)
        assertEquals "value was incorrect: ${str}", "FEB,JAN", str

        //test that when all values are seen, the result is *
        params = [:]
        ScheduledExecution.monthsofyearlist.each {
            params["crontab.month.${it}"] = "true"
        }
        str = se.parseCheckboxFieldFromParams("month", params, true, ScheduledExecution.monthsofyearlist)
        assertEquals "value was incorrect: ${str}", "*", str
    }


    @Test void testPopulateTimeDateFields() {
        t: {
            def ScheduledExecution se = new ScheduledExecution()
            def params = [
                    'crontab.month.Jan': "true",
                    'crontab.month.Feb': "true",
                    'crontab.dayOfWeek.Mon': "true",
                    'crontab.dayOfWeek.Wed': "true",
            ]
            se.populateTimeDateFields(params)
            assertEquals "month field was incorrect: ${se.month}", "FEB,JAN", se.month
            assertEquals "dayOfWeek field was incorrect: ${se.dayOfWeek}", "MON,WED", se.dayOfWeek

            se.populateTimeDateFields([everyDayOfWeek: "true", 'crontab.month.Jan': "true", 'crontab.month.Feb': "true",])
            assertEquals "month field was incorrect: ${se.month}", "FEB,JAN", se.month
            assertEquals "dayOfWeek field was incorrect: ${se.dayOfWeek}", "*", se.dayOfWeek

            se.populateTimeDateFields([everyMonth: "true", 'crontab.dayOfWeek.Mon': "true", 'crontab.dayOfWeek.Wed': "true",])
            assertEquals "month field was incorrect: ${se.month}", "*", se.month
            assertEquals "dayOfWeek field was incorrect: ${se.dayOfWeek}", "MON,WED", se.dayOfWeek

            se.populateTimeDateFields([everyDayOfWeek: true, 'crontab.month.Jan': "true", 'crontab.month.Feb': "true",])
            assertEquals "month field was incorrect: ${se.month}", "FEB,JAN", se.month
            assertEquals "dayOfWeek field was incorrect: ${se.dayOfWeek}", "*", se.dayOfWeek

            se.populateTimeDateFields([everyMonth: true, 'crontab.dayOfWeek.Mon': "true", 'crontab.dayOfWeek.Wed': "true",])
            assertEquals "month field was incorrect: ${se.month}", "*", se.month
            assertEquals "dayOfWeek field was incorrect: ${se.dayOfWeek}", "MON,WED", se.dayOfWeek

        }
        //use seconds, year and dayOfMonth values input:
        t: {
            def ScheduledExecution se = new ScheduledExecution()

            def params = [seconds: '1,2', year: '2010', dayOfMonth: '*/2']
            se.populateTimeDateFields(params)
            assertEquals '1,2', se.seconds
            assertEquals '2010', se.year
            assertEquals '*/2', se.dayOfMonth
            assertEquals '?', se.dayOfWeek
        }
        t: {
            def ScheduledExecution se = new ScheduledExecution()

            def params = [seconds: '1,2', year: '2010']
            se.populateTimeDateFields(params)
            assertEquals '1,2', se.seconds
            assertEquals '2010', se.year
            assertEquals '?', se.dayOfMonth
            assertEquals '*', se.dayOfWeek
        }

        //test crontabString usage
        t: {
            def ScheduledExecution se = new ScheduledExecution()

            def params = [crontabString: '0 0 0 3 * ? 2009', useCrontabString: 'true', seconds: '1,2', year: '2010', dayOfMonth: '*/2']
            se.populateTimeDateFields(params)
            assertEquals '0', se.seconds
            assertEquals '0', se.minute
            assertEquals '0', se.hour
            assertEquals '3', se.dayOfMonth
            assertEquals '*', se.month
            assertEquals '?', se.dayOfWeek
            assertEquals '2009', se.year
        }
        //test crontabString usage, useCrontabString='false'
        t: {
            def ScheduledExecution se = new ScheduledExecution()

            def params = [crontabString: '0 0 0 3 * ? 2009', useCrontabString: 'false', seconds: '1,2', year: '2010', dayOfMonth: '*/2']
            se.populateTimeDateFields(params)
            assertEquals '1,2', se.seconds
            assertEquals '0', se.minute
            assertEquals '0', se.hour
            assertEquals '*/2', se.dayOfMonth
            assertEquals '*', se.month
            assertEquals '?', se.dayOfWeek
            assertEquals '2010', se.year
        }
    }

    @Test void testTimeAndDateAsBooleanMap() {
        def ScheduledExecution se = new ScheduledExecution()
        def params = [
                'crontab.month.Jan': "true",
                'crontab.month.Feb': "true",
                'crontab.dayOfWeek.Mon': "true",
                'crontab.dayOfWeek.Wed': "true",
        ]
        se.populateTimeDateFields(params)
        def map = se.timeAndDateAsBooleanMap()
        assertNotNull "map should not be null", map
        assertEquals "map was wrong size: ${map.size()}", 4, map.size()
        assertEquals "month.JAN was not true", "true", map['month.JAN']
        assertEquals "month.FEB was not true", "true", map['month.FEB']
        assertEquals "dayOfWeek.MON was not true", "true", map['dayOfWeek.MON']
        assertEquals "dayOfWeek.WED was not true", "true", map['dayOfWeek.WED']


    }
    /**
     * Test crontab index values. month 1-12, day of week 1-7 (sun-sat)
     */
    @Test void testTimeAndDateAsBooleanMapFromCrontab() {
        t: {

            def ScheduledExecution se = new ScheduledExecution()
            assertTrue(se.parseCrontabString('0 5 4,3,2 ? 1,12 1,3,5,7 8'))
            def map = se.timeAndDateAsBooleanMap()
            assertNotNull "map should not be null", map
            assertEquals "map was wrong size: ${map.size()}", 6, map.size()
            assertEquals "month.JAN was not true", "true", map['month.JAN']
            assertEquals "month.JAN was not true", "true", map['month.DEC']
            assertEquals "dayOfWeek.MON was not true", "true", map['dayOfWeek.SUN']
            assertEquals "dayOfWeek.MON was not true", "true", map['dayOfWeek.TUE']
            assertEquals "dayOfWeek.MON was not true", "true", map['dayOfWeek.THU']
            assertEquals "dayOfWeek.MON was not true", "true", map['dayOfWeek.SAT']
        }
    }

    @Test void testFromMapNodeHealthCheckNullEditable() {
        ScheduledExecution se = ScheduledExecution.fromMap(
                [
                        jobName: 'abc'
                ]
        )
        assertNotNull(se)
        assertNotNull(se.excludeFilterUncheck)
        assertEquals(false, se.excludeFilterUncheck)
    }

    @Test void testFromMapNodeHealthCheckEditable() {
        ScheduledExecution se = ScheduledExecution.fromMap(
                [
                        jobName: 'abc',
                        excludeFilterUncheck: true
                ]
        )
        assertNotNull(se)
        assertNotNull(se.excludeFilterUncheck)
        assertEquals(true, se.excludeFilterUncheck)
    }

    @Test void testToMapNodeHealthCheckDefault_false() {
        ScheduledExecution se = createBasicScheduledExecution()
        se.doNodedispatch=true
        def jobMap = se.toMap()
        assertNotNull(jobMap)
        assertEquals(null,jobMap.excludeFilterUncheck)
    }

    @Test void testToMapNodeHealthCheck_true() {
        ScheduledExecution se = createBasicScheduledExecution()
        se.doNodedispatch=true
        se.filterExclude="tags: unhealthy"
        se.excludeFilterUncheck=true
        def jobMap = se.toMap()
        assertNotNull(jobMap)
        assertEquals(true,jobMap.excludeFilterUncheck)
    }


}
