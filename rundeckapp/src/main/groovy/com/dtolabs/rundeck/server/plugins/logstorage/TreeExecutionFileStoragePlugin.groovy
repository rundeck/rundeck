/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.server.plugins.logstorage

import com.dtolabs.rundeck.core.logging.ExecutionFileStorageException
import com.dtolabs.rundeck.core.logging.ExecutionFileStorageOptions
import com.dtolabs.rundeck.core.logging.ExecutionMultiFileStorage
import com.dtolabs.rundeck.core.logging.MultiFileStorageRequest
import com.dtolabs.rundeck.core.logging.StorageFile
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageTree
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.logging.ExecutionFileStoragePlugin
import groovy.transform.ToString
import org.rundeck.storage.api.Path
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.Resource
import org.rundeck.storage.data.DataUtil

/**
 * @author greg
 * @since 2/14/17
 */

@Plugin(name = TreeExecutionFileStoragePlugin.PROVIDER_NAME, service = ServiceNameConstants.ExecutionFileStorage)
@PluginDescription(title = 'Tree Storage',
        description = 'Uses the configured Tree Storage backend for log file storage.')
@ToString(includeNames = true)
class TreeExecutionFileStoragePlugin
        implements ExecutionFileStoragePlugin, ExecutionMultiFileStorage, ExecutionFileStorageOptions {
    static final String PROVIDER_NAME = 'storage-tree'

    @PluginProperty(title = 'Base Path', description = 'Top-level root path for storage', defaultValue = '/logs')
    String basePath = '/logs';

    StorageTree rundeckStorageTree
    boolean retrieveSupported = true
    boolean storeSupported = true
    private Path baseStoragePath
    private Map<String, ?> context

    @Override
    void initialize(final Map<String, ?> context) {
        this.context = context
        if (null == basePath) {
            throw new IllegalStateException("basePath cannot be null")
        }
        def root = PathUtil.asPath(basePath)
        if (PathUtil.isRoot(root)) {
            throw new IllegalStateException("basePath cannot be /")
        }
        if (!context.project) {
            throw new IllegalStateException("init context does not contain 'project'")
        }
        if (!context.execid) {
            throw new IllegalStateException("init context does not contain 'execid'")
        }
        def build = "project/${context.project}/"
        if (context.id) {
            build += "job/${context.id}/"
        } else {
            build += "run/"
        }
        build += "exec/${context.execid}"

        baseStoragePath = PathUtil.appendPath(root, build)
    }

    @Override
    boolean isAvailable(final String filetype) throws ExecutionFileStorageException {
        Path path = createFilePath(filetype)
        def hasResource = rundeckStorageTree.hasResource(path)
        return hasResource
    }

    private Path createFilePath(String filetype) {
        PathUtil.appendPath(baseStoragePath, filetype)
    }
    static final Map<String, String> CONTENT_TYPES = [
            rdlog          : 'text/plain',
            'state.json'   : 'application/json',
            'execution.xml': 'application/xml'
    ]

    static Map<String, String> createMetadata(String type, long length, Date lastModified) {
        /*
        [loglevel:INFO, wasRetry:false, url:http://ecto1.local:4440/project/ctest/execution/follow/1701,
        id:cb1f39af-5d15-4d13-bcfe-b3e963c9b21e, project:ctest, username:admin, retryAttempt:0, user.name:admin,
        name:schedule test, executionType:scheduled, serverUUID:3425B691-7319-4EEE-8425-F053C628B4BA, group:null,
        execid:1701, serverUrl:http://ecto1.local:4440/]
         */
        String contentType = CONTENT_TYPES[type] ?: 'application/octet-stream'
        [
                (StorageUtil.RES_META_RUNDECK_CONTENT_TYPE)  : contentType,
                'Rundeck-execution-file-type'                : type,
                'Rundeck-execution-file-date-modified'       : StorageUtil.formatDate(lastModified),
                (StorageUtil.RES_META_RUNDECK_CONTENT_LENGTH): Long.toString(length),
        ]
    }

    @Override
    boolean store(final String filetype, final InputStream stream, final long length, final Date lastModified)
            throws IOException, ExecutionFileStorageException
    {
        def path = createFilePath(filetype)
        if (isAvailable(filetype)) {
            rundeckStorageTree.updateResource(
                    path,
                    DataUtil.withStream(stream, createMetadata(filetype, length, lastModified), StorageUtil.factory())
            )
        } else {
            rundeckStorageTree.createResource(
                    path,
                    DataUtil.withStream(stream, createMetadata(filetype, length, lastModified), StorageUtil.factory())
            )
        }
        return true
    }


    @Override
    boolean retrieve(final String filetype, final OutputStream stream)
            throws IOException, ExecutionFileStorageException
    {
        def path = createFilePath(filetype)
        if (!isAvailable(filetype)) {
            throw new ExecutionFileStorageException("Not available: $path")
        }
        Resource<ResourceMeta> resource = rundeckStorageTree.getResource(path)
        resource.contents.writeContent(stream)
        return true
    }


    @Override
    void storeMultiple(final MultiFileStorageRequest files) throws IOException, ExecutionFileStorageException {
        files.availableFiletypes.each { String filetype ->
            StorageFile file = files.getStorageFile(filetype)
            store(filetype, file.inputStream, file.length, file.lastModified)
            files.storageResultForFiletype(filetype, true)
        }
    }
}
