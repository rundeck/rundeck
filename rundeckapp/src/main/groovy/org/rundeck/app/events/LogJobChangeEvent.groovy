package org.rundeck.app.events

import org.rundeck.app.data.model.v1.job.JobData

class LogJobChangeEvent {
    Map<String,String> changeinfo = [:]
    JobData jobData

    LogJobChangeEvent() {}
    LogJobChangeEvent(String changeType, String source, String user) {
        setChangeType(changeType)
        setSource(source)
        setUser(user)
    }

    void setChangeType(String type) {
        changeinfo['change'] = type
    }

    void setSource(String source) {
        changeinfo['method'] = source
    }

    void setUser(String user) {
        changeinfo['user'] = user
    }
}
