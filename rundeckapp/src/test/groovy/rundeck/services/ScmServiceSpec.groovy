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

import com.dtolabs.rundeck.app.internal.framework.RundeckFramework
import com.dtolabs.rundeck.core.authorization.AuthContextProvider
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.core.plugins.CloseableProvider
import com.dtolabs.rundeck.core.plugins.Closeables
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.plugins.jobs.JobChangeListener
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import com.dtolabs.rundeck.plugins.scm.JobScmReference
import com.dtolabs.rundeck.plugins.scm.JobState
import com.dtolabs.rundeck.plugins.scm.ScmCommitInfo
import com.dtolabs.rundeck.plugins.scm.ScmExportPlugin
import com.dtolabs.rundeck.plugins.scm.ScmExportPluginFactory
import com.dtolabs.rundeck.plugins.scm.ScmExportResult
import com.dtolabs.rundeck.plugins.scm.ScmExportSynchState
import com.dtolabs.rundeck.plugins.scm.ScmImportPlugin
import com.dtolabs.rundeck.plugins.scm.ScmImportPluginFactory
import com.dtolabs.rundeck.plugins.scm.ScmOperationContext
import com.dtolabs.rundeck.plugins.scm.ScmPluginException
import com.dtolabs.rundeck.plugins.scm.ScmPluginInvalidInput
import com.dtolabs.rundeck.core.plugins.ValidatedPlugin
import com.dtolabs.rundeck.server.plugins.services.ScmExportPluginProviderService
import com.dtolabs.rundeck.server.plugins.services.ScmImportPluginProviderService
import grails.test.hibernate.HibernateSpec
import grails.testing.services.ServiceUnitTest
import rundeck.PluginMeta
import rundeck.ScheduledExecution
import rundeck.User
import rundeck.Storage
import rundeck.services.scm.ScmPluginConfigData
import spock.lang.Unroll
import testhelper.RundeckHibernateSpec

/**
 * Created by greg on 10/15/15.
 */
class ScmServiceSpec extends RundeckHibernateSpec implements ServiceUnitTest<ScmService> {

