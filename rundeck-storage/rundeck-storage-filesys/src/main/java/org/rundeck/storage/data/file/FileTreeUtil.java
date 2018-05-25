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

import org.rundeck.storage.api.ContentFactory;
import org.rundeck.storage.api.ContentMeta;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.PathUtil;

import java.io.File;
import java.util.regex.Pattern;

/**
 * Utility to create {@link FileTree} and manage paths.
 */
public class FileTreeUtil {
    public static <T extends ContentMeta> FileTree<T> forRoot(File root,
            ContentFactory<T> factory) {
        return new FileTree<T>(factory, new DirectFilepathMapper(root), new JsonMetadataMapper());
    }

    /**
     * Return a storage Path given a file within a given root dir
     *
     * @param rootDir root dir to use
     * @param file    file
     *
     * @return sub path corresponding to the file
     *
     * @throws IllegalArgumentException if the file is not within the root
     */
    public static Path pathForFileInRoot(
            final File rootDir,
            final File file
    )
    {
        String filePath = file.getAbsolutePath();
        String rootPath = rootDir.getAbsolutePath();
        if (!filePath.startsWith(rootPath)) {
            throw new IllegalArgumentException("not a file in the root directory: " + file);
        }
        return pathForRelativeFilepath(filePath.substring(rootPath.length()));
    }

    /**
     * Return a storage path given a relative file path string, using the native file path
     * separator.
     *
     * @param filepath file path with native file separator
     *
     * @return storage path
     */
    public static Path pathForRelativeFilepath(
            final String filepath
    )
    {
        return pathForRelativeFilepath(filepath, File.separator);
    }

    /**
     * Return a storage path given a relative file path string
     *
     * @param filepath  file path with given separator
     * @param separator separator string
     *
     * @return storage path
     */
    public static Path pathForRelativeFilepath(
            final String filepath,
            final String separator
    )
    {
        String[] comps = filepath.split(Pattern.quote(separator));
        return PathUtil.pathFromComponents(comps);
    }
}
