package com.dtolabs.rundeck.core.storage;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.Resource;
import org.rundeck.storage.conf.BaseListener;

import java.util.Map;

/**
 * StorageLogger logs storage events to a Log4j logger.
 *
 * @author greg
 * @since 2014-03-14
 */
public class StorageLogger extends BaseListener<ResourceMeta> {
    public StorageLogger(Logger logger) {
        this.logger = logger;
    }

    public StorageLogger(String loggerName) {
        this.logger = Logger.getLogger(loggerName);
    }

    @Override
    public void didGetResource(Path path, Resource<ResourceMeta> resource) {
        log(path, "get", false, resource.getContents().getMeta(), null);
    }

    protected void log(final Path path, String action, boolean dir, Map<String, String> meta, String status) {
        MDC.put("path", path.toString());
        MDC.put("action", action);
        String metastring = null != meta ? meta.toString() : "-";
        MDC.put("metadata", metastring);
        final String type = dir ? "directory" : "file";
        MDC.put("type", type);
        String statusString = null != status ? status : "-";
        MDC.put("status", statusString);
        logger.info(action +
                    ":[" +
                    type +
                    "]:" +
                    String.valueOf(path) +
                    ":" +
                    (dir ? "" : metastring) +
                    ": " +
                    statusString);
        MDC.clear();
    }

    @Override
    public void didGetPath(Path path, Resource<ResourceMeta> resource) {
        log(path, "get", resource.getContents() == null, null, null);

    }

    @Override
    public void didDeleteResource(Path path, boolean success) {
        log(path, "delete", false, null, success ? "success" : "failed");
    }

    @Override
    public void didCreateResource(Path path, ResourceMeta content, Resource<ResourceMeta> contents) {
        log(path, "create", false, content.getMeta(), null);
    }

    @Override
    public void didUpdateResource(Path path, ResourceMeta content, Resource<ResourceMeta> contents) {
        log(path, "update", false, content.getMeta(), null);
    }

    private Logger logger;
}
