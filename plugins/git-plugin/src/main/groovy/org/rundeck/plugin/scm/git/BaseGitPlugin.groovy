package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.core.plugins.views.Action
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.plugins.scm.*
import org.apache.log4j.Logger
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.PullResult
import org.eclipse.jgit.api.RebaseCommand
import org.eclipse.jgit.api.RebaseResult
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.diff.RawTextComparator
import org.eclipse.jgit.lib.BranchConfig
import org.eclipse.jgit.lib.ConfigConstants
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.merge.MergeStrategy
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.TrackingRefUpdate
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.util.FileUtils
import org.rundeck.plugin.scm.git.config.Common
import org.rundeck.storage.api.StorageException

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Common features of the import and export plugins
 */
class BaseGitPlugin {
    public static final String REMOTE_NAME = "origin"
    Git git
    Repository repo
    File workingDir
    String branch
    Map<String, String> input
    Common commonConfig
    JobFileMapper mapper
    RawTextComparator COMP = RawTextComparator.DEFAULT
    Map<String, Map> jobStateMap = Collections.synchronizedMap([:])

    BaseGitPlugin(Common commonConfig) {
        this.input = commonConfig.rawInput
        this.commonConfig = commonConfig
    }

    Map<String, String> getSshConfig() {
        def config = [:]

        if (commonConfig.strictHostKeyChecking in ['yes', 'no']) {
            config['StrictHostKeyChecking'] = commonConfig.strictHostKeyChecking
        }
        config
    }

