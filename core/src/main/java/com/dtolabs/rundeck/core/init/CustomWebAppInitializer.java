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
package com.dtolabs.rundeck.core.init;

public interface CustomWebAppInitializer<T> {
    /**
     * This method will be given the org.eclipse.jetty.webapp.WebAppContext that
     * comes from the running Rundeck with the embedded jetty servlet container
     *
     * The type information is not hard coded to avoid creating a hard dependency
     * on jetty
     * @param webAppContext
     */
    public void customizeWebAppContext(T webAppContext);
}
