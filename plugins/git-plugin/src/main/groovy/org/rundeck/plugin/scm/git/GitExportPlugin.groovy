package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.core.jobs.JobExportReference
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.plugins.scm.*
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import org.apache.log4j.Logger
import org.eclipse.jgit.api.*
import org.eclipse.jgit.diff.DiffAlgorithm
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.EditList
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.diff.RawTextComparator
import org.eclipse.jgit.lib.BranchTrackingStatus
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.FileMode
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.TrackingRefUpdate
import org.eclipse.jgit.treewalk.TreeWalk

import java.util.regex.Pattern

/**
 * Git export plugin
 */
class GitExportPlugin implements ScmExportPlugin {
    static final Logger log = Logger.getLogger(GitExportPlugin)
    public static final String SERIALIZE_FORMAT = 'xml'
    String format = SERIALIZE_FORMAT
    boolean inited = false
    Git git;
    Repository repo;
    File workingDir;
    String committerName;
    String committerEmail;
    String branch;
    final Map<String, ?> input
    final String project
    JobFileMapper mapper;
    Map<String, Map> jobStateMap = Collections.synchronizedMap([:])

    RawTextComparator COMP = RawTextComparator.DEFAULT

    GitExportPlugin(final Map<String, ?> input, final String project) {
        this.input = input
        this.project = project
    }

    void initialize() {
        setup(input)
    }

    boolean isSetup() {
        return inited
    }

