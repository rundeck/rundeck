package rundeck.data.job

import com.dtolabs.rundeck.core.jobs.JobRevReference

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
