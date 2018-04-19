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

package rundeck.services

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.plugins.CloseableProvider
import com.dtolabs.rundeck.core.plugins.Closeables
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.plugins.jobs.JobChangeListener
import com.dtolabs.rundeck.plugins.scm.ScmCommitInfo
import com.dtolabs.rundeck.plugins.scm.ScmExportPlugin
import com.dtolabs.rundeck.plugins.scm.ScmExportPluginFactory
import com.dtolabs.rundeck.plugins.scm.ScmExportResult
import com.dtolabs.rundeck.plugins.scm.ScmExportSynchState
import com.dtolabs.rundeck.plugins.scm.ScmImportPlugin
import com.dtolabs.rundeck.plugins.scm.ScmImportPluginFactory
import com.dtolabs.rundeck.plugins.scm.ScmOperationContext
import com.dtolabs.rundeck.plugins.scm.ScmPluginInvalidInput
import com.dtolabs.rundeck.server.plugins.ValidatedPlugin
import com.dtolabs.rundeck.server.plugins.services.ScmExportPluginProviderService
import com.dtolabs.rundeck.server.plugins.services.ScmImportPluginProviderService
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.ScheduledExecution
import rundeck.User
import rundeck.services.scm.ScmPluginConfigData
import spock.lang.Specification

/**
 * Created by greg on 10/15/15.
 */
@TestFor(ScmService)
@Mock([ScheduledExecution, User])
class ScmServiceSpec extends Specification {

    class TestCloseable implements Closeable {
        boolean closed

        @Override
        void close() throws IOException {
            closed = true
        }
    }
    def "disablePlugin"() {
        given:
        service.frameworkService = Mock(FrameworkService)
        service.pluginConfigService = Mock(PluginConfigService)
        service.jobEventsService = Mock(JobEventsService)
        ScmPluginConfigData config = Mock(ScmPluginConfigData)
        ScmExportPlugin exportPlugin = Mock(ScmExportPlugin)
        ScmImportPlugin importPlugin = Mock(ScmImportPlugin)
        TestCloseable exportCloser = new TestCloseable()
        TestCloseable importCloser = new TestCloseable()
        service.loadedExportPlugins['test1'] = Closeables.closeableProvider(exportPlugin, exportCloser)
        service.loadedImportPlugins['test1'] = Closeables.closeableProvider(importPlugin, importCloser)
        def dummyListener = Mock(JobChangeListener)
        service.loadedExportListeners['test1'] = dummyListener
        service.loadedImportListeners['test1'] = dummyListener
        service.renamedJobsCache['test1'] = [:]
        service.deletedJobsCache['test1'] = [:]

        when:
        service.disablePlugin(integration, 'test1', null)

        then:
        1 * service.pluginConfigService.loadScmConfig(
                'test1',
                "etc/scm-${integration}.properties",
                'scm.' + integration
        ) >> config
        1 * config.setEnabled(false)
        1 * service.pluginConfigService.storeConfig(config, 'test1', "etc/scm-${integration}.properties")
        1 * service.jobEventsService.removeListener(dummyListener)

        if (integration == ScmService.EXPORT) {
            exportCloser.closed
            !importCloser.closed
            service.loadedExportPlugins['test1'] == null
            service.loadedExportListeners['test1'] == null
            service.renamedJobsCache['test1'] == null
            service.deletedJobsCache['test1'] == null
            1 * exportPlugin.cleanup()
        } else {
            !exportCloser.closed
            importCloser.closed
            service.loadedImportPlugins['test1'] == null
            service.loadedImportListeners['test1'] == null
            1 * importPlugin.cleanup()
        }


        where:
        integration       | _
        ScmService.EXPORT | _
        ScmService.IMPORT | _
    }

