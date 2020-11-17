package com.dtolabs.rundeck.core.storage;

import com.dtolabs.utils.Streams;
import lombok.Builder;
import lombok.Getter;
import org.rundeck.storage.api.PathUtil;
import org.rundeck.storage.api.Resource;
import org.rundeck.storage.data.DataUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Storage Manager implementation backed by StorageTree
 */
@Builder
public class TreeStorageManager
        implements StorageManager
{

    @Getter private final StorageTree storage;
    private final List<StorageManagerListener> listeners = Collections.synchronizedList(new ArrayList<>());


    @Override
    public void addListener(final StorageManagerListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(final StorageManagerListener listener) {
        listeners.remove(listener);
    }

    public boolean existsFileResource(String path) {
        String storagePath = cleanPath(path);
        return getStorage().hasResource(storagePath);
    }

    private String cleanPath(final String path) {
        return path.startsWith("/") ? path : ("/" + path);
    }

    public boolean existsDirResource(String path) {
        String storagePath = cleanPath(path);
        return getStorage().hasDirectory(storagePath);
    }

    /**
     * List the full paths of file resources in the directory at the given path
     *
     * @param path path directory path
     */
    public List<String> listDirPaths(String path) {
        return listDirPaths(path, null);
    }

    /**
     * List the full paths of file resources in the directory at the given path
     *
     * @param path    path directory path
     * @param pattern pattern match
     */
    public List<String> listDirPaths(String path, String pattern) {
        String storagePath = cleanPath(path);
        Collection<Resource<ResourceMeta>> resources =
                getStorage().hasDirectory(storagePath)
                ? getStorage().listDirectory(storagePath)
                : new ArrayList<>();
        String outprefix = path.endsWith("/") ? path : path + "/";
        Pattern pat = pattern != null ? Pattern.compile(pattern) : null;
        return resources.stream()
                        .map(
                                (res) -> res.getPath().getName() + (res.isDirectory() ? "/" : "")
                        )
                        .filter(
                                (pathName) -> pat == null || pat.matcher(pathName).matches()
                        )
                        .map(
                                (pathName) -> outprefix + pathName
                        )
                        .collect(Collectors.toList());
    }

    /**
     * Return config file contents
     *
     * @param path
     */
    public Resource<ResourceMeta> getFileResource(String path) {

        String storagePath = cleanPath(path);
        if (!getStorage().hasResource(storagePath)) {
            return null;
        }
        return getStorage().getResource(storagePath);
    }

    public long loadFileResource(String path, OutputStream output) throws IOException {

        String storagePath = cleanPath(path);
        Resource<ResourceMeta> resource = getStorage().getResource(storagePath);

        return Streams.copyStream(resource.getContents().getInputStream(), output);
    }

    /**
     * Update existing resource, fails if it does not exist
     *
     * @param path  path
     * @param input stream
     * @param meta  metadata
     * @return resource
     */
    public Resource<ResourceMeta> updateFileResource(String path, InputStream input, Map<String, String> meta) {

        String storagePath = cleanPath(path);
        Resource<ResourceMeta>
                res =
                getStorage().updateResource(storagePath, DataUtil.withStream(input, meta, StorageUtil.factory()));
        listeners.forEach((a) -> a.resourceModified(path));
        return res;
    }

    /**
     * Create new resource, fails if it exists
     *
     * @param path  path
     * @param input stream
     * @param meta  metadata
     * @return resource
     */
    public Resource<ResourceMeta> createFileResource(String path, InputStream input, Map<String, String> meta) {

        String storagePath = cleanPath(path);
        Resource<ResourceMeta>
                res =
                getStorage().createResource(storagePath, DataUtil.withStream(input, meta, StorageUtil.factory()));

        listeners.forEach((a) -> a.resourceCreated(path));
        return res;
    }

    /**
     * Write to a resource, create if it does not exist
     *
     * @param path  path
     * @param input stream
     * @param meta  metadata
     * @return resource
     */
    public Resource<ResourceMeta> writeFileResource(String path, InputStream input, Map<String, String> meta) {
        String storagePath = cleanPath(path);
        if (!getStorage().hasResource(storagePath)) {
            return createFileResource(path, input, meta);
        } else {
            return updateFileResource(path, input, meta);
        }
    }

    /**
     * delete a resource
     *
     * @param path path
     * @return true if file was deleted or does not exist
     */
    public boolean deleteFileResource(String path) {
        String storagePath = cleanPath(path);
        if (!getStorage().hasResource(storagePath)) {
            return true;
        } else {
            boolean result = getStorage().deleteResource(storagePath);
            if (result) {
                listeners.forEach((a) -> a.resourceDeleted(path));
            }
            return result;
        }
    }

    /**
     * Recursively delete all files from the root path
     *
     * @param root root path
     * @return true if successful
     */
    public boolean deleteAllFileResources(String root) {
        return StorageUtil.deletePathRecursive(getStorage(), PathUtil.asPath(root));
    }

    public static TreeStorageManager createFromStorageTree(StorageTree storageTree){
        return TreeStorageManager.builder().storage(storageTree).build();
    }
}
