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
import spock.lang.Specification

/**
 * Created by greg on 11/2/15.
 */
class GitUtilSpec extends Specification {
    File tempdir

    def setup() {
        tempdir = File.createTempFile("GitUtilSpec", "-test")
        tempdir.delete()
    }

    def cleanup() {
        if (tempdir.exists()) {
            FileUtils.delete(tempdir, FileUtils.RECURSIVE)
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
}
