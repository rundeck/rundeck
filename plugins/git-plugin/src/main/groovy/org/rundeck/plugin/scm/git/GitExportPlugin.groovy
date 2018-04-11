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

import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.core.plugins.views.Action
import com.dtolabs.rundeck.core.plugins.views.ActionBuilder
import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.plugins.scm.*
import org.apache.log4j.Logger
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.api.errors.JGitInternalException
import org.eclipse.jgit.lib.BranchTrackingStatus
import org.eclipse.jgit.revwalk.RevCommit
import org.rundeck.plugin.scm.git.config.Export
import org.rundeck.plugin.scm.git.exp.actions.CommitJobsAction
import org.rundeck.plugin.scm.git.exp.actions.FetchAction
import org.rundeck.plugin.scm.git.exp.actions.PushAction
import org.rundeck.plugin.scm.git.exp.actions.SynchAction
import org.rundeck.plugin.scm.git.exp.actions.TagAction

/**
 * Git export plugin
 */
class GitExportPlugin extends BaseGitPlugin implements ScmExportPlugin {
    static final Logger log = Logger.getLogger(GitExportPlugin)
    public static final String SERIALIZE_FORMAT = 'xml'

    public static final String JOB_COMMIT_ACTION_ID = "job-commit"
    public static final String PROJECT_COMMIT_ACTION_ID = "project-commit"
    public static final String PROJECT_PUSH_ACTION_ID = "project-push"
    public static final String PROJECT_TAG_ACTION_ID = "tag-commit"
    public static final String PROJECT_SYNCH_ACTION_ID = "project-synch"
    public static final String PROJECT_FETCH_ACTION_ID = "project-fetch"


    String format = SERIALIZE_FORMAT
    boolean inited = false
    String committerName;
    String committerEmail;
    Map<String, GitExportAction> actions = [:]
    Export config

    GitExportPlugin(Export config) {
        super(config)
        this.config = config
    }

    void initialize(ScmOperationContext context) {
        setup(context, config)
        actions = [
                (JOB_COMMIT_ACTION_ID)    : new CommitJobsAction(
                        JOB_COMMIT_ACTION_ID,
                        "Commit Changes to Git",
                        "Commit changes to local git repo."
                ),
                (PROJECT_COMMIT_ACTION_ID): new CommitJobsAction(
                        PROJECT_COMMIT_ACTION_ID,
                        "Commit Changes to Git",
                        "Commit changes to local git repo."
                ),
                (PROJECT_PUSH_ACTION_ID)  : new PushAction(
                        PROJECT_PUSH_ACTION_ID,
                        "Push to Remote",
                        "Push committed changes to the remote branch."
                ),
                (PROJECT_SYNCH_ACTION_ID) : new SynchAction(
                        PROJECT_SYNCH_ACTION_ID,
                        "Synch with Remote",
                        "Synch incoming changes from Remote"
                ),
                (PROJECT_FETCH_ACTION_ID) : new FetchAction(
                        PROJECT_FETCH_ACTION_ID,
                        "Fetch Remote Changes",
                        "Fetch changes from Remote for local comparison"
                ),
                (PROJECT_TAG_ACTION_ID)   : new TagAction(
                        PROJECT_TAG_ACTION_ID,
                        "Create Tag",
                        "Tag commit"
                ),

        ]
    }

    void setup(ScmOperationContext context, Export config) throws ScmPluginException {

        if (inited) {
            log.debug("already inited, not doing setup")
            return
        }

        format = config.format ?: 'xml'

        if (!(format in ['xml', 'yaml'])) {
            throw new IllegalArgumentException("format cannot be ${format}, must be one of: xml,yaml")
        }

        branch = config.branch
        committerName = config.committerName
        committerEmail = config.committerEmail
        File base = new File(config.dir)
        mapper = new TemplateJobFileMapper(expand(config.pathTemplate, [format: config.format], "config"), base)
        cloneOrCreate(context, base, config.url)

        workingDir = base
        inited = true
    }

    @Override
    void cleanup() {
        git?.close()
    }

    @Override
    void totalClean(){
        File base = new File(config.dir)
        base?.deleteDir()
    }


    @Override
    BasicInputView getInputViewForAction(final ScmOperationContext context, String actionId) {
        actions[actionId]?.getInputView(context, this)
    }


