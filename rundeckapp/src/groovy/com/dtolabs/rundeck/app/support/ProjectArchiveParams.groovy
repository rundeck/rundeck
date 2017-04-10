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
import groovy.transform.ToString
import rundeck.services.ArchiveOptions

/**
 * ProjectArchiveParams is ...
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-08-14
 */
@Validateable
@ToString(includeNames = true, includePackage = false)
class ProjectArchiveParams implements ProjectArchiveImportRequest{
    String project
    String jobUuidOption='preserve'
    Boolean importExecutions=true
    Boolean importConfig=false
    Boolean importACL=false
    Boolean exportAll
    Boolean exportJobs
    Boolean exportExecutions
    Boolean exportConfigs
    Boolean exportReadmes
    Boolean exportAcls

    static constraints={
        project(matches: FrameworkResource.VALID_RESOURCE_NAME_REGEX)
        jobUuidOption(nullable: true,inList: ['preserve','remove'])
        importExecutions(nullable: true)
        importConfig(nullable: true)
        importACL(nullable: true)
        exportAll(nullable: true)
        exportJobs(nullable: true)
        exportExecutions(nullable: true)
        exportConfigs(nullable: true)
        exportReadmes(nullable: true)
        exportAcls(nullable: true)
    }

    ArchiveOptions toArchiveOptions() {
        def a = new ArchiveOptions(
                all: exportAll,
                jobs: exportJobs,
                executions: exportExecutions,
                configs: exportConfigs,
                readmes: exportReadmes,
                acls: exportAcls
        )
        return a;
    }

}
