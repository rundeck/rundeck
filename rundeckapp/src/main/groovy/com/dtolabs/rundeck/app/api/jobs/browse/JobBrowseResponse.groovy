package com.dtolabs.rundeck.app.api.jobs.browse

import groovy.transform.CompileStatic

@CompileStatic
class JobBrowseResponse {
    String path
    List<JobBrowseItemData> items
}
