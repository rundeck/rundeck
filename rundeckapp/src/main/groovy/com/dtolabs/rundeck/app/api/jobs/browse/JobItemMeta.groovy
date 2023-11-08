package com.dtolabs.rundeck.app.api.jobs.browse

import groovy.transform.CompileStatic
import org.rundeck.app.components.jobs.JobMeta

@CompileStatic
class JobItemMeta {
    String name
    Map<String,Object> data

    static JobItemMeta from(JobMeta meta) {
        new JobItemMeta(name: meta.name, data: meta.data)
    }
}
