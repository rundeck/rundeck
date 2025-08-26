package org.rundeck.util.api.responses.jobs

import org.rundeck.util.api.responses.execution.Execution

class JobExecutionsResponse {
    Object paging
    List<Execution> executions
}
