package com.dtolabs.rundeck.core.storage;

import com.dtolabs.rundeck.core.authorization.AuthContext;

public interface AuthStorageManager {
    StorageTree storageTreeWrapper(AuthContext authContext);
}