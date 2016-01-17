package rundeck.services

import grails.transaction.Transactional
import rundeck.services.scm.ScmPluginConfig
import rundeck.services.scm.ScmPluginConfigData

@Transactional
class PluginConfigService {
    def frameworkService

    ScmPluginConfigData loadScmConfig(String project, String path, String prefix) {
        def project1 = frameworkService.getFrameworkProject(project)
        if (!project1.existsFileResource(path)) {
            return null
        }
        def baos = new ByteArrayOutputStream()
        project1.loadFileResource(path, baos)
        return ScmPluginConfig.loadFromStream(prefix, new ByteArrayInputStream(baos.toByteArray()))
    }

    def storeConfig(ScmPluginConfigData scmPluginConfig, String project, String configPath) {
        def project1 = frameworkService.getFrameworkProject(project)
        project1.storeFileResource configPath, scmPluginConfig.asInputStream()
    }


    def removePluginConfiguration(String project, String path) {
        def project1 = frameworkService.getFrameworkProject(project)
        if (project1.existsFileResource(path)) {
            project1.deleteFileResource(path)
        }
    }
}
