package com.dtolabs.rundeck.core.storage.files;

import com.dtolabs.rundeck.core.storage.ResourceMeta;
import com.dtolabs.rundeck.core.storage.StorageTree;
import org.rundeck.app.spi.AppService;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.Resource;

import java.io.IOException;

public interface FileStorageTree extends StorageTree, AppService {
    /**
     * @param path path
     * @return file resource
     * @throws com.dtolabs.rundeck.core.storage.WrongContentType if not the right content type
     * @throws org.rundeck.storage.api.StorageException if not found
     */
    Resource<ResourceMeta> getFIle(Path path);

    /**
     * @param path path
     * @return file resource
     * @throws com.dtolabs.rundeck.core.storage.WrongContentType if not the right content type
     * @throws org.rundeck.storage.api.StorageException if not found
     */
    Resource<ResourceMeta> getFIle(String path);

    /**
     *
     * @param path path
     * @return file data
     *
     * @throws com.dtolabs.rundeck.core.storage.WrongContentType if not the right content type
     * @throws org.rundeck.storage.api.StorageException if not found
     */
    byte[] readFile(Path path) throws IOException;

    /**
     *
     * @param path path
     * @return file data
     *
     * @throws com.dtolabs.rundeck.core.storage.WrongContentType if not the right content type
     * @throws org.rundeck.storage.api.StorageException if not found
     */
    byte[] readFile(String path) throws IOException;

    /**
     *
     * @param path path
     * @return true if the resource exists and is the right content type
     *
     */
    boolean hasFile(String path);


    /**
     * @param path path
     * @param content content
     * @param project project name
     * @param jobName job name
     * @param execId execution Id
     * @return file resource
     */
    Resource<ResourceMeta> createResource(
            Path path, ResourceMeta content, String project, String jobName, String execId);

    /**
     * @param path path
     * @param content content
     * @param project project name
     * @param jobName job name
     * @param execId execution Id
     * @return file resource
     */
    Resource<ResourceMeta> updateResource(
            Path path, ResourceMeta content, String project, String jobName, String execId);

    /**
     * @param path path
     * @param project project name
     * @param jobName job name
     * @param execId execution Id
     * @return contentType resource contentType
     */
    public String getContentType(Path path, String project, String jobName, String execId) throws IOException;


    /**
     * @param path path
     * @param project project name
     * @param jobName job name
     * @param execId execution Id
     * @return true if the path exists
     */
    public boolean hasDirectoryOnExecWorkpacePath(Path path, String project, String jobName, String execId);


    /**
     * @param path path
     * @param project project name
     * @param jobName job name
     * @param execId execution Id
     * @return true if the resource exists
     */
    public boolean hasFileOnExecWorkpacePath(Path path, String project, String jobName, String execId);


    /**
     * @param path path
     * @param project project name
     * @param jobName job name
     * @param execId execution Id
     * @return file data
     */
    public byte[] readFileOnExecWorkspacePath(Path path, String project, String jobName, String execId) throws IOException;

    /**
     * @param path path
     * @param project project name
     * @param jobName job name
     * @param execId execution Id
     * @return file resource
     */
    public Resource<ResourceMeta> getFIle(Path path, String project, String jobName, String execId);

    /**
     *
     * @param path path
     * @return true if the resource exists and is the right content type
     *
     */
    boolean hasFile(Path path);

    /**
     *
     * @param path path
     * @return file contentType
     *
     */
    public String getContentType(Path path) throws IOException;
}
