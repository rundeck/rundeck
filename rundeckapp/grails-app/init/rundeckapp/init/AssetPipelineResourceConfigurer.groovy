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
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
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
 * 
 * @Configuration ensures Spring picks this up as a configuration class
 * @Order(Ordered.HIGHEST_PRECEDENCE) ensures this runs before security filters
 */
@Slf4j
@CompileStatic
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
class AssetPipelineResourceConfigurer implements WebMvcConfigurer {

    @PostConstruct
    void init() {
        log.info("═══════════════════════════════════════════════════════")
        log.info("AssetPipelineResourceConfigurer BEAN LOADED")
        log.info("  This configurer will map /assets/** to classpath:/assets/")
        log.info("═══════════════════════════════════════════════════════")
    }

    @Override
    void addResourceHandlers(final ResourceHandlerRegistry registry) {
        log.info("═══════════════════════════════════════════════════════")
        log.info("CONFIGURING ASSET PIPELINE RESOURCE HANDLERS")
        log.info("  Pattern: /assets/**")
        log.info("  Location: classpath:/assets/")
        log.info("  Cache Period: 1 year (31536000 seconds)")
        log.info("═══════════════════════════════════════════════════════")
        
        // Map /assets/** to classpath:/assets/ with caching and proper resource chain
        registry
            .addResourceHandler("/assets/**")
            .addResourceLocations("classpath:/assets/")
            .setCachePeriod(31536000) // 1 year cache for asset pipeline (hashed filenames)
            .resourceChain(true)  // Enable resource chain for proper resolution
        
        log.info("✅ Asset pipeline resource handler configured successfully")
    }
}

