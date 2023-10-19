package org.rundeck.tests.functional.selenium

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.setup.BaseTest
import org.rundeck.util.setup.StorageKeyType

@SeleniumCoreTest
class KeyStorageSpec extends BaseTest {

    def "create and delete key storage"() {
        when:
        doLogin('admin', 'admin123')
        then:
        addKeyStorage(StorageKeyType.PASSWORD, 'root', 'git', 'git.pass')
        deleteKeyStorage('git', 'git.pass')
    }

    def "overwrite and delete key storage"() {
        when:
        doLogin('admin', 'admin123')
        then:
        addKeyStorage(StorageKeyType.PASSWORD, 'root', 'git', 'git.pass')
        overwriteKeyStorage('new-root', 'git', 'git.pass')
        deleteKeyStorage('git', 'git.pass')
    }

}
