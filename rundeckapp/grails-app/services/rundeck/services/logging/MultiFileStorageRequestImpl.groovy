package rundeck.services.logging

import com.dtolabs.rundeck.core.logging.MultiFileStorageRequest
import com.dtolabs.rundeck.core.logging.StorageFile

/**
 * Created by greg on 11/12/15.
 */
class MultiFileStorageRequestImpl implements MultiFileStorageRequest {
    Map<String, File> files
    Map<String, Boolean> completion = [:]

    @Override
    void storageResultForFiletype(final String filetype, boolean success) {
        completion[filetype] = success
    }

    @Override
    Set<String> getAvailableFiletypes() {
        files.keySet()
    }

    @Override
    StorageFile getStorageFile(final String filetype) {
        if (!files[filetype]) {
            return null
        }
        new StorageFileImpl(filetype: filetype, file: files[filetype])
    }
}

class StorageFileImpl implements StorageFile {
    String filetype
    File file

    @Override
    InputStream getInputStream() {
        new FileInputStream(file)
    }

    @Override
    long getLength() {
        file.length()
    }

    @Override
    Date getLastModified() {
        new Date(file.lastModified())
    }
}
