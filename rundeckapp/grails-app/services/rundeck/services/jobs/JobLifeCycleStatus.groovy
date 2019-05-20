package rundeck.services.jobs

import com.dtolabs.rundeck.core.jobs.JobStatus

class JobLifeCycleStatus implements JobStatus{

    boolean successful;
    String description;

    JobLifeCycleStatus (boolean successful, String description){
        this.successful = successful
        this.description = description
    }

    JobLifeCycleStatus(){}

    @Override
    public String toString() {
        return "${this.class.name}{" +
                "successful=" + successful +
                ", description=" + description +
                '}';
    }
}
