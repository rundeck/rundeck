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

package com.dtolabs.rundeck.server.plugins.storage

import com.dtolabs.rundeck.core.storage.ResourceMeta
import org.apache.log4j.Logger
import org.apache.log4j.MDC
import org.rundeck.storage.api.Path
import org.rundeck.storage.api.Resource
import org.rundeck.storage.conf.BaseListener

/**
 * StorageLogger logs storage events to a Log4j logger.
 * @author greg
 * @since 2014-03-14
 */
class StorageLogger extends BaseListener<ResourceMeta> {
    private Logger logger

    StorageLogger(Logger logger) {
        this.logger = logger
    }

    StorageLogger(String loggerName) {
        this.logger = Logger.getLogger(loggerName)
    }

    @Override
    void didGetResource(Path path, Resource<ResourceMeta> resource) {
        log(path, "get", false, resource.contents.meta, null)
    }

    protected void log(Path path, String action, boolean dir, Map<String, String> meta, String status) {
        MDC.put("path", path.toString())
        MDC.put("action", action)
        def metastring = null!=meta?meta.toString():"-"
        MDC.put("metadata", metastring)
        def type = dir ? "directory" : "file"
        MDC.put("type", type)
        def statusString = null != status ? status : "-"
        MDC.put("status", statusString)
        logger.info(action + ":[${type}]:${path}:" + (dir ? "" : metastring) + ": " + statusString)
        MDC.clear()
    }

    @Override
    void didGetPath(Path path, Resource<ResourceMeta> resource) {
        log(path, "get", resource.contents == null, null, null)

    }

    @Override
    void didDeleteResource(Path path, boolean success) {
        log(path, "delete", false, null, success ? "success" : "failed")
    }

    @Override
    void didCreateResource(Path path, ResourceMeta content, Resource<ResourceMeta> contents) {
        log(path, "create", false, content.meta, null)
    }

    @Override
    void didUpdateResource(Path path, ResourceMeta content, Resource<ResourceMeta> contents) {
        log(path, "update", false, content.meta, null)
    }
}
