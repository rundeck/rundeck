package com.dtolabs.rundeck.server.plugins.jobs

import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.plugins.scm.PluginState
import com.dtolabs.rundeck.plugins.scm.ScmPlugin
import com.dtolabs.rundeck.plugins.scm.ScmPluginException
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import org.eclipse.jgit.api.AddCommand
import org.eclipse.jgit.api.CommitCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.api.StatusCommand
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

class TestScmPlugin {
    boolean inited = false
    Git git;
    Repository repo;
    File workingDir;
    PersonIdent commitIdent;
    String branch;
    final Map<String, ?> input
    final PluginState data
    final String project

    TestScmPlugin(final Map<String, ?> input, final PluginState data, final String project) {
        this.input = input
        this.data = data
        this.project = project
    }

    void initialize() {

    }

    boolean isSetup() {
        return inited
    }

//    @Override
    void setup(final Map<String, Object> input)
            throws ScmPluginException
    {
        //XXX: using ssh http://stackoverflow.com/questions/23692747/specifying-ssh-key-for-jgit
        System.err.println("setup: " + input + ", capabilities")
        if (inited) {
            System.err.println("already inited, not doing setup")
            return
        }
        branch = input.get("branch").toString()
        commitIdent = new PersonIdent(input.committerName.toString(), input.committerEmail.toString())
        File base = new File(input.get("dir").toString())
        if (base.isDirectory() && new File(base, ".git").isDirectory()) {
            System.err.println("base dir exists, not cloning")
            repo = new FileRepositoryBuilder().setGitDir(new File(base, ".git")).setWorkTree(base).build()
            git = new Git(repo)
            workingDir = base
            inited = true
            return
        }
        System.err.println("cloning...")
        String url = input.get("url").toString()
        git = Git.cloneRepository().setBranch(branch).setRemote("origin").setDirectory(base).setURI(url).call()
        repo = git.getRepository()
        workingDir = base
        inited = true

    }

    List<Property> getExportProperties() {
        []
    }

    void export(final Map<String, Object> input) throws ScmPluginException {

    }

    List<Property> getCommitProperties(final Set<File> files) {
        [
                PropertyBuilder.builder().string("commitMessage").
                        title("Commit Message").
                        description("Enter a commit message. Committing to branch: `"+branch+'`').
                        required(true).
                        renderingAsTextarea().
                        build(),
                PropertyBuilder.builder().string("tagName").
                        title("Tag").
                        description("Enter a tag name to include, will be pushed with the branch.").
                        required(false).
                        build(),
        ]
    }

    String commit(final Set<File> files, final Map<String, Object> input) throws ScmPluginException {
        String commitMessage = input.commitMessage.toString()
        StatusCommand statusCmd = git.status()
        files.each {
            statusCmd.addPath(relativePath(it))
        }
        Status status = statusCmd.call()
        //add all changes to index
        AddCommand addCommand = git.add()
        files.each {
            addCommand.addFilepattern(relativePath(it))
        }
        addCommand.call()

        CommitCommand commit1 = git.commit().setMessage(commitMessage).setCommitter(commitIdent)
        files.each {
            commit1.setOnly(relativePath(it))
        }
        RevCommit commit = commit1.call()

        return commit.name
    }

//    @Override
    ScmPlugin.ScmFileStatus fileStatus(final File reference) throws ScmPluginException {
        if (!inited) {
            return null
        }
        def path = relativePath(reference)
        Status status = git.status().addPath(path).call()
        System.err.println("status for ${path}, workingdir ${workingDir}: " + [
                conflicting:status.conflicting,
                added:status.added,
                changed:status.changed,
                clean:status.clean,
                conflictingStageState:status.conflictingStageState,
                ignoredNotInIndex:status.ignoredNotInIndex,
                missing:status.missing,
                modified:status.modified,
                removed:status.removed,
                uncommittedChanges:status.uncommittedChanges,
                untracked:status.untracked,
                untrackedFolders:status.untrackedFolders,
        ])
        if (path in status.added || path in status.untracked) {
            return ScmPlugin.ScmFileStatus.CREATED
        } else if (path in status.changed || path in status.modified) {
            //changed== changes in index
            //modified == changes on disk
            return ScmPlugin.ScmFileStatus.MODIFIED
        } else if (path in status.removed || path in status.missing) {
            return ScmPlugin.ScmFileStatus.DELETED
        }else if(path in status.untracked){
            return ScmPlugin.ScmFileStatus.NOT_PRESENT
        }else if(path in status.conflicting){
            return ScmPlugin.ScmFileStatus.CONFLICT
        }
        null
    }

    String relativePath(File reference) {
        reference.absolutePath.substring(workingDir.getAbsolutePath().length() + 1)
    }

//    @Override
//    ScmPlugin.ScmFileDiff diffFile(final File file) throws ScmPlugin.ScmPluginException {
//        def baos=new ByteArrayOutputStream()
//        List<DiffEntry> diffs=git.diff().setPathFilter(TreeFilter.ALL).setOutputStream(baos).call()
//
//        return [contentType:'text/plain',diff:baos.toString()] as ScmPlugin.ScmFileDiff
//    }
}
