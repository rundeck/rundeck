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
package rundeckapp.init

import org.springframework.core.annotation.Order
import org.springframework.web.WebApplicationInitializer
import rundeckapp.Application

import javax.servlet.ServletContext
import javax.servlet.ServletException

/**
 * This class only runs when Rundeck is inside a container.
 * The preboot routine runs before any of the Spring web application initialization occurs so
 * that all of the directories and system properties are setup properly
 */
@Order(-1)
class RundeckWebAppInitializer implements WebApplicationInitializer {
    @Override
    void onStartup(final ServletContext servletContext) throws ServletException {
        if (!Application.runPrebootstrap()) {
            throw new ServletException("Error in Preboostrap")
        }
    }
}
