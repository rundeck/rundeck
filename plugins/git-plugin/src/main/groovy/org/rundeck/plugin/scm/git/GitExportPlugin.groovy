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
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.api.errors.JGitInternalException
import org.eclipse.jgit.lib.BranchTrackingStatus
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.WindowCacheConfig
import org.eclipse.jgit.util.FileUtils
import org.rundeck.plugin.scm.git.config.Export
import org.rundeck.plugin.scm.git.exp.actions.CommitJobsAction
import org.rundeck.plugin.scm.git.exp.actions.FetchAction
import org.rundeck.plugin.scm.git.exp.actions.PushAction
import org.rundeck.plugin.scm.git.exp.actions.SynchAction
import org.rundeck.plugin.scm.git.exp.actions.TagAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Git export plugin
 */
class GitExportPlugin extends BaseGitPlugin implements ScmExportPlugin {
    static final Logger log = LoggerFactory.getLogger(GitExportPlugin)
    public static final String SERIALIZE_FORMAT = 'xml'

    public static final String JOB_COMMIT_ACTION_ID = "job-commit"
    public static final String PROJECT_COMMIT_ACTION_ID = "project-commit"
    public static final String PROJECT_PUSH_ACTION_ID = "project-push"
    public static final String PROJECT_TAG_ACTION_ID = "tag-commit"
    public static final String PROJECT_SYNCH_ACTION_ID = "project-synch"
    public static final String PROJECT_FETCH_ACTION_ID = "project-fetch"
    public static final String PLUGIN_INTEGRATION = 'export'


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

        if (!(format in ['xml', 'yaml', 'json'])) {
            throw new IllegalArgumentException("format cannot be ${format}, must be one of: xml,yaml,json")
        }

