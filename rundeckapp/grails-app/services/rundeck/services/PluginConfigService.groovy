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

package rundeck.services

import grails.transaction.Transactional
import rundeck.services.scm.ScmPluginConfig
import rundeck.services.scm.ScmPluginConfigData

@Transactional
class PluginConfigService {
    def frameworkService

    ScmPluginConfigData loadScmConfig(String project, String path, String prefix) {
        def project1 = frameworkService.getFrameworkProject(project)
        if (!project1.existsFileResource(path)) {
            return null
        }
        def baos = new ByteArrayOutputStream()
        project1.loadFileResource(path, baos)
        return ScmPluginConfig.loadFromStream(prefix, new ByteArrayInputStream(baos.toByteArray()))
    }

    def storeConfig(ScmPluginConfigData scmPluginConfig, String project, String configPath) {
        def project1 = frameworkService.getFrameworkProject(project)
        project1.storeFileResource configPath, scmPluginConfig.asInputStream()
    }


    def removePluginConfiguration(String project, String path) {
        def project1 = frameworkService.getFrameworkProject(project)
        if (project1.existsFileResource(path)) {
            project1.deleteFileResource(path)
        }
    }
}
