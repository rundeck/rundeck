package org.rundeck.tests.functional.selenium

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.setup.BaseSpec
import org.rundeck.util.setup.StorageKeyType

@SeleniumCoreTest
class KeyStorageSpec extends BaseSpec {

    def "create and delete key storage"() {
        when:
            doLogin()
        then:
            addKeyStorage(StorageKeyType.PASSWORD, 'root', 'git', 'git.pass')
        cleanup:
            deleteKeyStorage('git', 'git.pass')
    }

    def "overwrite and delete key storage"() {
        when:
            doLogin()
        then:
            addKeyStorage(StorageKeyType.PASSWORD, 'root', 'git', 'git.pass')
            overwriteKeyStorage('new-root', 'git', 'git.pass')
        cleanup:
            deleteKeyStorage('git', 'git.pass')
    }

}
