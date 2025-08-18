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
package org.rundeck.plugin.objectstore.directorysource

import com.dtolabs.rundeck.core.storage.BaseStreamResource
import groovy.transform.CompileStatic
import io.minio.BucketExistsArgs
import io.minio.ListObjectsArgs
import io.minio.MinioClient
import io.minio.StatObjectArgs
import org.rundeck.plugin.objectstore.stream.LazyAccessObjectStoreInputStream
import org.rundeck.plugin.objectstore.tree.ObjectStoreResource
import org.rundeck.plugin.objectstore.tree.ObjectStoreUtils
import org.rundeck.storage.api.Resource

/*
* Stores the object store directory structure in memory. Keeps the directory as updates are made
* through the client.
*
* If the object store is modified by a third part client this directory store will have to be resynced.
* Currently the way to resync the directory structure is to restart Rundeck.
*
* This source works best when using a single instance of Rundeck, where all updates to the object store
* will be done through the object tree.
 */

@CompileStatic
class ObjectStoreMemoryDirectorySource implements ObjectStoreDirectorySource {
    private static final String DIR_MARKER = "/"
    DirectoryNode root = new DirectoryNode("")
    private final String bucket
    private final MinioClient mClient

    ObjectStoreMemoryDirectorySource(MinioClient mClient, String bucket) {
        this.mClient = mClient
        this.bucket = bucket
        init()
    }

    private init() {
        try {
            BucketExistsArgs args = BucketExistsArgs.builder()
                    .bucket(bucket)
                    .build();
            if (mClient.bucketExists(args)) {
                resyncDirectory()
            }
        } catch(SocketTimeoutException stex) {
            throw new RuntimeException("Unable to connect to the server. Please check your firewall, or make sure your server is accepting connections.")
        }
    }

    @Override
    boolean checkPathExists(final String path) {
        List<String> parts = new ArrayList<>(Arrays.asList(path.split(DIR_MARKER)))
        String resourceName = parts.removeLast()
        DirectoryNode dir = getDir(parts)
        if(!dir) return false
        return dir.getSubdir(resourceName) || dir.getEntry(resourceName)
    }

    @Override
    boolean checkResourceExists(final String path) {
        List<String> parts = new ArrayList<>(Arrays.asList(path.split(DIR_MARKER)))
        String resourceName = parts.removeLast()
        DirectoryNode dir = getDir(parts)
        if(!dir) return false
        return dir.hasEntry(resourceName)
    }

    @Override
    boolean checkPathExistsAndIsDirectory(final String path) {
        List<String> parts = new ArrayList<>(Arrays.asList(path.split(DIR_MARKER)))
        return getDir(parts) != null
    }

    @Override
    Map<String, String> getEntryMetadata(final String path) {
        List<String> parts = new ArrayList<>(Arrays.asList(path.split(DIR_MARKER)))
        String resourceName = parts.removeLast()
        DirectoryNode dir = getDir(parts)
        return dir.getEntry(resourceName).meta
    }

    @Override
    Set<Resource<BaseStreamResource>> listSubDirectoriesAt(final String path) {
        List<String> parts = new ArrayList<>(Arrays.asList(path.split(DIR_MARKER)))
        DirectoryNode dir = getDir(parts)
        if(!dir) return Collections.emptySet()
        def list = dir.listSubDirs().collect { entry -> new ObjectStoreResource(path+ "/"+ entry, null, true) }
        return list.toSet() as Set<Resource<BaseStreamResource>>
    }

    @Override
    Set<Resource<BaseStreamResource>> listEntriesAndSubDirectoriesAt(final String path) {
        List<String> parts = new ArrayList<>(Arrays.asList(path.split(DIR_MARKER)))
        DirectoryNode dir = getDir(parts)
        if(!dir) return Collections.emptySet()
        def resources = new HashSet<Resource<BaseStreamResource>>()
        dir.listEntries().each { entry ->
            resources.add(new ObjectStoreResource(path+ "/"+ entry.nodeName, new BaseStreamResource(entry.meta, new LazyAccessObjectStoreInputStream(mClient, bucket, path+ "/"+ entry.nodeName))))
        }
        dir.listSubDirs().each { entry ->
            resources.add(new ObjectStoreResource(path+ "/"+ entry, null, true))
        }
        return resources
    }

