/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rundeck.quartzjobs

import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.execution.WorkflowExecutionServiceThread
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionService
import com.dtolabs.rundeck.core.jobs.IJobPluginService
import com.dtolabs.rundeck.core.logging.LoggingManager

/**
 * Created by greg on 2/9/15.
 */
class TestWEServiceThread extends WorkflowExecutionServiceThread {
    boolean mysuccess
    TestWEServiceThread(
            final WorkflowExecutionService eservice,
            final WorkflowExecutionItem eitem,
            final StepExecutionContext econtext,
            LoggingManager loggingManager,
            IJobPluginService iJobPluginService,
            ExecutionReference executionReference
    )
    {
        super(eservice, eitem, econtext, loggingManager, iJobPluginService, executionReference)
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
