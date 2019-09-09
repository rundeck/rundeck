package com.dtolabs.rundeck.core.storage.files;

import com.dtolabs.rundeck.core.storage.ResourceMeta;
import com.dtolabs.rundeck.core.storage.TypedStorageTreeImpl;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.PathUtil;
import org.rundeck.storage.api.Resource;
import org.rundeck.storage.api.Tree;

import java.io.IOException;

public class FileStorageTreeImpl extends TypedStorageTreeImpl
        implements FileStorageTree {
    public static final String FILE_PATH_DEFAULT = "/files/";

    public FileStorageTreeImpl(Tree<ResourceMeta> delegate) {
        super(delegate);
    }

    @Override
    public Resource<ResourceMeta> getFIle(Path path) {
        return getResource(pathWithDefaultRoot(path));
    }

    @Override
    public Resource<ResourceMeta> getFIle(String path) {
        return getFIle(PathUtil.asPath(path));
    }

    @Override
    public Resource<ResourceMeta> getFIle(Path path, String project, String jobName, String execId) {
        return getResource(pathWithJobWorkspaceRoot(path, project, jobName, execId));
    }

    @Override
    public byte[] readFile(Path path) throws IOException {
        return readResourceWithType(pathWithDefaultRoot(path), getContentType(path));
    }

    @Override
    public byte[] readFile(String path) throws IOException {
        return readFile(PathUtil.asPath(path));
    }

    @Override
    public boolean hasFile(Path path) {
        return hasResource(pathWithDefaultRoot(path));
    }

    @Override
    public byte[] readFileOnExecWorkspacePath(Path path, String project, String jobName, String execId) throws IOException {
        return readResourceWithType(pathWithJobWorkspaceRoot(path, project, jobName, execId),
                getContentType(path, project, jobName, execId));
    }

    @Override
    public boolean hasFileOnExecWorkpacePath(Path path, String project, String jobName, String execId){
        return hasResource(pathWithJobWorkspaceRoot(path,project, jobName, execId));
    }

    @Override
    public boolean hasDirectoryOnExecWorkpacePath(Path path, String project, String jobName, String execId){
        return hasDirectory(pathWithJobWorkspaceRoot(path, project, jobName, execId));
    }

    @Override
    public boolean hasFile(String path) {
        return hasFile(PathUtil.asPath(path));
    }

    @Override
    public Resource<ResourceMeta> createResource(Path path, ResourceMeta content) {
        return super.createResource(pathWithDefaultRoot(path), content);
    }

    @Override
    public Resource<ResourceMeta> createResource(
            Path path, ResourceMeta content, String project, String jobName, String execId) {
        return super.createResource(pathWithJobWorkspaceRoot(path, project, jobName, execId), content);
    }

    @Override
    public Resource<ResourceMeta> updateResource(Path path, ResourceMeta content) {
        return super.updateResource(pathWithDefaultRoot(path), content);
    }

    @Override
    public Resource<ResourceMeta> updateResource(Path path, ResourceMeta content, String project, String jobName, String execId) {
        return super.updateResource(pathWithJobWorkspaceRoot(path, project, jobName, execId), content);
    }

    @Override
    public String getContentType(Path path) throws IOException {
        Resource<ResourceMeta> resource = getResource(pathWithDefaultRoot(path));
        return resource.getContents().getContentType();
    }

    @Override
    public String getContentType(Path path, String project, String jobName, String execId) throws IOException {
        Resource<ResourceMeta> resource = getResource(pathWithJobWorkspaceRoot(path, project, jobName, execId));
        return resource.getContents().getContentType();
    }

    @Override
    public Path getJobFilesPath(String project, String jobName){
        return pathWithDefaultRoot(PathUtil.asPath(getFileWorkspacePath(project, jobName, null)));
    }

    private Path pathWithDefaultRoot(Path path){
        String fullPath = FILE_PATH_DEFAULT + path.getPath();
        return PathUtil.asPath(fullPath);
    }

    private Path pathWithJobWorkspaceRoot(Path path, String project, String jobName, String execId){
        String p = getFileWorkspacePath(project, jobName, execId)  + path.getPath();
        return pathWithDefaultRoot(PathUtil.asPath(p));
    }

    private String getFileWorkspacePath(String project, String jobName, String execId){
        return "/" + project + "/" + (null != jobName ? jobName + "/" : "") + (null != execId ? execId + "/" : "");
    }
}
