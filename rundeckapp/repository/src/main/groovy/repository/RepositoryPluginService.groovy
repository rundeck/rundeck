package repository

import com.dtolabs.rundeck.core.storage.StorageTree
import com.rundeck.repository.artifact.RepositoryArtifact
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class RepositoryPluginService implements InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(this)

    File localFilesystemPluginDir
    StorageTree installedPluginTree
    String storageTreePath
    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1)

    def syncInstalledArtifactsToPluginTarget() {
        //Give the filesystem enough time to flush the artifact that was just copied
        executorService.schedule({
             log.debug("coordinating plugin tree with local file system");

             def installed = []
             localFilesystemPluginDir.traverse(type: groovy.io.FileType.FILES) { file ->
                 installed.add(file.name)
             }

             installedPluginTree.listDirectoryResources(storageTreePath).each {
                 log.debug("found plugin: "+ it.getPath().getName());

                 if(!installed.contains(it.getPath().getName())) {
                     log.info("installing plugin: "+ it.getPath().getName());
                     File dest = new File(localFilesystemPluginDir,it.getPath().getName())
                     dest.withOutputStream { outStream ->
                         it.contents.writeContent(outStream)
                     }
                 }
             }
         }, 2, TimeUnit.SECONDS)

    }

    void removeOldPlugin(final RepositoryArtifact artifact) {
        File oldPlugin = new File(localFilesystemPluginDir,artifact.installationFileName)
        if(oldPlugin.exists()) oldPlugin.delete()
    }

    void uninstallArtifact(final RepositoryArtifact artifact) {
        File oldPlugin = new File(localFilesystemPluginDir,artifact.installationFileName)
        if(oldPlugin.exists()) oldPlugin.delete()
        installedPluginTree.deleteResource("${storageTreePath}/${artifact.installationFileName}")
    }

    @Override
    void afterPropertiesSet() throws Exception {
        if(!localFilesystemPluginDir) throw new Exception("Local plugin dir must be set")
        if(!installedPluginTree) throw new Exception("Storage Tree for installed plugins must be set")
    }
}
