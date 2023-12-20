package com.dtolabs.rundeck.core.logging

import spock.lang.Specification

class StorageFileTest extends Specification {
    def "storage file exists always true by default"() {
        given:
        StorageFile storageFileNoDefaultMethodImplemented = new StorageFileImplExmpl()

        when:
        Boolean storageFileExists = storageFileNoDefaultMethodImplemented.storageFileExists()

        then:
        storageFileExists == true
    }

    class StorageFileImplExmpl implements StorageFile{
        @Override
        String getFiletype() {
            return null
        }

        @Override
        InputStream getInputStream() {
            return null
        }

        @Override
        long getLength() {
            return 0
        }

        @Override
        Date getLastModified() {
            return null
        }

        @Override
        boolean isComplete() {
            return false
        }
    }
}
