package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.plugins.views.Action
import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.plugins.scm.*
import org.apache.log4j.Logger
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.OrTreeFilter
import org.eclipse.jgit.treewalk.filter.PathFilter
import org.eclipse.jgit.treewalk.filter.PathFilterGroup
import org.rundeck.plugin.scm.git.imp.actions.FetchAction
import org.rundeck.plugin.scm.git.imp.actions.ImportJobs
import org.rundeck.plugin.scm.git.imp.actions.SetupTracking

/**
 * Import jobs via git
 */
class GitImportPlugin extends BaseGitPlugin implements ScmImportPlugin {
    static final Logger log = Logger.getLogger(GitImportPlugin)
    public static final String ACTION_INITIALIZE_TRACKING = 'initialize_tracking'
    public static final String ACTION_IMPORT_ALL = 'import-all'
    public static final String ACTION_GIT_FETCH = 'git-fetch'
    boolean inited
    boolean trackedItemsSelected = false
    boolean useTrackingRegex = false
    String trackingRegex
    List<String> trackedItems = null
    /**
     * path -> commitId, tracks which commits were imported, if path has a newer commit ID, then
     * it needs to be imported.
     */
    Map<String,String> trackedImportedItems = Collections.synchronizedMap([:])

    protected Map<String, GitImportAction> actions = [:]

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
                        "Import Remote Changes",
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
    ScmImportSynchState getStatus() {
        //look for any unimported paths
        if(!trackedItemsSelected){
            return null
        }
        int importNeeded=0
        int notFound=0
        walkTreePaths('HEAD^{tree}',true) { TreeWalk walk ->
            if(trackedItemNeedsImport(walk.getPathString())){
                importNeeded++
            }else if(trackedItemIsUnknown(walk.getPathString())){
                notFound++
            }
        }
        def state = new GitImportSynchState()
        state.state = importNeeded ? ImportSynchState.IMPORT_NEEDED :
                notFound ? ImportSynchState.UNKNOWN : ImportSynchState.CLEAN
        StringBuilder sb = new StringBuilder()
        if(importNeeded){
            sb<<"${importNeeded} items need to be imported"
        }
        if(notFound){
            if(sb.length()>0) {
                sb << ", "
            }
            sb << "${notFound} unimported files found"
        }
        state.message = sb.toString()
        return state
    }

    private hasJobStatusCached(final JobImportReference job, final String originalPath) {
//        def path = relativePath(job)
//
//        def commit = GitUtil.lastCommitForPath repo, git, path
//
//        def ident = job.id + ':' + String.valueOf(job.version) + ':' + (commit ? commit.name : '')
//
//        if (jobStateMap[job.id] && jobStateMap[job.id].ident == ident) {
//            log.debug("hasJobStatusCached(${job.id}): FOUND")
//            return jobStateMap[job.id]
//        }
//        log.debug("hasJobStatusCached(${job.id}): (no)")

        null
    }

    private refreshJobStatus(final JobImportReference job, final String originalPath) {

        def path = relativePath(job)

        jobStateMap.remove(job.id)

        def jobstat = Collections.synchronizedMap([:])
        def commit = GitUtil.lastCommitForPath repo, git, path

//        log.debug(debugStatus(status))
        ImportSynchState synchState = importSynchStateForStatus(job, commit, path)
        log.debug("import job status: ${synchState} with meta ${job.scmImportMetadata}, commit ${commit?.name}")

//        if (originalPath) {
//            def origCommit = GitUtil.lastCommitForPath repo, git, originalPath
//            SynchState osynchState = synchStateForStatus(status, origCommit, originalPath)
//            def oscmState = scmStateForStatus(status, origCommit, originalPath)
//            log.debug("for original path: commit ${origCommit}, synch: ${osynchState}, scm: ${oscmState}")
//            if (origCommit && !commit) {
//                commit = origCommit
//            }
//            if (synchState == SynchState.CREATE_NEEDED && oscmState == 'DELETED') {
//                synchState = SynchState.EXPORT_NEEDED
//            }
//        }

        def ident = job.id + ':' + String.valueOf(job.version) + ':' + (commit ? commit.name : '')

        jobstat['ident'] = ident
        jobstat['id'] = job.id
        jobstat['version'] = job.version
        jobstat['synch'] = synchState
        jobstat['path'] = path
        if (commit) {
            jobstat['commitId'] = commit.name
            jobstat['commitMeta'] = GitUtil.metaForCommit(commit)
        }
        log.debug("refreshJobStatus(${job.id}): ${jobstat}")

        jobStateMap[job.id] = jobstat

        jobstat
    }


