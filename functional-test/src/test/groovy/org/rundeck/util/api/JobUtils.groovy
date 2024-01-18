package org.rundeck.util.api

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.tests.functional.api.ResponseModels.Job
import org.rundeck.util.container.RdClient

class JobUtils {

    static def executeJobWithArgs = (String jobId, RdClient client, String args) -> {
        return client.doPostWithoutBody("/job/${jobId}/run?argString=${args}")
    }

    static executeJobWithArgsInvalidMethod = (String jobId, RdClient client, String args) -> {
        return client.doGetAcceptAll("/job/${jobId}/run?argString=${args}")
    }

    static def executeJob = (jobId, RdClient client) -> {
        return client.doPost("/job/${jobId}/run", "{}")
    }

    static def createJob(
            final String project,
            final String jobDefinitionXml,
            RdClient client
    ) {
        final String CREATE_JOB_ENDPOINT = "/project/${project}/jobs/import"
        return client.doPostWithRawText(CREATE_JOB_ENDPOINT, "application/xml", jobDefinitionXml)
    }

    static def generateExecForEnabledXmlTest(String jobName){
        return  "<joblist>\n" +
                "   <job>\n" +
                "      <name>${jobName}</name>\n" +
                "      <group>api-test</group>\n" +
                "      <description></description>\n" +
                "      <loglevel>INFO</loglevel>\n" +
                "      <dispatch>\n" +
                "        <threadcount>1</threadcount>\n" +
                "        <keepgoing>true</keepgoing>\n" +
                "      </dispatch>\n" +
                "      <sequence>\n" +
                "        <command>\n" +
                "        <exec>echo hello there</exec>\n" +
                "        </command>\n" +
                "      </sequence>\n" +
                "   </job>\n" +
                "</joblist>"
    }

    static def generateScheduledJobsXml(String jobname){
        return "<joblist>\n" +
                "   <job>\n" +
                "      <name>${jobname}</name>\n" +
                "      <group>api-test</group>\n" +
                "      <description></description>\n" +
                "      <loglevel>INFO</loglevel>\n" +
                "      <dispatch>\n" +
                "        <threadcount>1</threadcount>\n" +
                "        <keepgoing>true</keepgoing>\n" +
                "      </dispatch>\n" +
                "      <schedule>\n" +
                "        <time hour='*' seconds='*' minute='0/20' />\n" +
                "        <month month='*' />\n" +
                "        <year year='*' />\n" +
                "      </schedule>\n" +
                "      <sequence>\n" +
                "        <command>\n" +
                "        <exec>echo hello there</exec>\n" +
                "        </command>\n" +
                "      </sequence>\n" +
                "   </job>\n" +
                "</joblist>"
    }

    static Job getJobDetailsById(final String jobId, final ObjectMapper mapper, RdClient client){
        def jobInfo = client.doGetAcceptAll("/job/${jobId}")
        List<Job> jobs = mapper.readValue(jobInfo.body().string(), mapper.getTypeFactory().constructCollectionType(List.class, Job.class))
        return jobs[0]
    }
}
