package org.rundeck.plugin.scm.git
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.plugins.scm.*
import org.apache.log4j.Logger
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.diff.RawTextComparator
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
/**
 * Common features of the import and export plugins
 */
class BaseGitPlugin {
    public static final String GIT_PASSWORD_PATH = "gitPasswordPath"
    public static final String SSH_PRIVATE_KEY_PATH = "sshPrivateKeyPath"
    Git git
    Repository repo
    File workingDir
    String branch
    Map<String, String> input
    String project
    JobFileMapper mapper
    RawTextComparator COMP = RawTextComparator.DEFAULT
    Map<String, Map> jobStateMap = Collections.synchronizedMap([:])

    BaseGitPlugin(final Map<String, String> input, final String project) {
        this.input = input
        this.project = project
    }

    def serialize(final JobExportReference job, format) {
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

    def serializeTemp(final JobExportReference job, format) {
        File outfile = File.createTempFile("${this.class.name}-serializeTemp", ".${format}")
        outfile.deleteOnExit()
        outfile.withOutputStream { out ->
            job.jobSerializer.serialize(format, out)
        }
        return outfile
    }

    def serializeAll(final Set<JobExportReference> jobExportReferences, String format) {
        jobExportReferences.each { serialize(it, format) }
    }

    def fetchFromRemote(ScmOperationContext context,Git git1=null) {
        def fetchCommand = (git1?:git).fetch()
        def fetchResult = fetchCommand.call()

        def update = fetchResult.getTrackingRefUpdate("refs/remotes/origin/${this.branch}")

        def fetchMessage = update ? update.toString() : "No changes were found"
        Logger.getLogger(this.class).debug("fetchFromRemote: ${fetchMessage}")
        //make sure tracking is configured for the branch
        def branchConfig = new BranchConfig(repo.getConfig(), branch)
        def trackingBranch = branchConfig.getTrackingBranch()
        if(null==trackingBranch) {
            (git1 ?: git).repository.config.setString(
                    ConfigConstants.CONFIG_BRANCH_SECTION,
                    branch,
                    ConfigConstants.CONFIG_KEY_REMOTE,
                    'origin'
            );
            //if remote branch name exists, track it for merging
            def remoteRef = fetchResult.getAdvertisedRef(branch) ?: fetchResult.getAdvertisedRef(Constants.R_HEADS + branch)

            (git1 ?: git).repository.config.setString(
                    ConfigConstants.CONFIG_BRANCH_SECTION,
                    branch,
                    ConfigConstants.CONFIG_KEY_MERGE,
                    remoteRef.name
            );

            (git1 ?: git).repository.config.save()
        }

        return update
    }

    PullResult gitPull(ScmOperationContext context,Git git1=null) {
        def pullCommand = (git1?:git).pull().setRemote('origin').setRemoteBranchName(branch)
        pullCommand.call()
    }

    ScmExportResult gitResolve(
            final ScmOperationContext context,
            boolean rebase,
            String mergeResolutionStrategy
    )
    {
        if (rebase) {
            def pullCommand = git.pull().setRemote('origin').setRemoteBranchName(branch)
            pullCommand.setRebase(true)
            def pullResult = pullCommand.call()

            def result = new ScmExportResultImpl()
            result.success = pullResult.successful
            result.message = pullResult.toString()
            return result
        } else {
            //fetch, then
            //merge
            fetchFromRemote(context)

            def strategy = MergeStrategy.get(resolutionStrategy)
            def mergebuild = git.merge().setStrategy(strategy)
            def commit = git.repository.resolve("refs/remotes/origin/${branch}")

            mergebuild.include(commit)

            def mergeresult = mergebuild.call()

            def result = new ScmExportResultImpl()
            result.success = mergeresult.mergeStatus.successful
            result.message = mergeresult.toString()
            return result

        }
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

    File getLocalFileForJob(final JobReference job) {
        mapper.fileForJob(job)
    }

    String relativePath(File reference) {
        reference.absolutePath.substring(workingDir.getAbsolutePath().length() + 1)
    }

    String relativePath(JobReference reference) {
        relativePath(getLocalFileForJob(reference))
    }
    /**
     * get RevCommit for HEAD rev of the path
     * @return RevCommit or null if HEAD not found (empty git)
     */
    RevCommit getHead() {
        GitUtil.getHead repo
    }

    ObjectId lookupId(RevCommit commit, String path) {
        GitUtil.lookupId repo, commit, path
    }

    byte[] getBytes(ObjectId id) {
        GitUtil.getBytes repo, id
    }

    int diffContent(OutputStream out, byte[] left, File right) {
        GitUtil.diffContent out, left, right, COMP
    }

    int diffContent(OutputStream out, File left, byte[] right) {
        GitUtil.diffContent out, left, right, COMP
    }

    int diffContent(OutputStream out, byte[] left, byte[] right) {
        GitUtil.diffContent out, left, right, COMP
    }

    RevCommit lastCommit() {
        GitUtil.lastCommit repo, git
    }

    RevCommit lastCommitForPath(String path) {
        GitUtil.lastCommitForPath repo, git, path
    }

    JobState createJobStatus(final Map map) {
        //TODO: include scm status
        return new JobGitState(
                synchState: map['synch'],
                commit: map.commitMeta ? new GitScmCommit(map.commitMeta) : null
        )
    }
    JobImportState createJobImportStatus(final Map map) {
        //TODO: include scm status
        return new JobImportGitState(
                synchState: map['synch'],
                commit: map.commitMeta ? new GitScmCommit(map.commitMeta) : null
        )
    }
    protected void cloneOrCreate(final ScmOperationContext context, File base, String url) throws ScmPluginException {
        if (base.isDirectory() && new File(base, ".git").isDirectory()) {
            def arepo = new FileRepositoryBuilder().setGitDir(new File(base, ".git")).setWorkTree(base).build()
            def agit = new Git(arepo)

            //test url matches origin
            def config = agit.getRepository().getConfig()
            def found = config.getString("remote", "origin", "url")

            if (found != url) {

                logger.debug("url differs, re-cloning")
                //need to reconfigured
                removeWorkdir(base)
                performClone(base, url, context)
            }else if (null == arepo.resolve("refs/remotes/origin/${branch}")) {
                logger.debug("pulling from remote")

                try {
                    fetchFromRemote(context, agit)
                } catch (Exception e) {
                    logger.debug("Failed fetch from the repository from ${url}: ${e.message}",e)
                    throw new ScmPluginException("Failed fetch from the repository from ${url}: ${e.message}: ${e.cause?.message}", e)
                }


                git=agit
                repo=arepo

            }else{
                git=agit
                repo=arepo
                logger.debug("not cloning")
            }

        }else{
            performClone(base, url, context)
        }
    }

    private void performClone(File base, String url, ScmOperationContext context) {
        logger.debug("cloning...");
        def cloneCommand = Git.cloneRepository().
                setBranch(this.branch).
                setRemote("origin").
                setDirectory(base).
                setURI(url)
        try {
            git = cloneCommand.call()
        } catch (Exception e) {
            logger.debug("Failed cloning the repository from ${url}: ${e.message}", e)
            throw new ScmPluginException("Failed cloning the repository from ${url}: ${e.message}", e)
        }
        repo = git.getRepository()
    }
}
