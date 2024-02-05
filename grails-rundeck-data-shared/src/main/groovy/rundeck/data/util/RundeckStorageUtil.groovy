package rundeck.data.util

import org.rundeck.app.data.model.v1.storage.RundeckStorage
import org.rundeck.storage.api.Path
import org.rundeck.storage.api.PathUtil

class RundeckStorageUtil {
    static Path getPath(RundeckStorage storage) {
        return PathUtil.asPath((storage.dir?(storage.dir+'/'):'')+storage.name)
    }
}
