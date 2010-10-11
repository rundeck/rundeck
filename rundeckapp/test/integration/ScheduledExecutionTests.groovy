import org.quartz.CronExpression
import grails.test.GrailsUnitTestCase

class ScheduledExecutionTests extends GrailsUnitTestCase {

    protected void setUp() {
        super.setUp();
    }


    void testConstraints() {
		def ScheduledExecution se = new ScheduledExecution()
        def props=[jobName:"TestName",project:"TestFrameworkProject",type:"AType",command:"doCommand",argString:"-test",description:"whatever"]
        se.properties=props
        se.validate()
        def StringBuffer sb = new StringBuffer()
        se.errors.allErrors.each{sb<<it.toString()}
        assertTrue "ScheduledExecution should validate: ${sb}",se.validate()

        //change values for jobName
        se.jobName=null
        assertFalse "ScheduledExecution shouldn't validate",se.validate()
        se.jobName=""
        assertFalse "ScheduledExecution shouldn't validate",se.validate()

        List notBlankFields = ['jobName', 'project']
        notBlankFields.each { key ->
            se = new ScheduledExecution()
            se.properties=props
            assertTrue se.validate()
            //change values for project
            se."${key}"=null
            assertFalse "ScheduledExecution shouldn't validate for null value of ${key}",se.validate()
            se."${key}"=""
            assertFalse "ScheduledExecution shouldn't validate for blank value of ${key}",se.validate()
        }
        List blankNotNullFields = [ 'description']
        blankNotNullFields.each { key ->
            se = new ScheduledExecution()
            se.properties=props
            assertTrue se.validate()
            //change values for project
            se."${key}"=null
            assertFalse "ScheduledExecution shouldn't validate for null value of ${key}",se.validate()
            se."${key}"=""
            assertTrue "ScheduledExecution shouldn't validate for blank value of ${key}",se.validate()
        }
    }

    void testUserRoles(){
        def ScheduledExecution se = new ScheduledExecution()
        assertNull "should be null",se.userRoleList
        se.setUserRoles(["a","b","c"])
        assertEquals "User roles not set correctly","a,b,c",se.userRoleList
        def x = se.getUserRoles()
        assertEquals "incorrect number of roles found",3,x.size()
        assertEquals "invalid role item","a",x[0]
        assertEquals "invalid role item","b",x[1]
        assertEquals "invalid role item","c",x[2]

        se.userRoleList=null
        x = se.getUserRoles()
        assertEquals "incorrect number of roles found",0,x.size()
    }