    def "disablePlugin not enabled"() {
        given:
        service.frameworkService = Mock(FrameworkService)
        service.pluginConfigService = Mock(PluginConfigService)
        service.jobEventsService = Mock(JobEventsService)
        ScmExportPlugin exportPlugin = Mock(ScmExportPlugin)
        ScmImportPlugin importPlugin = Mock(ScmImportPlugin)
        TestCloseable exportCloser = new TestCloseable()
        TestCloseable importCloser = new TestCloseable()
        service.loadedExportPlugins['test1'] = Closeables.closeableProvider(exportPlugin, exportCloser)
        service.loadedImportPlugins['test1'] = Closeables.closeableProvider(importPlugin, importCloser)
        def dummyListener = Mock(JobChangeListener)
        service.loadedExportListeners['test1'] = dummyListener
        service.loadedImportListeners['test1'] = dummyListener
        service.renamedJobsCache['test1'] = [:]
        service.deletedJobsCache['test1'] = [:]

        when:
        service.disablePlugin(integration, 'test1', null)

        then:
        1 * service.pluginConfigService.loadScmConfig(
                'test1',
                "etc/scm-${integration}.properties",
                'scm.' + integration
        ) >> null
        1 * service.jobEventsService.removeListener(dummyListener)

        if (integration == ScmService.EXPORT) {
            service.loadedExportPlugins['test1'] == null
            service.loadedExportListeners['test1'] == null
            service.renamedJobsCache['test1'] == null
            service.deletedJobsCache['test1'] == null
            1 * exportPlugin.cleanup()
        } else {
            service.loadedImportPlugins['test1'] == null
            service.loadedImportListeners['test1'] == null
            1 * importPlugin.cleanup()
        }


        where:
        integration       | _
        ScmService.EXPORT | _
        ScmService.IMPORT | _
    }

    def "removePluginConfiguration"() {
        given:
        service.frameworkService = Mock(FrameworkService)
        service.pluginConfigService = Mock(PluginConfigService)
        service.jobEventsService = Mock(JobEventsService)
        ScmPluginConfigData config = Mock(ScmPluginConfigData)

        when:
        service.removePluginConfiguration(integration, 'test1', null)
        then:
        1 * service.pluginConfigService.loadScmConfig(
                'test1',
                "etc/scm-${integration}.properties",
                'scm.' + integration
        ) >> config
        1 * config.setEnabled(false)
        1 * service.pluginConfigService.storeConfig(config, 'test1', "etc/scm-${integration}.properties")
        1 * service.jobEventsService.removeListener(_)
        1 * service.pluginConfigService.removePluginConfiguration('test1', "etc/scm-${integration}.properties")

        where:
        integration       | _
        ScmService.EXPORT | _
        ScmService.IMPORT | _
    }

    def "removeAllPluginConfiguration"() {
        given:
        service.frameworkService = Mock(FrameworkService)
        service.pluginConfigService = Mock(PluginConfigService)
        service.jobEventsService = Mock(JobEventsService)
        ScmPluginConfigData config = Mock(ScmPluginConfigData)
        ScmPluginConfigData config2 = Mock(ScmPluginConfigData)

        when:
        service.removeAllPluginConfiguration('test1')
        then:
        service.pluginConfigService.loadScmConfig(
                'test1',
                "etc/scm-import.properties",
                'scm.import'
        ) >> config
        service.pluginConfigService.loadScmConfig(
                'test1',
                "etc/scm-export.properties",
                'scm.export'
        ) >> config2
        config.getType()>>'scm.import'
        config2.getType()>>'scm.export'
        1 * config.setEnabled(false)
        1 * config2.setEnabled(false)
        1 * service.pluginConfigService.storeConfig(config, 'test1', "etc/scm-import.properties")
        1 * service.pluginConfigService.storeConfig(config2, 'test1', "etc/scm-export.properties")
        2 * service.jobEventsService.removeListener(_)
        1 * service.pluginConfigService.removePluginConfiguration('test1', "etc/scm-import.properties")
        1 * service.pluginConfigService.removePluginConfiguration('test1', "etc/scm-export.properties")

    }

    def "validatePluginSetup"() {
        given:
        def config = [:]
        service.pluginService = Mock(PluginService)
        service.frameworkService = Mock(FrameworkService)
        def resolver = Mock(PropertyResolver)
        service.scmExportPluginProviderService = Mock(ScmExportPluginProviderService)
        service.scmImportPluginProviderService = Mock(ScmImportPluginProviderService)

        def validated = new ValidatedPlugin(valid: false)

        when:
        def result = service.validatePluginSetup(integration, 'test', 'type', config)


        then:

        1 * service.frameworkService.getFrameworkPropertyResolver('test', config) >> resolver
        1 * service.pluginService.validatePlugin(
                'type',
                integration == ScmService.EXPORT ? service.scmExportPluginProviderService :
                        service.scmImportPluginProviderService,
                resolver,
                PropertyScope.Instance,
                PropertyScope.Project
        ) >>
                validated
        result == validated

        where:
        integration       | _
        ScmService.EXPORT | _
        ScmService.IMPORT | _

    }

