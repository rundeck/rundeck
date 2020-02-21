package com.dtolabs.rundeck.app.support

interface ProjectArchiveExportRequest {
    Set<String> getExecutionIds()
    /**
     * if true, only include the executions in the executionIds set
     */
    boolean isExecutionsOnly()

    boolean isAll()

    boolean isJobs()

    boolean isExecutions()

    boolean isConfigs()

    boolean isReadmes()

    boolean isAcls()

    boolean isScm()

    boolean isWebhooks()

    boolean isWebhooksIncludeAuthTokens()

    Map<String, Map<String, String>> getExportOpts()

    Map<String, Boolean> getExportComponents()

    String getStripJobRef()

}