    void testGenerateJobScheduledName(){
        def ScheduledExecution se = new ScheduledExecution()
        def props=[jobName:"TestName",project:"TestFrameworkProject",type:"AType",command:"doCommand",argString:"-test",description:"whatever"]
        se.properties=props
        se.validate()
        def StringBuffer sb = new StringBuffer()
        se.errors.allErrors.each{sb<<it.toString()}
        assertTrue "should validate: ${sb.toString()}",se.validate()
        se.save(flush:true)
        assertEquals 1,ScheduledExecution.count()
        assertNotNull "id should be set: ${se.id}",se.id
        assertEquals "incorrect job name: ${se.generateJobScheduledName()}",se.id+":TestName",se.generateJobScheduledName()
    }
    void testGenerateJobGroupName(){
        def ScheduledExecution se =new ScheduledExecution()
        se.properties = [jobName:'TestName',project:"AFrameworkProject"]
        assertEquals "incorrect group name: ${se.generateJobGroupName()}","AFrameworkProject:TestName:",se.generateJobGroupName()

        se =new ScheduledExecution()
        se.properties = [jobName:'TestName',project:"AFrameworkProject", groupPath: 'The Group']
        assertEquals "incorrect group name: ${se.generateJobGroupName()}","AFrameworkProject:TestName:The Group",se.generateJobGroupName()
    }
    void testGenerateCrontabExpression(){
        def ScheduledExecution se =new ScheduledExecution()

        //use default values
        
        assertEquals "incorrect expression: ${se.generateCrontabExression()}","0 0 0 ? * * *",se.generateCrontabExression()

        se =new ScheduledExecution()
        se.properties = [minute:"5"]
        assertEquals "incorrect expression: ${se.generateCrontabExression()}","0 5 0 ? * * *",se.generateCrontabExression()
        try{
            def CronExpression ce=new CronExpression(se.generateCrontabExression())
            assertNotNull(ce)
        }catch(Exception e){
            fail("Unexpected exception: ${e}")
        }

        se =new ScheduledExecution()
        se.properties = [minute:"5",hour:"4"]
        assertEquals "incorrect expression: ${se.generateCrontabExression()}","0 5 4 ? * * *",se.generateCrontabExression()
        try{
            def CronExpression ce=new CronExpression(se.generateCrontabExression())
            assertNotNull(ce)
        }catch(Exception e){
            fail("Unexpected exception: ${e}")
        }

        se =new ScheduledExecution()
        se.properties = [minute:"5",hour:"4",dayOfMonth:"3"]
        String expr1 = se.generateCrontabExression()
        assertEquals "incorrect expression: ${se.generateCrontabExression()}","0 5 4 3 * ? *",expr1
        try{
            def CronExpression ce=new CronExpression(expr1)
            assertNotNull(ce)
        }catch(Exception e){
            e.printStackTrace()
            fail("Unexpected exception for expression '${expr1}': ${e}")
        }

        se =new ScheduledExecution()
        se.properties = [minute:"5",hour:"4",dayOfMonth:"3",month:"2"]
        assertEquals "incorrect expression: ${se.generateCrontabExression()}","0 5 4 3 2 ? *",se.generateCrontabExression()
        try{
            def CronExpression ce=new CronExpression(se.generateCrontabExression())
            assertNotNull(ce)
        }catch(Exception e){
            fail("Unexpected exception: ${e}")
        }

        se =new ScheduledExecution()
        //dayOfMonth and dayOfWeek should produce ? for dayOfWeek
        se.properties = [minute:"5",hour:"4",dayOfMonth:"3",month:"2",dayOfWeek:"1"]
        assertEquals "incorrect expression: ${se.generateCrontabExression()}","0 5 4 3 2 ? *",se.generateCrontabExression()
        try{
            def CronExpression ce=new CronExpression(se.generateCrontabExression())
            assertNotNull(ce)
        }catch(Exception e){
            fail("Unexpected exception: ${e}")
        }

        se =new ScheduledExecution()
        //dayOfMonth set to ? will allow dayOfWeek
        se.properties = [minute:"5",hour:"4",dayOfMonth:"?",month:"2",dayOfWeek:"1"]
        assertEquals "incorrect expression: ${se.generateCrontabExression()}","0 5 4 ? 2 1 *",se.generateCrontabExression()
        try{
            def CronExpression ce=new CronExpression(se.generateCrontabExression())
            assertNotNull(ce)
        }catch(Exception e){
            fail("Unexpected exception: ${e}")
        }

        se =new ScheduledExecution()
        se.properties = [minute:"5",hour:"4,3,2",dayOfMonth:"3",month:"2",dayOfWeek:"1"]
        assertEquals "incorrect expression: ${se.generateCrontabExression()}","0 5 4,3,2 3 2 ? *",se.generateCrontabExression()
        try{
            def CronExpression ce=new CronExpression(se.generateCrontabExression())
            assertNotNull(ce)
        }catch(Exception e){
            fail("Unexpected exception: ${e}")
        }

        se =new ScheduledExecution()
        se.properties = [minute:"5",hour:"4,3,2",dayOfMonth:"3",month:"2-6",dayOfWeek:"1"]
        assertEquals "incorrect expression: ${se.generateCrontabExression()}","0 5 4,3,2 3 2-6 ? *",se.generateCrontabExression()
        try{
            def CronExpression ce=new CronExpression(se.generateCrontabExression())
            assertNotNull(ce)
        }catch(Exception e){
            fail("Unexpected exception: ${e}")
        }
        se =new ScheduledExecution()

        se.properties = [minute:"5",hour:"4,3,2",dayOfMonth:"?",month:"Feb-Jun",dayOfWeek:"Mon"]
        assertEquals "incorrect expression: ${se.generateCrontabExression()}","0 5 4,3,2 ? FEB-JUN MON *",se.generateCrontabExression()
        try{
            def CronExpression ce=new CronExpression(se.generateCrontabExression())
            assertNotNull(ce)
        }catch(Exception e){
            fail("Unexpected exception: ${e}")
        }


        //test invalid cron expression

        se =new ScheduledExecution()

        se.properties = [minute:"5",hour:"4,3,2",dayOfMonth:"3",month:"shibby",dayOfWeek:"Mon"]
        assertEquals "incorrect expression: ${se.generateCrontabExression()}","0 5 4,3,2 3 SHIBBY ? *",se.generateCrontabExression()
        try{
            def CronExpression ce=new CronExpression(se.generateCrontabExression())
            fail("should fail: ${ce}")
        }catch(Exception e){
            assertNotNull(e)
        }
    }

