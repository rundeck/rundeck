package rundeck.services

import com.dtolabs.rundeck.core.storage.StorageTree
import com.rundeck.verb.artifact.VerbArtifact

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class RepositoryPluginService {

    File localFilesystemPluginDir
    StorageTree installedPluginTree
    ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1)


    def syncInstalledArtifactsToPluginTarget() {
        //Give the filesystem enough time to flush the artifact that was just copied
        executorService.schedule({
            log.debug("coordinating plugin tree with local file system");

            def installed = []
            localFilesystemPluginDir.traverse(type: groovy.io.FileType.FILES) { file ->
                installed.add(file.name)
            }

            installedPluginTree.listDirectoryResources("plugins").each {
                log.debug("found plugin: "+it.getPath().getName());

                if(!installed.contains(it.getPath().getName())) {
                    log.info("installing plugin: "+it.getPath().getName());
                    File dest = new File(localFilesystemPluginDir,it.getPath().getName())
                    dest.withOutputStream { outStream ->
                        it.contents.writeContent(outStream)
                    }
                }
            }
        }, 2, TimeUnit.SECONDS)

    }

    void removeOldPlugin(final VerbArtifact verbArtifact) {
        File oldPlugin = new File(localFilesystemPluginDir,verbArtifact.installationFileName)
        if(oldPlugin.exists()) oldPlugin.delete()
    }

    void uninstallArtifact(final VerbArtifact verbArtifact) {
        File oldPlugin = new File(localFilesystemPluginDir,verbArtifact.installationFileName)
        if(oldPlugin.exists()) oldPlugin.delete()
        installedPluginTree.deleteResource("plugins/${verbArtifact.installationFileName}")
    }
}
