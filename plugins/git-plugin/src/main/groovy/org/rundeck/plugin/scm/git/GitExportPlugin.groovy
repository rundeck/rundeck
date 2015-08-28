package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.core.jobs.JobExportReference
import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.plugins.scm.*
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import org.eclipse.jgit.api.*
import org.eclipse.jgit.diff.DiffAlgorithm
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.EditList
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.diff.RawTextComparator
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.FileMode
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.TreeWalk

/**
 * Created by greg on 8/21/15.
 */
class GitExportPlugin implements ScmExportPlugin {
    public static final String SERIALIZE_FORMAT = 'xml'
    String format = SERIALIZE_FORMAT
    boolean inited = false
    Git git;
    Repository repo;
    File workingDir;
    PersonIdent commitIdent;
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
        System.err.println("setup: " + input + ", capabilities")
        if (inited) {
            System.err.println("already inited, not doing setup")
            return
        }

        //verify input
        if (!input.pathTemplate) {
            throw new IllegalStateException("pathTemplate cannot be null")
        }
        //verify input
        if (!input.dir) {
            throw new IllegalStateException("dir cannot be null")
        }
        format = input.format ?: 'xml'

        File base = new File(input.get("dir").toString())
        mapper = new TemplateJobFileMapper(input.pathTemplate.toString(), base)

        branch = input.get("branch").toString()
        commitIdent = new PersonIdent(input.committerName.toString(), input.committerEmail.toString())
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

    @Override
    void cleanup() {
        git.close()
    }

    @Override
    List<Property> getExportProperties(final Set<JobRevReference> jobs) {
        [
                PropertyBuilder.builder().string("commitMessage").
                        title("Commit Message").
                        description("Enter a commit message. Committing to branch: `" + branch + '`').
                        required(true).
                        renderingAsTextarea().
                        build(),
                PropertyBuilder.builder().string("tagName").
                        title("Tag").
                        description("Enter a tag name to include, will be pushed with the branch.").
                        required(false).
                        build(),
                PropertyBuilder.builder().booleanType("push").
                        title("Push Remotely?").
                        description("Check to push to the remote").
                        required(false).
                        build(),
        ]
    }


    @Override
    String export(final Set<JobExportReference> jobs, final Map<String, Object> input)
            throws ScmPluginException
    {
        serializeAll(jobs)
        String commitMessage = input.commitMessage.toString()
        StatusCommand statusCmd = git.status()
        jobs.each {
            statusCmd.addPath(relativePath(it))
        }
        Status status = statusCmd.call()
        //add all changes to index
        AddCommand addCommand = git.add()
        jobs.each {
            addCommand.addFilepattern(relativePath(it))
        }
        addCommand.call()

        CommitCommand commit1 = git.commit().setMessage(commitMessage).setCommitter(commitIdent)
        jobs.each {
            commit1.setOnly(relativePath(it))
        }
        RevCommit commit = commit1.call()

        //todo: push

        return commit.name
    }

    def serialize(final JobExportReference job) {
        File outfile = mapper.fileForJob(job)

        outfile.withOutputStream { out ->
            job.jobSerializer.serialize(format, out)
        }
    }

    def serializeAll(final Set<JobExportReference> jobExportReferences) {
        jobExportReferences.each(this.&serialize)
    }

    JobState getStoredJobStatus(final JobRevReference job) {
        Map state = storedPluginState.state[job.id]
        if (state && state[job.version]) {
            Map revState = state[job.version]
            return new JobGitState(
                    synchState: SynchState.valueOf((String) revState.synchState),
                    stateMeta: revState.meta ?: [:]
            )
        }
        return null
    }

    @Override
    JobState jobChanged(JobChangeEvent event, JobExportReference exportReference) {
        File origfile = mapper.fileForJob(event.originalJobReference)
        File outfile = mapper.fileForJob(event.jobReference)
        System.err.println("Job event (${event}), writing to path: ${outfile}")
        switch (event.eventType) {
            case JobChangeEvent.JobChangeEventType.DELETE:
                origfile.delete()
                break;

            case JobChangeEvent.JobChangeEventType.MODIFY_RENAME:
            case JobChangeEvent.JobChangeEventType.CREATE:
            case JobChangeEvent.JobChangeEventType.MODIFY:
                if (!origfile.getAbsolutePath().equals(outfile.getAbsolutePath())) {
                    origfile.delete()
                }
                if (!outfile.getParentFile().exists()) {
                    if (!outfile.getParentFile().mkdirs()) {
                        System.err.println("Failed to create parent dirs for ${outfile}")
                    }
                }
                outfile.withOutputStream { out ->
                    exportReference.jobSerializer.serialize(format, out)
                }
        }
        return getJobStatus(exportReference)
    }

    private hasJobStatusCached(final JobExportReference job) {
        def path = relativePath(job)

        def commit = lastCommitForPath(path)

        def ident = job.id + ':' + String.valueOf(job.version) + ':' + (commit ? commit.name : '')

        if (jobStateMap[job.id] && jobStateMap[job.id].ident == ident) {
            System.err.println("hasJobStatusCached(${job.id}): FOUND")
            return jobStateMap[job.id]
        }
        System.err.println("hasJobStatusCached(${job.id}): (no)")

        null
    }