    @Override
    ScmExportResult export(
            final ScmOperationContext context,
            final String actionId,
            final Set<JobExportReference> jobs,
            final Set<String> pathsToDelete,
            final Map<String, String> input
    )
            throws ScmPluginException
    {
        if (!actions[actionId]) {
            throw new ScmPluginException("Unexpected action ID: " + actionId)
        }
        actions[actionId].perform(this, jobs, pathsToDelete, context, input)
    }


    @Override
    List<String> getDeletedFiles() {
        Status status = git.status().call()
        def set = new HashSet<String>(status.removed)
        set.addAll(status.missing)
        return set as List
    }

    protected List<Action> actionRefs(String... ids) {
        actions.subMap(Arrays.asList(ids)).values().collect { ActionBuilder.from(it) }
    }

    @Override
    List<Action> actionsAvailableForContext(ScmOperationContext context) {
        if (context.jobId) {
            //todo: get job status to determine actions
//            actionRefs JOB_COMMIT_ACTION_ID
            null
        } else if (context.frameworkProject) {
            //actions in project view
            def status = getStatusInternal(context, false)
            if (!status.gitStatus.clean) {
                actionRefs PROJECT_COMMIT_ACTION_ID
            } else if (status.state == SynchState.EXPORT_NEEDED) {
                //need a push
                actionRefs PROJECT_PUSH_ACTION_ID
            } else if (status.state == SynchState.REFRESH_NEEDED) {
                //need to fast forward
                actionRefs PROJECT_SYNCH_ACTION_ID
            } else if(!config.shouldFetchAutomatically()){
                actionRefs PROJECT_FETCH_ACTION_ID
            }else{
                null
            }
        } else {
            null
        }
    }


    @Override
    ScmExportSynchState getStatus(ScmOperationContext context) {
        return getStatusInternal(context, config.shouldFetchAutomatically())
    }


    GitExportSynchState getStatusInternal(ScmOperationContext context, boolean performFetch) {
        //perform fetch
        def msgs=[]
        boolean fetchError=false
        if (performFetch) {
            try {
                fetchFromRemote(context)
            } catch (Exception e) {
                fetchError=true
                msgs<<"Fetch from the repository failed: ${e.message}"
                logger.error("Failed fetch from the repository: ${e.message}")
                logger.debug("Failed fetch from the repository: ${e.message}", e)
            }
            if(config.shouldPullAutomatically()){
                try{
                    def pullResult = gitPull(context)
                    if(pullResult.successful){
                        logger.debug(pullResult.mergeResult?.toString())
                    }
                } catch (Exception e) {
                    msgs << "Automatic pull from the repository failed: ${e.message}"
                    logger.error("Failed automatic pull from the repository: ${e.message}")
                    logger.debug("Failed automatic pull from the repository: ${e.message}", e)
                }

            }
        }

        Status status = git.status().call()

        def synchState = new GitExportSynchState()
        synchState.gitStatus = status
        synchState.state = status.isClean() ? SynchState.CLEAN : SynchState.EXPORT_NEEDED
        if (!status.isClean()) {
            msgs<< "Some changes have not been committed"
        }

        //if clean, check remote tracking status
        if (status.isClean()) {
            def bstat = BranchTrackingStatus.of(repo, branch)
            if (bstat) {
                synchState.branchTrackingStatus = bstat
                if (bstat && bstat.aheadCount > 0 && bstat.behindCount > 0) {
                    msgs<< "${bstat.aheadCount} ahead and ${bstat.behindCount} behind remote branch"
                    synchState.state = SynchState.REFRESH_NEEDED
                    //TODO: test if merge would fail
                } else if (bstat && bstat.aheadCount > 0) {
                    msgs<< "${bstat.aheadCount} changes need to be pushed"
                    synchState.state = SynchState.EXPORT_NEEDED
                } else if (bstat && bstat.behindCount > 0) {
                    msgs<< "${bstat.behindCount} changes from remote need to be pulled"
                    synchState.state = SynchState.REFRESH_NEEDED
                }
            } else if (!remoteTrackingBranch()) {
                //if any paths exist, need to export
                def head = GitUtil.getHead(git.repository)
                if (head) {
                    //if no remote branch exists, i.e. bare repo, need to push local files
                    msgs<< "Changes need to be pushed"
                    synchState.state = SynchState.EXPORT_NEEDED
                }
            }
        }
        synchState.message=msgs? msgs.join(', ') : null
        if (fetchError && synchState.state == SynchState.CLEAN) {
            synchState.state = SynchState.REFRESH_NEEDED
        }

        return synchState
    }

