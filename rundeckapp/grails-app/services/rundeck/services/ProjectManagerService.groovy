package rundeck.services

import com.codahale.metrics.Gauge
import com.codahale.metrics.MetricRegistry
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.common.ProjectNodeSupport
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageTree
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.dtolabs.rundeck.core.utils.IPropertyLookup
import com.dtolabs.rundeck.core.utils.PropertyLookup
import com.dtolabs.rundeck.server.projects.RundeckProject
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListenableFutureTask
import grails.transaction.Transactional
import org.apache.commons.fileupload.util.Streams
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.Resource
import org.rundeck.storage.data.DataUtil
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import rundeck.Project

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Transactional
class ProjectManagerService implements ProjectManager, ApplicationContextAware, InitializingBean {
    public static final String ETC_PROJECT_PROPERTIES_PATH = "/etc/project.properties"
    public static final String MIME_TYPE_PROJECT_PROPERTIES = 'text/x-java-properties'
    def FrameworkService frameworkService
    private StorageTree rundeckConfigStorageTree
    ApplicationContext applicationContext
    def grailsApplication
    def metricService

    /**
     * Load on demand due to cyclical spring dependency
     * @return
     */
    private StorageTree getStorage() {
        if (null == rundeckConfigStorageTree) {
            rundeckConfigStorageTree = applicationContext.getBean("rundeckConfigStorageTree", StorageTree)
        }
        return rundeckConfigStorageTree
    }
    public void setStorage(StorageTree tree){
        rundeckConfigStorageTree=tree
    }

    @Override
    Collection<IRundeckProject> listFrameworkProjects() {
        return Project.findAll().collect {
            getFrameworkProject(it.name)
        }
    }

    @Override
    IRundeckProject getFrameworkProject(final String name) {
        if (!existsFrameworkProject(name)) {
            throw new IllegalArgumentException("Project does not exist: " + name)
        }
        def result = projectCache.get(name)
        if (!result) {
            throw new IllegalArgumentException("Project does not exist: " + name)
        }
        return result
    }

    @Override
    boolean existsFrameworkProject(final String project) {
        Project.withNewSession{
            Project.findByName(project) ? true : false
        }
    }

    @Override
    IRundeckProject createFrameworkProject(final String projectName) {
        return createFrameworkProject(projectName, new Properties())
    }
    /**
     * Scheduled executor for retries
     */
    private ExecutorService executor = Executors.newFixedThreadPool(2)

    //basic creation, created via spec string in afterPropertiesSet()
    private LoadingCache<String, IRundeckProject> projectCache =
            CacheBuilder.newBuilder()
                        .expireAfterAccess(10, TimeUnit.MINUTES)
                        .refreshAfterWrite(1, TimeUnit.MINUTES)
                        .build(
                    new CacheLoader<String, IRundeckProject>() {
                        public IRundeckProject load(String key) {
                            return loadProject(key);
                        }
                    }
            );

    @Override
    void afterPropertiesSet() throws Exception {
        def spec = grailsApplication.config.rundeck?.projectManagerService?.projectCache?.spec ?:
                "expireAfterAccess=10m,refreshAfterWrite=1m"

        log.debug("projectCache: creating from spec: ${spec}")

        projectCache = CacheBuilder.from(spec)
                                   .recordStats()
                                   .build(
                new CacheLoader<String, IRundeckProject>() {
                    public IRundeckProject load(String key) {
                        return loadProject(key);
                    }

                    @Override
                    ListenableFuture<IRundeckProject> reload(final String key, final IRundeckProject oldValue)
                            throws Exception
                    {
                        if (needsReload(oldValue)) {
                            ListenableFutureTask<IRundeckProject> task = ListenableFutureTask.create(
                                    new Callable<IRundeckProject>() {
                                        public IRundeckProject call() {
                                            return loadProject(key);
                                        }
                                    }
                            );
                            executor.execute(task);
                            return task;
                        } else {
                            return Futures.immediateFuture(oldValue)
                        }
                    }
                }
        )
        MetricRegistry registry = metricService?.getMetricRegistry()
        registry?.register(
                MetricRegistry.name(this.class.name + ".projectCache", "hitCount"),
                new Gauge<Long>() {
                    @Override
                    Long getValue() {
                        projectCache.stats().hitCount()
                    }
                }
        )

        registry?.register(
                MetricRegistry.name(this.class.name + ".projectCache", "evictionCount"),
                new Gauge<Long>() {
                    @Override
                    Long getValue() {
                        projectCache.stats().evictionCount()
                    }
                }
        )
        registry?.register(
                MetricRegistry.name(this.class.name + ".projectCache", "missCount"),
                new Gauge<Long>() {
                    @Override
                    Long getValue() {
                        projectCache.stats().missCount()
                    }
                }
        )
        registry?.register(
                MetricRegistry.name(this.class.name + ".projectCache", "loadExceptionCount"),
                new Gauge<Long>() {
                    @Override
                    Long getValue() {
                        projectCache.stats().loadExceptionCount()
                    }
                }
        )
        registry?.register(
                MetricRegistry.name(this.class.name + ".projectCache", "hitRate"),
                new Gauge<Double>() {
                    @Override
                    Double getValue() {
                        projectCache.stats().hitRate()
                    }
                }
        )
    }

