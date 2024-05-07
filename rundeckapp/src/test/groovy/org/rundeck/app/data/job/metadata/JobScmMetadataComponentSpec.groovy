package org.rundeck.app.data.job.metadata

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.plugins.views.Action
import com.dtolabs.rundeck.plugins.scm.ImportSynchState
import com.dtolabs.rundeck.plugins.scm.JobImportState
import com.dtolabs.rundeck.plugins.scm.JobRenamed
import com.dtolabs.rundeck.plugins.scm.JobState
import com.dtolabs.rundeck.plugins.scm.ScmCommitInfo
import com.dtolabs.rundeck.plugins.scm.SynchState
import org.rundeck.app.data.providers.v1.job.JobDataProvider
import org.rundeck.core.auth.AuthConstants
import rundeck.services.ScmService
import rundeck.services.scm.ScmPluginConfigData
import spock.lang.Specification

class JobScmMetadataComponentSpec extends Specification {
    def "scm import data"() {
        given:
            def auth = Mock(UserAndRolesAuthContext)
            def sut = new JobScmMetadataComponent()
            sut.scmService = Mock(ScmService) {
                1 * projectHasConfiguredImportPlugin('project') >> true
                1 * loadScmConfig('project', ScmService.IMPORT) >> Mock(ScmPluginConfigData) {
                    _ * getEnabled() >> true
                }
                1 * userHasAccessToScmConfiguredKeyOrPassword(auth, ScmService.IMPORT, 'project') >> true
                1 * getJobsPluginMeta('project', false) >> [:]
                1 * importStatusForJobIds('project', auth, ['id'], true, [:]) >>
                [id: Mock(JobImportState) {
                    _ * getSynchState() >> ImportSynchState.CLEAN
                    _ * getActions() >> [
                        Mock(Action) {
                            _ * getId() >> 'action1'
                            _ * getTitle() >> 'actionTitle'
                            _ * getDescription() >> 'actionDesc'
                            _ * getIconName() >> 'actionIcon'
                        }
                    ]
                    _ * getCommit() >> Mock(ScmCommitInfo) {
                        _ * asMap() >> [
                            commit: 'info'
                        ]
                    }
                    _ * getJobRenamed() >> Mock(JobRenamed) {
                        _ * getUuid() >> 'renamedUuid'
                        _ * getSourceId() >> 'renameSourceId'
                        _ * getRenamedPath() >> 'renamedPath'
                    }
                }]
            }
            sut.rundeckAuthContextProcessor = Mock(AuthContextProcessor) {
                _ * authorizeApplicationResourceAny(
                    auth,
                    _,
                    [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN, AuthConstants.ACTION_IMPORT, AuthConstants.ACTION_SCM_IMPORT]
                ) >> true
            }
            sut.jobDataProvider = Mock(JobDataProvider)
        when:
            def result = sut.getMetadataForJob('id', 'project', [JobScmMetadataComponent.SCM_IMPORT].toSet(), auth)
        then:
            result != null
            result.isPresent()
            def parts = result.get()
            parts.size() == 1

            def scmImport = parts.find { it.name == JobScmMetadataComponent.SCM_IMPORT }
            scmImport != null
            scmImport.data == [
                jobState: [
                    synchState: ImportSynchState.CLEAN.toString(),
                    actions   : [
                        [
                            id         : 'action1',
                            title      : 'actionTitle',
                            description: 'actionDesc',
                            iconName   : 'actionIcon'
                        ]
                    ],
                    jobRenamed: [
                        uuid       : 'renamedUuid',
                        sourceId   : 'renameSourceId',
                        renamedPath: 'renamedPath'
                    ],
                    commit    : [
                        commit: 'info'
                    ]
                ]
            ]

    }
    def "scm export data"() {
        given:
            def auth = Mock(UserAndRolesAuthContext)
            def sut = new JobScmMetadataComponent()
            sut.scmService = Mock(ScmService) {
                1 * projectHasConfiguredExportPlugin('project') >> true
                1 * loadScmConfig('project', ScmService.EXPORT) >> Mock(ScmPluginConfigData) {
                    _ * getEnabled() >> true
                }
                1 * userHasAccessToScmConfiguredKeyOrPassword(auth, ScmService.EXPORT, 'project') >> true
                1 * getJobsPluginMeta('project', true) >> [:]
                1 * exportStatusForJobIds('project', auth, ['id'], true, [:]) >>
                [id: Mock(JobState) {
                    _ * getSynchState() >> SynchState.CLEAN
                    _ * getActions() >> [
                        Mock(Action) {
                            _ * getId() >> 'action1'
                            _ * getTitle() >> 'actionTitle'
                            _ * getDescription() >> 'actionDesc'
                            _ * getIconName() >> 'actionIcon'
                        }
                    ]
                    _ * getCommit() >> Mock(ScmCommitInfo) {
                        _ * asMap() >> [
                            commit: 'info'
                        ]
                    }
                }]
            }
            sut.rundeckAuthContextProcessor = Mock(AuthContextProcessor) {
                _ * authorizeApplicationResourceAny(
                    auth,
                    _,
                    [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN, AuthConstants.ACTION_EXPORT, AuthConstants.ACTION_SCM_EXPORT]
                ) >> true
            }
            sut.jobDataProvider = Mock(JobDataProvider)
        when:
            def result = sut.getMetadataForJob('id', 'project', [JobScmMetadataComponent.SCM_EXPORT].toSet(), auth)
        then:
            result != null
            result.isPresent()
            def parts = result.get()
            parts.size() == 1

            def scmImport = parts.find { it.name == JobScmMetadataComponent.SCM_EXPORT }
            scmImport != null
            scmImport.data == [
                jobState: [
                    synchState: SynchState.CLEAN.toString(),
                    actions   : [
                        [
                            id         : 'action1',
                            title      : 'actionTitle',
                            description: 'actionDesc',
                            iconName   : 'actionIcon'
                        ]
                    ],
                    commit    : [
                        commit: 'info'
                    ]
                ]
            ]

    }
}
