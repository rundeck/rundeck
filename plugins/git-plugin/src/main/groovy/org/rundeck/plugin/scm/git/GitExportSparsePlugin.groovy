package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.plugins.scm.ScmOperationContext
import com.dtolabs.rundeck.plugins.scm.ScmPluginException
import org.eclipse.jgit.api.Git
import org.rundeck.plugin.scm.git.config.SparseExport

class GitExportSparsePlugin extends GitExportPlugin {
    SparseExport sparseConfig

    GitExportSparsePlugin(SparseExport config) {
        super(config)
        this.sparseConfig = config
    }

    @Override
    protected void cloneOrCreate(ScmOperationContext context, File base, String url, String integration) throws ScmPluginException {
        try {
            if (!base.exists()) {
                throw new ScmPluginException("Base directory does not exist: ${base.absolutePath}")
            }

            if (base.isDirectory() && new File(base, ".git").isDirectory()) {
                git = Git.open(base)
                repo = git.getRepository()
                return
            }
            def commands = [
                ["git", "init"] as String[],
                ["git", "config", "core.sparseCheckout", "true"] as String[],
                ["git", "remote", "add", "origin", url] as String[]
            ]

            runGitCommands(commands, base)

            def sparseCheckoutFile = new File(base, ".git/info/sparse-checkout")
            sparseCheckoutFile.parentFile.mkdirs()
            sparseCheckoutFile.text = "${sparseConfig.jobsDirectory}/*\n"

            def remainingCommands = [
                ["git", "fetch", "--depth", "1", "origin", branch] as String[],
                ["git", "checkout", branch] as String[]
            ]

            runGitCommands(remainingCommands, base)

            git = Git.open(base)
            repo = git.getRepository()


        } catch (Exception e) {
            throw new ScmPluginException("Failed to setup repository: ${e.message}", e)
        }
    }

    private static void runGitCommands(List<String[]> commands, File directory) {
        for (String[] command : commands) {
            def process = new ProcessBuilder(command)
                    .directory(directory)
                    .redirectErrorStream(true)
                    .start()

            def output = new StringBuilder()
            process.inputStream.eachLine { output.append(it).append('\n') }

            if (process.waitFor() != 0) {
                throw new ScmPluginException("Command failed: ${command.join(' ')}\nOutput: ${output}")
            }
        }
    }


    @Override
    String getRelativePathForJob(JobReference job) {
        def basePath = super.getRelativePathForJob(job)
        return "${sparseConfig.jobsDirectory}/${basePath}"
    }


} 