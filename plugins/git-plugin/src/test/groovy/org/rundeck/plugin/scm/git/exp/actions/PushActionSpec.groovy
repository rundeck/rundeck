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

package org.rundeck.plugin.scm.git.exp.actions

import com.dtolabs.rundeck.plugins.scm.JobExportReference
import com.dtolabs.rundeck.plugins.scm.ScmOperationContext
import com.dtolabs.rundeck.plugins.scm.ScmUserInfo
import org.eclipse.jgit.util.FileUtils
import org.rundeck.plugin.scm.git.GitExportPlugin
import org.rundeck.plugin.scm.git.GitExportPluginSpec
import spock.lang.Specification

/**
 * Covers the UP_TO_DATE / null-HEAD handling added alongside the resource-lifecycle fixes
 * (RUN-4881): a successful push with nothing new to send must not be reported as a failure,
 * and pushing from a brand-new repository with no commits yet must not NPE.
 */
class PushActionSpec extends Specification {
    File tempdir
    GitExportPlugin openedPlugin

    def setup() {
        tempdir = File.createTempFile("PushActionSpec", "-test")
        tempdir.delete()
    }

    def cleanup() {
        //close the plugin's open repository before deleting tempdir, or its JGit resources stay
        //open (can block pack-file deletion on Windows, and IGNORE_ERRORS below would hide it)
        openedPlugin?.cleanup()
        if (tempdir.exists()) {
            FileUtils.delete(tempdir, FileUtils.RECURSIVE | FileUtils.IGNORE_ERRORS)
        }
    }

    private GitExportPlugin initedPlugin(File gitdir, File origindir) {
        def config = GitExportPluginSpec.createTestConfig(gitdir, origindir)
        def userInfo = Mock(ScmUserInfo)
        def ctxt = Mock(ScmOperationContext) {
            getUserInfo() >> userInfo
        }
        def plugin = new GitExportPlugin(config)
        plugin.initialize(ctxt)
        openedPlugin = plugin
        plugin
    }

    def "a push with nothing new to send (UP_TO_DATE) is reported as success"() {
        given: "a clone that is already in sync with its origin"
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        def origin = GitExportPluginSpec.createGit(origindir)
        GitExportPluginSpec.addCommitFile(origindir, origin, 'testcommit.txt', 'blah')
        origin.close()

        def plugin = initedPlugin(gitdir, origindir)
        def action = new PushAction('push', 'Push', 'desc')

        when: "pushing immediately after cloning, with no local changes"
        def result = action.perform(plugin, [] as Set<JobExportReference>, [] as Set<String>, Mock(ScmOperationContext), [:])

        then: "UP_TO_DATE is not treated as a failure"
        result.success
        !(result.message =~ /(?i)fail/)
        result.id != null
        result.commit != null
    }

    def "a genuinely rejected (non-fast-forward) update is still reported as a failure"() {
        given: "the origin moves ahead with a commit the local clone never fetched"
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        def origin = GitExportPluginSpec.createGit(origindir)
        GitExportPluginSpec.addCommitFile(origindir, origin, 'testcommit.txt', 'blah')

        def plugin = initedPlugin(gitdir, origindir)

        //origin advances independently of the already-cloned local copy
        GitExportPluginSpec.addCommitFile(origindir, origin, 'testcommit.txt', 'changed-on-origin')
        origin.close()

        //local also advances, so the push is a genuine (non-fast-forward) divergence, not a no-op
        GitExportPluginSpec.addCommitFile(gitdir, plugin.git, 'testcommit.txt', 'changed-on-local')

        def action = new PushAction('push', 'Push', 'desc')

        when:
        def result = action.perform(plugin, [] as Set<JobExportReference>, [] as Set<String>, Mock(ScmOperationContext), [:])

        then:
        !result.success
        result.message =~ /(?i)fail/
    }

    def "a null HEAD commit does not NPE when building the push result message"() {
        given: "a push that succeeds normally, but HEAD resolves to no commit (e.g. an empty repository)"
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        def origin = GitExportPluginSpec.createGit(origindir)
        GitExportPluginSpec.addCommitFile(origindir, origin, 'testcommit.txt', 'blah')
        origin.close()

        def plugin = Spy(initedPlugin(gitdir, origindir))
        plugin.getHead() >> null
        def action = new PushAction('push', 'Push', 'desc')

        when:
        def result = action.perform(plugin, [] as Set<JobExportReference>, [] as Set<String>, Mock(ScmOperationContext), [:])

        then:
        noExceptionThrown()
        result.success
        result.id == null
        result.commit == null
        result.message == 'Remote push result: OK.'
    }
}
