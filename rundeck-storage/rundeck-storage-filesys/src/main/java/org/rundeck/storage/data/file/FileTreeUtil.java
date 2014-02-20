package org.rundeck.storage.data.file;

import org.rundeck.storage.api.ContentFactory;
import org.rundeck.storage.api.ContentMeta;

import java.io.File;

/**
 */
public class FileTreeUtil {
    public static <T extends ContentMeta> FileTree<T> forRoot(File root, ContentFactory<T> factory) {
        return new FileTree<T>(factory, new DirectFilepathMapper(root), new JsonMetadataMapper());
    }
}