        branch = config.branch
        committerName = config.committerName
        committerEmail = config.committerEmail
        File base = new File(config.dir)
        mapper = new TemplateJobFileMapper(expand(config.pathTemplate, [format: config.format], "config"), base)
        cloneOrCreate(context, base, config.url, PLUGIN_INTEGRATION)
        //check clone was ok
        if (git?.repository.getFullBranch() != "refs/heads/$branch") {
            logger.debug("branch differs")
            if(config.createBranch){
                if(config.baseBranch && existBranch("refs/remotes/${this.REMOTE_NAME}/${config.baseBranch}")){
                    createBranch(context, config.branch, config.baseBranch)
                    cloneOrCreate(context, base, config.url, PLUGIN_INTEGRATION)
                }else{
                    logger.debug("Non existent remote branch: ${config.baseBranch}")
                    throw new ScmPluginException("Non existent remote branch: ${config.baseBranch}")
                }
            }else{
                throw new ScmPluginException("Could not clone the remote branch: ${this.branch}, " +
                        "because it does not exist. To create it, you need to set the Create Branch option to true.")
            }
        }
        workingDir = base
        inited = true
    }

    @Override
    void cleanup() {
        git?.close()
    }

    @Override
    void totalClean(){
        repo?.close()
        git?.getRepository()?.close()
        git?.close()
        File base = new File(config.dir)

        if(base.exists()){
            try {
                if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                    //this operation forces a cache clean freeing any lock -> windows only issue!
                    WindowCacheConfig windowCacheConfig = new WindowCacheConfig()
                    windowCacheConfig.install()
                }
                FileUtils.delete(base, FileUtils.RECURSIVE | FileUtils.RETRY)
            } catch(IOException e){
                logger.error("Failed to delete repo folder ", e)
            }
        }

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

    protected List<Action> actionRefs(List ids) {
        actions.subMap(ids).values().collect { ActionBuilder.from(it) }
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
            def actions = []
            if (status.state == SynchState.LOADING) {
                return []
            } else if (!status.gitStatus.clean) {
                actions << PROJECT_COMMIT_ACTION_ID
            }else if (status.state == SynchState.EXPORT_NEEDED) {
                //need a push
                actions << PROJECT_PUSH_ACTION_ID
            } else if (status.state == SynchState.REFRESH_NEEDED) {
                //need to fast forward
                actions << PROJECT_SYNCH_ACTION_ID
            } else if(!config.shouldFetchAutomatically()){
                actions << PROJECT_FETCH_ACTION_ID
            }else{
                null
            }
            //It only checks for push action if no push or refresh is yet added to actions
            if((!actions || !actions.contains(PROJECT_PUSH_ACTION_ID)) && !actions.contains(PROJECT_SYNCH_ACTION_ID)){
                status = git.status().call()
                def synchState = new GitExportSynchState()
                synchState.gitStatus = status
                def bstat = BranchTrackingStatus.of(repo, branch)
                if (bstat && bstat.aheadCount > 0) {
                    actions << PROJECT_PUSH_ACTION_ID
                }
            }
            if(actions && !actions.isEmpty()){
                return actionRefs(actions)
            }else{
                return null
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

        def loadingStatus = jobStateMap.find {key, meta -> meta["synch"] == SynchState.LOADING }

        if(loadingStatus){
            def synchState = new GitExportSynchState()
            synchState.state = SynchState.LOADING
            return synchState
        }

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
        switch (event.eventType) {
            case JobChangeEvent.JobChangeEventType.DELETE:
                origfile.delete()
                def status = refreshJobStatus(exportReference, origPath, false)
                jobStateMap.remove(exportReference.id)
                resetFileCounterFor(outfile)
                return createJobStatus(status, jobActionsForStatus(status))
                break;

            case JobChangeEvent.JobChangeEventType.MODIFY_RENAME:
                origPath = relativePath(event.originalJobReference)
                resetFileCounterFor(origfile)
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
        def state = jobStateMap[job.id]

        if (state && state.synch == SynchState.LOADING) {
            return state
        }

        if(originalPath && originalPath!=path){
            //job renamed can lost last commit track, so not check it again
            if(state && state.synch == SynchState.EXPORT_NEEDED){
                return state
            }
        }

        def commit = lastCommitForPath(path)
        String ident = createStatusCacheIdent(job, commit)

        if (state && state.ident == ident) {
            return state
        }

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


        //check if local commit has changed from the stored status
        def storedCommitId = ((JobScmReference)job).scmImportMetadata?.commitId
        if(storedCommitId != null && commit == null){
            fileSerializeRevisionCounter.remove(mapper.fileForJob(job))
        }else if(storedCommitId != null && commit?.name != storedCommitId){
            fileSerializeRevisionCounter.remove(mapper.fileForJob(job))
        }

        if (job instanceof JobExportReference && doSerialize) {
            serialize(job, format, config.exportPreserve, config.exportOriginal)
        }

        def statusb = git.status().addPath(path)
        if (originalPath) {
            statusb.addPath(originalPath)
        }
        Status status = statusb.call()
        SynchState synchState = synchStateForStatus(status, commit, path)
        def scmState = scmStateForStatus(status, commit, path)

        if (originalPath) {
            def origCommit = lastCommitForPath(originalPath)
            SynchState osynchState = synchStateForStatus(status, origCommit, originalPath)
            def oscmState = scmStateForStatus(status, origCommit, originalPath)
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
        getJobStatus(job, originalPath, true)
    }

    @Override
    JobState getJobStatus(final JobExportReference job, final String originalPath, final boolean serialize) {
        if (!inited) {
            return null
        }
        def status = hasJobStatusCached(job, originalPath)

        if (!status) {
            status = refreshJobStatus(job, originalPath,serialize)
        }

        return createJobStatus(status, jobActionsForStatus(status))
    }

    @Override
    JobState getJobStatus(ScmOperationContext ctx, JobExportReference job, String originalPath) {
        log.debug(ScmAuthMessages.CHECKING.getMessage())
        def userStorageTree = ctx.getStorageTree()
        def scmAuthPath = commonConfig?.sshPrivateKeyPath ? commonConfig?.sshPrivateKeyPath : commonConfig?.gitPasswordPath
        def expandedAuthPath = expandContextVarsInPath(ctx, scmAuthPath)
        if( expandedAuthPath !== null && userStorageTree.hasPath(expandedAuthPath) ){
            log.debug(ScmAuthMessages.HAS_ACCESS.getMessage())
            return getJobStatus(job, originalPath)
        }else{
            def scmExceptionMessage = ScmAuthMessages.NO_ACCESS.getMessage()
            throw new ScmPluginException(scmExceptionMessage)
        }
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


    Map clusterFixJobs(ScmOperationContext context, final List<JobExportReference> jobs, final Map<String,String> originalPaths){
        //force fetch
        fetchFromRemote(context)

        def retSt = [:]
        List<JobExportReference> refreshJobCache = []

        retSt.deleted = []
        retSt.restored = []
        def toPull = false

        //behind branch on deleted job
        def bstat = BranchTrackingStatus.of(repo, branch)
        if (bstat && bstat.behindCount > 0) {
            toPull = true
            retSt.behind = true
        }

        if(toPull){
            jobs.each { job ->
                def storedCommitId = ((JobScmReference)job).scmImportMetadata?.commitId
                def path = getRelativePathForJob(job)
                def commitId = lastCommitForPath(path)

                if(storedCommitId != null && commitId == null){
                    //file to delete-pull
                    git.rm().addFilepattern(path).call()
                    retSt.deleted.add(path)
                    refreshJobCache.add(job)
                }else if(storedCommitId != null && commitId?.name != storedCommitId){
                    if(toPull){
                        git.checkout().addPath(path).call()
                    }
                    retSt.restored.add(job)
                    refreshJobCache.add(job)
                }
            }

            retSt.pull = true
            try{
                gitPull(context)
            }catch (JGitInternalException e){
                retSt.error=e
            }catch(GitAPIException e){
                retSt.error = e
                log.info("Git error",e)
            }
        }

        try{
            refreshJobCache.each{job ->
                refreshJobStatus(job, originalPaths?.get(job.id))
            }
        }catch (ScmPluginException e){
            retSt.error = e
        }

        retSt
    }

    def cleanJobStatusCache(Set<JobExportReference> jobs){
        if (!inited) {
            return null
        }

        jobs.each {job->
            log.debug("cleanJobStatusCache(${job.id}): ${job}")

            def status = hasJobStatusCached(job, null)

            if (status) {
                refreshJobStatus(job, null,false)
            }
        }
    }

    @Override
    void initJobsStatus(List<JobExportReference> jobs) {
        jobs.each {job->
            if(!jobStateMap[job.id]){
                def jobstat = initJobStatus(job)
                jobStateMap[job.id] = jobstat
            }

        }
    }

    private Map initJobStatus(final JobRevReference job) {
        def jobstat = Collections.synchronizedMap([:])
        jobstat['synch'] = SynchState.LOADING
        jobstat['id'] = job.id
        jobstat['version'] = job.version
        return jobstat
    }

    @Override
    void refreshJobsStatus(List<JobExportReference> jobs){
        jobs.each{job ->
            refreshJobStatus(job,null)
        }
    }

    @Override
    String getExportPushActionId(){
        return PROJECT_PUSH_ACTION_ID
    }

}