    @Override
    JobState jobChanged(JobChangeEvent event, JobExportReference exportReference) {
        File origfile = mapper.fileForJob(event.originalJobReference)
        File outfile = mapper.fileForJob(exportReference)
        String origPath = null
        log.debug("Job event (${event}), writing to path: ${outfile}")
        switch (event.eventType) {
            case JobChangeEvent.JobChangeEventType.DELETE:
                origfile.delete()
                def status = refreshJobStatus(exportReference, origPath, false)
                jobStateMap.remove(exportReference.id)
                return createJobStatus(status, jobActionsForStatus(status))
                break;

            case JobChangeEvent.JobChangeEventType.MODIFY_RENAME:
                origPath = relativePath(event.originalJobReference)
            case JobChangeEvent.JobChangeEventType.CREATE:
            case JobChangeEvent.JobChangeEventType.MODIFY:
                if (origfile != outfile) {
                    origfile.delete()
                }
                try {
                    serialize(exportReference, format, config.exportPreserve, config.exportOriginal, outfile)
                } catch (Throwable t) {
                    getLogger().warn("Could not serialize job: ${t}", t)
                }
        }
        def status = refreshJobStatus(exportReference, origPath, false)
        return createJobStatus(status, jobActionsForStatus(status))
    }

    private hasJobStatusCached(final JobExportReference job, final String originalPath) {
        def path = relativePath(job)

        def commit = lastCommitForPath(path)

        String ident = createStatusCacheIdent(job, commit)

        if (jobStateMap[job.id] && jobStateMap[job.id].ident == ident) {
            log.debug("hasJobStatusCached(${ident}): FOUND for path $path")
            return jobStateMap[job.id]
        }
        log.debug("hasJobStatusCached(${ident}): (no) for path $path")

        null
    }

    private String createStatusCacheIdent(JobRevReference job, RevCommit commit) {
        def ident = job.id + ':' +
                String.valueOf(job.version) +
                ':' +
                (commit ? commit.name : '') +
                ":" +
                (getLocalFileForJob(job)?.exists())
        ident
    }

    private refreshJobStatus(final JobRevReference job, final String originalPath, boolean doSerialize = true) {

        def path = relativePath(job)

        jobStateMap.remove(job.id)

        def jobstat = Collections.synchronizedMap([:])
        def commit = lastCommitForPath(path)

        if (job instanceof JobExportReference && doSerialize) {
            serialize(job, format, config.exportPreserve, config.exportOriginal)
        }

        def statusb = git.status().addPath(path)
        if (originalPath) {
            statusb.addPath(originalPath)
        }
        Status status = statusb.call()
//        log.debug(debugStatus(status))
        SynchState synchState = synchStateForStatus(status, commit, path)
        def scmState = scmStateForStatus(status, commit, path)
        log.debug("for new path: commit ${commit}, synch: ${synchState}, scm: ${scmState}")

        if (originalPath) {
            def origCommit = lastCommitForPath(originalPath)
            SynchState osynchState = synchStateForStatus(status, origCommit, originalPath)
            def oscmState = scmStateForStatus(status, origCommit, originalPath)
            log.debug("for original path: commit ${origCommit}, synch: ${osynchState}, scm: ${oscmState}")
            if (origCommit && !commit) {
                commit = origCommit
            }
            if (synchState == SynchState.CREATE_NEEDED && oscmState == 'DELETED') {
                synchState = SynchState.EXPORT_NEEDED
            }
        }

        def ident = createStatusCacheIdent(job, commit)
//job.id + ':' + String.valueOf(job.version) + ':' + (commit ? commit.name : '')

        jobstat['ident'] = ident
        jobstat['id'] = job.id
        jobstat['version'] = job.version
        jobstat['synch'] = synchState
        jobstat['scm'] = scmState
        jobstat['path'] = path
        if (commit) {
            jobstat['commitId'] = commit.name
            jobstat['commitMeta'] = GitUtil.metaForCommit(commit)
        }
        log.debug("refreshJobStatus(${job.id}): ${jobstat}")

        jobStateMap[job.id] = jobstat

        jobstat
    }


    private SynchState synchStateForStatus(Status status, RevCommit commit, String path) {
        if (path && status.untracked.contains(path) || !path && status.untracked) {
            SynchState.CREATE_NEEDED
        } else if (path && status.uncommittedChanges.contains(path) || !path && status.uncommittedChanges) {
            SynchState.EXPORT_NEEDED
        } else if (commit) {
            SynchState.CLEAN
        } else {
            SynchState.CREATE_NEEDED
        }
    }

