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
