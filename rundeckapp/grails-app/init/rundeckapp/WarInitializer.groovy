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
package rundeckapp

import grails.web.context.ServletContextHolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.web.WebApplicationInitializer
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext
import org.springframework.web.servlet.DispatcherServlet
import rundeckapp.init.RundeckInitConfig

import javax.servlet.ServletContext
import javax.servlet.ServletException
import javax.servlet.ServletRegistration


class WarInitializer implements WebApplicationInitializer, Ordered {
    Logger logger = LoggerFactory.getLogger(WarInitializer)
    @Override
    void onStartup(final ServletContext servletContext) throws ServletException {
        String rdeckbase = servletContext.getInitParameter(RundeckInitConfig.SYS_PROP_RUNDECK_BASE_DIR)
        if(rdeckbase) {
            logger.info("Found servlet context init parameter for ${RundeckInitConfig.SYS_PROP_RUNDECK_BASE_DIR}: ${rdeckbase}")
            System.setProperty(RundeckInitConfig.SYS_PROP_RUNDECK_BASE_DIR, rdeckbase)
        }

        String rdeckConfigFileServletCtxParam = servletContext.getInitParameter(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION)
        if( rdeckConfigFileServletCtxParam) {
            logger.info("Found servlet context init parameter for ${RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION}: ${rdeckbase}")
            System.setProperty(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION,  rdeckConfigFileServletCtxParam)
        }

        ServletRegistration.Dynamic d = servletContext.addServlet("init", new DispatcherServlet())
        d.setLoadOnStartup(-1)
        d.addMapping("/rd-init")
    }

    @Override
    int getOrder() {
        return Integer.parseInt(System.getProperty("war.init.order","0"))
    }
}