    private ImportSynchState importSynchStateForStatus(
            JobImportReference job,
            RevCommit commit,
            String path
    )
    {
        if (!isTrackedPath(path) || !commit) {
            //not tracked
            return ImportSynchState.UNKNOWN
        }
        if (job.scmImportMetadata && job.scmImportMetadata.commitId ==
                commit.name &&
                job.scmImportMetadata.version ==
                job.version) {
            return ImportSynchState.CLEAN
        } else {
            //different commit was imported previously, or job has been modified
            return ImportSynchState.IMPORT_NEEDED
        }
    }

    @Override
    JobImportState getJobStatus(final JobImportReference job) {
        return getJobStatus(job, null)
    }

    @Override
    JobImportState getJobStatus(final JobImportReference job, final String originalPath) {
        log.debug("getJobStatus(${job.id},${originalPath})")
        def status = hasJobStatusCached(job, originalPath)
        if (!status) {
            status = refreshJobStatus(job, originalPath)
        }
        return createJobImportStatus(status)
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
                return [actions[ACTION_GIT_FETCH], actions[ACTION_IMPORT_ALL]]
            }
        }
        return null
    }

    @Override
    String getRelativePathForJob(final JobReference job) {
        relativePath(job)
    }


    @Override
    ScmDiffResult getFileDiff(final JobScmReference job) {
        return getFileDiff(job, null)

    }

    @Override
    ScmDiffResult getFileDiff(final JobScmReference job, final String originalPath) {
        def file = getLocalFileForJob(job)
        def path = originalPath ?: relativePath(job)
        serialize(job)

        def id = lookupId(getHead(), path)
        if (!id) {
            return new GitDiffResult(oldNotFound: true)
        }
        def bytes = getBytes(id)
        def baos = new ByteArrayOutputStream()
        def diffs = printDiff(baos, file, bytes)


        return new GitDiffResult(content: baos.toString(), modified: diffs > 0)
    }

    @Override
    List<ScmImportTrackedItem> getTrackedItemsForAction(final String actionId) {
        if (actionId == ACTION_INITIALIZE_TRACKING) {
            if (!trackedItems) {
                List<ScmImportTrackedItem> found = []

                //walk the repo files and look for possible candidates
                walkTreePaths('HEAD^{tree}') { TreeWalk walk ->
                    found << trackPath(walk.getPathString())
                }
                return found
            } else {
                //list
                return trackedItems.collect {
                    ScmImportTrackedItemBuilder.builder().id(it).iconName('glyphicon-file').build()
                }
            }
        } else if (actionId == ACTION_IMPORT_ALL) {

            List<ScmImportTrackedItem> found = []

            //walk the repo files and look for possible candidates
            //TODO: modified items only
            walkTreePaths('HEAD^{tree}') { TreeWalk walk ->
                if (isTrackedPath(walk.getPathString())) {
                    found << trackPath(walk.getPathString())
                }
            }
            return found
        }
        null
    }

    /**
     * Return true if the path last imported commit does not match the latest commit
     * @param path
     * @return true if changes need to be imported
     */
    private boolean trackedItemNeedsImport(String path) {
        def commit = lastCommitForPath(path)
        commit.name != trackedImportedItems[path]
    }
    /**
     * Return true if the path has not been imported
     * @param path
     * @return true if path is not imported
     */
    private boolean trackedItemIsUnknown(String path) {
        !trackedImportedItems[path]
    }

    ScmImportTrackedItem trackPath(final String path, final boolean selected=false) {
        ScmImportTrackedItemBuilder.builder().id(path).iconName('glyphicon-file').selected(selected).build()
    }

    void walkTreePaths(String ref, boolean useFilter=false, Closure callback) {
        ObjectId head = repo.resolve ref
        def tree = new TreeWalk(repo)
        tree.addTree(head)
        tree.setRecursive(true)
        if(useFilter){
            if(isUseTrackingRegex()){
                tree.setFilter(PathRegexFilter.create(trackingRegex))
            }else{
                tree.setFilter(PathFilterGroup.createFromStrings(trackedItems))
            }
        }

        while (tree.next()) {
            callback(tree)
        }
        tree.release();
    }

    boolean isTrackedPath(final String path) {
        return trackedItems?.contains(path) || isUseTrackingRegex() && trackingRegex && path.matches(trackingRegex)
    }
}
