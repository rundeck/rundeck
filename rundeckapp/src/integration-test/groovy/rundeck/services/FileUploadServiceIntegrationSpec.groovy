/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

@Integration
class FileUploadServiceIntegrationSpec extends Specification {

    FileUploadService fileUploadService

    def "Ensure getPlugin returns a configured plugin"() {
        when:
        def emptyRetriever = PropertyResolverFactory.instanceRetriever([:])
        def frameworkRetriever = PropertyResolverFactory.instanceRetriever(["framework.plugin.FileUpload.filesystem-temp.basePath":"/tmp"])
        PropertyResolver frameworkPropertyResolver = PropertyResolverFactory.createResolver(emptyRetriever,emptyRetriever,frameworkRetriever)
        fileUploadService.getFrameworkService().metaClass.getFrameworkPropertyResolver = {
            frameworkPropertyResolver
        }
        def plugin = fileUploadService.getPlugin()

        then:
        plugin.basePath == "/tmp"
    }
}
