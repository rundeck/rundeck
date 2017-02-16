package rundeck.services

import com.dtolabs.rundeck.plugins.file.FileUploadPlugin

import java.nio.file.Files

/**
 * Manage receiving and retrieving files uploaded for job execution
 */
class FileUploadService {
    static transactional = false
    public static final String FS_FILE_UPLOAD_PLUGIN = 'filesystem-file-upload'
    PluginService pluginService
    ConfigurationService configurationService
    //TODO:
    Map<String, File> localFileMap = [:]

    FileUploadPlugin getPlugin() {
        String pluginType = configurationService.getString('fileupload.plugin.type', FS_FILE_UPLOAD_PLUGIN)

        def plugin = pluginService.getPlugin(pluginType, FileUploadPlugin)
        plugin.initialize([:])
        plugin
    }

    String receiveFile(InputStream input, long length, String inputName, String jobId) {
        getPlugin().uploadFile(input, length, inputName, jobId)
    }

    def retrieveFile(OutputStream output, String reference) {
        getPlugin().retrieveFile(reference, output)
    }

    def removeFile(String reference) {
        getPlugin().removeFile(reference)
        if (localFileMap[reference]) {
            localFileMap[reference].delete()
            localFileMap.remove(reference)
        }
    }

    File retrieveTempFileForExecution(String reference) {
        def plugin = getPlugin()
        File file = plugin.retrieveLocalFile(reference)
        if (file) {
            return file
        }
        //copy locally
        file = Files.createTempFile(reference, "tmp").toFile()
        file.withOutputStream {
            plugin.retrieveFile(reference, it)
        }
        file.deleteOnExit()
        localFileMap[reference] = file
        file
    }
}
