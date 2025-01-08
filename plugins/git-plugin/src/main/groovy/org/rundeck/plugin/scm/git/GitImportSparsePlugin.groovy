package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.plugins.scm.ScmOperationContext
import com.dtolabs.rundeck.plugins.scm.ScmPluginException
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.treewalk.TreeWalk
import org.rundeck.plugin.scm.git.config.SparseImport

class GitImportSparsePlugin extends GitImportPlugin {
    SparseImport sparseConfig

    GitImportSparsePlugin(SparseImport config, List<String> trackedItems) {
        super(config, trackedItems)
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
    void walkTreePaths(String tree, boolean recursive, Closure walker) {
        TreeWalk walk = new TreeWalk(repo)
        walk.setRecursive(recursive)
        try {
            ObjectId id = repo.resolve(tree)
            if (id == null) {
                return
            }
            walk.addTree(id)
            while (walk.next()) {
                if (walk.getPathString().startsWith(sparseConfig.jobsDirectory + '/')) {
                    walker.call(walk)
                }
            }
        } finally {
            walk.close()
        }
    }
}