package rundeck.quartzjobs

import com.dtolabs.rundeck.core.execution.WorkflowExecutionServiceThread
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionService

/**
 * Created by greg on 2/9/15.
 */
class TestWEServiceThread extends WorkflowExecutionServiceThread {
    boolean mysuccess
    TestWEServiceThread(
            final WorkflowExecutionService eservice,
            final WorkflowExecutionItem eitem,
            final StepExecutionContext econtext
    )
    {
        super(eservice, eitem, econtext)
    }

    void setSuccessful(boolean success){
        this.mysuccess=success
    }

    @Override
    void run() {
        Thread.sleep(500)
    }

    @Override
    boolean isSuccessful() {
        return mysuccess
    }
}