    def scmStateForStatus(Status status, RevCommit commit, String path) {
        if (!commit) {
            new File(workingDir, path).exists() ? 'NEW' : 'NOT_FOUND'
        } else if (path in status.added || path in status.untracked) {
            'NEW'
        } else if (path in status.changed || path in status.modified) {
            //changed== changes in index
            //modified == changes on disk
            'MODIFIED'
        } else if (path in status.removed || path in status.missing) {
            'DELETED'
        } else if (path in status.untracked) {
            'UNTRACKED'
        } else if (path in status.conflicting) {
            'CONFLICT'
        } else {
            'NOT_FOUND'
        }
    }

    @Override
    JobState getJobStatus(final JobExportReference job) {
        getJobStatus(job, null)
    }

    @Override
    JobState getJobStatus(final JobExportReference job, final String originalPath) {
        log.debug("getJobStatus(${job.id},${originalPath}): ${job}")
        if (!inited) {
            return null
        }
        def status = hasJobStatusCached(job, originalPath)
        if (!status) {
            status = refreshJobStatus(job, originalPath)
        }

        return createJobStatus(status, jobActionsForStatus(status))
    }

    @Override
    JobState getJobStatus(final JobExportReference job, final String originalPath, final boolean serialize) {
        log.debug("getJobStatus(${job.id},${originalPath}): ${job}, serialize ${serialize}")
        if (!inited) {
            return null
        }
        def status = hasJobStatusCached(job, originalPath)
        if (!status) {
            status = refreshJobStatus(job, originalPath,serialize)
        }
        return createJobStatus(status, jobActionsForStatus(status))
    }

    List<Action> jobActionsForStatus(Map status) {
        if (status.synch != SynchState.CLEAN) {
            actionRefs(JOB_COMMIT_ACTION_ID)
        } else {
            []
        }
    }

    @Override
    String getRelativePathForJob(final JobReference job) {
        relativePath(job)
    }


    ScmDiffResult getFileDiff(final JobExportReference job) throws ScmPluginException {
        return getFileDiff(job, null)
    }

    ScmDiffResult getFileDiff(final JobExportReference job, final String originalPath) throws ScmPluginException {
        def file = getLocalFileForJob(job)
        def path = originalPath ?: relativePath(job)
        serialize(job, format, config.exportPreserve, config.exportOriginal)

        def id = lookupId(getHead(), path)
        if (!id) {
            return new GitDiffResult(oldNotFound: true)
        }
        def bytes = getBytes(id)
        def baos = new ByteArrayOutputStream()
        def diffs = diffContent(baos, bytes, file)



        def availableActions = diffs > 0 ? [actions[JOB_COMMIT_ACTION_ID]] : null
        return new GitDiffResult(content: baos.toString(),
                                 modified: diffs > 0,
                                 actions: availableActions
        )
    }


    Map clusterFixJobs(final List<JobExportReference> jobs){
        def retSt = [:]
        retSt.deleted = []
        retSt.restored = []
        def toPull = false
        jobs.each { job ->
            def storedCommitId = ((JobScmReference)job).scmImportMetadata?.commitId
            def commitId = lastCommitForPath(getRelativePathForJob(job))
            def path = getRelativePathForJob(job)
            if(storedCommitId != null && commitId == null){
                //file to delete-pull
                git.rm().addFilepattern(path).call()
                toPull = true
                retSt.deleted.add(path)
            }else if(storedCommitId != null && commitId?.name != storedCommitId){
                git.checkout().addPath(path).call()
                toPull = true
                retSt.restored.add(job)
            }
        }
        Status status = git.status().call()
        if (status.isClean()) {
            //behind branch on deleted job
            def bstat = BranchTrackingStatus.of(repo, branch)
            if (bstat && bstat.behindCount > 0) {
                toPull = true
                retSt.behind = true
            }
        }
        if(toPull){
            retSt.pull = true
            try{
                git.pull().call()
            }catch (JGitInternalException e){
                retSt.error=e
            }catch(GitAPIException e){
                retSt.error = e
                log.info(e)
            }
        }

        try{
            jobs.each{job ->
                refreshJobStatus(job,null)
            }
        }catch (ScmPluginException e){
            retSt.error = e
        }

        retSt
    }


}