    boolean existsProjectFileResource(String projectName, String path) {
        def storagePath = "projects/" + projectName + (path.startsWith("/")?path:"/${path}")
        return getStorage().hasResource(storagePath)
    }
    Resource<ResourceMeta> getProjectFileResource(String projectName, String path) {
        def storagePath = "projects/" + projectName + (path.startsWith("/")?path:"/${path}")
        if (!getStorage().hasResource(storagePath)) {
            return null
        }
        getStorage().getResource(storagePath)
    }
    long readProjectFileResource(String projectName, String path, OutputStream output) {
        def storagePath = "projects/" + projectName + (path.startsWith("/")?path:"/${path}")
        def resource = getStorage().getResource(storagePath)
        Streams.copy(resource.contents.inputStream,output,true)
    }
    /**
     * Update existing resource, fails if it does not exist
     * @param projectName project
     * @param path path
     * @param input stream
     * @param meta metadata
     * @return resource
     */
    Resource<ResourceMeta> updateProjectFileResource(String projectName, String path, InputStream input, Map<String,String> meta) {
        def storagePath = "projects/" + projectName + (path.startsWith("/")?path:"/${path}")
        getStorage().
                updateResource(storagePath, DataUtil.withStream(input, meta, StorageUtil.factory()))
    }
    /**
     * Create new resource, fails if it exists
     * @param projectName project
     * @param path path
     * @param input stream
     * @param meta metadata
     * @return resource
     */
    Resource<ResourceMeta> createProjectFileResource(String projectName, String path, InputStream input, Map<String,String> meta) {
        def storagePath = "projects/" + projectName + (path.startsWith("/")?path:"/${path}")
        getStorage().
                createResource(storagePath, DataUtil.withStream(input, meta, StorageUtil.factory()))
    }
    /**
     * Write to a resource, create if it does not exist
     * @param projectName project
     * @param path path
     * @param input stream
     * @param meta metadata
     * @return resource
     */
    Resource<ResourceMeta> writeProjectFileResource(String projectName, String path, InputStream input, Map<String,String> meta) {
        def storagePath = "projects/" + projectName + (path.startsWith("/")?path:"/${path}")
        if (!getStorage().hasResource(storagePath)) {
            createProjectFileResource(projectName, path, input, meta)
        }else{
            updateProjectFileResource(projectName, path, input, meta)
        }
    }
    /**
     * delete a resource
     * @param projectName project
     * @param path path
     * @return true if file was deleted or does not exist
     */
    boolean deleteProjectFileResource(String projectName, String path) {
        def storagePath = "projects/" + projectName + (path.startsWith("/")?path:"/${path}")
        if (!getStorage().hasResource(storagePath)) {
            return true
        }else{
            return getStorage().deleteResource(storagePath)
        }
    }
    /**
     * delete all project file resources
     * @param projectName project
     * @return true if all files were deleted, false otherwise
     */
    boolean deleteAllProjectFileResources(String projectName) {
        def storagePath = "projects/" + projectName
        return StorageUtil.deletePathRecursive(getStorage(), PathUtil.asPath(storagePath))
    }

    Date getProjectConfigLastModified(String projectName) {
        def resource = getProjectFileResource(projectName, ETC_PROJECT_PROPERTIES_PATH)
        if(null==resource){
            return null
        }

        resource.getContents().modificationTime
    }
    boolean isValidConfigFile(byte[] bytes){
        def test = '#' + MIME_TYPE_PROJECT_PROPERTIES
        //validate header
        def validate=bytes.length>=test.length()?new String(bytes,0,test.length(),'ISO-8859-1'):null
        return test==validate
    }

    private Map loadProjectConfigResource(String projectName) {
        def resource = getProjectFileResource(projectName,ETC_PROJECT_PROPERTIES_PATH)
        if (null==resource) {
            return [:]
        }
        def properties = new Properties()
        def bytestream = new ByteArrayOutputStream()
        Streams.copy(resource.contents.inputStream,bytestream,true)
        def bytes=bytestream.toByteArray()
        //validate header
        if(isValidConfigFile(bytes)){
            //load as properties file
            try {
                properties.load(new ByteArrayInputStream(bytes))
            } catch (IOException e) {
                //TODO: throw exception?
    //            throw new RuntimeException("Failed loading project properties from storage: ${resource.path}: " + e.message)
                log.error("Failed loading project properties from storage: ${resource.path}: " + e.message,e)
            }
        }else{
            log.error("Failed loading project properties from storage: ${resource.path}: could not validate contents")
        }

        return [
                config      : properties,
                lastModified: resource.contents.modificationTime,
                creationTime: resource.contents.creationTime
        ]
    }

