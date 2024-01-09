package rundeck.services

import com.dtolabs.rundeck.core.schedule.JobScheduleFailure
import com.dtolabs.rundeck.core.schedule.JobScheduleManager
import grails.testing.services.ServiceUnitTest
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.quartz.Scheduler
import org.quartz.core.QuartzScheduler
import rundeck.data.quartz.QuartzJobSpecifier
import spock.lang.Specification

class QuartzJobScheduleManagerServiceSpec extends Specification implements ServiceUnitTest<QuartzJobScheduleManagerService> {
    def "interrupt schedule job NOW if executions are disabled"(){
        when:
        service.scheduleJobNow('a', 'b', [:], true)
        then:
        JobScheduleFailure e = thrown(JobScheduleFailure)
        e.getMessage() == 'Execution mode is PASSIVE'

    }
    def "interrupt schedule job if executions are disabled"(){
        when:
        service.scheduleJob('a', 'b', [:], new Date(), true)
        then:
        JobScheduleFailure e = thrown(JobScheduleFailure)
        e.getMessage() == 'Execution mode is PASSIVE'
    }

    def setup(){
        defineBeans {
            quartzScheduler(InstanceFactoryBean, Mock(Scheduler))
            frameworkService(InstanceFactoryBean, Mock(FrameworkService){
                configurationService >> Mock(ConfigurationService){
                    isExecutionModeActive() >> false
                }
            })
            scheduledExecutionService(InstanceFactoryBean, Mock(ScheduledExecutionService))
            quartzJobSpecifier(InstanceFactoryBean, Mock(QuartzJobSpecifier))
        }
    }


}
