package com.rundeck.plugin

import com.rundeck.plugin.jobs.ExecutionStatusJob
import org.quartz.JobBuilder
import org.quartz.JobDataMap
import org.quartz.SimpleTrigger

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

import static org.quartz.TriggerBuilder.newTrigger

class PluginUtil {
    static final Map<String, Long> TIME_UNITS = [s: 1, m: 60, h: 60 * 60, d: 24 * 60 * 60,w: 7 * 24 * 60 * 60, y: 365 * 24 * 60 * 60]
    public static final int V34 = 34

    static boolean validateTimeDuration(String time){
        def matcher = (time =~ /(\d+)(.)?/)
        if(matcher){
            return true
        }else{
            return false
        }
    }

    static long parseTimeDuration(String time, TimeUnit unit = TimeUnit.SECONDS) {
        long timeval = 0
        def matcher = (time =~ /(\d+)(.)?/)
        matcher.each { m ->
            long val
            try {
                val = Long.parseLong(m[1])
            } catch (NumberFormatException e) {
                return
            }
            if (m[2] && TIME_UNITS[m[2]]) {
                timeval += (val * TIME_UNITS[m[2]])
            } else if (!m[2]) {
                timeval += val
            }
        }
        return unit.convert(timeval, TimeUnit.SECONDS)
    }

    def static laterDate(def dateSaved, def timeDefinition, String dateFormatPattern){
        DateFormat dateFormat = new SimpleDateFormat(dateFormatPattern);
        Date date = dateFormat.parse(dateSaved)

        Long seconds = PluginUtil.parseTimeDuration(timeDefinition)

        Calendar calendar = Calendar.getInstance()
        calendar.setTime(date)
        calendar.add(Calendar.SECOND, seconds.intValue())

        Date finalDate = calendar.getTime()
        return finalDate
    }

    def static createTrigger(String jobname, String jobgroup, Date startTime) {
        SimpleTrigger trigger = (SimpleTrigger) newTrigger()
                .withIdentity("${jobname}", "${jobgroup}")
                .startAt(startTime) // some Date
                .forJob(jobname, jobgroup) // identify job with name, group strings
                .build()

        return trigger
    }


    def static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillies,TimeUnit.MILLISECONDS);
    }

}
