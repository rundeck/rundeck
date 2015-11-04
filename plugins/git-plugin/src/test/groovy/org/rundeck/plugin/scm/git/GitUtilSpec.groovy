package org.rundeck.plugin.scm.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import spock.lang.Specification

/**
 * Created by greg on 11/2/15.
 */
class GitUtilSpec extends Specification {
    def "getcommit found"() {
        given:
        def tempdir = File.createTempFile("GitUtilSpec", "-test")
        tempdir.delete()
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
        def tempdir = File.createTempFile("GitUtilSpec", "-test")
        tempdir.delete()
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
