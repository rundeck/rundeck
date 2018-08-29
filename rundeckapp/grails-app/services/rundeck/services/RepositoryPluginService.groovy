package rundeck.services

import com.dtolabs.rundeck.core.storage.StorageTree

class RepositoryPluginService {

    File localFilesystemPluginDir
    StorageTree installedPluginTree


    def syncInstalledArtifactsToPluginTarget() {
        System.out.println("coordinating plugin tree with local file system");
        System.out.println("plugins");
        installedPluginTree.listDirectory("").each {
            println it.path.name
            println it.path.path
        }
        def installed = []
        localFilesystemPluginDir.traverse(type: groovy.io.FileType.FILES) { file ->
            installed.add(file.name)
        }

        installedPluginTree.listDirectoryResources("plugins").each {
            println("found plugin: "+it.getPath().getName());

            if(!installed.contains(it.getPath().getName())) {
                println("installing plugin: "+it.getPath().getName());
                File dest = new File(localFilesystemPluginDir,it.getPath().getName())
                dest.withOutputStream { outStream ->
                    it.contents.writeContent(outStream)
                }
            }
        }
    }
}
