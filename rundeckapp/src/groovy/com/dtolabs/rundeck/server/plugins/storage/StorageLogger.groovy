package com.dtolabs.rundeck.server.plugins.storage

import com.dtolabs.rundeck.core.storage.ResourceMeta
import org.apache.log4j.Logger
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
    StorageLogger(String loggerName){
        this.logger=Logger.getLogger(loggerName)
    }

    @Override
    void didGetResource(Path path, Resource<ResourceMeta> resource) {
        logger.info("get:${path}:"+resource.contents.meta)
    }

    @Override
    void didGetPath(Path path, Resource<ResourceMeta> resource) {
        logger.info("get:${path}: " + (resource.contents!=null? resource.contents.meta:"(dir)"))
    }

    @Override
    void didDeleteResource(Path path, boolean success) {
        logger.info("delete:${path}: " + success)
    }

    @Override
    void didCreateResource(Path path, ResourceMeta content, Resource<ResourceMeta> contents) {
        logger.info("create:${path}: " + content.meta)
    }

    @Override
    void didUpdateResource(Path path, ResourceMeta content, Resource<ResourceMeta> contents) {
        logger.info("update:${path}: " + content.meta)
    }
}
