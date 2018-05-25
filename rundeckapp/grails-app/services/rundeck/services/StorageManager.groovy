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

package rundeck.services
import com.dtolabs.rundeck.core.storage.ResourceMeta
import org.rundeck.storage.api.Resource
/**
 * Interface for managing a storage backend
 */
interface StorageManager {

    /**
     * Receive notification of changes
     * @param listener
     */
    void addListener(StorageManagerListener listener)

    /**
     * Remove a listener
     * @param listener
     */
    void removeListener(StorageManagerListener listener)

    /**
     * @param path
     * @return true if a file exists at the path
     */
    boolean existsFileResource(String path)

    /**
     * @param path
     * @return true if a dir exists at the path
     */
    boolean existsDirResource(String path)
    /**
     * List the full paths of all resources in the directory at the given path
     * @param path path directory path
     * @param pattern pattern match
     * @return
     */
    List<String> listDirPaths(String path, String pattern);

    /**
     * List the full paths of all resources in the directory at the given path
     * @param path path directory path
     * @return
     */
    List<String> listDirPaths(String path);

    /**
     * Return config file contents
     * @param path
     * @return
     */
    Resource<ResourceMeta> getFileResource(String path);

    /**
     * Read the contents of a file at the path into the outputstream
     * @param path
     * @param output
     * @return number of bytes copied
     */
    long loadFileResource(String path, OutputStream output);

    /**
     * Update contents of an existing file
     * @param path path
     * @param input data
     * @param meta meta
     * @return resource
     */
    Resource<ResourceMeta> updateFileResource(String path, InputStream input, Map<String, String> meta)

    /**
     * Create new resource, fails if it exists
     * @param path path
     * @param input stream
     * @param meta metadata
     * @return resource
     */
    Resource<ResourceMeta> createFileResource(String path, InputStream input, Map<String, String> meta)

    /**
     * Write to a resource, create if it does not exist
     * @param path path
     * @param input stream
     * @param meta metadata
     * @return resource
     */
    Resource<ResourceMeta> writeFileResource(String path, InputStream input, Map<String, String> meta)


    /**
     * delete a resource
     * @param path path
     * @return true if file was deleted or does not exist
     */
    boolean deleteFileResource(String path)

    /**
     * Recursively delete all files from the root path
     * @param root root path
     * @return true if successful
     */
    boolean deleteAllFileResources(String root)
}