    void testParseCrontabString(){
        t:{
            //too few input parts
            def ScheduledExecution se = new ScheduledExecution()
            assertFalse(se.parseCrontabString('0 5 4,3,2 3 *'))
            assertFalse(se.parseCrontabString('0 5 4,3,2 3'))
            assertFalse(se.parseCrontabString('0 5 4,3,2'))
            assertFalse(se.parseCrontabString('0 5'))
            assertFalse(se.parseCrontabString('0'))
            assertFalse(se.parseCrontabString(''))
        }
        t:{ //without optional year
            def ScheduledExecution se = new ScheduledExecution()
            assertTrue(se.parseCrontabString('0 5 4,3,2 3 SHIBBY ?'))
            assertEquals '0',se.seconds
            assertEquals '5',se.minute
            assertEquals '4,3,2',se.hour
            assertEquals '3',se.dayOfMonth
            assertEquals 'SHIBBY',se.month
            assertEquals '?',se.dayOfWeek
            assertEquals '*',se.year
        }
        t:{
            def ScheduledExecution se = new ScheduledExecution()
            assertTrue(se.parseCrontabString('0 5 4,3,2 3 SHIBBY ? *'))
            assertEquals '0',se.seconds
            assertEquals '5',se.minute
            assertEquals '4,3,2',se.hour
            assertEquals '3',se.dayOfMonth
            assertEquals 'SHIBBY',se.month
            assertEquals '?',se.dayOfWeek
            assertEquals '*',se.year
        }
        t:{
            //ignores extra content at the end
            def ScheduledExecution se = new ScheduledExecution()
            assertTrue(se.parseCrontabString('0 5 4,3,2 3 SHIBBY ? * test blah test'))
            assertEquals '0',se.seconds
            assertEquals '5',se.minute
            assertEquals '4,3,2',se.hour
            assertEquals '3',se.dayOfMonth
            assertEquals 'SHIBBY',se.month
            assertEquals '?',se.dayOfWeek
            assertEquals '*',se.year
        }
    }

    void testCronExpressionIsValid(){
        assertFalse(CronExpression.isValidExpression('0 21 */4 */4 */6 3 2010-2040'))
        
    }

