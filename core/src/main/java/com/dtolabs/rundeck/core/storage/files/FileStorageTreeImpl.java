package com.dtolabs.rundeck.core.storage.files;

import com.dtolabs.rundeck.core.storage.ResourceMeta;
import com.dtolabs.rundeck.core.storage.TypedStorageTreeImpl;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.PathUtil;
import org.rundeck.storage.api.Resource;
import org.rundeck.storage.api.Tree;

import java.io.IOException;

import static com.dtolabs.rundeck.core.storage.FileStorageLayer.FILE_MIME_TYPE;

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

    public Resource<ResourceMeta> getFIle(String path) {
        return getFIle(PathUtil.asPath(path));
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
    public boolean hasFile(String path) {
        return hasFile(PathUtil.asPath(path));
    }

    @Override
    public Resource<ResourceMeta> createResource(Path path, ResourceMeta content) {
        return super.createResource(pathWithDefaultRoot(path), content);
    }

    @Override
    public Resource<ResourceMeta> updateResource(Path path, ResourceMeta content) {
        return super.updateResource(pathWithDefaultRoot(path), content);
    }

    @Override
    public String getContentType(Path path) throws IOException {
        Resource<ResourceMeta> resource = getResource(pathWithDefaultRoot(path));
        return resource.getContents().getContentType();
    }

    private Path pathWithDefaultRoot(Path path){
        String fullPath = FILE_PATH_DEFAULT + path.getPath();
        return PathUtil.asPath(fullPath);
    }
}
