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
@ToString(includeNames = true, includePackage = false)
class ProjectArchiveParams implements ProjectArchiveImportRequest, Validateable{
    String project
    String jobUuidOption='preserve'
    Boolean importExecutions=true
    Boolean importConfig=false
    Boolean importNodesSources=false
    Boolean importACL=false
    Boolean importScm=false
    Boolean validateJobref=false
    Boolean exportAll
    Boolean exportJobs
    Boolean exportExecutions
    Boolean exportConfigs
    Boolean exportReadmes
    Boolean exportAcls
    Boolean exportScm
    Map<String, Boolean> importComponents
    Map<String, Boolean> exportComponents
    // Deliberately raw. Grails 8.0.0-M6 hands these fields both forms of a dotted request
    // parameter -- the flat 'testcomponent.someoption': 'avalue' and the nested
    // 'testcomponent': [someoption: 'avalue'] -- and a declared value type makes the binder try to
    // convert the flat entry's String into it. That conversion fails, the whole field is rejected
    // with a typeMismatch, and the action returns a validation error before doing any work.
    // cleanMapData() below already drops dotted keys and non-Map values, so the declared type was
    // buying nothing the normalisation does not already guarantee.
    Map importOpts
    Map exportOpts
    String stripJobRef
    /*  used by "promote" action */
    String targetproject
    String apitoken
    String url
    Boolean preserveuuid
    Boolean asyncImport

    static constraints={
        project(matches: FrameworkResource.VALID_RESOURCE_NAME_REGEX)
        jobUuidOption(nullable: true,inList: ['preserve','remove'])
        importExecutions(nullable: true)
        importConfig(nullable: true)
        importNodesSources(nullable: true)
        importACL(nullable: true)
        importScm(nullable: true)
        exportAll(nullable: true)
        exportJobs(nullable: true)
        exportExecutions(nullable: true)
        exportConfigs(nullable: true)
        exportReadmes(nullable: true)
        exportAcls(nullable: true)
        exportScm(nullable: true)
        stripJobRef(nullable: true)
        importOpts(nullable: true)
        exportOpts(nullable: true)
        exportComponents(nullable: true)
        importComponents(nullable: true)
        targetproject(nullable: true)
        apitoken(nullable: true)
        url(nullable: true)
        preserveuuid(nullable: true)
        asyncImport(nullable: true)
    }

    void cleanComponentOpts(){
        if(exportComponents) {
            exportComponents = cleanBooleanMap(exportComponents)
        }
        if(importComponents) {
            importComponents = cleanBooleanMap(importComponents)
        }
        if(exportOpts){
            exportOpts=cleanMapData(exportOpts)
        }
        if(importOpts){
            importOpts=cleanMapData(importOpts)
        }
    }

    public LinkedHashMap<String, Boolean> cleanBooleanMap(Map<String, Boolean> components) {
        Map<String, Boolean> nexportComponents = [:]
        components.each { k, v ->
            if (v && v in ['true', true]) {
                nexportComponents[k] = true
            } else {
                nexportComponents[k] = false
            }
        }
        nexportComponents
    }

    public Map<String, Map> cleanMapData(Map opts) {
        Map<String, Map<String, String>> nexportOpts = [:]
        opts.each { k, v ->
            if (!k.contains('.') && v instanceof Map) {
                nexportOpts[k] = new HashMap<>(v)
            }
        }
        nexportOpts
    }

    ProjectArchiveExportRequest toArchiveOptions() {
        cleanComponentOpts()
        new ArchiveOptions(
                all: exportAll ?: false,
                jobs: exportJobs ?: false,
                executions: exportExecutions ?: false,
                configs: exportConfigs ?: false,
                readmes: exportReadmes ?: false,
                acls: exportAcls ?: false,
                scm: exportScm ?: false,
                stripJobRef: stripJobRef,
                exportOpts: exportOpts,
                exportComponents: exportComponents,
        )
    }

}
