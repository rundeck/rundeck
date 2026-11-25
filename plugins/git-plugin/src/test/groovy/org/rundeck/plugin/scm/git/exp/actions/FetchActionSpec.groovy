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
import com.dtolabs.rundeck.plugins.scm.ScmPluginException
import com.dtolabs.rundeck.plugins.scm.ScmUserInfo
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.TrackingRefUpdate
import org.eclipse.jgit.util.FileUtils
import org.rundeck.plugin.scm.git.GitExportPlugin
import org.rundeck.plugin.scm.git.GitExportPluginSpec
import spock.lang.Specification

/**
 * fetchFromRemote already wraps its own failure in remoteAccessFailure()
 * (redacted, user-facing), including failures from setupTransportAuthentication. FetchAction
 * must not wrap that again - doing so doubled the message prefix and, for a
 * setupTransportAuthentication failure (which runs before fetchFromRemote's own try/catch),
 * bypassed the redaction entirely by rebuilding the message from the raw exception.
 */
class FetchActionSpec extends Specification {
    File tempdir
    GitExportPlugin openedPlugin

    def setup() {
        tempdir = File.createTempFile("FetchActionSpec", "-test")
        tempdir.delete()
    }

    def cleanup() {
        openedPlugin?.cleanup()
        if (tempdir.exists()) {
            FileUtils.delete(tempdir, FileUtils.RECURSIVE | FileUtils.IGNORE_ERRORS)
        }
    }

    def "a fetchFromRemote failure propagates unwrapped, not re-wrapped with a doubled message"() {
        given: "fetchFromRemote's own exception is already the safe, redacted one"
        def gitdir = new File(tempdir, 'scm')
        def origindir = new File(tempdir, 'origin')
        def config = GitExportPluginSpec.createTestConfig(gitdir, origindir)
        def origin = GitExportPluginSpec.createGit(origindir)
        origin.close()

        def alreadyWrapped = new ScmPluginException(
                'Failed fetch from the repository: Could not access the Git repository.'
        )
        def plugin = new GitExportPlugin(config) {
            @Override
            TrackingRefUpdate fetchFromRemote(ScmOperationContext context, Git git1 = null) {
                throw alreadyWrapped
            }
        }
        def userInfo = Mock(ScmUserInfo)
        def ctxt = Mock(ScmOperationContext) {
            getUserInfo() >> userInfo
        }
        plugin.initialize(ctxt)
        openedPlugin = plugin
        def action = new FetchAction('fetch', 'Fetch', 'desc')

        when:
        action.perform(plugin, [] as Set<JobExportReference>, [] as Set<String>, ctxt, [:])

        then: "the exact same exception propagates - not caught and re-thrown as a new one"
        def e = thrown(ScmPluginException)
        e.is(alreadyWrapped)
        e.message == 'Failed fetch from the repository: Could not access the Git repository.'
    }
}