    def "init plugin invalid"() {
        given:
        def ctx = Mock(ScmOperationContext)
        def config = [:]
        service.pluginService = Mock(PluginService)
        service.frameworkService = Mock(FrameworkService)
        def resolver = Mock(PropertyResolver)


        def report = Validator.errorReport('a', 'b')
        def validated = new ValidatedPlugin(valid: false, report: report)
        when:
        def result = service.initPlugin(integration, ctx, 'atype', config)

        then:
        1 * service.frameworkService.getFrameworkPropertyResolver(*_)
        1 * service.pluginService.validatePlugin(*_) >> validated

        ScmPluginInvalidInput err = thrown()
        err.report == report


        where:
        integration       | _
        ScmService.EXPORT | _
        ScmService.IMPORT | _
    }

    def "init import plugin valid"() {
        given:
        def ctx = Mock(ScmOperationContext) {
            getFrameworkProject() >> 'testProject'
        }
        def config = [:]
        def configobj = Mock(ScmPluginConfigData)

        ScmImportPluginFactory importFactory = Mock(ScmImportPluginFactory)
        ScmImportPlugin plugin = Mock(ScmImportPlugin)

        service.pluginService = Mock(PluginService)
        service.pluginConfigService = Mock(PluginConfigService)
        service.jobEventsService = Mock(JobEventsService)
        service.frameworkService = Mock(FrameworkService)

        def testCloser = new TestCloseable()
        def validated = new ValidatedPlugin(valid: true)

        when:
        def result = service.initPlugin(integration, ctx, 'atype', config)

        then:
        1 * service.frameworkService.getFrameworkPropertyResolver(*_)
        1 * service.pluginService.validatePlugin(*_) >> validated
        1 * service.pluginService.retainPlugin('atype', _) >> Closeables.closeableProvider(importFactory, testCloser)
        1 * service.pluginConfigService.loadScmConfig(*_) >> configobj
        1 * configobj.getSettingList('trackedItems') >> ['a', 'b']
        1 * importFactory.createPlugin(ctx, config, ['a', 'b']) >> plugin
        1 * service.jobEventsService.addListenerForProject(_, 'testProject')

        result == plugin



        where:
        integration       | _
        ScmService.IMPORT | _
    }

    def "init export plugin valid"() {
        given:
        def ctx = Mock(ScmOperationContext) {
            getFrameworkProject() >> 'testProject'
        }
        def config = [:]

        ScmExportPluginFactory exportFactory = Mock(ScmExportPluginFactory)
        ScmExportPlugin plugin = Mock(ScmExportPlugin)

        service.pluginService = Mock(PluginService)
        service.pluginConfigService = Mock(PluginConfigService)
        service.jobEventsService = Mock(JobEventsService)
        service.frameworkService = Mock(FrameworkService)
        def exportCloser = new TestCloseable()

        def validated = new ValidatedPlugin(valid: true)

        when:
        def result = service.initPlugin(integration, ctx, 'atype', config)

        then:
        1 * service.frameworkService.getFrameworkPropertyResolver(*_)
        1 * service.pluginService.validatePlugin(*_) >> validated
        1 * service.pluginService.retainPlugin('atype', _) >> Closeables.closeableProvider(exportFactory, exportCloser)
        1 * exportFactory.createPlugin(ctx, config) >> plugin
        1 * service.jobEventsService.addListenerForProject(_, 'testProject')

        result == plugin



        where:
        integration       | _
        ScmService.EXPORT | _
    }


