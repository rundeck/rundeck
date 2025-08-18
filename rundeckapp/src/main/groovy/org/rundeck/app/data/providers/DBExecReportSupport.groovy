package org.rundeck.app.data.providers

/**
 * Support for deleting all exec reports by execution id
 */
interface DBExecReportSupport {
    void deleteAllByExecutionId(Long id)
}
