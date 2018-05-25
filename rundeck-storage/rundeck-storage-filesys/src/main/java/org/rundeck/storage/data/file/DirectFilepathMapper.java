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

package org.rundeck.storage.data.file;

import org.rundeck.storage.api.Path;

import java.io.File;

/**
 * Maps the content file of a resource into a root content directory, and metadat files into a _meta subdirectory at the
 * top level.
 */
public class DirectFilepathMapper implements FilepathMapper {
    private File rootDir;
    private File contentDir;
    private File metaDir;

    public DirectFilepathMapper(File rootDir) {
        this.rootDir = rootDir;
        this.contentDir = new File(rootDir, "content");
        this.metaDir = new File(rootDir, "meta");
        contentDir.mkdirs();
        metaDir.mkdirs();
    }

    /**
     * file for a given path in the specified subdir
     *
     * @param path path
     * @param dir  dir
     *
     * @return file
     */
    private File withPath(Path path, File dir) {
        return new File(dir, path.getPath());
    }

    @Override
    public File contentFileForPath(Path path) {
        return withPath(path, contentDir);
    }

    @Override
    public File metadataFileFor(Path path) {
        return withPath(path, metaDir);
    }

    @Override
    public File directoryForPath(Path path) {
        return withPath(path, contentDir);
    }

    @Override
    public Path pathForContentFile(File datafile) {
        return FileTreeUtil.pathForFileInRoot(contentDir, datafile);
    }

    @Override
    public Path pathForMetadataFile(File metafile) {
        return FileTreeUtil.pathForFileInRoot(metaDir, metafile);
    }

    @Override
    public Path pathForDirectory(File directory) {
        return FileTreeUtil.pathForFileInRoot(contentDir, directory);
    }
}