    private Map storeProjectConfig(String projectName, Properties properties) {
        def storagePath = ETC_PROJECT_PROPERTIES_PATH
        def baos = new ByteArrayOutputStream()
        properties.store(baos, MIME_TYPE_PROJECT_PROPERTIES+";name=" + projectName)
        def bais = new ByteArrayInputStream(baos.toByteArray())

        def metadata = [(StorageUtil.RES_META_RUNDECK_CONTENT_TYPE):MIME_TYPE_PROJECT_PROPERTIES]
        def resource = writeProjectFileResource(projectName, storagePath, bais, metadata)

        projectCache.invalidate(projectName)
        return [
                config      : properties,
                lastModified: resource.contents.modificationTime,
                creationTime: resource.contents.creationTime
        ]
    }

    private void deleteProjectResources(String projectName) {
        if (!deleteAllProjectFileResources(projectName)) {
            log.error("Failed to delete all associated resources for project ${projectName}")
        }
        projectCache.invalidate(projectName)
    }

    private IPropertyLookup createProjectPropertyLookup(String projectName, Properties config) {
        final Properties ownProps = new Properties();
        ownProps.setProperty("project.name", projectName);
        def create = PropertyLookup.create(
                createDirectProjectPropertyLookup(projectName,config),
                frameworkService.getRundeckFramework().propertyLookup
        )

        create.expand()
        return create
    }
    private IPropertyLookup createDirectProjectPropertyLookup(String projectName, Properties config) {
        final Properties ownProps = new Properties();
        ownProps.setProperty("project.name", projectName);
        ownProps.putAll(config)
        def create = PropertyLookup.create(ownProps)
        create.expand()
        return create
    }

    @Override
    IRundeckProject createFrameworkProject(final String projectName, final Properties properties) {
        Project found = Project.findByName(projectName)
        if (!found) {
            def project = new Project(name: projectName)
            project.save()
        }
        def res = storeProjectConfig(projectName, properties)
        return new RundeckProject(
                projectName,
                createProjectPropertyLookup(projectName, res.config),
                createDirectProjectPropertyLookup(projectName, res.config),
                this,
                res.lastModified
        )
    }

    @Override
    void removeFrameworkProject(final String projectName) {
        Project found = Project.findByName(projectName)
        if (!found) {
            throw new IllegalArgumentException("project does not exist: " + projectName)
        }
        found.delete(flush: true)
        deleteProjectResources(projectName)
    }

    @Override
    IRundeckProject createFrameworkProjectStrict(final String projectName, final Properties properties) {
        Project found = Project.findByName(projectName)
        if (found) {
            throw new IllegalArgumentException("project exists: " + projectName)
        }
        return createFrameworkProject(projectName, properties)
    }


    void mergeProjectProperties(
            final RundeckProject project,
            final Properties properties,
            final Set<String> removePrefixes
    )
    {
        def resource=mergeProjectProperties(project.name,properties,removePrefixes)
        project.lookup = createProjectPropertyLookup(project.name, resource.config ?: new Properties())
        project.projectLookup = createDirectProjectPropertyLookup(project.name, resource.config ?: new Properties())
        project.lastModifiedTime = resource.lastModified
    }

    Map mergeProjectProperties(
            final String projectName,
            final Properties properties,
            final Set<String> removePrefixes
    )
    {
        Project found = Project.findByName(projectName)
        if (!found) {
            throw new IllegalArgumentException("project does not exist: " + projectName)
        }
        def res = loadProjectConfigResource(projectName)
        def oldprops = res.config
        Properties newprops = mergeProperties(removePrefixes, oldprops, properties)
        Map newres=storeProjectConfig(projectName, newprops)
        projectCache.invalidate(projectName)
        newres
    }

    /**
     * Merge input properties with old properties, and remove any old properties with any of the given prefixes
     * @param removePrefixes prefix set
     * @param oldprops old properties
     * @param inProps input properties
     * @return merged properties
     */
    static Properties mergeProperties(Set<String> removePrefixes, Properties oldprops, Properties inProps) {
        def newprops = new Properties()
        if (removePrefixes) {
            oldprops.propertyNames().each { String k ->
                if (!removePrefixes.find { k.startsWith(it) }) {
                    newprops.put(k, oldprops.getProperty(k))
                }
            }
        }else{
            newprops.putAll(oldprops)
        }
        newprops.putAll(inProps)
        newprops
    }