    def "export project status basic"() {
        given:
        def ctx = Mock(ScmOperationContext) {
            getFrameworkProject() >> 'testProject'
        }
        def config = [:]

        ScmExportPluginFactory exportFactory = Mock(ScmExportPluginFactory)
        ScmExportPlugin plugin = Mock(ScmExportPlugin)
        TestCloseable exportCloser = new TestCloseable()

        service.pluginService = Mock(PluginService)
        service.pluginConfigService = Mock(PluginConfigService)
        service.jobEventsService = Mock(JobEventsService)
        service.frameworkService = Mock(FrameworkService)
        service.storageService = Mock(StorageService)

        def validated = new ValidatedPlugin(valid: true)

        when:
        def result = service.exportPluginStatus(Mock(UserAndRolesAuthContext),'testProject')

        then:
        1 * service.pluginConfigService.loadScmConfig('testProject', 'etc/scm-export.properties', 'scm.export')>>Mock(ScmPluginConfigData){
            1 * getEnabled()>>true
            1 * getSetting('username') >> 'testuser'
            1 * getSettingList('roles') >> ['arole']
            getType()>>'atype'
            getConfig()>>config
        }
        service.frameworkService.getAuthContextForUserAndRolesAndProject(_, _, 'testProject') >> Mock(
                UserAndRolesAuthContext
        ) {
            getUsername()>>'testuser'
            getRoles()>>new HashSet<String>(['arole'])
        }
        1 * service.frameworkService.getFrameworkPropertyResolver(*_)
        1 * service.pluginService.validatePlugin(*_) >> validated
        1 * service.pluginService.retainPlugin('atype', _) >> Closeables.closeableProvider(exportFactory, exportCloser)
        1 * exportFactory.createPlugin(_, config) >> plugin
        1 * service.jobEventsService.addListenerForProject(_, 'testProject')
        1 * plugin.getStatus(_)>>Mock(ScmExportSynchState)

        result != null
    }


    def "export project status exception"() {
        given:
        def ctx = Mock(ScmOperationContext) {
            getFrameworkProject() >> 'testProject'
        }
        def config = [:]

        ScmExportPluginFactory exportFactory = Mock(ScmExportPluginFactory)
        ScmExportPlugin plugin = Mock(ScmExportPlugin)

        TestCloseable exportCloser = new TestCloseable()

        service.pluginService = Mock(PluginService)
        service.pluginConfigService = Mock(PluginConfigService)
        service.jobEventsService = Mock(JobEventsService)
        service.frameworkService = Mock(FrameworkService)
        service.storageService = Mock(StorageService)

        def validated = new ValidatedPlugin(valid: true)

        when:
        def result = service.exportPluginStatus(Mock(UserAndRolesAuthContext),'testProject')

        then:
        1 * service.pluginConfigService.loadScmConfig('testProject', 'etc/scm-export.properties', 'scm.export')>>Mock(ScmPluginConfigData){
            1 * getEnabled()>>true
            1 * getSetting('username') >> 'testuser'
            1 * getSettingList('roles') >> ['arole']
            getType()>>'atype'
            getConfig()>>config
        }
        service.frameworkService.getAuthContextForUserAndRolesAndProject(_, _, 'testProject') >> Mock(
                UserAndRolesAuthContext
        ) {
            getUsername()>>'testuser'
            getRoles()>>new HashSet<String>(['arole'])
        }
        1 * service.frameworkService.getFrameworkPropertyResolver(*_)
        1 * service.pluginService.validatePlugin(*_) >> validated
        1 * service.pluginService.retainPlugin('atype', _) >> Closeables.closeableProvider(exportFactory, exportCloser)
        1 * exportFactory.createPlugin(_, config) >> plugin
        1 * service.jobEventsService.addListenerForProject(_, 'testProject')
        1 * plugin.getStatus(_)>>{
            throw new RuntimeException("get status failed")
        }
        result == null
    }

    def "perform export plugin action should store commit metadata into job import metadata"() {
        given:
        service.jobMetadataService = Mock(JobMetadataService)
        service.storageService = Mock(StorageService)
        service.frameworkService = Mock(FrameworkService)

        ScmExportPlugin plugin = Mock(ScmExportPlugin)
        TestCloseable exportCloser = new TestCloseable()
        service.loadedExportPlugins['test1'] = Closeables.closeableProvider(plugin, exportCloser)
        service.initedProjects.add('test1')

        def input = [:]
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'bob'
        }
        def job = new ScheduledExecution()
        job.version = 1
        def bobuser = new User(login: 'bob').save()

        //returned by export result, should be stored in job metadata
        def commitMetadata = [commit: 'data']


        when:
        def result = service.performExportAction('actionId', auth, 'test1', input, [job], [])

        then:
        1 * plugin.getInputViewForAction(_, 'actionId') >> Mock(BasicInputView) {
            getProperties() >> []
        }
        1 * service.frameworkService.getFrameworkPropertyResolver('test1', input)
        1 * plugin.export(_, 'actionId', _, _, input) >> Mock(ScmExportResult) {
            isSuccess() >> true
            getCommit() >> Mock(ScmCommitInfo) {
                asMap() >> commitMetadata
                getCommitId() >> 'a-commit-id'
            }
            getId() >> 'a-commit-id'

        }

