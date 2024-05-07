package rundeck.services

import com.dtolabs.rundeck.core.schedule.JobScheduleFailure
import grails.testing.services.ServiceUnitTest
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.quartz.JobKey
import org.quartz.Scheduler
import rundeck.data.quartz.QuartzJobSpecifier
import rundeck.quartzjobs.ExecutionJob
import spock.lang.Specification

class QuartzJobScheduleManagerServiceSpec extends Specification implements ServiceUnitTest<QuartzJobScheduleManagerService> {
    def "interrupt schedule job NOW if execution mode is PASSIVE"(){
        given:
        defineBeans{
            quartzScheduler(InstanceFactoryBean, Mock(Scheduler))
            frameworkService(InstanceFactoryBean, Mock(FrameworkService){
                configurationService >> Mock(ConfigurationService){
                    isExecutionModeActive() >> false
                }
            })
        }
        when:
        service.scheduleJobNow('a', 'b', [:], true)
        then:
        JobScheduleFailure e = thrown(JobScheduleFailure)
        e.getMessage() == 'Execution mode is PASSIVE'
    }

    def "interrupt schedule job if execution mode is PASSIVE"(){
        given:
        defineBeans{
            quartzScheduler(InstanceFactoryBean, Mock(Scheduler))
            frameworkService(InstanceFactoryBean, Mock(FrameworkService){
                configurationService >> Mock(ConfigurationService){
                    isExecutionModeActive() >> false
                }
            })
        }
        when:
        service.scheduleJob('a', 'b', [:], new Date(), true)
        then:
        JobScheduleFailure e = thrown(JobScheduleFailure)
        e.getMessage() == 'Execution mode is PASSIVE'
    }

    def "schedule job NOW if execution mode is ACTIVE"(){
        given:
        defineBeans{
            quartzScheduler(InstanceFactoryBean, Mock(Scheduler))
            frameworkService(InstanceFactoryBean, Mock(FrameworkService){
                configurationService >> Mock(ConfigurationService){
                    isExecutionModeActive() >> true
                }
            })
        }
        when:
        service.scheduleJobNow('a', 'b', [:], true)
        then:
        notThrown(JobScheduleFailure)
        1 * service.quartzScheduler.scheduleJob(_,_)
    }

    def "schedule job if execution mode is ACTIVE"(){
        given:
        JobKey jobKey = new JobKey('a', 'b')
        defineBeans{
            quartzScheduler(InstanceFactoryBean, Mock(Scheduler){
                checkExists(jobKey) >> scheduleExists
            })
            frameworkService(InstanceFactoryBean, Mock(FrameworkService){
                configurationService >> Mock(ConfigurationService){
                    isExecutionModeActive() >> true
                }
            })
        }
        when:
        service.scheduleJob(jobKey.getName(), jobKey.getGroup(), [:], new Date(), true)
        then:
        notThrown(JobScheduleFailure)
        scheduleJobCalls * service.quartzScheduler.scheduleJob(_,_)
        rescheduleJobCalls * service.quartzScheduler.rescheduleJob(_,_)

        where:
        scheduleExists | rescheduleJobCalls | scheduleJobCalls
        true           | 1                  | 0
        false          | 0                  | 1
    }

    def setup(){
        defineBeans {
            scheduledExecutionService(InstanceFactoryBean, Mock(ScheduledExecutionService))
            quartzJobSpecifier(InstanceFactoryBean, Mock(QuartzJobSpecifier){
                getJobClass() >> ExecutionJob
            })
        }
    }
}