    void setProjectProperties(final RundeckProject project, final Properties properties) {
        def resource=setProjectProperties(project.name,properties)
        project.lookup = createProjectPropertyLookup(project.name, resource.config ?: new Properties())
        project.projectLookup = createDirectProjectPropertyLookup(project.name, resource.config ?: new Properties())
        project.lastModifiedTime = resource.lastModified
    }
    Map setProjectProperties(final String projectName, final Properties properties) {
        Project found = Project.findByName(projectName)
        if (!found) {
            throw new IllegalArgumentException("project does not exist: " + projectName)
        }
        Map resource=storeProjectConfig(projectName, properties)
        projectCache.invalidate(projectName)
        resource
    }

    /**
     * Load the project config and node support
     * @param project
     * @return
     */
    IRundeckProject loadProject(final String project) {
        if (!existsFrameworkProject(project)) {
            throw new IllegalArgumentException("Project does not exist: " + project)
        }
        def resource = loadProjectConfigResource(project)
        def rdproject = new RundeckProject(
                project,
                createProjectPropertyLookup(project, resource.config ?: new Properties()),
                createDirectProjectPropertyLookup(project, resource.config ?: new Properties()),
                this,
                resource.lastModified
        )

        def framework = frameworkService.getRundeckFramework()
        def nodes = new ProjectNodeSupport(
                rdproject,
                framework.getResourceFormatGeneratorService(),
                framework.getResourceModelSourceService()
        )
        rdproject.projectNodes = nodes
        return rdproject
    }

    boolean needsReload(IRundeckProject project) {
        Project.withNewSession {
            Project rdproject = Project.findByName(project.name)
            boolean needsReload = rdproject == null ||
                    project.configLastModifiedTime == null ||
                    getProjectConfigLastModified(project.name) > project.configLastModifiedTime
            needsReload
        }
    }

    /**
     * @return specific nodes resources file path for the project, based on the framework.nodes.file.name property
     */
    public String getNodesResourceFilePath(IRundeckProject project) {
        ProjectNodeSupport.getNodesResourceFilePath(project, frameworkService.getRundeckFramework())
    }

    /**
     * Import any projects that do not exist from the source
     * @param source
     */
    boolean testProjectWasImported(ProjectManager source, String projectName) {
        return source.existsFrameworkProject(projectName) &&
                source.getFrameworkProject(projectName).existsFileResource("etc/project.properties.imported")
    }

    /**
     * Import any projects that do not exist from the source
     * @param source
     */
    void markProjectAsImported(ProjectManager source, String projectName){
        //mark as imported
        if(source.existsFrameworkProject(projectName)) {
            IRundeckProject other = source.getFrameworkProject(projectName)
            try {
                def baos = new ByteArrayOutputStream()
                other.loadFileResource("etc/project.properties", baos)
                other.storeFileResource(
                        "etc/project.properties.imported",
                        new ByteArrayInputStream(baos.toByteArray())
                )
                other.deleteFileResource("etc/project.properties")
                log.warn("Filesystem project ${other.name}, marked as imported. Rename etc/project.properties to etc/project.properties.imported")
            } catch (IOException e) {
                log.error(
                        "Failed marking ${other.name} as imported (rename etc/project.properties to etc/project.properties.imported): ${e.message}",
                        e
                )
            }
        }
    }
    /**
     * Import any projects that do not exist from the source
     * @param source
     */
    public void importProjectsFromProjectManager(ProjectManager source){
        source.listFrameworkProjects().each{IRundeckProject other->
            if(testProjectWasImported(source,other.name)){
                //marked as imported, so skip re-import.
                log.warn("Discovered filesystem project ${other.name}, was previously imported, skipping.")
                return
            }
            if(!existsFrameworkProject(other.name)){
                log.warn("Discovered filesystem project ${other.name}, importing...")
                def projectProps=new Properties()
                projectProps.putAll(other.getProjectProperties())
                def newProj=createFrameworkProject(other.name,projectProps)
                //import resources
                ["readme.md","motd.md"].each{ fpath ->
                    if(other.existsFileResource(fpath)){
                        log.warn("Importing ${fpath} for project ${other.name}...")
                        def baos=new ByteArrayOutputStream()
                        try {
                            other.loadFileResource(fpath, baos)
                            newProj.storeFileResource(fpath, new ByteArrayInputStream(baos.toByteArray()))
                        }catch (IOException e){
                            log.error("Failed importing ${fpath} for project ${other.name}: ${e.message}",e)
                        }
                    }
                }
                //mark as imported
                markProjectAsImported(source,other.name)
            }else{
                log.warn("Skipping import for filesystem project ${other.name}, it already exists...")
                markProjectAsImported(source,other.name)
            }
        }
    }
}
