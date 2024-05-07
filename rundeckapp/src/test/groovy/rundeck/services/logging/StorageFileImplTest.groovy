package rundeck.services.logging

import com.dtolabs.rundeck.core.logging.StorageFile
import spock.lang.Specification

class StorageFileImplTest extends Specification {
    def "should return false if storage file is not present in FS"() {
        given:
        StorageFile storageFile = new StorageFileImpl()
        storageFile.file = Mock(File){
            exists() >> fileExistsInFs
        }

        when:
        Boolean storageFileExists = storageFile.storageFileExists()

        then:
        storageFileExists == fileExistsInFs

        where:
        fileExistsInFs | _
                  true | _
                 false | _
    }
}
