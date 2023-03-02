package rundeck.data.storage;

import com.dtolabs.rundeck.core.authorization.AuthContext;
import com.dtolabs.rundeck.core.storage.keys.KeyStorageTree;

public interface AuthorizedKeyStorageTreeProvider {
    KeyStorageTree storageTreeWithContext(AuthContext ctx);
}
