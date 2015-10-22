package rundeck.services.scm

import com.dtolabs.rundeck.plugins.jobs.JobChangeListener
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import com.dtolabs.rundeck.plugins.scm.JobSerializer

/**
 * only passes events for the given project
 */
class ProjectJobChangeListener extends FilterJobChangeListener {
    String project

    ProjectJobChangeListener(final JobChangeListener listener, final String project) {
        super(listener)
        this.project = project
    }

    @Override
    void jobChangeEvent(final JobChangeEvent event, final JobSerializer serializer) {
        if (event.originalJobReference.project == project) {
            super.jobChangeEvent(event, serializer)
        }
    }
}