    void testCrontabSpecialValue(){
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

    void testFilterCrontabParams(){
        def ScheduledExecution se = new ScheduledExecution()

        def params = [
            'crontab.month.Jan':"true",
            'crontab.month.Feb':"false",
            'crontab.month.Monkey':"true",
            'crontab.month.Elfkin':"false",
            "something.else":"b",
            "crontab.second.1":"two"]
        def map = se.filterCrontabParams("month",params)
        assertEquals "map is wrong size: ${map.size()}",4,map.size()
        assertNotNull "map missing element Jan",map.Jan
        assertNotNull "map missing element Feb",map.Feb
        assertNotNull "map missing element Monkey",map.Monkey
        assertNotNull "map missing element Elfkin",map.Elfkin
        assertEquals "map element Jan had incorrect value","true",map.Jan
        assertEquals  "map element Feb had incorrect value","false",map.Feb
        assertEquals  "map element Monkey had incorrect value","true",map.Monkey
        assertEquals  "map element Elfkin had incorrect value","false",map.Elfkin
    }

    void testConstants(){
        assertEquals "Incorrect months",12,ScheduledExecution.monthsofyearlist.size()
        assertEquals "Incorrect weekdays",7,ScheduledExecution.daysofweeklist.size()
    }

    void testParseCheckboxFieldFromParams(){
        def ScheduledExecution se = new ScheduledExecution()
        def str

        //check defaultsToAsterix parameter
        str = se.parseCheckboxFieldFromParams("month",[:],true,ScheduledExecution.monthsofyearlist)
        assertEquals "value was incorrect: ${str}","*",str
        str = se.parseCheckboxFieldFromParams("month",[:],false,ScheduledExecution.monthsofyearlist)
        assertNull "value should be null: ${str}",str

        def params = [
            'crontab.month.Jan':"true",
            'crontab.month.Feb':"false",
            'crontab.month.Monkey':"true",
            'crontab.month.Elfkin':"false",
            "something.else":"b",
            "crontab.second.1":"two"]
        str= se.parseCheckboxFieldFromParams("month",params,true,ScheduledExecution.monthsofyearlist)
        assertEquals "value was incorrect: ${str}","JAN",str

        params = [
            'crontab.month.Jan':"true",
            'crontab.month.Feb':"true",
            'crontab.month.Monkey':"true",
            'crontab.month.Elfkin':"false",
            "something.else":"b",
            "crontab.second.1":"two"]
        str= se.parseCheckboxFieldFromParams("month",params,true,ScheduledExecution.monthsofyearlist)
        assertEquals "value was incorrect: ${str}","FEB,JAN",str

        //test that when all values are seen, the result is *
        params = [:]
        ScheduledExecution.monthsofyearlist.each{
            params["crontab.month.${it}"]="true"
        }
        str= se.parseCheckboxFieldFromParams("month",params,true,ScheduledExecution.monthsofyearlist)
        assertEquals "value was incorrect: ${str}","*",str
    }


    void testPopulateTimeDateFields(){
        t:{
        def ScheduledExecution se = new ScheduledExecution()
        def params = [
            'crontab.month.Jan':"true",
            'crontab.month.Feb':"true",
            'crontab.dayOfWeek.Mon':"true",
            'crontab.dayOfWeek.Wed':"true",
            ]
        se.populateTimeDateFields(params)
        assertEquals "month field was incorrect: ${se.month}","FEB,JAN",se.month
        assertEquals "dayOfWeek field was incorrect: ${se.dayOfWeek}","MON,WED",se.dayOfWeek

        se.populateTimeDateFields([everyDayOfWeek:"true", 'crontab.month.Jan':"true", 'crontab.month.Feb':"true",])
        assertEquals "month field was incorrect: ${se.month}","FEB,JAN",se.month
        assertEquals "dayOfWeek field was incorrect: ${se.dayOfWeek}","*",se.dayOfWeek

        se.populateTimeDateFields([everyMonth:"true", 'crontab.dayOfWeek.Mon':"true", 'crontab.dayOfWeek.Wed':"true",])
        assertEquals "month field was incorrect: ${se.month}","*",se.month
        assertEquals "dayOfWeek field was incorrect: ${se.dayOfWeek}","MON,WED",se.dayOfWeek
        
        se.populateTimeDateFields([everyDayOfWeek:true, 'crontab.month.Jan':"true", 'crontab.month.Feb':"true",])
        assertEquals "month field was incorrect: ${se.month}","FEB,JAN",se.month
        assertEquals "dayOfWeek field was incorrect: ${se.dayOfWeek}","*",se.dayOfWeek

        se.populateTimeDateFields([everyMonth:true, 'crontab.dayOfWeek.Mon':"true", 'crontab.dayOfWeek.Wed':"true",])
        assertEquals "month field was incorrect: ${se.month}","*",se.month
        assertEquals "dayOfWeek field was incorrect: ${se.dayOfWeek}","MON,WED",se.dayOfWeek

        }
        //use seconds, year and dayOfMonth values input:
        t:{
            def ScheduledExecution se = new ScheduledExecution()

            def params = [seconds:'1,2',year:'2010',dayOfMonth:'*/2']
            se.populateTimeDateFields(params)
            assertEquals '1,2',se.seconds
            assertEquals '2010',se.year
            assertEquals '*/2',se.dayOfMonth
            assertEquals '?',se.dayOfWeek
        }
        t:{
            def ScheduledExecution se = new ScheduledExecution()

            def params = [seconds:'1,2',year:'2010']
            se.populateTimeDateFields(params)
            assertEquals '1,2',se.seconds
            assertEquals '2010',se.year
            assertEquals '?',se.dayOfMonth
            assertEquals '*',se.dayOfWeek
        }

        //test crontabString usage
        t:{
            def ScheduledExecution se = new ScheduledExecution()

            def params = [crontabString:'0 0 0 3 * ? 2009',useCrontabString:'true',seconds:'1,2',year:'2010',dayOfMonth:'*/2']
            se.populateTimeDateFields(params)
            assertEquals '0',se.seconds
            assertEquals '0',se.minute
            assertEquals '0',se.hour
            assertEquals '3',se.dayOfMonth
            assertEquals '*',se.month
            assertEquals '?',se.dayOfWeek
            assertEquals '2009',se.year
        }
        //test crontabString usage, useCrontabString='false'
        t:{
            def ScheduledExecution se = new ScheduledExecution()

            def params = [crontabString:'0 0 0 3 * ? 2009',useCrontabString:'false',seconds:'1,2',year:'2010',dayOfMonth:'*/2']
            se.populateTimeDateFields(params)
            assertEquals '1,2',se.seconds
            assertEquals '0',se.minute
            assertEquals '0',se.hour
            assertEquals '*/2',se.dayOfMonth
            assertEquals '*',se.month
            assertEquals '?',se.dayOfWeek
            assertEquals '2010',se.year
        }
    }

    void testTimeAndDateAsBooleanMap(){
        def ScheduledExecution se = new ScheduledExecution()
        def params = [
            'crontab.month.Jan':"true",
            'crontab.month.Feb':"true",
            'crontab.dayOfWeek.Mon':"true",
            'crontab.dayOfWeek.Wed':"true",
            ]
        se.populateTimeDateFields(params)
        def map = se.timeAndDateAsBooleanMap()
        assertNotNull "map should not be null",map
        assertEquals "map was wrong size: ${map.size()}",4,map.size()
        assertEquals "month.JAN was not true","true",map['month.JAN']
        assertEquals "month.FEB was not true","true",map['month.FEB']
        assertEquals "dayOfWeek.MON was not true","true",map['dayOfWeek.MON']
        assertEquals "dayOfWeek.WED was not true","true",map['dayOfWeek.WED']


    }
}
