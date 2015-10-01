package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.plugins.views.Action
import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.plugins.scm.*
import org.apache.log4j.Logger
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.BranchTrackingStatus
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.PathFilterGroup
import org.rundeck.plugin.scm.git.imp.actions.FetchAction
import org.rundeck.plugin.scm.git.imp.actions.ImportJobs
import org.rundeck.plugin.scm.git.imp.actions.PullAction
import org.rundeck.plugin.scm.git.imp.actions.SetupTracking

/**
 * Import jobs via git
 */
class GitImportPlugin extends BaseGitPlugin implements ScmImportPlugin {
    static final Logger log = Logger.getLogger(GitImportPlugin)
    public static final String ACTION_INITIALIZE_TRACKING = 'initialize-tracking'
    public static final String ACTION_IMPORT_ALL = 'import-all'
    public static final String ACTION_PULL = 'remote-pull'
    boolean inited
    boolean trackedItemsSelected = false
    boolean useTrackingRegex = false
    String trackingRegex
    List<String> trackedItems = null
    /**
     * path -> commitId, tracks which commits were imported, if path has a newer commit ID, then
     * it needs to be imported.
     */
    Map<String, String> trackedImportedItems = Collections.synchronizedMap([:])

    protected Map<String, GitImportAction> actions = [:]

