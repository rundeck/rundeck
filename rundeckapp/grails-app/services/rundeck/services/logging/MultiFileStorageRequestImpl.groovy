/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
