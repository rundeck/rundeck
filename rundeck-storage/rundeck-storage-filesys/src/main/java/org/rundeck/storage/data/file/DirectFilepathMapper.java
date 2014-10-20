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
        if (!contentDir.exists()) {
            if (!contentDir.mkdirs()) {
                throw new IllegalArgumentException("Unable to create root dir: " + rootDir);
            }
        }
        if (!metaDir.exists()) {
            if (!metaDir.mkdirs()) {
                throw new IllegalArgumentException("Unable to create meta dir: " + metaDir);
            }
        }
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