    GitImportPlugin(final Map<String, String> input, List<String> trackedItems, final String project) {
        super(input, project)
        this.trackedItems = trackedItems
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

                (ACTION_PULL) : new PullAction(
                        ACTION_PULL,
                        "Pull Remote Changes",
                        "Synch incoming changes from Remote"
                ),

        ]
    }

    void setup(final Map<String, Object> input) throws ScmPluginException {

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

        //todo store tracked items list
        SetupTracking.setupWithInput(this, this.trackedItems, input)

        inited = true
    }

    @Override
    void cleanup() {
        git.close()
    }


    @Override
    ScmImportSynchState getStatus() {
        return getStatusInternal(true)
    }


    GitImportSynchState getStatusInternal(Boolean performFetch) {
        //look for any unimported paths
        if (!trackedItemsSelected) {
            return null
        }

        if(performFetch){
            fetchFromRemote()
        }

        int importNeeded = 0
        int notFound = 0
        int deleted = 0
        Set<String> expected = new HashSet(trackedImportedItems.keySet())
        Set<String> newitems = new HashSet()
        walkTreePaths('HEAD^{tree}', true) { TreeWalk walk ->
            if (expected.contains(walk.getPathString())) {
                expected.remove(walk.getPathString())
            } else {
                newitems.add(walk.getPathString())
            }
            if (trackedItemNeedsImport(walk.getPathString())) {
                importNeeded++
            } else if (trackedItemIsUnknown(walk.getPathString())) {
                notFound++
            }
        }
        //find any paths we are tracking that are no longer present
        if (expected) {
            //deleted paths
            deleted = expected.size()
        }
        def state = new GitImportSynchState()
        state.importNeeded = importNeeded
        state.notFound = notFound
        state.deleted = deleted

        StringBuilder sb = new StringBuilder()

        //compare to tracked branch
        def bstat = BranchTrackingStatus.of(repo, branch)
        state.branchTrackingStatus = bstat
        if (bstat && bstat.behindCount > 0) {
            state.state = ImportSynchState.REFRESH_NEEDED
        } else if (importNeeded) {
            state.state = ImportSynchState.IMPORT_NEEDED
        } else if (notFound) {
            state.state = ImportSynchState.UNKNOWN
        } else if (deleted) {
            state.state = ImportSynchState.DELETE_NEEDED
        } else {
            state.state = ImportSynchState.CLEAN
        }

        if (bstat && bstat.behindCount > 0) {
            sb << "${bstat.behindCount} changes from remote need to be pulled"
        }
        if (importNeeded) {
            if (sb.length() > 0) {
                sb << ", "
            }
            sb << "${importNeeded} file(s) need to be imported"
        }
        if (notFound) {
            if (sb.length() > 0) {
                sb << ", "
            }
            sb << "${notFound} unimported file(s) found"
        }
        if (deleted) {
            if (sb.length() > 0) {
                sb << ", "
            }
            sb << "${deleted} tracked file(s) were deleted"
        }
        state.message = sb.toString()
        return state
    }

    private hasJobStatusCached(final JobScmReference job, final String originalPath) {
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

    private refreshJobStatus(final JobScmReference job, final String originalPath) {

        def previousImportCommit = job.scmImportMetadata?.commitId ? GitUtil.getCommit(
                repo,
                job.scmImportMetadata.commitId
        ) : null

        def path = relativePath(job)

        jobStateMap.remove(job.id)

        def jobstat = Collections.synchronizedMap([:])
        def latestCommit = GitUtil.lastCommitForPath repo, git, path

//        log.debug(debugStatus(status))
        ImportSynchState synchState = importSynchStateForStatus(job, latestCommit, path)
        if (job.scmImportMetadata?.commitId) {
            //update tracked commit info
            trackedImportedItems[path] = job.scmImportMetadata?.commitId
        }

        log.debug(
                "import job status: ${synchState} with meta ${job.scmImportMetadata}, version ${job.importVersion}/${job.version} commit ${latestCommit?.name}"
        )

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

        def ident = job.id + ':' + String.valueOf(job.version) + ':' + (latestCommit ? latestCommit.name : '')

        jobstat['ident'] = ident
        jobstat['id'] = job.id
        jobstat['version'] = job.version
        jobstat['synch'] = synchState
        jobstat['path'] = path
        if (previousImportCommit) {
            jobstat['commitId'] = previousImportCommit.name
            jobstat['commitMeta'] = GitUtil.metaForCommit(previousImportCommit)
        }
        log.debug("refreshJobStatus(${job.id}): ${jobstat}")

        jobStateMap[job.id] = jobstat

        jobstat
    }


    private ImportSynchState importSynchStateForStatus(
            JobScmReference job,
            RevCommit commit,
            String path
    )
    {
        if (!isTrackedPath(path) || !commit) {
            //not tracked
            return ImportSynchState.UNKNOWN
        }
        if (job.scmImportMetadata && job.scmImportMetadata.commitId == commit.name) {
            if (job.importVersion == job.version) {
                return ImportSynchState.CLEAN
            } else {
                log.debug("job version differs, fall back to content diff")
                //serialize job and determine if there is a difference
                if (contentDiffers(job, commit, path)) {
                    return ImportSynchState.IMPORT_NEEDED
                } else {
                    return ImportSynchState.CLEAN
                }
            }
        } else {
            if (job.scmImportMetadata && job.scmImportMetadata.commitId && commit) {
                //determine change between tracked commit ID and head commit, if available
                //i.e. detect if path was deleted
                def oldCommit = GitUtil.getCommit repo, job.scmImportMetadata.commitId
                def changes = GitUtil.listChanges(git, oldCommit.tree.name, commit.tree.name)
                def pathChanges = changes.findAll { it.oldPath == path || it.newPath == path }
                log.debug("Found changes for ${path}: " + pathChanges.collect { entry ->
                    "${entry.changeType} ${entry.oldPath}->${entry.newPath}"
                }.join("\n")
                )
                def found = pathChanges.find { it.oldPath == path }
                if (found && found.changeType == DiffEntry.ChangeType.DELETE) {
                    return ImportSynchState.DELETE_NEEDED
                } else if (found && found.changeType == DiffEntry.ChangeType.MODIFY) {
                    return ImportSynchState.IMPORT_NEEDED
                } else if (found && found.changeType == DiffEntry.ChangeType.RENAME) {
                    log.error("Rename detected from ${found.oldPath} to ${found.newPath}")
                    return ImportSynchState.IMPORT_NEEDED
                }
            }
            //different commit was imported previously, or job has been modified
            return ImportSynchState.IMPORT_NEEDED
        }
    }

    boolean contentDiffers(final JobScmReference job, RevCommit commit, final String path) {
        def currentJob = new ByteArrayOutputStream()
        job.jobSerializer.serialize(path.endsWith('.xml') ? 'xml' : 'yaml', currentJob)
        def id = lookupId(commit, path)
        if (!id) {
            return true
        }
        def diffCount = diffContent(null, currentJob.toByteArray(), getBytes(id))
        log.debug("diffContent: found ${diffCount} changes for ${path}")
        return diffCount > 0
    }

    @Override
    JobImportState getJobStatus(final JobScmReference job) {
        return getJobStatus(job, null)
    }

    @Override
    JobImportState getJobStatus(final JobScmReference job, final String originalPath) {
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
    Action getSetupAction(final Map<String, String> context) {
        return actions[ACTION_INITIALIZE_TRACKING]
    }

    @Override
    List<Action> actionsAvailableForContext(final Map<String, String> context) {
        if (context.project) {
            //project-level actions
            if (!trackedItemsSelected) {
                return [actions[ACTION_INITIALIZE_TRACKING]]
            } else {

                def avail = []
                def status = getStatusInternal(false)
                if( status.state == ImportSynchState.REFRESH_NEEDED){
                    avail << actions[ACTION_PULL]
                }
                if (status.state != ImportSynchState.CLEAN) {
                    avail << actions[ACTION_IMPORT_ALL]
                }
                return avail
            }
        }
        return null
    }

    @Override
    String getRelativePathForJob(final JobReference job) {
        relativePath(job)
    }


    @Override
    ScmImportDiffResult getFileDiff(final JobScmReference job) {
        return getFileDiff(job, null)

    }

    @Override
    ScmImportDiffResult getFileDiff(final JobScmReference job, final String originalPath) {
        def path = originalPath ?: relativePath(job)
        def temp=serializeTemp(job, 'xml')
        def latestCommit = GitUtil.lastCommitForPath repo, git, path
        def id = latestCommit ? lookupId(latestCommit, path) : null
        if (!latestCommit || !id) {
            return new GitDiffResult(newNotFound: true)
        }
        def bytes = getBytes(id)
        def baos = new ByteArrayOutputStream()
        def diffs = diffContent(baos, temp, bytes)
        temp.delete()

        return new GitDiffResult(
                content: baos.toString(),
                modified: diffs > 0,
                incomingCommit: new GitScmCommit(GitUtil.metaForCommit(latestCommit))
        )
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
                    trackPath(it)
                }
            }
        } else if (actionId == ACTION_IMPORT_ALL) {

            List<ScmImportTrackedItem> found = []

            //walk the repo files and look for possible candidates
            walkTreePaths('HEAD^{tree}', true) { TreeWalk walk ->
                found << trackPath(walk.getPathString(), trackedItemNeedsImport(walk.getPathString()))
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

    ScmImportTrackedItem trackPath(final String path, final boolean selected = false) {
        ScmImportTrackedItemBuilder.builder().id(path).iconName('glyphicon-file').selected(selected).build()
    }

    void walkTreePaths(String ref, boolean useFilter = false, Closure callback) {
        ObjectId head = repo.resolve ref
        def tree = new TreeWalk(repo)
        tree.addTree(head)
        tree.setRecursive(true)
        if (useFilter) {
            if (isUseTrackingRegex()) {
                tree.setFilter(PathRegexFilter.create(trackingRegex))
            } else {
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
