package org.rundeck.app.execution;

import com.dtolabs.rundeck.core.authorization.AuthContext;

public interface StorageAccessChecks {
    boolean canReadStoragePassword(AuthContext authContext, String storagePath, boolean failIfMissing);
}
