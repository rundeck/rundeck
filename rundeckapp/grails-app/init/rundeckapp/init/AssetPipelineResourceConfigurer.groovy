/*
 * Copyright 2025 Rundeck, Inc. (http://rundeck.com)
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
import groovy.util.logging.Slf4j
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Grails 7 / Jetty 12: Explicit resource handler for asset pipeline resources
 * 
 * In Jetty 12 / Spring Boot 3, the asset pipeline's automatic resource handlers don't
 * properly serve assets from classpath:/assets/ in all cases. This configurer ensures
 * that /assets/** URLs are mapped to classpath:/assets/ resources.
 * 
 * This fixes the issue where Vue SPA page assets like /assets/static/pages/webhooks-*.js
 * were returning 404 errors even though they existed in the WAR file.
 */
@Slf4j
@CompileStatic
class AssetPipelineResourceConfigurer implements WebMvcConfigurer {

    @Override
    void addResourceHandlers(final ResourceHandlerRegistry registry) {
        log.info("Configuring asset pipeline resource handlers for Grails 7 / Jetty 12")
        
        // Map /assets/** to classpath:/assets/ with caching and proper resource chain
        registry
            .addResourceHandler("/assets/**")
            .addResourceLocations("classpath:/assets/")
            .setCachePeriod(31536000) // 1 year cache for asset pipeline (hashed filenames)
            .resourceChain(true)  // Enable resource chain for proper resolution
        
        log.info("Asset pipeline resource handler configured: /assets/** -> classpath:/assets/")
    }
}