    void setup(final Map<String, ?> input) throws ScmPluginException {

        //TODO: using ssh http://stackoverflow.com/questions/23692747/specifying-ssh-key-for-jgit
        if (inited) {
            log.debug("already inited, not doing setup")
            return
        }

        GitExportPluginFactory.requiredProperties.each { key ->
            //verify input
            if (!input[key]) {
                throw new IllegalArgumentException("${key} cannot be null")
            }
        }

        format = input.format ?: 'xml'

        if (!(format in ['xml', 'yaml'])) {
            throw new IllegalArgumentException("format cannot be ${format}, must be one of: xml,yaml")
        }

        def dir = input.get("dir").toString()
        def branch = input.get("branch").toString()
        def pathTemplate = input.pathTemplate.toString()
        committerName = input.committerName.toString()
        committerEmail = input.committerEmail.toString()
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
    List<Property> getExportProperties(final Set<JobRevReference> jobs) {
        def status = getStatusInternal()
        if (!status.gitStatus.clean) {
            [
                    PropertyBuilder.builder().with {
                        string "commitMessage"
                        title "Commit Message"
                        description "Enter a commit message. Committing to branch: `" + branch + '`'
                        required true
                        renderingAsTextarea()
                        build()
                    },

                    PropertyBuilder.builder().with {
                        string "tagName"
                        title "Tag"
                        description "Enter a tag name to include, will be pushed with the branch."
                        required false
                        build()
                    },

                    PropertyBuilder.builder().with {
                        booleanType "push"
                        title "Push Remotely?"
                        description "Check to push to the remote"
                        required false
                        build()
                    },
            ]
        } else if (status.state == SynchState.EXPORT_NEEDED) {
            //need a push
            [
                    PropertyBuilder.builder().with {
                        string "status"
                        title "Export Status"
                        renderingOption StringRenderingConstants.DISPLAY_TYPE_KEY, StringRenderingConstants.DisplayType.STATIC_TEXT
                        renderingOption StringRenderingConstants.STATIC_TEXT_CONTENT_TYPE_KEY, "text/x-markdown"
                        defaultValue status.message + """

Pushing to remote branch: `${branch}`"""
                        build()
                    },

                    PropertyBuilder.builder().with {
                        booleanType "push"
                        title "Push Remotely?"
                        description "Check to push to the remote"
                        defaultValue "true"
                        required false
                        build()
                    },
            ]
        } else if (status.state == SynchState.REFRESH_NEEDED) {
            //need to fast forward
            def props = [
                    PropertyBuilder.builder().with {
                        string "status"
                        title "Git Status"
                        renderingOption StringRenderingConstants.DISPLAY_TYPE_KEY, StringRenderingConstants.DisplayType.STATIC_TEXT
                        renderingOption StringRenderingConstants.STATIC_TEXT_CONTENT_TYPE_KEY, "text/x-markdown"
                        defaultValue status.message + """

Pulling from remote branch: `${branch}`"""
                        build()
                    },
            ]
            if (status.branchTrackingStatus?.behindCount > 0 && status.branchTrackingStatus?.aheadCount > 0) {
                props.addAll([
                        PropertyBuilder.builder().with {
                            select "refresh"
                            title "Refresh Strategy"
                            description """Method to refresh local repo from remote

* `pull` - pull changes
* `rebase` - rebase local changes on top of remote
* `merge` - merge remote changes into local changes"""
                            values "pull", "rebase", "merge"
                            defaultValue "pull"
                            required true
                            build()
                        },
                        PropertyBuilder.builder().with {
                            select "resolution"
                            title "Conflict Resolution Strategy"
                            description """Method to resolve conflicts from remote

* `ours` - apply our changes over theirs
* `theirs` - apply their changes over ours"""
                            values "ours", "theirs"
                            defaultValue "ours"
                            required true
                            build()
                        },
                ]
                )

            }else if (status.branchTrackingStatus?.behindCount>0){

                props << PropertyBuilder.builder().with {
                    booleanType "pull"
                    title "Pull from Remote"
                    description "Check to pull changes from the remote"
                    defaultValue "true"
                    required true
                    build()
                }
            }
            props
        }else{
            []
        }
    }


    @Override
    ScmExportResult export(
            final Set<JobExportReference> jobs,
            final Set<String> pathsToDelete,
            final ScmUserInfo userInfo,
            final Map<String, Object> input
    )
            throws ScmPluginException
    {
        //determine action
        def internal = getStatusInternal()
        def localGitChanges = !internal.gitStatus.isClean()

        def dopush = input.push == 'true'
        RevCommit commit
        def result = new ScmExportResultImpl()

        if (localGitChanges) {
            if (!jobs && !pathsToDelete) {
                throw new ScmPluginException("No jobs or were selected")
            }
            if (!input.commitMessage) {
                throw new ScmPluginException("A commitMessage is required")
            }
            def commitIdentName = expand(committerName, userInfo)
            if (!commitIdentName) {
                ScmUserInfoMissing.fieldMissing("committerName")
            }
            def commitIdentEmail = expand(committerEmail, userInfo)
            if (!commitIdentEmail) {
                ScmUserInfoMissing.fieldMissing("committerEmail")
            }
            serializeAll(jobs)
            String commitMessage = input.commitMessage.toString()
            Status status = git.status().call()
            //add all changes to index
            if (jobs) {
                AddCommand addCommand = git.add()
                jobs.each {
                    addCommand.addFilepattern(relativePath(it))
                }
                addCommand.call()
            }
            def rmfiles = new HashSet<String>(status.removed + status.missing)
            def todelete = pathsToDelete.intersect(rmfiles)
            if (todelete) {
                def rm = git.rm()
                todelete.each {
                    rm.addFilepattern(it)
                }
                rm.call()
            }

            CommitCommand commit1 = git.commit().
                    setMessage(commitMessage).
                    setCommitter(commitIdentName, commitIdentEmail)
            jobs.each {
                commit1.setOnly(relativePath(it))
            }
            pathsToDelete.each {
                commit1.setOnly(it)
            }
            commit = commit1.call()
            result.success = true

        } else if (jobs || deletedFiles) {
            //no git changes, but some jobs were selected
            throw new ScmPluginException("No changes to local git repo need to be exported")
        } else {
            commit = getHead()
        }
        result.id = commit?.name


        if (dopush) {
            //todo: tag
            if (input.tag) {
                System.err.println("TODO: tag ${input.tag}")
            }
            if (input.push == 'true') {
                def pushb = git.push()

                pushb.setRemote("origin")
                pushb.add(branch)

//                pushb.add(input.tag) //todo: push tag

                def push = pushb.call()
                def sb = new StringBuilder()
                def updates = (push*.remoteUpdates).flatten()
                updates.each {
                    sb.append it.toString()
                }
                def failedUpdates = updates.findAll { it.status != RemoteRefUpdate.Status.OK }
                result.success = !failedUpdates
                if (failedUpdates) {
                    result.message = "Some updates failed: " + failedUpdates
                } else {
                    result.message = "Remote push result: OK"
                }
            }
        } else if (!localGitChanges) {
            result.success = true
            result.message = "Nothing happened"
        }
        result
    }

    static String expand(final String source, final ScmUserInfo scmUserInfo) {
        ['fullName', 'firstName', 'lastName', 'email', 'userName'].inject(source) { String x, String y ->
            return x.replaceAll(Pattern.quote('${user.' + y + '}'), scmUserInfo[y] ?: '')
        }
    }

    def serialize(final JobExportReference job) {
        File outfile = mapper.fileForJob(job)
        if (!outfile.parentFile.exists()) {
            if (!outfile.parentFile.mkdirs()) {
                throw new ScmPluginException(
                        "Cannot create necessary dirs to serialize file to path: ${outfile.absolutePath}"
                )
            }
        }
        outfile.withOutputStream { out ->
            job.jobSerializer.serialize(format, out)
        }
    }

    def serializeAll(final Set<JobExportReference> jobExportReferences) {
        jobExportReferences.each(this.&serialize)
    }

    @Override
    List<String> getDeletedFiles() {
        Status status = git.status().call()
        def set = new HashSet<String>(status.removed)
        set.addAll(status.missing)
        return set as List
    }

    @Override
    ScmExportSynchState getStatus() {
        return getStatusInternal()
    }


    GitSynchState getStatusInternal() {
        //fetch remote changes
        def fetchResult = git.fetch().call()

        def update = fetchResult.getTrackingRefUpdate("refs/remotes/origin/master")
        if(update) {
            println(update)
        }
        Status status = git.status().call()

        def synchState = new GitSynchState()
        synchState.gitStatus = status
        synchState.state = status.isClean() ? SynchState.CLEAN : SynchState.EXPORT_NEEDED
        if (!status.isClean()) {
            synchState.message = "Some changes have not been committed"
        }

        //if clean, check remote tracking status
        if (status.isClean()) {
            def bstat = BranchTrackingStatus.of(repo, branch)
            synchState.branchTrackingStatus = bstat
            if (bstat && bstat.aheadCount > 0 && bstat.behindCount > 0) {
                synchState.message = "${bstat.aheadCount} ahead and ${bstat.behindCount} behind remote branch"
                synchState.state = SynchState.REFRESH_NEEDED
                //TODO: test if merge would fail
            } else if (bstat && bstat.aheadCount > 0) {
                synchState.message = "${bstat.aheadCount} changes need to be pushed"
                synchState.state = SynchState.EXPORT_NEEDED
            } else if (bstat && bstat.behindCount > 0) {
                synchState.message = "${bstat.behindCount} changes from remote need to be pulled"
                synchState.state = SynchState.REFRESH_NEEDED
            }
        }


        return synchState
    }

    @Override
    JobState jobChanged(JobChangeEvent event, JobExportReference exportReference) {
        File origfile = mapper.fileForJob(event.originalJobReference)
        File outfile = mapper.fileForJob(event.jobReference)
        String origPath = null
        log.debug("Job event (${event}), writing to path: ${outfile}")
        switch (event.eventType) {
            case JobChangeEvent.JobChangeEventType.DELETE:
                origfile.delete()
                def status = refreshJobStatus(event.jobReference, origPath)
                return createJobStatus(status)
                break;

            case JobChangeEvent.JobChangeEventType.MODIFY_RENAME:
                origPath = relativePath(event.originalJobReference)
            case JobChangeEvent.JobChangeEventType.CREATE:
            case JobChangeEvent.JobChangeEventType.MODIFY:
                if (!origfile.getAbsolutePath().equals(outfile.getAbsolutePath())) {
                    origfile.delete()
                }
                if (!outfile.getParentFile().exists()) {
                    if (!outfile.getParentFile().mkdirs()) {
                        log.debug("Failed to create parent dirs for ${outfile}")
                    }
                }
                outfile.withOutputStream { out ->
                    exportReference.jobSerializer.serialize(format, out)
                }
        }
        def status = refreshJobStatus(exportReference, origPath)
        return createJobStatus(status)
    }

    private hasJobStatusCached(final JobExportReference job, final String originalPath) {
        def path = relativePath(job)

        def commit = lastCommitForPath(path)

        def ident = job.id + ':' + String.valueOf(job.version) + ':' + (commit ? commit.name : '')

        if (jobStateMap[job.id] && jobStateMap[job.id].ident == ident) {
            log.debug("hasJobStatusCached(${job.id}): FOUND")
            return jobStateMap[job.id]
        }
        log.debug("hasJobStatusCached(${job.id}): (no)")

        null
    }

    private refreshJobStatus(final JobRevReference job, final String originalPath) {

        def path = relativePath(job)

        jobStateMap.remove(job.id)

        def jobstat = Collections.synchronizedMap([:])
        def commit = lastCommitForPath(path)



        if (job instanceof JobExportReference) {
            serialize(job)
        }



        def statusb = git.status().addPath(path)
        if (originalPath) {
            statusb.addPath(originalPath)
        }
        Status status = statusb.call()
        log.debug(debugStatus(status))
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

        def ident = job.id + ':' + String.valueOf(job.version) + ':' + (commit ? commit.name : '')

        jobstat['ident'] = ident
        jobstat['id'] = job.id
        jobstat['version'] = job.version
        jobstat['synch'] = synchState
        jobstat['scm'] = scmState
        jobstat['path'] = path
        if (commit) {
            jobstat['commitId'] = commit.name
            jobstat['commitMeta'] = metaForCommit(commit)
        }
        log.debug("refreshJobStatus(${job.id}): ${jobstat}")

        jobStateMap[job.id] = jobstat

        jobstat
    }

    String debugStatus(final Status status) {
        def smap = [
                conflicting          : status.conflicting,
                added                : status.added,
                changed              : status.changed,
                clean                : status.clean,
                conflictingStageState: status.conflictingStageState,
                ignoredNotInIndex    : status.ignoredNotInIndex,
                missing              : status.missing,
                modified             : status.modified,
                removed              : status.removed,
                uncommittedChanges   : status.uncommittedChanges,
                untracked            : status.untracked,
                untrackedFolders     : status.untrackedFolders,
        ]
        def sb = new StringBuilder()
        smap.each {
            sb << "${it.key}:\n"
            it.value.each {
                sb << "\t${it}\n"
            }
        }
        sb.toString()
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
        log.debug("getJobStatus(${job.id},${originalPath})")
        if (!inited) {
            return null
        }
        def status = hasJobStatusCached(job, originalPath)
        if (!status) {
            status = refreshJobStatus(job, originalPath)
        }
        return createJobStatus(status)
    }

    JobState createJobStatus(final Map map) {
        //TODO: include scm status
        return new JobGitState(
                synchState: map['synch'],
                commit: map.commitMeta ? new GitScmCommit(map.commitMeta) : null
        )
    }

    private Map<String, Serializable> metaForCommit(RevCommit commit) {
        [
                commitId      : commit.name,
                commitId6     : commit.abbreviate(6).name(),
                date          : new Date(commit.commitTime * 1000L),
                authorName    : commit.authorIdent.name,
                authorEmail   : commit.authorIdent.emailAddress,
                authorTime    : commit.authorIdent.when,
                authorTimeZone: commit.authorIdent.timeZone.displayName,
                message       : commit.shortMessage
        ]
    }

    RevCommit lastCommit() {
        lastCommitForPath(null)
    }

    RevCommit lastCommitForPath(String path) {
        def head = getHead()
        if (!head) {
            return null
        }
        def logb = git.log()
        if (path) {
            logb.addPath(path)
        }
        def log = logb.call()
        def iter = log.iterator()
        if (iter.hasNext()) {
            def commit = iter.next()
            if (commit) {
                return commit
            }
        }
        null
    }

    @Override
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

    ScmDiffResult getFileDiff(final JobExportReference job) throws ScmPluginException {
        return getFileDiff(job, null)
    }

    ScmDiffResult getFileDiff(final JobExportReference job, final String originalPath) throws ScmPluginException {
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

    /**
     * get RevCommit for HEAD rev of the path
     * @return RevCommit or null if HEAD not found (empty git)
     */
    RevCommit getHead() {

        final RevWalk walk = new RevWalk(repo);
        walk.setRetainBody(true);

        def resolve = repo.resolve(Constants.HEAD)
        if (!resolve) {
            return null
        }
        final RevCommit headCommit = walk.parseCommit(resolve);
        walk.release()
        headCommit
    }

    ObjectId lookupId(RevCommit commit, String path) {
        if (!commit) {
            return null
        }
        final TreeWalk walk2 = TreeWalk.forPath(repo, path, commit.getTree());

        if (walk2 == null) {
            return null
        };
        if ((walk2.getRawMode(0) & FileMode.TYPE_MASK) != FileMode.TYPE_FILE) {
            return null
        };

        def id = walk2.getObjectId(0)
        walk2.release()
        return id;
    }

    byte[] getBytes(ObjectId id) {
        repo.open(id, Constants.OBJ_BLOB).getCachedBytes(Integer.MAX_VALUE)
    }


    int printDiff(OutputStream out, File file1, byte[] data) {
        RawText rt1 = new RawText(data);
        RawText rt2 = new RawText(file1);
        EditList diffList = new EditList();
        DiffAlgorithm differ = DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.HISTOGRAM)

        diffList.addAll(differ.diff(COMP, rt1, rt2));
        if (diffList.size() > 0) {
            new DiffFormatter(out).format(diffList, rt1, rt2);
        }
        diffList.size()
    }

}