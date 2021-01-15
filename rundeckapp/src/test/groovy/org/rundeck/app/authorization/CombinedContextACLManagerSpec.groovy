package org.rundeck.app.authorization

import com.dtolabs.rundeck.core.authorization.providers.BaseValidator
import com.dtolabs.rundeck.core.authorization.providers.ValidatorFactory
import com.dtolabs.rundeck.core.common.FrameworkProjectMgr
import com.dtolabs.rundeck.core.utils.FileUtils
import org.rundeck.app.acl.ACLFileManager
import org.rundeck.app.acl.AppACLContext
import org.rundeck.app.acl.ContextACLManager
import spock.lang.Specification

import java.nio.file.Files

class CombinedContextACLManagerSpec extends Specification {
    def "create system"() {
        given:
            def sysManager = Mock(ACLFileManager)
            def systemACLManager = Mock(ContextACLManager) {
                forContext(AppACLContext.system()) >> sysManager
            }
            def sut = new CombinedContextACLManager(
                systemACLManager: systemACLManager
            )
        when:
            def result = sut.createManager(AppACLContext.system())
        then:
            result == sysManager
    }

    def "create for project empty dir"() {
        given:
            File basedir = Files.createTempDirectory("test").toFile()
            def systemACLManager = Mock(ContextACLManager)
            FrameworkProjectMgr projectManager = Mock(FrameworkProjectMgr) {
                getBaseDir() >> basedir
            }
            File projDir = new File(basedir, 'aproject')
            File aclsDir = new File(projDir, 'acls')
            ValidatorFactory validatorFactory = Mock(ValidatorFactory)
            def sut = new CombinedContextACLManager(
                systemACLManager: systemACLManager,
                projectManager: projectManager,
                validatorFactory: validatorFactory
            )
        when:
            def result = sut.createManager(AppACLContext.project('aproject'))
        then:
            1 * validatorFactory.forProjectOnly('aproject')
        when:
            def list = result.listStoredPolicyFiles()
        then:
            list == []
            aclsDir.isDirectory()
        cleanup:
            FileUtils.deleteDir(basedir)
    }

    def "create for project existing dir"() {
        given:
            File basedir = Files.createTempDirectory("test").toFile()
            def systemACLManager = Mock(ContextACLManager)
            FrameworkProjectMgr projectManager = Mock(FrameworkProjectMgr) {
                getBaseDir() >> basedir
            }
            File projDir = new File(basedir, 'aproject')
            File aclsDir = new File(projDir, 'acls')
            aclsDir.mkdirs()
            File temp = new File(aclsDir, "test.aclpolicy")
            temp << 'acldata'
            File temp2 = new File(aclsDir, "test.wrong")
            temp2 << 'xxx'
            ValidatorFactory validatorFactory = Mock(ValidatorFactory)
            def sut = new CombinedContextACLManager(
                systemACLManager: systemACLManager,
                projectManager: projectManager,
                validatorFactory: validatorFactory
            )
            def result = sut.createManager(AppACLContext.project('aproject'))
        when:
            def list = result.listStoredPolicyFiles()
        then:
            list == ['test.aclpolicy']
        cleanup:
            FileUtils.deleteDir(basedir)
    }

    def "create for project validate"() {
        given:
            File basedir = Files.createTempDirectory("test").toFile()
            def systemACLManager = Mock(ContextACLManager)
            FrameworkProjectMgr projectManager = Mock(FrameworkProjectMgr) {
                getBaseDir() >> basedir
            }
            File projDir = new File(basedir, 'aproject')
            File aclsDir = new File(projDir, 'acls')
            aclsDir.mkdirs()
            File temp = new File(aclsDir, "test.aclpolicy")
            temp << 'acldata'
            File temp2 = new File(aclsDir, "test.wrong")
            temp2 << 'xxx'
            def validator = Mock(BaseValidator)
            ValidatorFactory validatorFactory = Mock(ValidatorFactory) {
                1 * forProjectOnly('aproject') >> validator
            }
            def sut = new CombinedContextACLManager(
                systemACLManager: systemACLManager,
                projectManager: projectManager,
                validatorFactory: validatorFactory
            )
            def manager = sut.createManager(AppACLContext.project('aproject'))
        when:
            def result = manager.validatePolicyFile('test.aclpolicy')
        then:
            1 * validator.validateYamlPolicy('test.aclpolicy', 'acldata')

        cleanup:
            FileUtils.deleteDir(basedir)
    }
}
