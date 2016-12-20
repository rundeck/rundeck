package rundeck.services.logging

import com.dtolabs.rundeck.core.logging.MultiFileStorageRequest
import com.dtolabs.rundeck.core.logging.MultiFileStorageRequestErrors
import com.dtolabs.rundeck.core.logging.StorageFile

/**
 * Created by greg on 11/12/15.
 */
class MultiFileStorageRequestImpl implements MultiFileStorageRequestErrors {
    Map<String, File> files
    Map<String, Boolean> completion = [:]
    Map<String, String> errors = [:]

    @Override
    void storageResultForFiletype(final String filetype, boolean success) {
        completion[filetype] = success
    }

    @Override
    void storageFailureForFiletype(final String filetype, final String message) {
        completion[filetype] = false
        errors[filetype] = message
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
