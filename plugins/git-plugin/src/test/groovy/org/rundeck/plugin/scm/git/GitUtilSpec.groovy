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

package org.rundeck.plugin.scm.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.util.FileUtils
import org.eclipse.jgit.util.SystemReader
import spock.lang.Specification

/**
 * Created by greg on 11/2/15.
 */
class GitUtilSpec extends Specification {
    File tempdir

    def setup() {
        tempdir = File.createTempFile("GitUtilSpec", "-test")
        tempdir.delete()
        SystemReader.setInstance(new TestSystemReader())
    }

    def cleanup() {
        if (tempdir.exists()) {
            FileUtils.delete(tempdir, FileUtils.RECURSIVE | FileUtils.IGNORE_ERRORS)
        }
    }
    def "getcommit found"() {
        given:
        def origindir = new File(tempdir, 'origin')
        //create a git dir
        Git git = BaseGitPluginSpec.createGit(origindir)
        new File(origindir,"test1")<<'data'
        git.add().addFilepattern('test1').call()
        RevCommit newcommit = git.commit().setOnly('test1').setAuthor('a','b@test.com').setMessage('abc').call()


        when:
        def result1 = GitUtil.getCommit(git.repository, newcommit.name)

        then:
        result1 == newcommit
    }
    def "getcommit not found"() {
        given:
        def origindir = new File(tempdir, 'origin')
        //create a git dir
        Git git = BaseGitPluginSpec.createGit(origindir)
        new File(origindir,"test1")<<'data'
        git.add().addFilepattern('test1').call()
        RevCommit newcommit = git.commit().setOnly('test1').setAuthor('a','b@test.com').setMessage('abc').call()


        when:
        //not a commit ID
        def result2 = GitUtil.getCommit(git.repository, newcommit.tree.id.name)

        then:
        result2 == null
    }
    def "lastCommitsForPaths matches lastCommitForPath across edits, deletion and merge"() {
        given:
        def origindir = new File(tempdir, 'origin')
        Git git = BaseGitPluginSpec.createGit(origindir)
        def repo = git.repository
        ['root-only.xml', 'a.xml', 'b.xml', 'c.xml'].each { GitExportPluginSpec.addCommitFile(origindir, git, it, it) }
        GitExportPluginSpec.addCommitFile(origindir, git, 'b.xml', 'b2')
        git.rm().addFilepattern('c.xml').call()
        git.commit().setMessage('rm c').setAuthor('a', 'b@test.com').call()
        git.branchCreate().setName('dev').call()
        git.checkout().setName('dev').call()
        GitExportPluginSpec.addCommitFile(origindir, git, 'd.xml', 'd')
        git.checkout().setName('master').call()
        GitExportPluginSpec.addCommitFile(origindir, git, 'a.xml', 'a2')
        git.merge().include(repo.resolve('dev')).setMessage('merge dev').call()
        def paths = GitUtil.listPaths(git, 'HEAD^{tree}')

        when:
        def result = GitUtil.lastCommitsForPaths(repo, GitUtil.getHead(repo), paths + ['c.xml', 'never.xml'])

        then:
        paths.sort() == ['a.xml', 'b.xml', 'd.xml', 'root-only.xml']
        result.size() == 5
        result['root-only.xml'].parentCount == 0
        paths.every { result[it] == GitUtil.lastCommitForPath(repo, git, it) }
        result['c.xml'] == GitUtil.lastCommitForPath(repo, git, 'c.xml')
        !result.containsKey('never.xml')
        GitUtil.lastCommitsForPaths(repo, GitUtil.getHead(repo), []) == [:]
        GitUtil.lastCommitsForPaths(repo, null, paths) == [:]

        cleanup:
        git.close()
    }
}