    List<Class> getDomainClasses() { [ScheduledExecution, User, Storage ] }

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
        0 * service.pluginConfigService.loadScmConfig(
                'test1',
                "etc/scm-${integration}.properties",
                'scm.' + integration
        ) >> config
        0 * config.setEnabled(false)
        0 * service.pluginConfigService.storeConfig(config, 'test1', "etc/scm-${integration}.properties")
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
        0 * config.setEnabled(false)
        0 * config2.setEnabled(false)
        0 * service.pluginConfigService.storeConfig(config, 'test1', "etc/scm-import.properties")
        0 * service.pluginConfigService.storeConfig(config2, 'test1', "etc/scm-export.properties")
        2 * service.jobEventsService.removeListener(_)
        1 * service.pluginConfigService.removePluginConfiguration('test1', "etc/scm-import.properties")
        1 * service.pluginConfigService.removePluginConfiguration('test1', "etc/scm-export.properties")

    }

    def "validatePluginSetup"() {
        given:
        def config = [:]
        service.pluginService = Mock(PluginService)
        def resolver = Mock(PropertyResolver)
        service.frameworkService = Mock(FrameworkService){
            pluginConfigFactory(_,_) >> Mock(PropertyResolverFactory.Factory){
                create(_,_) >> resolver
            }
        }
        service.scmExportPluginProviderService = Mock(ScmExportPluginProviderService)
        service.scmImportPluginProviderService = Mock(ScmImportPluginProviderService)

        def validated = new ValidatedPlugin(valid: false)

        when:
        def result = service.validatePluginSetup(integration, 'test', 'type', config)


        then:


        1 * service.pluginService.validatePlugin(
                'type',
                integration == ScmService.EXPORT ? service.scmExportPluginProviderService :
                        service.scmImportPluginProviderService,
                _,
                PropertyScope.Instance,
                PropertyScope.Project,
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
        def resolver = Mock(PropertyResolver)
        service.frameworkService = Mock(FrameworkService){
            pluginConfigFactory(_,_) >> Mock(PropertyResolverFactory.Factory){
                create(_,_) >> resolver
            }
        }

        def report = Validator.errorReport('a', 'b')
        def validated = new ValidatedPlugin(valid: false, report: report)
        when:
        def result = service.initPlugin(integration, ctx, 'atype', config)

        then:

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
        1 * service.pluginService.validatePlugin(*_) >> validated
        1 * service.pluginService.retainPlugin('atype', _) >> Closeables.closeableProvider(importFactory, testCloser)
        1 * service.pluginConfigService.loadScmConfig(*_) >> configobj
        1 * configobj.getSettingList('trackedItems') >> ['a', 'b']
        1 * importFactory.createPlugin(ctx, config, ['a', 'b'], true) >> plugin
        1 * service.jobEventsService.addListenerForProject(_, 'testProject')

        result == plugin



        where:
        integration       | _
        ScmService.IMPORT | _
    }

    def "init import plugin twice will replace registered listener"() {
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
            def mockListener = Mock(JobChangeListener)

        when:
            def result = service.initPlugin(integration, ctx, 'atype', config)
            def result2 = service.initPlugin(integration, ctx, 'atype', config)

        then:
            2 * service.pluginService.validatePlugin(*_) >> validated
            2 * service.pluginService.retainPlugin('atype', _) >>
            Closeables.closeableProvider(importFactory, testCloser)
            2 * service.pluginConfigService.loadScmConfig(*_) >> configobj
            2 * configobj.getSettingList('trackedItems') >> ['a', 'b']
            2 * importFactory.createPlugin(ctx, config, ['a', 'b'], true) >> plugin
            1 * service.jobEventsService.removeListener(null)
            1 * service.jobEventsService.removeListener(mockListener)
            2 * service.jobEventsService.addListenerForProject(_, 'testProject') >> mockListener

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
        1 * service.pluginService.validatePlugin(*_) >> validated
        1 * service.pluginService.retainPlugin('atype', _) >> Closeables.closeableProvider(exportFactory, exportCloser)
        1 * exportFactory.createPlugin(ctx, config, true) >> plugin
        1 * service.jobEventsService.addListenerForProject(_, 'testProject')

        result == plugin



        where:
        integration       | _
        ScmService.EXPORT | _
    }

    def "init export plugin twice will replace registered plugin"() {
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
            def mockListener = Mock(JobChangeListener)

        when:
            def result = service.initPlugin(integration, ctx, 'atype', config)
            def result2 = service.initPlugin(integration, ctx, 'atype', config)

        then:
            2 * service.pluginService.validatePlugin(*_) >> validated
            2 * service.pluginService.retainPlugin('atype', _) >>
            Closeables.closeableProvider(exportFactory, exportCloser)
            2 * exportFactory.createPlugin(ctx, config, true) >> plugin
            1 * service.jobEventsService.removeListener(null)
            1 * service.jobEventsService.removeListener(mockListener)
            2 * service.jobEventsService.addListenerForProject(_, 'testProject') >> mockListener

            result == plugin
        where:
            integration       | _
            ScmService.EXPORT | _
    }


    def "export project status basic"() {
        given:
        def ctx = Mock(ScmOperationContext) {
            getFrameworkProject() >> null
        }
        def config = [:]

        ScmExportPluginFactory exportFactory = Mock(ScmExportPluginFactory)
        ScmExportPlugin plugin = Mock(ScmExportPlugin){
        }
        TestCloseable exportCloser = new TestCloseable()

        service.pluginService = Mock(PluginService)
        service.pluginConfigService = Mock(PluginConfigService)
        service.jobEventsService = Mock(JobEventsService)
        service.frameworkService = Mock(FrameworkService){
            getRundeckFramework() >> Mock(RundeckFramework){
                getFrameworkProjectMgr() >> Mock(ProjectManager) {
                    listFrameworkProjectNames() >> ['testProject']
                }
            }
        }
        service.storageService = Mock(StorageService)
        service.rundeckAuthContextProvider=Mock(AuthContextProvider)

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
        service.rundeckAuthContextProvider.getAuthContextForUserAndRolesAndProject(_, _, 'testProject') >> Mock(
                UserAndRolesAuthContext
        ) {
            getUsername()>>'testuser'
            getRoles()>>new HashSet<String>(['arole'])
        }
        1 * service.pluginService.validatePlugin(*_) >> validated
        1 * service.pluginService.retainPlugin('atype', _) >> Closeables.closeableProvider(exportFactory, exportCloser)
        1 * exportFactory.createPlugin(_, config, true) >> plugin
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
        service.rundeckAuthContextProvider=Mock(AuthContextProvider)

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
        service.rundeckAuthContextProvider.getAuthContextForUserAndRolesAndProject(_, _, 'testProject') >> Mock(
                UserAndRolesAuthContext
        ) {
            getUsername()>>'testuser'
            getRoles()>>new HashSet<String>(['arole'])
        }
        1 * service.pluginService.validatePlugin(*_) >> validated
        1 * service.pluginService.retainPlugin('atype', _) >> Closeables.closeableProvider(exportFactory, exportCloser)
        1 * exportFactory.createPlugin(_, _, true) >> plugin
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
        def config1 = Mock(ScmPluginConfigData) {
            getEnabled() >> true
            getSetting("username") >> 'bob'
            getSettingList("roles") >> ['arole']
            getType() >> 'pluginType'
            getConfig() >> [plugin: 'config']
            getSettingList('trackedItems') >> ['a', 'b']
        }
        service.pluginConfigService = Mock(PluginConfigService){
            loadScmConfig('test1', _, _) >> config1
        }
        ScmExportPlugin plugin = Mock(ScmExportPlugin)
        TestCloseable exportCloser = new TestCloseable()
        service.loadedExportPlugins['test1'] = Closeables.closeableProvider(plugin, exportCloser)

        def input = [:]
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'bob'
        }
        def job = new ScheduledExecution()
        job.version = 1
        job.jobName = "test"
        job.groupPath = "test"
        def bobuser = new User(login: 'bob').save()

        //returned by export result, should be stored in job metadata
        def commitMetadata = [commit: 'data']


        when:
        def result = service.performExportAction('actionId', auth, 'test1', input, [job], [])

        then:
        1 * plugin.getInputViewForAction(_, 'actionId') >> Mock(BasicInputView) {
            getProperties() >> []
        }
        1 * plugin.export(_, 'actionId', _, _, input) >> Mock(ScmExportResult) {
            isSuccess() >> true
            getCommit() >> Mock(ScmCommitInfo) {
                asMap() >> commitMetadata
                getCommitId() >> 'a-commit-id'
            }
            getId() >> 'a-commit-id'

        }

        //store metadata about commit
        1 * service.jobMetadataService.setJobPluginMeta(job, 'scm-export', [version: 1, pluginMeta: commitMetadata, 'name': 'test', 'groupPath': 'test'])

        result.valid
        result.commitId == 'a-commit-id'
    }

    def "initialize should read correct configs when initDeferred is false"() {
        given:
        service.pluginConfigService = Mock(PluginConfigService)
        service.frameworkService = Mock(FrameworkService)
        service.configurationService = Mock(ConfigurationService){
            getBoolean("scm.startup.initDeferred")>>false
        }
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
        service.rundeckAuthContextProvider=Mock(AuthContextProvider)
        def validated = new ValidatedPlugin(valid: true)
        service.configurationService = Mock(ConfigurationService){
            getBoolean("scm.startup.initDeferred")>>false
        }

        when:
        service.initialize()
        then:
        1 * service.frameworkService.projectNames() >> ['projectA']
        2 * service.pluginConfigService.loadScmConfig('projectA', 'etc/scm-import.properties', 'scm.import') >> config1

        1 * service.rundeckAuthContextProvider.getAuthContextForUserAndRolesAndProject('bob', ['arole'], 'projectA') >>
                Mock(UserAndRolesAuthContext) {
            getUsername() >> 'bob'
        }
        1 * service.pluginService.validatePlugin(*_) >> validated
        1 * service.pluginService.retainPlugin('pluginType', _) >> Closeables.closeableProvider(importFactory)
        1 * importFactory.createPlugin(_, [plugin: 'config'], ['a', 'b'], true) >> plugin
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

    def "initProject is idempotent"() {
        given:
            service.pluginConfigService = Mock(PluginConfigService)
            service.frameworkService = Mock(FrameworkService) {
                isClusterModeEnabled() >> true
            }
        when:
            service.initProject('testproj1', integration)
            service.initProject('testproj1', integration)
        then:
            2 * service.pluginConfigService.loadScmConfig(
                    'testproj1',
                    "etc/scm-${integration}.properties",
                    "scm.$integration"
            ) >> Mock(ScmPluginConfigData) {
                2 * getEnabled() >> false
            }

        where:
            integration << ['export', 'import']
    }

    def "check if job was renamed after SCM export was disabled"() {
        given:
        service.pluginConfigService = Mock(PluginConfigService)
        service.frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> true
        }
        service.storageService = Mock(StorageService)
        service.pluginService = Mock(PluginService)
        service.jobEventsService = Mock(JobEventsService)

        def project = "test"
        service.rundeckAuthContextProvider = Mock(AuthContextProvider) {
            getAuthContextForUserAndRolesAndProject(_,_,_) >>
                    Mock(UserAndRolesAuthContext) {
                        getUsername() >> 'admin'
                    }
        }
        def job = new ScheduledExecution()
        job.uuid = "1234"
        job.version = 1
        job.jobName = "test"
        job.groupPath = "test"

        def jobs = [job]
        def originalMeta = [name: origName, groupPath: origGroup]
        def jobsPluginMeta = ["1234":originalMeta]

        ScmExportPlugin plugin = Mock(ScmExportPlugin)

        service.jobMetadataService = Mock(JobMetadataService){
            getJobPluginMeta(_,'scm-import')>>originalMeta
            getJobsPluginMeta(project, _)>> jobsPluginMeta
        }

        when:
        service.refreshExportPluginMetadata(project,plugin, jobs, jobsPluginMeta)
        then:

        1 * plugin.getJobStatus(_,_)>> Mock(JobState)
        jobChangeCalls * plugin.jobChanged(_,_)

        where:
        origName | origGroup | jobChangeCalls
        "test2"  | "test"    | 1
        "test"   | "test"    | 0
    }

    def "exportStatusForJobs calls plugin cluster fix in cluster mode"() {
        given:
        service.pluginConfigService = Mock(PluginConfigService)
        service.frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> true
        }
        service.storageService = Mock(StorageService)
        service.pluginService = Mock(PluginService)
        service.jobEventsService = Mock(JobEventsService)

        def project = "test"

        service.rundeckAuthContextProvider = Mock(AuthContextProvider) {
            getAuthContextForUserAndRolesAndProject(_,_,_) >>
                    Mock(UserAndRolesAuthContext) {
                        getUsername() >> 'admin'
                    }
        }
        def job = new ScheduledExecution()
        job.version = 1
        job.jobName = "test"
        job.groupPath = "test"

        def jobs = [job]
        def integration = "export"
        def originalMeta = [name: "test", groupPath: "tes"]

        ScmExportPlugin plugin = Mock(ScmExportPlugin)

        service.jobMetadataService = Mock(JobMetadataService){
            getJobPluginMeta(_,'scm-import')>>originalMeta
        }

        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'admin'
        }
        service.initedProjects<<"import/" + project
        service.initedProjects<<"export/" + project
        service.loadedExportPlugins[project]=Closeables.closeableProvider(plugin)

        when:
        service.exportStatusForJobs(project, auth, jobs,true)
        then:
        2 * service.pluginConfigService.loadScmConfig(
                project,
                "etc/scm-${integration}.properties",
                "scm.$integration"
        ) >> Mock(ScmPluginConfigData) {
            getEnabled() >> true
            getSetting("username")>>"admin"
            getSettingList("roles")>>["admin"]
            _ * getType() >> 'pluginType'
            getConfig() >> [plugin: 'config']
        }

        1 * plugin.clusterFixJobs(_,_,_)>> [:]
        1 * plugin.getJobStatus(_,_)>> Mock(JobState)
    }

    def "get job plugin meta"(){
        given:
            def job = new ScheduledExecution()
            service.jobMetadataService=Mock(JobMetadataService)
        when:
            def result = service.getJobPluginMeta(job, "scm-import")
        then:
            1 * service.jobMetadataService.getJobPluginMeta(job, ScmService.STORAGE_NAME_IMPORT)>>expect
            result == expect
        where:
            expect = [a:'map']
    }

    def "update job plugin metadata"() {
        given:
        service.pluginConfigService = Mock(PluginConfigService)
        service.frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> true
        }
        service.storageService = Mock(StorageService)
        service.pluginService = Mock(PluginService)
        service.jobEventsService = Mock(JobEventsService)

        def project = "test"

        service.rundeckAuthContextProvider = Mock(AuthContextProvider) {
            getAuthContextForUserAndRolesAndProject(_,_,_) >>
                    Mock(UserAndRolesAuthContext) {
                        getUsername() >> 'admin'
                    }
        }
        def job = new ScheduledExecution()
        job.version = 1
        job.uuid = "1234"
        job.jobName = "test"
        job.groupPath = "test"

        def jobs = [job]
        def jobsPluginMeta = ["1234":originalMeta]

        ScmExportPlugin plugin = Mock(ScmExportPlugin){
            getJobStatus(_,_)>>Mock(JobState){
                getCommit()>>Mock(ScmCommitInfo){
                    asMap()>>[commit:"123"]
                }
            }
        }

        service.jobMetadataService = Mock(JobMetadataService){
            getJobPluginMeta(_,'scm-export')>>originalMeta
        }
        ScmExportPluginFactory exportFactory = Mock(ScmExportPluginFactory)

        when:
        service.refreshExportPluginMetadata(project,plugin, jobs, jobsPluginMeta)
        then:
        jobMetadataCalls * service.jobMetadataService.setJobPluginMeta(job, 'scm-export', _)

        where:
        originalMeta                                                    | jobMetadataCalls
        [name: 'test', groupPath: 'test', pluginMeta: [commit:'123']]   | 0
        [pluginMeta: [commit:'123']]                                    | 1
        [:]                                                             | 1
        [name: 'test', groupPath: 'test']                               | 1

    }

    def "clean plugin closes provider"(){
        given:
            def auth = Mock(UserAndRolesAuthContext)
            service.pluginConfigService=Mock(PluginConfigService)
            service.storageService=Mock(StorageService)
            service.pluginService=Mock(PluginService)
            def config=Mock(ScmPluginConfigData){
                _*getType()>>type
                1 * setEnabled(false)
            }
            service.frameworkService=Mock(FrameworkService)
            def provider
            if(integration=='export'){
                provider=Mock(ScmExportPluginFactory){
                    1 * createPlugin(_,_, false)>>Mock(ScmExportPlugin){
                        1 * totalClean()
                    }
                }
            }else{
                provider=Mock(ScmImportPluginFactory){
                    1 * createPlugin(_, _, _, false)>>Mock(ScmImportPlugin){
                        1 * totalClean()
                    }
                }
            }
            service.jobMetadataService = Mock(JobMetadataService)
        when:
            service.cleanPlugin(integration,project,type,auth)
        then:
            (integration == 'import' ? 2 : 1) * service.pluginConfigService.loadScmConfig(project, _, _) >> config
            1 * service.pluginConfigService.storeConfig(config,project,_)
            1 * service.pluginService.retainPlugin(type,_)>>Mock(CloseableProvider){
                 1 * getProvider()>>provider
                 1 * close()
            }
            1 * service.jobMetadataService.removeProjectPluginMeta(project, _)
        where:
            integration << ['export','import']
            project = 'aproj'
            type = 'aplugin'
    }

    def "export file path maps for jobs"(){
        given:
            String project='aproj'
            def job1 = new ScheduledExecution(jobName: 'job1', uuid: 'job1id', project: project)
            def job2 = new ScheduledExecution(jobName: 'job2', uuid: 'job2id', project: project)
            def jobs = [job1, job2]
            service.jobMetadataService=Mock(JobMetadataService)
            service.initedProjects << "export/$project".toString()
            service.initedProjects << "import/$project".toString()
            def plugin = Mock(ScmExportPlugin)
            service.loadedExportPlugins[project] = Closeables.closeableProvider(plugin)
            service.frameworkService = Mock(FrameworkService){
                isClusterModeEnabled()>>false
            }
            service.pluginConfigService = Mock(PluginConfigService)
            service.pluginConfigService.loadScmConfig(
                    project,
                    "etc/scm-export.properties",
                    'scm.export'
            ) >> Mock(ScmPluginConfigData){
                getEnabled()>>true
            }
        when:
            def result = service.exportFilePathsMapForJobs(project, jobs)
        then:
            0 * service.jobMetadataService.getJobsPluginMeta(project, ScmService.STORAGE_NAME_EXPORT)
            1 * service.jobMetadataService.getJobPluginMeta(job1, ScmService.STORAGE_NAME_EXPORT)
            1 * service.jobMetadataService.getJobPluginMeta(job2, ScmService.STORAGE_NAME_EXPORT)
            1 * plugin.getRelativePathForJob({it.id== 'job1id' })>> '/a/path/job1'
            1 * plugin.getRelativePathForJob({it.id=='job2id' })>>'/a/path/job2'
            result == [
                job1id:'/a/path/job1',
                job2id:'/a/path/job2'
            ]
    }
    def "getExportPushActionId"(){
        given:
            def projectName = 'testProject'
            ScmExportPlugin plugin = Mock(ScmExportPlugin){
                getJobStatus(_)>>Mock(JobState){
                    getCommit()>>Mock(ScmCommitInfo){
                        asMap()>>[commit:"123"]
                    }
                }
            }
            service.frameworkService = Mock(FrameworkService) {
                isClusterModeEnabled() >> true
            }
            service.pluginConfigService = Mock(PluginConfigService)
            def ctx = Mock(ScmOperationContext)
            ScmPluginConfigData config = Mock(ScmPluginConfigData){
                1 * getEnabled()>>true
                1 * getSetting('username') >> 'testuser'
                1 * getSettingList('roles') >> ['arole']
            }
                service.pluginConfigService.loadScmConfig(
                    projectName,
                    "etc/scm-${integration}.properties",
                    'scm.' + integration,
            ) >> config
            service.rundeckAuthContextProvider=Mock(AuthContextProvider)
            service.storageService = Mock(StorageService)
            service.rundeckAuthContextProvider.getAuthContextForUserAndRolesAndProject(_, _, 'testProject') >> Mock(
                    UserAndRolesAuthContext
            ) {
                getUsername()>>'testuser'
                getRoles()>>new HashSet<String>(['arole'])
            }
            def validated = new ValidatedPlugin(valid: true)
            service.pluginService = Mock(PluginService)
            1 * service.pluginService.validatePlugin(*_) >> validated
            def provider
            if(integration=='export'){
                provider=Mock(ScmExportPluginFactory){
                    1 * createPlugin(_,_, true)>>Mock(ScmExportPlugin){
                        1 * getExportPushActionId() >> 'project-push'
                    }
                }
            }else{
                provider=Mock(ScmImportPluginFactory){
                    1 * createPlugin(_, _, _, true)>>Mock(ScmImportPlugin){
                    }
                }
            }
            1 * service.pluginService.retainPlugin(_,_)>>Mock(CloseableProvider){
                1 * getProvider()>>provider
            }
            service.jobEventsService = Mock(JobEventsService)
        when:
            def result = service.getExportPushActionId(projectName)
        then:
            result == (integration == ScmService.EXPORT? 'project-push':null)
        where:
        integration         | _
        ScmService.EXPORT   | _
        //ScmService.IMPORT   | _
    }

    def "export change listener event Delete"() {
        given:
            def project='aproj'
            def context = Mock(ScmOperationContext){
                getFrameworkProject()>>project
            }
            def plugin = Mock(ScmExportPlugin)
            def sut = new ScmService.ExportChangeListener(service: service, context: context, plugin: plugin)
            def jobref = Mock(JobRevReference){
                getProject()>>project
                getId()>>'123'
            }
            def event = new StoredJobChangeEvent(eventType: evtType,jobReference: jobref)
            def serializer = null
            service.jobMetadataService=Mock(JobMetadataService)
        when:
            sut.jobChangeEvent(event, serializer)
        then:

            1 * plugin.getRelativePathForJob({
                it.id=='123'
            })>>'/a/path'
            1 * service.jobMetadataService.getJobPluginMeta(project,'123','scm-export')
            1 * plugin.jobChanged({ it.eventType==evtType },_)
            service.deletedJobsCache[project]['/a/path']!=null
        where:
            evtType                                  | _
            JobChangeEvent.JobChangeEventType.DELETE | _

    }

    def "export change listener event MODIFY_RENAME"() {
        given:
            def project='aproj'
            def context = Mock(ScmOperationContext){
                getFrameworkProject()>>project
            }
            def plugin = Mock(ScmExportPlugin)
            def sut = new ScmService.ExportChangeListener(service: service, context: context, plugin: plugin)
            def jobref = Mock(JobRevReference){
                getProject()>>project
                getId()>>'123'
                getJobName()>>'aname'
            }
            def origref=Mock(JobReference){
                getProject()>>project
                getId()>>'123'
                getJobName()>>'bname'
            }
            def event = new StoredJobChangeEvent(
                eventType: evtType,
                jobReference: jobref,
                originalJobReference: origref
            )
            def serializer = null
            service.jobMetadataService = Mock(JobMetadataService)
        when:
            sut.jobChangeEvent(event, serializer)
        then:


            1 * plugin.getRelativePathForJob({
                it.id=='123'
                it.jobName=='aname'
            })>> '/a/path'
            1 * plugin.getRelativePathForJob({
                it.id=='123'
                it.jobName=='bname'
            })>> '/b/path'
            1 * plugin.jobChanged({ it.eventType==evtType },_)
            0 * plugin._(*_)
            service.renamedJobsCache[project]['123'] == '/b/path'
        where:
            evtType                                  | _
            JobChangeEvent.JobChangeEventType.MODIFY_RENAME | _
    }
    @Unroll
    def "export change listener event #evtType default behavior"() {
        given:
            def project='aproj'
            def context = Mock(ScmOperationContext){
                getFrameworkProject()>>project
            }
            def plugin = Mock(ScmExportPlugin)
            def sut = new ScmService.ExportChangeListener(service: service, context: context, plugin: plugin)
            def jobref = Mock(JobRevReference){
                getProject()>>project
                getId()>>'123'
                getJobName()>>'aname'
            }
            def origref=Mock(JobReference){
                getProject()>>project
                getId()>>'123'
                getJobName()>>'bname'
            }
            def event = new StoredJobChangeEvent(
                eventType: evtType,
                jobReference: jobref,
                originalJobReference: origref
            )
            def serializer = null
            service.jobMetadataService = Mock(JobMetadataService)
        when:
            sut.jobChangeEvent(event, serializer)
        then:
            1 * plugin.jobChanged({ it.eventType==evtType },_)
            0 * plugin._(*_)
        where:
            evtType                                  | _
            JobChangeEvent.JobChangeEventType.CREATE | _
            JobChangeEvent.JobChangeEventType.MODIFY | _
    }

    def "get srcId jobs export plugin metadata"() {
        given:
        service.pluginConfigService = Mock(PluginConfigService)
        service.frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> true
        }
        service.storageService = Mock(StorageService)
        service.pluginService = Mock(PluginService)
        service.jobEventsService = Mock(JobEventsService)

        def project = "test"
        def job = new ScheduledExecution()
        job.uuid = "1234"
        job.version = 1
        job.jobName = "test"
        job.groupPath = "test"


        PluginMeta importPluginMeta = new PluginMeta()
        importPluginMeta.key = "1234/scm-import"
        importPluginMeta.project = project
        importPluginMeta.jsonData = '{"name":"test","groupPath":"test","srcId":"456"}'

        PluginMeta exportPluginMeta = new PluginMeta()
        exportPluginMeta.key = jobsExportMetaKey
        exportPluginMeta.project = project
        exportPluginMeta.jsonData = jobsExportMetaKeyJson

        def jobsImportMeta = [importPluginMeta]
        def jobsExportMeta = [exportPluginMeta]

        when:
        def result = service.getJobsPluginMeta(project,isExport)

        then:

        service.jobMetadataService = Mock(JobMetadataService){
            1 * getJobsPluginMeta(_,'scm-import')>>jobsImportMeta
            calls * getJobsPluginMeta(_,'scm-export')>>jobsExportMeta
        }

        result.get("1234")["srcId"] == checkResult

        where:
        isExport    | jobsExportMetaKey   | jobsExportMetaKeyJson                                   | calls | checkResult
        true        | "xxx/scm-export"    | "{}"                                                    | 1     | "456"
        false       | "xxx/scm-export"    | "{}"                                                    | 0     | "456"
        true        | "1234/scm-export"   | '{"name":"test","groupPath":"test"}'                    | 1     | "456"
        true        | "1234/scm-export"   | '{"name":"test","groupPath":"test","srcId":"1234"}'     | 1     | "1234"
    }

    def "get srcId job export plugin metadata"() {
        given:
        service.pluginConfigService = Mock(PluginConfigService)
        service.frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> true
        }
        service.storageService = Mock(StorageService)
        service.pluginService = Mock(PluginService)
        service.jobEventsService = Mock(JobEventsService)

        def job = new ScheduledExecution()
        job.uuid = "1234"
        job.version = 1
        job.jobName = "test"
        job.groupPath = "test"

        when:
        def result = service.getJobPluginMeta(job,type)

        then:

        service.jobMetadataService = Mock(JobMetadataService){
            1 * getJobPluginMeta(_,'scm-import')>>importMeta
            calls * getJobPluginMeta(_,'scm-export')>>exportMeta
        }

        result["srcId"] == checkResult

        where:
        type            | exportMeta                                          | importMeta      | calls | checkResult
        "scm-export"    | null                                                | [srcId:"456"]   | 1     | "456"
        "scm-import"    | "{}"                                                | [srcId:"456"]   | 0     | "456"
        "scm-export"    | [name: 'test', groupPath: 'test']                   | [srcId:"456"]   | 1     | "456"
        "scm-export"    | [name: 'test', groupPath: 'test', srcId:"1234"]     | [srcId:"456"]   | 1     | "1234"
        "scm-export"    | [name: 'test', groupPath: 'test', srcId:"1234"]     | null            | 1     | "1234"
        "scm-export"    | [name: 'test', groupPath: 'test']                   | null            | 1     | null

    }

}
