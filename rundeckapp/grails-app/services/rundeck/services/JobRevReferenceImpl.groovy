package rundeck.services

import com.dtolabs.rundeck.core.jobs.JobRevReference

/**
 * Created by greg on 8/21/15.
 */
class JobRevReferenceImpl extends JobReferenceImpl implements JobRevReference {
    Long version


    @Override
    public String toString() {
        return "JobReference{" +
                "id='" + id + '\'' +
                ", project='" + project + '\'' +
                ", jobName='" + jobName + '\'' +
                ", groupPath='" + groupPath + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
