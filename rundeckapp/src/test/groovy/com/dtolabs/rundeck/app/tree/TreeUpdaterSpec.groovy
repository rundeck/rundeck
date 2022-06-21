package com.dtolabs.rundeck.app.tree

import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageTree
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.Resource
import spock.lang.Specification

class TreeUpdaterSpec extends Specification {
    def "performTreeUpdate"() {
        given:

            def content1 = Stub(ResourceMeta) {
                getMeta() >> [special: 'true']
            }
            def res2 = Stub(Resource) {
                isDirectory() >> false
                getPath() >> PathUtil.asPath('apath/sub1/res2')

                getContents() >> content1
            }
            StorageTree tree = Mock(StorageTree) {
                1 * hasDirectory('apath') >> true
                1 * hasDirectory('apath/sub1') >> true
                1 * listDirectory('apath') >> [
                    Stub(Resource) {
                        isDirectory() >> true
                        getPath() >> PathUtil.asPath('apath/sub1')
                    },
                    Stub(Resource) {
                        isDirectory() >> false
                        getPath() >> PathUtil.asPath('apath/res1')
                        getContents() >> Stub(ResourceMeta) {
                            getMeta() >> [special: 'false']
                        }
                    }
                ].toSet()


                1 * listDirectory('apath/sub1') >> [res2].toSet()
            }

            UpdaterConfig updaterConfig = Mock(UpdaterConfig)
            def sut = new TreeUpdater()

        when: "update performed"
            sut.updateTree(tree, 'apath', updaterConfig)
        then: "resources matching test should be updated"
            1 * updaterConfig.shouldPerform() >> true
            1 * updaterConfig.getUpdatedContents({ it.contents.meta.special == 'true' }) >> {
                it[0].contents
            }
            1 * updaterConfig.getUpdatedContents({ it.contents.meta.special == 'false' }) >> null
            1 * tree.updateResource({ it.toString() == 'apath/sub1/res2' }, content1)
            0 * tree.updateResource(*_)

    }
}
