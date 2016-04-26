package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.plugins.views.Action
import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.plugins.scm.*
import org.apache.log4j.Logger
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.BranchTrackingStatus
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.PathFilterGroup
import org.rundeck.plugin.scm.git.config.Import
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
    Import config
    /**
     * path -> commitId, tracks which commits were imported, if path has a newer commit ID, then
     * it needs to be imported.
     */
    ImportTracker importTracker = new ImportTracker()

    protected Map<String, GitImportAction> actions = [:]

    GitImportPlugin(final Import config, List<String> trackedItems) {
        super(config)
        this.config=config
        this.trackedItems = trackedItems
    }

    @Override
    ScmExportResult scmImport(
            final ScmOperationContext context,
            final String actionId,
            final JobImporter importer,
            final List<String> selectedPaths,
            final Map<String, String> input
    ) throws ScmPluginException
    {
        return actions[actionId]?.performAction(context, this, importer, selectedPaths, input)
    }

    void initialize(final ScmOperationContext context) {
        setup(context)
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

                (ACTION_PULL)               : new PullAction(
                        ACTION_PULL,
                        "Pull Remote Changes",
                        "Synch incoming changes from Remote"
                ),

        ]
    }

    void setup(final ScmOperationContext context) throws ScmPluginException {

        if (inited) {
            log.debug("already inited, not doing setup")
            return
        }

        File base = new File(config.dir)
        mapper = new TemplateJobFileMapper(expand(config.pathTemplate, [format: config.format], "config"), base)

        this.branch = config.branch
        cloneOrCreate(context, base, config.url)

        workingDir = base

        SetupTracking.setupWithInput(this, this.trackedItems, config.rawInput)

        inited = true
    }


    @Override
    void cleanup() {
        git.close()
    }


    @Override
    ScmImportSynchState getStatus(ScmOperationContext context) {
        return getStatusInternal(context, true)
    }


    GitImportSynchState getStatusInternal(ScmOperationContext context, boolean performFetch) {
        //look for any unimported paths
        if (!trackedItemsSelected) {
            return null
        }

        def msgs = []
        if (performFetch) {
            try {
                fetchFromRemote(context)
            } catch (Exception e) {
                msgs<<"Fetch from the repository failed: ${e.message}"
                logger.error("Failed fetch from the repository: ${e.message}")
                logger.debug("Failed fetch from the repository: ${e.message}", e)
            }
        }

        int importNeeded = 0
        int notFound = 0
        int deleted = 0
        log.debug("import tracker: ${importTracker}")
        Set<String> expected = new HashSet(importTracker.trackedPaths())
        Set<String> newitems = new HashSet()
        Set<String> renamed = new HashSet()
        walkTreePaths('HEAD^{tree}', true) { TreeWalk walk ->
            if (expected.contains(walk.getPathString())) {
                //saw an existing tracked item
                expected.remove(walk.getPathString())
                if (trackedItemNeedsImport(walk.getPathString())) {
                    importNeeded++
                }
            } else if (importTracker.wasRenamed(walk.getPathString())) {
                //item is tracked to a job which was renamed
                expected.remove(importTracker.renamedValue(walk.getPathString()))
                renamed.add(walk.getPathString())
            } else if (importTracker.trackedItemIsUnknown(walk.getPathString())) {
                //path is new and needs import
                newitems.add(walk.getPathString())
                notFound++
            }
        }
        //find any paths we are tracking that are no longer present
        if (expected) {
            //deleted paths
            deleted = expected.size()
            log.debug("deleted files ${expected}")
        }
        def state = new GitImportSynchState()
        state.importNeeded = importNeeded
        state.notFound = notFound
        state.deleted = deleted

        //compare to tracked branch
        def bstat = BranchTrackingStatus.of(repo, branch)
        state.branchTrackingStatus = bstat
        if (bstat && bstat.behindCount > 0) {
            state.state = ImportSynchState.REFRESH_NEEDED
        } else if (importNeeded || renamed || notFound) {
            state.state = ImportSynchState.IMPORT_NEEDED
        } else if (deleted) {
            state.state = ImportSynchState.DELETE_NEEDED
        } else {
            state.state = ImportSynchState.CLEAN
        }

        if (bstat && bstat.behindCount > 0) {
            msgs << "${bstat.behindCount} changes from remote need to be pulled"
        }
        if (importNeeded) {
            msgs << "${importNeeded} file(s) need to be imported"
        }
        if (renamed) {
            msgs << "${renamed.size()} file(s) were renamed"
        }
        if (notFound) {
            msgs << "${notFound} unimported file(s) found"
        }
        if (deleted) {
            msgs << "${deleted} tracked file(s) were deleted"
        }
        state.message = msgs.join(', ')
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


        if (originalPath && synchState == ImportSynchState.UNKNOWN) {
            //job was renamed but not file
            synchState = ImportSynchState.IMPORT_NEEDED
        } else if (job.scmImportMetadata?.commitId) {
            //update tracked commit info
            importTracker.trackJobAtPath(job, path)
        }
        log.debug(
                "import job status: ${synchState} with meta ${job.scmImportMetadata}, version ${job.importVersion}/${job.version} commit ${latestCommit?.name}"
        )

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
    JobImportState getJobStatus(final JobScmReference job, String originalPath) {
        log.debug("getJobStatus(${job.id},${originalPath})")
        def path = relativePath(job)
        if (null == originalPath) {
            originalPath = importTracker.originalValue(path)
        }
        def status = hasJobStatusCached(job, originalPath)
        if (!status) {
            status = refreshJobStatus(job, originalPath)
        }
        return createJobImportStatus(status,jobActionsForStatus(status))
    }

    List<Action> jobActionsForStatus(Map status) {
        if (status.synch == ImportSynchState.IMPORT_NEEDED) {
            [actions[ACTION_IMPORT_ALL]]
        } else {
            []
        }
    }


    @Override
    JobImportState jobChanged(JobChangeEvent event, JobScmReference reference) {
        def path = relativePath(event.originalJobReference)
        def newpath = relativePath(event.jobReference)
        String origPath = null
        if (!isTrackedPath(path) && !isTrackedPath(newpath)) {
            return null
        }
        log.debug("Job event (${event.eventType}), path: ${path}")
        switch (event.eventType) {
            case JobChangeEvent.JobChangeEventType.DELETE:
                importTracker.untrackPath(path)

                def status = [synch: ImportSynchState.IMPORT_NEEDED]
                return createJobImportStatus(status,jobActionsForStatus(status))
                break;

            case JobChangeEvent.JobChangeEventType.MODIFY_RENAME:
                importTracker.jobRenamed(reference, path, newpath)
        //TODO
//            case JobChangeEvent.JobChangeEventType.CREATE:
//            case JobChangeEvent.JobChangeEventType.MODIFY:

        }
//        def status = refreshJobStatus(reference, origPath)
//        return createJobImportStatus(status)
        null
    }


    @Override
    BasicInputView getInputViewForAction(final ScmOperationContext context, final String actionId) {
        return actions[actionId]?.getInputView(context, this)
    }

    @Override
    Action getSetupAction(ScmOperationContext context) {
        if (!trackedItemsSelected) {
            return actions[ACTION_INITIALIZE_TRACKING]
        }
        null
    }

    @Override
    List<Action> actionsAvailableForContext(ScmOperationContext context) {
        if (context.frameworkProject) {
            //project-level actions
            if (!trackedItemsSelected) {
                return [actions[ACTION_INITIALIZE_TRACKING]]
            } else {

                def avail = []
                def status = getStatusInternal(context, false)
                if (status.state == ImportSynchState.REFRESH_NEEDED) {
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
    ScmImportDiffResult getFileDiff(final JobScmReference job, String originalPath) {
        def path = relativePath(job)
        if (!originalPath) {
            originalPath = importTracker.originalValue(path)
        }
        path = originalPath ?: relativePath(job)
        def temp = serializeTemp(job, config.format)
        def latestCommit = GitUtil.lastCommitForPath repo, git, path
        def id = latestCommit ? lookupId(latestCommit, path) : null
        if (!latestCommit || !id) {
            return new GitDiffResult(newNotFound: true)
        }
        def bytes = getBytes(id)
        def baos = new ByteArrayOutputStream()
        def diffs = diffContent(baos, temp, bytes)
        temp.delete()


        def availableActions = diffs > 0 ? [actions[ACTION_IMPORT_ALL]] : null
        return new GitDiffResult(
                content: baos.toString(),
                modified: diffs > 0,
                incomingCommit: new GitScmCommit(GitUtil.metaForCommit(latestCommit)),
                actions: availableActions
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
                    trackPath(it, false, importTracker.trackedJob(it))
                }
            }
        } else if (actionId == ACTION_IMPORT_ALL) {

            List<ScmImportTrackedItem> found = []

            //walk the repo files and look for possible candidates
            walkTreePaths('HEAD^{tree}', true) { TreeWalk walk ->
                found << trackPath(
                        walk.getPathString(),
                        trackedItemNeedsImport(walk.getPathString()),
                        importTracker.trackedJob(walk.getPathString())
                )
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
        commit.name != importTracker.trackedCommit(path)
    }


    ScmImportTrackedItem trackPath(final String path, final boolean selected = false, String jobId = null) {
        ScmImportTrackedItemBuilder.builder().
                id(path).
                iconName('glyphicon-file').
                selected(selected).
                jobId(jobId).
                build()
    }

    void walkTreePaths(String ref, boolean useFilter = false, Closure callback) {
        ObjectId head = repo.resolve ref
        if(!head){
            return
        }
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
