/*
 * Copyright 2020 Rundeck, Inc. (http://rundeck.com)
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
package com.dtolabs.rundeck.app.gui

import com.dtolabs.rundeck.core.common.IRundeckProjectConfig
import org.rundeck.app.gui.JobListLinkHandler
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import rundeck.services.FrameworkService


class JobListLinkHandlerRegistry implements InitializingBean {

    private Map<String, JobListLinkHandler> handlers = [:]

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    FrameworkService frameworkService

    @Override
    void afterPropertiesSet() throws Exception {
        applicationContext.getBeansOfType(JobListLinkHandler). each { k, v ->
            handlers[v.name] = v
        }
    }

    String defaultHandlerName = GroupedJobListLinkHandler.NAME

    JobListLinkHandler getJobListLinkHandler(String projectConfigValue) {
        return handlers[projectConfigValue]
    }

    JobListLinkHandler getJobListLinkHandlerForProject(String project) {
        IRundeckProjectConfig rdprojectconfig = frameworkService.getRundeckFramework().projectManager.loadProjectConfig(project)
        String defaultJobListForProject = rdprojectconfig.properties['project.gui.job.list.default'] ?: defaultHandlerName
        return handlers[defaultJobListForProject]
    }

}
