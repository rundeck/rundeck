package com.dtolabs.rundeck.server.plugins.services;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.plugins.BaseProviderRegistryService;
import com.dtolabs.rundeck.plugins.storage.ResourceStoragePlugin;
import com.dtolabs.rundeck.server.plugins.storage.FileResourceStoragePlugin;

/**
 * BuiltinResourceStoragePluginProviderService holds built in plugins
 *
 * @author greg
 * @since 2014-02-21
 */
public class BuiltinResourceStoragePluginProviderService extends BaseProviderRegistryService<ResourceStoragePlugin> {
    private String name;

    public BuiltinResourceStoragePluginProviderService(Framework framework, String name) {
        super(framework);
        this.setName(name);
        registerClass(FileResourceStoragePlugin.PROVIDER_NAME, FileResourceStoragePlugin.class);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
