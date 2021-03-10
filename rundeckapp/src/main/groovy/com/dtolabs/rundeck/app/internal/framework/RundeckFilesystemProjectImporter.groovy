package com.dtolabs.rundeck.app.internal.framework


import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.ProjectManager
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.ConfigurationService

import java.util.regex.Pattern

/**
 * Performs importProjectsFromProjectManager
 */
@CompileStatic
@Slf4j
class RundeckFilesystemProjectImporter implements InitializingBean {
    @Autowired
    ProjectManager projectManagerService
    @Autowired
    ProjectManager filesystemProjectManager
    @Autowired
    ConfigurationService configurationService

    /**
     * Allow 'known' or 'all' option values
     */
    String importFilesOption = 'known'
    String importStartupMode = 'bootstrap'

    /**
     * Mark imported file
     * @param source
     */
    void markProjectFileAsImported(IRundeckProject other, String path) {
        //mark as imported
        try {
            def baos = new ByteArrayOutputStream()
            other.loadFileResource(path, baos)
            other.storeFileResource(
                "${path}.imported",
                new ByteArrayInputStream(baos.toByteArray())
            )
            other.deleteFileResource(path)
            log.warn("Filesystem project ${other.name}, marked as imported. Rename $path to ${path}.imported")
        } catch (IOException e) {
            log.error(
                "Failed marking ${other.name} as imported (rename $path to ${path}.imported): ${e.message}",
                e
            )
        }
    }

    @Override
    void afterPropertiesSet() throws Exception {
        if (importFilesOption == 'init') {
            importProjectsFromProjectManager()
        }
    }

    void bootstrap() throws Exception {
        if (importStartupMode != 'init') {
            importProjectsFromProjectManager()
        }
    }

    void importProjectsFromProjectManager() {
        //import filesystem projects if using DB storage
        log.warn("importing existing filesystem projects")
        //NB: this should be removed in Rundeck > 3.4
        filesystemProjectManager.listFrameworkProjects().each { IRundeckProject other ->
            if (other.existsFileResource("etc/project.properties.imported")) {
                //marked as imported, so skip re-import.
                log.warn("Discovered filesystem project ${other.name}, was previously imported.")
                return
            }
            boolean needsImport = configurationService?.getString('projectsStorageImportResources') == 'always'
            if (!projectManagerService.existsFrameworkProject(other.name)) {
                log.warn("Discovered filesystem project ${other.name}, importing...")
                def projectProps = new Properties()
                projectProps.putAll(other.getProjectProperties())
                def newProj = projectManagerService.createFrameworkProject(other.name, projectProps)
                needsImport = true
            } else {
                log.warn("Skipping creation for filesystem project ${other.name}, it already exists.")
            }
            //mark as imported
            markProjectFileAsImported(other, "etc/project.properties")
            if (needsImport) {
                log.warn("Importing resources for filesystem project: ${other.name} ...")
                def newProj = projectManagerService.getFrameworkProject(other.name)
                //import resources
                int count = 0
                List paths = other.listDirPaths('')
                while (paths.size() > 0) {
                    String path = paths.remove(0)
                    if (!acceptsPathForImport(path)) {
                        log.warn("Skipping $path ...")
                        continue
                    }
                    if (path.endsWith('/')) {
                        paths.addAll(other.listDirPaths(path))
                    } else {
                        log.warn("Importing ${path} for project ${other.name}...")
                        def baos = new ByteArrayOutputStream()
                        try {
                            other.loadFileResource(path, baos)
                            def data = baos.toByteArray()
                            newProj.storeFileResource(path, new ByteArrayInputStream(data))
                            other.storeFileResource("${path}.imported", new ByteArrayInputStream(data))
                            other.deleteFileResource(path)
                            count++
                        } catch (IOException e) {
                            log.error("Failed importing ${path} for project ${other.name}: ${e.message}", e)
                        }
                    }
                }
                log.warn("Imported ${count} resources for project: ${other.name}")
            }
        }
    }
    List<Pattern> ignoredList = [Pattern.compile('^\\..*$')]
    List<Pattern> knownPatterns = [
        Pattern.compile('^/acls/.*\\.aclpolicy$'),
        Pattern.compile('^/readme.md$'),
        Pattern.compile('^/motd.md$'),
        Pattern.compile('^/nodes.yaml$'),
    ]

    boolean acceptsPathForImport(String path) {
        if(path.endsWith('/')){
            return true
        }
        if (path.split("/").any { String part -> ignoredList.any { it.matcher(part).matches() } }) {
            return false
        }
        if (path == "/etc/project.properties") {
            return false
        }
        if (path.endsWith('.imported')) {
            return false
        }
        if (importFilesOption != 'all' && !knownPatterns.any { it.matcher(path).matches() }) {
            return false
        }
        return true
    }
}
