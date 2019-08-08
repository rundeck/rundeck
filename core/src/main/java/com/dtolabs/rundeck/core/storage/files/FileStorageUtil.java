package com.dtolabs.rundeck.core.storage.files;

import com.dtolabs.rundeck.core.storage.StorageTree;

public class FileStorageUtil {

    public static FileStorageTree fileStorageWrapper(StorageTree tree) {
        return new FileStorageTreeImpl(tree);
    }
}