        //store metadata about commit
        1 * service.jobMetadataService.setJobPluginMeta(job, 'scm-import', [version: 1, pluginMeta: commitMetadata])

        result.valid
        result.commitId == 'a-commit-id'
    }

    def "initialize should read correct configs when initDeferred is false"() {
        given:
        service.pluginConfigService = Mock(PluginConfigService)
        service.frameworkService = Mock(FrameworkService)
        grailsApplication.config.rundeck.scm.startup.initDeferred=false
        when:
        service.initialize()
        then:
        1 * service.frameworkService.projectNames() >> ['projectA']
        1 * service.pluginConfigService.loadScmConfig('projectA', 'etc/scm-import.properties', 'scm.import')
        1 * service.pluginConfigService.loadScmConfig('projectA', 'etc/scm-export.properties', 'scm.export')
    }

    def "initialize loads import plugins if enabled"() {
        given:
        service.pluginConfigService = Mock(PluginConfigService)
        service.frameworkService = Mock(FrameworkService)
        service.storageService = Mock(StorageService)
        def config1 = Mock(ScmPluginConfigData) {
            1 * getEnabled() >> true
            1 * getSetting("username") >> 'bob'
            1 * getSettingList("roles") >> ['arole']
            _ * getType() >> 'pluginType'
            1 * getConfig() >> [plugin: 'config']
            1 * getSettingList('trackedItems') >> ['a', 'b']
        }
        def bobuser = new User(login: 'bob').save()

        //init plugin mocks

        ScmImportPluginFactory importFactory = Mock(ScmImportPluginFactory)
        ScmImportPlugin plugin = Mock(ScmImportPlugin)

        service.pluginService = Mock(PluginService)
        service.jobEventsService = Mock(JobEventsService)
        def validated = new ValidatedPlugin(valid: true)
        grailsApplication.config.rundeck.scm.startup.initDeferred=false

        when:
        service.initialize()
        then:
        1 * service.frameworkService.projectNames() >> ['projectA']
        2 * service.pluginConfigService.loadScmConfig('projectA', 'etc/scm-import.properties', 'scm.import') >> config1

        1 * service.frameworkService.getAuthContextForUserAndRolesAndProject('bob', ['arole'], 'projectA') >>
                Mock(UserAndRolesAuthContext) {
            getUsername() >> 'bob'
        }
        1 * service.frameworkService.getFrameworkPropertyResolver(*_)
        1 * service.pluginService.validatePlugin(*_) >> validated
        1 * service.pluginService.retainPlugin('pluginType', _) >> Closeables.closeableProvider(importFactory)
        1 * importFactory.createPlugin(_, [plugin: 'config'], ['a', 'b']) >> plugin
        1 * service.jobEventsService.addListenerForProject(_, 'projectA')

        1 * service.pluginConfigService.loadScmConfig('projectA', 'etc/scm-export.properties', 'scm.export') >> null
    }

    def "lookup user profile, no User found"() {
        when:
        def info = service.lookupUserInfo('bob')

        then:
        info.userName == 'bob'
        info.firstName == null
        info.lastName == null
        info.email == null
    }

    def "lookup user profile, with User found"() {
        given:
        new User(login: 'bob', firstName: 'a', lastName: 'b', email: 'test@test.com').save()

        when:
        def info = service.lookupUserInfo('bob')

        then:
        info.userName == 'bob'
        info.firstName == 'a'
        info.lastName == 'b'
        info.email == 'test@test.com'
    }


    def "lookup config on cluster mode"() {
        given:
        service.frameworkService = Mock(FrameworkService){
            getServerUUID() >> uuid
        }

        when:
        def result = service.pathForConfigFile(integration)

        then:
        1 * service.frameworkService.isClusterModeEnabled() >> clusterMode
        path == result

        where:
        integration         | uuid      | clusterMode   | path
        ScmService.EXPORT   | 'abcd'    |false          | 'abcd/etc/scm-export.properties'
        ScmService.IMPORT   | 'efgh'    |false          | 'efgh/etc/scm-import.properties'
        ScmService.EXPORT   | 'ijkl'    |null           | 'ijkl/etc/scm-export.properties'
        ScmService.IMPORT   | 'mnop'    |null           | 'mnop/etc/scm-import.properties'
        ScmService.EXPORT   | 'qrst'    |true           | 'etc/scm-export.properties'
        ScmService.IMPORT   | 'uvwx'    |true           | 'etc/scm-import.properties'

    }
}
