package rundeck.data.job

import com.dtolabs.rundeck.core.jobs.JobReference

class JobReferenceImpl  implements JobReference {
    String id
    String project
    String jobName
    String groupPath
    String serverUUID
    String originalQuartzJobName
    String originalQuartzGroupName

    @Override
    String getJobAndGroup() {
        null != groupPath ? groupPath + '/' + jobName : jobName
    }

    @Override
    public String toString() {
        return "JobReference{" +
                "id='" + id + '\'' +
                ", project='" + project + '\'' +
                ", jobName='" + jobName + '\'' +
                ", groupPath='" + groupPath + '\'' +
                '}';
    }
}
