package rundeck.services.scm

import com.dtolabs.rundeck.plugins.jobs.JobChangeListener
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import com.dtolabs.rundeck.plugins.scm.JobSerializer

/**
 * Can be overridden to filter events to pass to the sub listener, default action is to pass all events
 */
class FilterJobChangeListener implements JobChangeListener {
    JobChangeListener listener

    FilterJobChangeListener(final JobChangeListener listener) {
        this.listener = listener
    }

    @Override
    void jobChangeEvent(final JobChangeEvent event, final JobSerializer serializer) {
        listener?.jobChangeEvent(event, serializer)
    }
}
