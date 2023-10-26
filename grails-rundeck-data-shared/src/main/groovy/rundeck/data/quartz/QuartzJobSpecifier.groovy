package rundeck.data.quartz

import org.quartz.Job

interface QuartzJobSpecifier {
    Class<? extends Job> getJobClass()
}