package org.rundeck.tests.functional.selenium

import org.rundeck.util.annotations.SeleniumCoreTest;
import org.rundeck.util.setup.BaseTest
import org.rundeck.util.setup.StorageKeyType;

@SeleniumCoreTest
class KeyStorageSpec extends BaseTest {

    def "create key storage"() {
        then:
        doLogin('admin', 'admin')
        addKeyStorage(StorageKeyType.PASSWORD, 'root', 'git', 'git.pass')
    }

    def "overwrite key storage"() {
        then:
        doLogin('admin', 'admin')
        addKeyStorage(StorageKeyType.PASSWORD, 'root', 'git', 'git.pass')
        overwriteKeyStorage('new-root', 'git', 'git.pass')
    }

    def "delete key storage"() {
        then:
        doLogin('admin', 'admin')
        addKeyStorage(StorageKeyType.PASSWORD, 'root', 'git', 'git.pass')
        deleteKeyStorage('git', 'git.pass')
    }

}
