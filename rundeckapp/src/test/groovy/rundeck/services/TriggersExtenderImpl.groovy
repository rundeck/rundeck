package rundeck.services

import groovy.transform.CompileStatic
import org.rundeck.app.components.schedule.TriggerBuilderHelper
import org.rundeck.app.components.schedule.TriggersExtender
import rundeck.ScheduledExecution

@CompileStatic
class TriggersExtenderImpl implements TriggersExtender {

    ScheduledExecution job

    TriggersExtenderImpl(ScheduledExecution job) {
        this.job = job
    }

    @Override
    void extendTriggers(Object jobDetail, List<TriggerBuilderHelper> triggerBuilderHelpers) {
        triggerBuilderHelpers << new TriggerBuilderHelper(){

            LocalJobSchedulesManager schedulesManager = new LocalJobSchedulesManager()
            @Override
            Object getTriggerBuilder() {
                schedulesManager.createTriggerBuilder(job).getTriggerBuilder()
            }

            @Override
            Map getParams() {
                return null
            }

            @Override
            Object getTimeZone() {
                return null
            }
        }
    }

}