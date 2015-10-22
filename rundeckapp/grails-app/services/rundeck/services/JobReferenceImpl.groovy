package rundeck.services

import com.dtolabs.rundeck.core.jobs.JobReference

/**
 * Created by greg on 2/3/15.
 */
class JobReferenceImpl implements JobReference {
    String id
    String project
    String jobName
    String groupPath

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
