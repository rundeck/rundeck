/*
 * Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>
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

package com.dtolabs.rundeck.app.support

import com.dtolabs.rundeck.core.common.FrameworkResource
import grails.validation.Validateable

/**
 * ProjectArchiveParams is ...
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-08-14
 */
@Validateable
class ProjectArchiveParams {
    String project
    String jobUUIDImportBehavior
    String executionImportBehavior

    static constraints={
        project(matches: FrameworkResource.VALID_RESOURCE_NAME_REGEX)
        jobUUIDImportBehavior(nullable: true,inList: ['preserve','remove'])
        executionImportBehavior(nullable: true,inList: ['import','skip'])
    }
}