    @Override
    Set<Resource<BaseStreamResource>> listResourceEntriesAt(final String path) {
        List<String> parts = new ArrayList<>(Arrays.asList(path.split(DIR_MARKER)))
        DirectoryNode dir = getDir(parts)
        if(!dir) return Collections.emptySet()
        def list = dir.listEntries().collect { entry ->
            new ObjectStoreResource(path+ "/"+ entry.nodeName, new BaseStreamResource(entry.meta, new LazyAccessObjectStoreInputStream(mClient, bucket, path+ "/"+ entry.nodeName)))
        }
        return list.toSet() as Set<Resource<BaseStreamResource>>
    }

    @Override
    void updateEntry(final String fullPathToEntry, final Map<String, String> meta) {
        List<String> parts =  new ArrayList<>(Arrays.asList(fullPathToEntry.split(DIR_MARKER)))
        String resourceName = parts.removeLast()
        DirectoryNode dir = root
        for(int i = 0; i < parts.size(); i++) {
            if(!dir.hasSubdir(parts[i])) {
                dir.addSubdir(new DirectoryNode(parts[i]))
            }
            dir = dir.getSubdir(parts[i])
        }
        dir.setEntry(new EntryNode(resourceName, meta))
    }

    @Override
    void deleteEntry(final String fullEntryPath) {
        List<String> parts = new ArrayList<>(Arrays.asList(fullEntryPath.split(DIR_MARKER)))
        String resourceName = parts.removeLast()
        DirectoryNode dir = getDir(parts)
        dir.removeEntry(resourceName)
        if(dir.isEmpty()) {
            //remove this dir
            List<String> patDirParts = new ArrayList<>(Arrays.asList(fullEntryPath.split(DIR_MARKER)))
            patDirParts.removeLast()
            if (!patDirParts.isEmpty()) {
                patDirParts.removeLast()
            }
            DirectoryNode parent = getDir(patDirParts)
            parent.removeSubdir(dir.dirName)
        }
    }

    @Override
    void resyncDirectory() {
        root = new DirectoryNode("")
        ListObjectsArgs args = ListObjectsArgs.builder()
                .bucket(bucket)
                .recursive(true)
                .build()
        mClient.listObjects(args).each {
            StatObjectArgs statArgs = StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(it.get().objectName())
                    .build()
            def meta = ObjectStoreUtils.objectStatToMap(mClient.statObject(statArgs))
            updateEntry(it.get().objectName(),meta)
        }
    }

    private DirectoryNode getDir(List<String> pathParts) {
        if(pathParts.size() == 1 && pathParts[0].isEmpty()) return root
        DirectoryNode dir = root
        for(int i = 0; i < pathParts.size(); i++) {
            if(!dir.hasSubdir(pathParts[i])) return null
            dir = dir.getSubdir(pathParts[i])
        }
        return dir
    }

    private DirectoryNode findDir(DirectoryNode dir, List<String> pathParts) {
        String path = pathParts.remove(0)
        if(dir.subdirs.containsKey(path)) {
            dir = dir.subdirs[path]
            if (!pathParts.isEmpty()) {
                findDir(dir, pathParts)
            }
        }
    }

    private class EntryNode {
        String nodeName
        Map<String,String> meta = [:]

        EntryNode(String nodeName, Map<String,String> meta) {
            this.nodeName = nodeName
            this.meta = meta
        }
    }

    private class DirectoryNode {
        String dirName
        private Map<String,EntryNode> children = new TreeMap<>()
        private Map<String,DirectoryNode> subdirs = new TreeMap<>()

        DirectoryNode(String dirName) {
            this.dirName = dirName
        }

        EntryNode getEntry(String entryName) {
            return children[entryName]
        }

        void setEntry(EntryNode entry) {
            children[entry.nodeName] = entry
        }

        void addSubdir(DirectoryNode dir) {
            subdirs[dir.dirName] = dir
        }

        boolean hasEntry(final String entryName) {
            return children.containsKey(entryName)
        }

        boolean hasSubdir(final String subdirName) {
            subdirs.containsKey(subdirName)
        }

        boolean isEmpty() {
            return children.isEmpty() && subdirs.isEmpty()
        }

        DirectoryNode getSubdir(final String subdirName) {
            subdirs[subdirName]
        }

        Set<String> listSubDirs() {
            subdirs.keySet()
        }

        Set<String> listEntryNames() {
            children.keySet()
        }

        List<EntryNode> listEntries() {
            children.values().asList()
        }

        void removeEntry(final String resourceName) {
            children.remove(resourceName)
        }

        void removeSubdir(final String subdirName) {
            subdirs.remove(subdirName)
        }
    }
}
