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
package rundeckapp.init

import groovy.transform.CompileStatic
import org.springframework.beans.factory.InitializingBean
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter

@CompileStatic
class ExternalStaticResourceConfigurer extends WebMvcConfigurerAdapter implements InitializingBean {

    String resourceUriLocation
    String resourceWebPathPrefix = "/user-assets/**" //If this changes, the spring security conf must be updated
                                                    // to allow unrestricted access to the chosen path.
                                                    //Also the path in IntercepterHelper.groovy must be updated.

    @Override
    void addResourceHandlers(final ResourceHandlerRegistry registry) {
        registry.addResourceHandler(resourceWebPathPrefix).addResourceLocations(resourceUriLocation)
    }

    @Override
    void afterPropertiesSet() throws Exception {
        if(!resourceUriLocation) throw new Exception("The file system base location for static user assets must be specified.")

        resourceUriLocation = ensurePrefix("file:",resourceUriLocation)
        resourceUriLocation = ensureSuffix("/",resourceUriLocation)
    }

    private String ensurePrefix(String prefix, String value) {
        if(!value.startsWith(prefix)) {
            return prefix + value
        }
        return value
    }
    private String ensureSuffix(String suffix, String value) {
        if(!value.endsWith(suffix)) {
            return value + suffix
        }
        return value
    }
}
