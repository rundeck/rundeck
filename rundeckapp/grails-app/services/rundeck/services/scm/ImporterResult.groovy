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

package rundeck.services.scm

import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.plugins.scm.ImportResult
import com.dtolabs.rundeck.plugins.scm.JobScmReference

/**
 * Created by greg on 10/2/15.
 */
class ImporterResult implements ImportResult {
    boolean successful
    String errorMessage
    JobScmReference job
    boolean created
    boolean modified

    static ImportResult fail(String message) {
        def result = new ImporterResult()
        result.successful = false
        result.errorMessage = message
        return result
    }

    @Override
    public String toString() {
        if (!successful) {
            return "Failed: " + errorMessage
        }
        return (created ? "Created " : "Modified ") + "Job: " + formatJob();
    }

    private String formatJob() {
        '{{Job ' + job.id + '}}'
    }
}
