package org.rundeck.plugin.example;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.storage.ResourceMeta;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.storage.StoragePlugin;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.Resource;

import java.util.Set;

/**
 * ExampleStoragePlugin is ...
 *
 * @author greg
 * @since 2014-03-18
 */
@Plugin(name = ExampleStoragePlugin.PROVIDER_NAME, service = ServiceNameConstants.ResourceStorage)
public class ExampleStoragePlugin implements StoragePlugin {
    public static final String PROVIDER_NAME = "example-storage";

    @PluginProperty(title = "Base directory", description = "Base directory to store files", required = true)
    private String baseDir;

    public ExampleStoragePlugin() {
    }
    public void init(){

    }

    @Override
    public boolean hasPath(Path path) {
        return false;
    }

    @Override
    public boolean hasPath(String path) {
        return false;
    }

    @Override
    public boolean hasResource(Path path) {
        return false;
    }

    @Override
    public boolean hasResource(String path) {
        return false;
    }

    @Override
    public boolean hasDirectory(Path path) {
        return false;
    }

    @Override
    public boolean hasDirectory(String path) {
        return false;
    }

    @Override
    public Resource<ResourceMeta> getPath(Path path) {
        return null;
    }

    @Override
    public Resource<ResourceMeta> getPath(String path) {
        return null;
    }

    @Override
    public Resource<ResourceMeta> getResource(Path path) {
        return null;
    }

    @Override
    public Resource<ResourceMeta> getResource(String path) {
        return null;
    }

    @Override
    public Set<Resource<ResourceMeta>> listDirectoryResources(Path path) {
        return null;
    }

    @Override
    public Set<Resource<ResourceMeta>> listDirectoryResources(String path) {
        return null;
    }

    @Override
    public Set<Resource<ResourceMeta>> listDirectory(Path path) {
        return null;
    }

    @Override
    public Set<Resource<ResourceMeta>> listDirectory(String path) {
        return null;
    }

    @Override
    public Set<Resource<ResourceMeta>> listDirectorySubdirs(Path path) {
        return null;
    }

    @Override
    public Set<Resource<ResourceMeta>> listDirectorySubdirs(String path) {
        return null;
    }

    @Override
    public boolean deleteResource(Path path) {
        return false;
    }

    @Override
    public boolean deleteResource(String path) {
        return false;
    }

    @Override
    public Resource<ResourceMeta> createResource(Path path, ResourceMeta content) {
        return null;
    }

    @Override
    public Resource<ResourceMeta> createResource(String path, ResourceMeta content) {
        return null;
    }

    @Override
    public Resource<ResourceMeta> updateResource(Path path, ResourceMeta content) {
        return null;
    }

    @Override
    public Resource<ResourceMeta> updateResource(String path, ResourceMeta content) {
        return null;
    }
}