    def serialize(final JobExportReference job, format, File outfile = null) {
        if (!outfile) {
            outfile = mapper.fileForJob(job)
        }
        if (!outfile.parentFile.isDirectory()) {
            if (!outfile.parentFile.mkdirs()) {
                throw new ScmPluginException(
                        "Cannot create necessary dirs to serialize file to path: ${outfile.absolutePath}"
                )
            }
        }
        File temp = new File(outfile.parentFile, outfile.name + ".tmp")
        try {
            temp.withOutputStream { out ->
                job.jobSerializer.serialize(format, out)
            }
        }catch(IOException e){
            throw new ScmPluginException(
                    "Failed to serialize job ${job}: ${e.message}",
                    e
            )

        }
        if (!temp.exists() || temp.size() < 1) {
            throw new ScmPluginException(
                    "Failed to serialize job, no content was written for job ${job}"
            )
        }
        Files.move(temp.toPath(), outfile.toPath(), StandardCopyOption.REPLACE_EXISTING)
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

    TrackingRefUpdate fetchFromRemote(ScmOperationContext context, Git git1 = null) {
        def agit = git1 ?: git
        def fetchCommand = agit.fetch()
        fetchCommand.setRemote(REMOTE_NAME)
        setupTransportAuthentication(sshConfig, context, fetchCommand)
        def fetchResult = fetchCommand.call()

        def update = fetchResult.getTrackingRefUpdate("refs/remotes/${REMOTE_NAME}/${this.branch}")

        def fetchMessage = update ? update.toString() : "No changes were found"
        Logger.getLogger(this.class).debug("fetchFromRemote: ${fetchMessage}")
        //make sure tracking is configured for the branch
        if (!remoteTrackingBranch(agit)) {
            agit.repository.config.setString(
                    ConfigConstants.CONFIG_BRANCH_SECTION,
                    branch,
                    ConfigConstants.CONFIG_KEY_REMOTE,
                    REMOTE_NAME
            );
            //if remote branch name exists, track it for merging
            def remoteRef = fetchResult.getAdvertisedRef(branch) ?:
                    fetchResult.getAdvertisedRef(Constants.R_HEADS + branch)
            if (remoteRef) {
                agit.repository.config.setString(
                        ConfigConstants.CONFIG_BRANCH_SECTION,
                        branch,
                        ConfigConstants.CONFIG_KEY_MERGE,
                        remoteRef.name
                );
            }

            agit.repository.config.save()
        }

        return update
    }
    /**
     * @param git1
     * @return remote tracking branch name, or null if tracking has not be set up
     */
    def remoteTrackingBranch(Git git1 = null) {
        def agit = git1 ?: git

        def branchConfig = new BranchConfig(agit.repository.config, branch)
        return branchConfig.getRemoteTrackingBranch()
    }

    PullResult gitPull(ScmOperationContext context, Git git1 = null) {
        def pullCommand = (git1 ?: git).pull().setRemote(REMOTE_NAME).setRemoteBranchName(branch)
        setupTransportAuthentication(sshConfig, context, pullCommand)
        pullCommand.call()
    }

    ScmExportResult gitResolve(
            final ScmOperationContext context,
            boolean rebase,
            String mergeResolutionStrategy
    )
    {
        if (rebase) {
            def pullCommand = git.pull().setRemote(REMOTE_NAME).setRemoteBranchName(branch)
            pullCommand.setRebase(true)
            setupTransportAuthentication(sshConfig, context, pullCommand)
            def pullResult = pullCommand.call()

            def result = new ScmExportResultImpl()
            result.success = pullResult.successful
            result.message = result.success ? "Rebase was successful" : "Rebase did not succeed"
            if (pullResult.rebaseResult) {
                result.extendedMessage = "Rebase result was: " + pullResult.rebaseResult.status?.toString()
                //get status
                boolean rebasestat = false
                if (pullResult.rebaseResult.conflicts) {
                    rebasestat = true

                    result.extendedMessage = result.extendedMessage + " Conflicts: " + pullResult.rebaseResult.conflicts
                }
                if (pullResult.rebaseResult.failingPaths) {
                    rebasestat = true
                    result.extendedMessage = result.extendedMessage + " Failures: " +
                            pullResult.rebaseResult.failingPaths
                }
                if (pullResult.rebaseResult.uncommittedChanges) {
                    rebasestat = true
                    result.extendedMessage = result.extendedMessage + " Uncommitted changes: " +
                            pullResult.rebaseResult.uncommittedChanges
                }
                if (!rebasestat) {
                    //rebase does not seem to actually include conflict info in result
                    def status = git.status().call()
                    if (status.conflicting || status.conflictingStageState) {
                        result.extendedMessage = result.extendedMessage + " Conflicts: " + status.conflictingStageState
                    }
                }
            }
            if (!result.success) {
                //abort rebase
                def abortResult = git.rebase().setOperation(RebaseCommand.Operation.ABORT).call()
                if (abortResult.status == RebaseResult.Status.ABORTED) {
                    result.message = result.message + ". Rebase automatically aborted."
                    result.extendedMessage = result.extendedMessage +
                            ". Note: local rebase was automatically aborted and restored to previous state."
                } else {
                    result.message = result.message + ". Rebase WAS NOT automatically aborted ${abortResult.status}."
                    result.extendedMessage = result.extendedMessage +
                            ". Warning: local rebase WAS NOT automatically aborted."
                }
            }
            return result
        } else {
            //fetch, then
            //merge
            fetchFromRemote(context)

            def strategy = MergeStrategy.get(mergeResolutionStrategy)
            def mergebuild = git.merge().setStrategy(strategy)
            def commit = git.repository.resolve("refs/remotes/${REMOTE_NAME}/${branch}")

            mergebuild.include(commit)

            def mergeresult = mergebuild.call()

            def result = new ScmExportResultImpl()
            result.success = mergeresult.mergeStatus.successful
            result.message = result.success ? "Merge was successful" : "Merge failed"
            result.extendedMessage = mergeresult.toString()
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

    static String expand(final String source, final ScmUserInfo scmUserInfo) {
        def userInfoProps = ['fullName', 'firstName', 'lastName', 'email', 'userName']
        def map = userInfoProps.collectEntries { [it, scmUserInfo[it]] }
        map['login'] = map['userName']
        expand(source, map, 'user')
    }

    static String expand(final String source, final Map<String, String> data, String prefix = '') {
        data.keySet().inject(source) { String x, String y ->
            return x.replaceAll(
                    Pattern.quote('${' + (prefix ? prefix + '.' : '') + y + '}'),
                    Matcher.quoteReplacement(data[y] ?: '')
            )
        }
    }

    JobState createJobStatus(final Map map, final List<Action> actions = []) {
        //TODO: include scm status
        return new JobGitState(
                synchState: map['synch'],
                commit: map.commitMeta ? new GitScmCommit(map.commitMeta) : null,
                actions: actions
        )
    }

    JobImportState createJobImportStatus(final Map map, final List<Action> actions = []) {
        //TODO: include scm status
        return new JobImportGitState(
                synchState: map['synch'],
                commit: map.commitMeta ? new GitScmCommit(map.commitMeta) : null,
                actions: actions
        )
    }

    Logger getLogger() {
        Logger.getLogger(this.class)
    }

    private InputStream getStoragePathStream(final ScmOperationContext context, String path) throws IOException {
        if (null == path) {
            return null;
        }
        def tree = context.getStorageTree()
        if (!tree.hasResource(path)) {
            throw new ScmPluginException("Path does not exist: ${path}")
        }
        ResourceMeta contents
        try {
            contents = tree.getResource(path).getContents()
        } catch (StorageException e) {
            logger.debug("getStoragePathStream", e)
            throw new ScmPluginException(e)
        }
        return contents.getInputStream()
    }

    private byte[] loadStoragePathData(final ScmOperationContext context, String path)
            throws IOException, ScmPluginException
    {
        if (null == path) {
            return null;
        }

        def tree = context.getStorageTree()
        if (!tree.hasResource(path)) {
            throw new ScmPluginException("Path does not exist: ${path}")
        }
        ResourceMeta contents
        try {
            contents = tree.getResource(path).getContents()
        } catch (StorageException e) {
            logger.debug("loadStoragePathData", e)
            throw new ScmPluginException(e)
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        contents.writeContent(byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    private void removeWorkdir(File base) {
        //remove the dir
        FileUtils.delete(base, FileUtils.RECURSIVE)
    }

    protected void cloneOrCreate(final ScmOperationContext context, File base, String url) throws ScmPluginException {
        if (base.isDirectory() && new File(base, ".git").isDirectory()) {
            def arepo = new FileRepositoryBuilder().setGitDir(new File(base, ".git")).setWorkTree(base).build()
            def agit = new Git(arepo)

            //test url matches origin
            def config = agit.getRepository().getConfig()
            def found = config.getString("remote", REMOTE_NAME, "url")
            def projectName = config.getString("rundeck", "scm-plugin", "project-name")
            if (projectName && !projectName.equals(context.frameworkProject)) {
                throw new ScmPluginInvalidInput(
                        "The base directory is already in use by another project: ${projectName}",
                        Validator.errorReport(
                                'dir',
                                "The base directory is already in use by another project: ${projectName}"
                        )
                )
            } else if (!projectName) {
                config.setString("rundeck", "scm-plugin", "project-name", context.frameworkProject)
                config.save()
            }
            def needsClone=false;

            if (found != url) {
                logger.debug("url differs, re-cloning")
                needsClone = true
            }else if (agit.repository.getFullBranch() != "refs/heads/$branch") {
                //check same branch
                logger.debug("branch differs, re-cloning")
                needsClone = true
            }

            if(needsClone){
                //need to reconfigured
                removeWorkdir(base)
                performClone(base, url, context)
                return
            }

            try {
                fetchFromRemote(context, agit)
            } catch (Exception e) {
                logger.debug("Failed fetch from the repository: ${e.message}", e)
                String msg = collectCauseMessages(e)
                throw new ScmPluginException("Failed fetch from the repository: ${msg}", e)
            }
            git = agit
            repo = arepo
        } else {
            performClone(base, url, context)
        }
    }

    private static String collectCauseMessages(Exception e) {
        List<String> msgs = [e.message]
        def cause = e.cause
        while (cause) {
            if (cause.message != msgs.last() && !msgs.last().endsWith(cause.message)) {
                msgs << cause.message
            }
            cause = cause.cause
        }
        return msgs.join("; ")
    }

    private void performClone(File base, String url, ScmOperationContext context) {
        logger.debug("cloning...");
        def cloneCommand = Git.cloneRepository().
                setBranch(this.branch).
                setRemote(REMOTE_NAME).
                setDirectory(base).
                setURI(url)
        setupTransportAuthentication(sshConfig, context, cloneCommand, url)
        try {
            git = cloneCommand.call()
        } catch (Exception e) {
            logger.debug("Failed cloning the repository from ${url}: ${e.message}", e)
            throw new ScmPluginException("Failed cloning the repository from ${url}: ${e.message}", e)
        }
        repo = git.getRepository()
    }

    /**
     * Configure authentication for the git command depending on the configured ssh private Key storage path, or password
     *
     * @param u
     * @param context
     * @param command
     */
    void setupTransportAuthentication(
            Map<String, String> sshConfig,
            ScmOperationContext context,
            TransportCommand command,
            String url = null
    )
            throws ScmPluginException
    {
        if (!url) {
            url = command.repository.config.getString('remote', REMOTE_NAME, 'url')
        }
        if (!url) {
            throw new NullPointerException("url for remote was not set")
        }

        URIish u = new URIish(url);
        logger.debug("transport url ${u}, scheme ${u.scheme}, user ${u.user}")
        if ((u.scheme == null || u.scheme == 'ssh') && u.user && commonConfig.sshPrivateKeyPath) {
            logger.debug("using ssh private key path ${commonConfig.sshPrivateKeyPath}")
            //setup ssh key authentication
            def expandedPath = expandContextVarsInPath(context, commonConfig.sshPrivateKeyPath)
            def keyData = loadStoragePathData(context, expandedPath)
            def factory = new PluginSshSessionFactory(keyData)
            factory.sshConfig = sshConfig
            command.setTransportConfigCallback(factory)
        } else if (u.user && commonConfig.gitPasswordPath) {
            //setup password authentication
            logger.debug("using password path ${commonConfig.gitPasswordPath}")
            def expandedPath = expandContextVarsInPath(context, commonConfig.gitPasswordPath)

            def data = loadStoragePathData(context, expandedPath)

            if (null != data && data.length > 0) {

                def pass = new String(data)
                command.setCredentialsProvider(new UsernamePasswordCredentialsProvider(u.user, pass))
            }
        }
    }

    /**
     * Expand variable references in the storage path, such as ${user.name} and ${project}* @param context
     * @param path
     * @return
     */
    public static String expandContextVarsInPath(ScmOperationContext context, String path) {
        expand(expand(path, context.userInfo), [project: context.frameworkProject])
    }
}
