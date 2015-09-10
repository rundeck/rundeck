package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.plugins.scm.JobExportReference
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.plugins.views.Action
import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.plugins.scm.JobFileMapper
import com.dtolabs.rundeck.plugins.scm.JobImportReference
import com.dtolabs.rundeck.plugins.scm.JobImporter
import com.dtolabs.rundeck.plugins.scm.JobState
import com.dtolabs.rundeck.plugins.scm.ScmDiffResult
import com.dtolabs.rundeck.plugins.scm.ScmExportResult
import com.dtolabs.rundeck.plugins.scm.ScmExportSynchState
import com.dtolabs.rundeck.plugins.scm.ScmImportPlugin
import com.dtolabs.rundeck.plugins.scm.ScmImportTrackedItem
import com.dtolabs.rundeck.plugins.scm.ScmImportTrackedItemBuilder
import com.dtolabs.rundeck.plugins.scm.ScmPluginException
import com.dtolabs.rundeck.plugins.scm.TemplateJobFileMapper
import org.apache.log4j.Logger
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.TreeWalk
import org.rundeck.plugin.scm.git.imp.actions.FetchAction
import org.rundeck.plugin.scm.git.imp.actions.ImportJobs
import org.rundeck.plugin.scm.git.imp.actions.SetupTracking

/**
 * Import jobs via git
 */
class GitImportPlugin implements ScmImportPlugin {
    static final Logger log = Logger.getLogger(GitImportPlugin)
    public static final String ACTION_INITIALIZE_TRACKING = 'initialize_tracking'
    public static final String ACTION_IMPORT_ALL = 'import-all'
    public static final String ACTION_GIT_FETCH = 'git-fetch'
    final Map<String, ?> input
    final String project
    boolean inited
    String branch
    Git git
    Repository repo
    File workingDir
    JobFileMapper mapper
    Map<String, GitImportAction> actions = [:]
    boolean trackedItemsSelected = false
    boolean useTrackingRegex = false
    String trackingRegex
    List<String> trackedItems = null


    GitImportPlugin(final Map<String, ?> input, final String project) {
        this.input = input
        this.project = project
    }

    @Override
    ScmExportResult scmImport(
            final String actionId,
            final JobImporter importer,
            final List<String> selectedPaths,
            final Map<String, Object> input
    ) throws ScmPluginException
    {
        return actions[actionId]?.performAction(this, importer, selectedPaths, input)
    }

    void initialize() {
        setup(input)
        actions = [
                (ACTION_INITIALIZE_TRACKING): new SetupTracking(
                        ACTION_INITIALIZE_TRACKING,
                        "Select Files to Import",
                        "Choose files and options for importing",
                        "glyphicon-cog"

                ),
                (ACTION_IMPORT_ALL)         : new ImportJobs(
                        ACTION_IMPORT_ALL,
                        "Import",
                        "Import Changes",
                        null

                ),
                (ACTION_GIT_FETCH)          : new FetchAction(
                        ACTION_GIT_FETCH,
                        "Fetch from Remote",
                        "Fetch incoming changes from Remote"
                )

        ]
    }

    void setup(final Map<String, ?> input) throws ScmPluginException {

        //TODO: using ssh http://stackoverflow.com/questions/23692747/specifying-ssh-key-for-jgit
        if (inited) {
            log.debug("already inited, not doing setup")
            return
        }

        GitImportPluginFactory.requiredProperties.each { key ->
            //verify input
            if (!input[key]) {
                throw new IllegalArgumentException("${key} cannot be null")
            }
        }


        def dir = input.get("dir").toString()
        def branch = input.get("branch").toString()
        def pathTemplate = input.pathTemplate.toString()
        def url = input.get("url").toString()

        File base = new File(dir)

        mapper = new TemplateJobFileMapper(pathTemplate, base)

        this.branch = branch

        if (base.isDirectory() && new File(base, ".git").isDirectory()) {
            log.debug("base dir exists, not cloning")
            repo = new FileRepositoryBuilder().setGitDir(new File(base, ".git")).setWorkTree(base).build()
            git = new Git(repo)
        } else {
            log.debug("cloning...")
            git = Git.cloneRepository().setBranch(this.branch).setRemote("origin").setDirectory(base).setURI(url).call()
            repo = git.getRepository()
        }
        workingDir = base
        inited = true
    }

    @Override
    void cleanup() {
        git.close()
    }


    @Override
    ScmExportSynchState getStatus() {
        return null
    }

    @Override
    JobState getJobStatus(final JobImportReference job) {
        return null
    }

    @Override
    JobState getJobStatus(final JobImportReference job, final String originalPath) {
        return null
    }


    @Override
    BasicInputView getInputViewForAction(final String actionId) {
        return actions[actionId]?.getInputView(this)
    }

    @Override
    List<Action> actionsAvailableForContext(final Map<String, String> context) {
        if (context.project) {
            //project-level actions
            if (!trackedItemsSelected) {
                return [actions[ACTION_INITIALIZE_TRACKING]]
            } else {
                return [actions[ACTION_GIT_FETCH],actions[ACTION_IMPORT_ALL]]
            }
        }
        return null
    }

    File getLocalFileForJob(final JobReference job) {
        mapper.fileForJob(job)
    }

    @Override
    String getRelativePathForJob(final JobReference job) {
        relativePath(job)
    }

    String relativePath(File reference) {
        reference.absolutePath.substring(workingDir.getAbsolutePath().length() + 1)
    }

    String relativePath(JobReference reference) {
        relativePath(getLocalFileForJob(reference))
    }

    @Override
    ScmDiffResult getFileDiff(final JobExportReference job) {
        return null
    }

    @Override
    ScmDiffResult getFileDiff(final JobExportReference job, final String originalPath) {
        return null
    }

    @Override
    List<ScmImportTrackedItem> getTrackedItemsForAction(final String actionId) {
        if (actionId == ACTION_INITIALIZE_TRACKING) {
            if (!trackedItems) {
                List<ScmImportTrackedItem> found = []

                //walk the repo files and look for possible candidates
                walkTreePaths('HEAD^{tree}'){TreeWalk walk->
                    found << trackPath(walk.getPathString())
                }
                return found
            } else {
                //list
                return trackedItems.collect {
                    ScmImportTrackedItemBuilder.builder().id(it).iconName('glyphicon-file').build()
                }
            }
        }
        null
    }

    ScmImportTrackedItem trackPath(final String path) {
        ScmImportTrackedItemBuilder.builder().id(path).iconName('glyphicon-file').build()
    }
    void walkTreePaths(String ref,Closure callback){

        ObjectId head = repo.resolve ref
        def tree = new TreeWalk(repo)
        tree.addTree(head)
        tree.setRecursive(true)

        while (tree.next()) {
            callback(tree)
        }
        tree.release();
    }
}