    private refreshJobStatus(final JobExportReference job) {

        def path = relativePath(job)

        jobStateMap.remove(job.id)

        def jobstat = Collections.synchronizedMap([:])
        def commit = lastCommitForPath(path)

        def ident = job.id + ':' + String.valueOf(job.version) + ':' + (commit ? commit.name : '')

        serialize(job)

        Status status = git.status().addPath(path).call()
        SynchState synchState = synchStateForStatus(status,commit, path)
        def scmState = scmStateForStatus(status, commit, path)

        jobstat['ident'] = ident
        jobstat['id'] = job.id
        jobstat['version'] = job.version
        jobstat['synch'] = synchState
        jobstat['scm'] = scmState
        if (commit) {
            jobstat['commitId'] = commit.name
            jobstat['commitMeta'] = metaForCommit(commit)
        }
        System.err.println("refreshJobStatus(${job.id}): ${jobstat}")

        jobStateMap[job.id] = jobstat

        jobstat
    }

    private SynchState synchStateForStatus(Status status, RevCommit commit, String path) {
        if (status.untracked.contains(path)) {
            SynchState.CREATE_NEEDED
        }else if (status.uncommittedChanges.contains(path)) {
            SynchState.EXPORT_NEEDED
        } else if(commit) {
            SynchState.CLEAN
        }else {
            SynchState.CREATE_NEEDED
        }
    }

    def scmStateForStatus(Status status, RevCommit commit, String path) {
        if(!commit){
            'NEW'
        }else if (path in status.added || path in status.untracked) {
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
        }
        'NOT_FOUND'
    }

    @Override
    JobState getJobStatus(final JobExportReference job) {
        System.err.println("getJobStatus(${job.id})")
        if (!inited) {
            return null
        }
        def status = hasJobStatusCached(job)
        if (!status) {
            status = refreshJobStatus(job)
        }
        return createJobStatus(status)
    }

    JobState createJobStatus(final Map map) {
        //TODO: include scm status
        return new JobGitState(synchState: map['synch'], stateMeta: map.commitMeta ?: [:])
    }

    JobState getJobStatusx(final JobExportReference job) {
        if (!inited) {
            return null
        }

        def file = getLocalFileForJob(job)
        def path = relativePath(job)
        Status status = git.status().addPath(path).call()
        System.err.println("status for ${file}, workingdir ${workingDir}: " + [
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
        )
        if (path in status.added || path in status.untracked) {
            return new JobGitState(synchState: SynchState.EXPORT_NEEDED, stateMeta: [:])
        } else if (path in status.changed || path in status.modified) {
            //changed== changes in index
            //modified == changes on disk
            return new JobGitState(synchState: SynchState.EXPORT_NEEDED, stateMeta: [:])
//            return ScmPlugin.ScmFileStatus.MODIFIED
        } else if (path in status.removed || path in status.missing) {
            return new JobGitState(synchState: SynchState.EXPORT_NEEDED, stateMeta: [:])
//            return ScmPlugin.ScmFileStatus.DELETED
        } else if (path in status.untracked) {
            return null
//            return new JobGitState(synchState: SynchState.EXPORT_NEEDED, stateMeta: [:])
//            return ScmPlugin.ScmFileStatus.NOT_PRESENT
        } else if (path in status.conflicting) {
            return new JobGitState(synchState: SynchState.EXPORT_NEEDED, stateMeta: [:])
//            return ScmPlugin.ScmFileStatus.CONFLICT
        }
        if (status.clean) {
            //no changes in working dir
            def commit = lastCommitForPath(path)
            if (commit) {
                return new JobGitState(synchState: SynchState.CLEAN, stateMeta: metaForCommit(commit))
            }
        }


        null
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

    RevCommit lastCommitForPath(String path) {
        def log = git.log().addPath(path).call()
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
    File getLocalFileForJob(final JobRevReference job) {
        mapper.fileForJob(job)
    }

    @Override
    String getRelativePathForJob(final JobRevReference job) {
        relativePath(job)
    }

    String relativePath(File reference) {
        reference.absolutePath.substring(workingDir.getAbsolutePath().length() + 1)
    }

    String relativePath(JobRevReference reference) {
        relativePath(getLocalFileForJob(reference))
    }

    ScmDiffResult getFileDiff(final JobExportReference job) throws ScmPluginException {
        def file=getLocalFileForJob(job)
        def path=relativePath(job)
        serialize(job)

        def id=lookupId(getHead(),path)
        if(!id) {
            return new GitDiffResult(sourceNotFound: true)
        }
        def bytes=getBytes(id)
        def baos = new ByteArrayOutputStream()
        def diffs = printDiff(baos, file, bytes)


        return new GitDiffResult(contentType: 'text/plain', content: baos.toString(), modified: diffs>0)
    }

//get blob for HEAD rev of the path
    def getHead(){

        final RevWalk walk = new RevWalk(repo);
        walk.setRetainBody(true);

        final RevCommit headCommit =  walk.parseCommit(repo.resolve(Constants.HEAD));
        walk.release()
        headCommit
    }

    def lookupId(RevCommit commit,String path){
        final TreeWalk walk2 = TreeWalk.forPath(repo, path, commit.getTree());

        if (walk2 == null)
            return null;
        if ((walk2.getRawMode(0) & FileMode.TYPE_MASK) != FileMode.TYPE_FILE)
            return null;
        return walk2.getObjectId(0);
    }
    def getBytes(ObjectId id){
        repo.open(id, Constants.OBJ_BLOB).getCachedBytes(Integer.MAX_VALUE)
    }


    def printDiff(OutputStream out, File file1, byte[] data){
        RawText rt1 = new RawText(data);
        RawText rt2 = new RawText(file1);
        EditList diffList = new EditList();
        DiffAlgorithm differ = DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.HISTOGRAM)

        diffList.addAll(differ.diff(COMP, rt1, rt2));
        if(diffList.size()>0) {
            new DiffFormatter(out).format(diffList, rt1, rt2);
        }
        diffList.size()
    }

}