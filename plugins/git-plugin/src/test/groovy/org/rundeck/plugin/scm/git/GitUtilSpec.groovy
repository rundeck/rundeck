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
import org.eclipse.jgit.lib.PersonIdent
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
    def "lastCommitsForPaths matches lastCommitForPath across edits, deletion and merges"() {
        given: "linear edits, a deletion and a merge that keeps one side"
        def origindir = new File(tempdir, 'origin')
        Git git = BaseGitPluginSpec.createGit(origindir)
        def repo = git.repository
        long t = 1_000_000L
        ['root-only.xml', 'a.xml', 'b.xml', 'c.xml'].each { commitFile(origindir, git, it, it, t++) }
        commitFile(origindir, git, 'b.xml', 'b2', t++)
        git.rm().addFilepattern('c.xml').call()
        git.commit().setMessage('rm c').setCommitter(committer(t++)).call()
        git.branchCreate().setName('dev').call()
        git.checkout().setName('dev').call()
        commitFile(origindir, git, 'd.xml', 'd', t++)
        git.checkout().setName('master').call()
        commitFile(origindir, git, 'a.xml', 'a2', t++)
        git.merge().include(repo.resolve('dev')).setMessage('merge dev').call()

        and: "both sides make the identical edit, the side branch later, and the merge is treesame to both"
        git.branchCreate().setName('twin').call()
        def masterTwin = commitFile(origindir, git, 'twin.xml', 'same', t + 1)
        git.checkout().setName('twin').call()
        commitFile(origindir, git, 'twin.xml', 'same', t + 50)
        git.checkout().setName('master').call()
        git.merge().include(repo.resolve('twin')).setMessage('merge twin').call()

        and: "a merge that resolves the path differently from both parents"
        git.branchCreate().setName('conflict').call()
        commitFile(origindir, git, 'a.xml', 'a-master', t + 100)
        git.checkout().setName('conflict').call()
        commitFile(origindir, git, 'a.xml', 'a-conflict', t + 101)
        git.checkout().setName('master').call()
        git.merge().include(repo.resolve('conflict')).call()
        new File(origindir, 'a.xml').text = 'a-resolved'
        git.add().addFilepattern('a.xml').call()
        def resolved = git.commit().setMessage('resolve a').setCommitter(committer(t + 102)).call()
        def paths = GitUtil.listPaths(git, 'HEAD^{tree}')

        when:
        def result = GitUtil.lastCommitsForPaths(repo, GitUtil.getHead(repo), paths + ['c.xml', 'never.xml'])

        then:
        paths.sort() == ['a.xml', 'b.xml', 'd.xml', 'root-only.xml', 'twin.xml']
        result.size() == 6
        (paths + ['c.xml']).every { result[it] == GitUtil.lastCommitForPath(repo, git, it) }
        result['root-only.xml'].parentCount == 0
        result['twin.xml'] == masterTwin
        result['a.xml'] == resolved
        result['a.xml'].parentCount == 2
        !result.containsKey('never.xml')
        GitUtil.lastCommitsForPaths(repo, GitUtil.getHead(repo), []) == [:]
        GitUtil.lastCommitsForPaths(repo, null, paths) == [:]

        cleanup:
        git.close()
    }

    def "lastCommitForPath from a start commit ignores later HEAD changes"() {
        given:
        def origindir = new File(tempdir, 'origin')
        Git git = BaseGitPluginSpec.createGit(origindir)
        def first = commitFile(origindir, git, 'a.xml', 'a1', 1_000_000L)
        def firstHead = GitUtil.getHead(git.repository)
        def second = commitFile(origindir, git, 'a.xml', 'a2', 1_000_001L)

        expect:
        GitUtil.lastCommitForPath(git.repository, git, firstHead, 'a.xml') == first
        GitUtil.lastCommitForPath(git.repository, git, 'a.xml') == second
        GitUtil.lastCommitForPath(git.repository, git, null, 'a.xml') == null

        cleanup:
        git.close()
    }

    private static PersonIdent committer(long epochSeconds) {
        new PersonIdent('test user', 'test@example.com', epochSeconds * 1000L, 0)
    }

    private static RevCommit commitFile(File gitdir, Git git, String path, String content, long epochSeconds) {
        new File(gitdir, path).text = content
        git.add().addFilepattern(path).call()
        git.commit().setOnly(path).setMessage("commit $path").setCommitter(committer(epochSeconds)).call()
    }
}
