package rundeck.services

import com.codahale.metrics.Gauge
import com.codahale.metrics.MetricRegistry
import com.dtolabs.rundeck.core.authorization.AclsUtil
import com.dtolabs.rundeck.core.authorization.Authorization
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.authorization.providers.CacheableYamlSource
import com.dtolabs.rundeck.core.authorization.providers.Policies
import com.dtolabs.rundeck.core.authorization.providers.PoliciesCache
import com.dtolabs.rundeck.core.authorization.providers.YamlProvider
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.IRundeckProjectConfig
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.common.ProjectNodeSupport
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageTree
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.dtolabs.rundeck.core.utils.IPropertyLookup
import com.dtolabs.rundeck.core.utils.PropertyLookup
import com.dtolabs.rundeck.server.projects.ProjectFile
import com.dtolabs.rundeck.server.projects.ProjectInfo
import com.dtolabs.rundeck.server.projects.RundeckProject
import com.dtolabs.rundeck.server.projects.RundeckProjectConfig
import com.google.common.base.Optional
import com.google.common.cache.Cache
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
import rundeck.Storage

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Transactional
class ProjectManagerService implements ProjectManager, ApplicationContextAware, InitializingBean {
    public static final String ETC_PROJECT_PROPERTIES_PATH = "/etc/project.properties"
    public static final String MIME_TYPE_PROJECT_PROPERTIES = 'text/x-java-properties'
    public static final String DEFAULT_PROJECT_CACHE_SPEC = "expireAfterAccess=10m,refreshAfterWrite=1m"
    public static final String DEFAULT_ACL_CACHE_SPEC = "refreshAfterWrite=2m"
    public static final String DEFAULT_FILE_CACHE_SPEC = "refreshAfterWrite=10m"
    def FrameworkService frameworkService
    //TODO: refactor to use configStorageService
    private StorageTree rundeckConfigStorageTree
    ApplicationContext applicationContext
    ConfigurationService configurationService
    def grailsApplication
    def metricService
    def nodeService
    /**
     * Scheduled executor for retries
     */
    private ExecutorService executor = Executors.newFixedThreadPool(2)

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
    Collection<String> listFrameworkProjectNames() {
        def c = Project.createCriteria()
        c.list {
            projections {
                property "name"
            }
        }
    }
    IRundeckProject getFrameworkProject(final String name) {
        if (null==projectCache.getIfPresent(name) && !existsFrameworkProject(name)) {
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

    //basic creation, created via spec string in afterPropertiesSet()
    def LoadingCache<String, IRundeckProject> projectCache =
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
        def spec = configurationService?.getCacheSpecFor("projectManagerService", "projectCache", DEFAULT_PROJECT_CACHE_SPEC)?:DEFAULT_PROJECT_CACHE_SPEC

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
        def spec2 = configurationService?.getCacheSpecFor("projectManagerService", "aclSourceCache", DEFAULT_ACL_CACHE_SPEC)?:DEFAULT_ACL_CACHE_SPEC

        log.debug("sourceCache: creating from spec: ${spec2}")

        sourceCache = CacheBuilder.from(spec2)
                                  .recordStats()
                                  .build(
                new CacheLoader<ProjectFile, CacheableYamlSource>() {
                    public CacheableYamlSource load(ProjectFile key) {
                        log.debug("sourceCache: loading source "+key)
                        return loadYamlSource(key);
                    }

                    @Override
                    ListenableFuture<CacheableYamlSource> reload(final ProjectFile key, final CacheableYamlSource oldValue)
                            throws Exception
                    {
                        if (needsReloadAcl(key,oldValue)) {
                            ListenableFutureTask<CacheableYamlSource> task = ListenableFutureTask.create(
                                    new Callable<CacheableYamlSource>() {
                                        public CacheableYamlSource call() {

                                            log.debug("sourceCache: reloading source "+key)
                                            return loadYamlSource(key);
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

        def spec3 = configurationService?.getCacheSpecFor("projectManagerService", "fileCache", DEFAULT_FILE_CACHE_SPEC)?:DEFAULT_FILE_CACHE_SPEC

        log.debug("fileCache: creating from spec: ${spec3}")
        //basic creation, created via spec string in afterPropertiesSet()
        fileCache =
                CacheBuilder.from(spec3)
                            .recordStats()
                            .build(
                        new CacheLoader<ProjectFile, Optional<String>>() {
                            public Optional<String> load(ProjectFile key) {
                                return Optional.fromNullable(readProjectFileResourceAsString(key.project,key.path));
                            }
                            @Override
                            ListenableFuture<Optional<String>> reload(final ProjectFile key, final Optional<String> oldValue)
                                    throws Exception
                            {
                                ListenableFutureTask<Optional<String>> task = ListenableFutureTask.create{
                                    log.debug("fileCache: reloading file "+key)
                                    return Optional.fromNullable(readProjectFileResourceAsString(key.project,key.path));
                                }
                                executor.execute(task);
                                return task;
                            }
                        }
                )

        MetricRegistry registry = metricService?.getMetricRegistry()
        Util.addCacheMetrics(this.class.name+".projectCache", registry, projectCache)
        Util.addCacheMetrics(this.class.name+".sourceCache", registry, sourceCache)
        Util.addCacheMetrics(this.class.name+".fileCache", registry, fileCache)
    }


    boolean existsProjectFileResource(String projectName, String path) {
        def storagePath = "projects/" + projectName + (path.startsWith("/")?path:"/${path}")
        return getStorage().hasResource(storagePath)
    }
    boolean existsProjectDirResource(String projectName, String path) {
        def storagePath = "projects/" + projectName + (path.startsWith("/")?path:"/${path}")
        return getStorage().hasDirectory(storagePath)
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
        Streams.copy(resource.contents.inputStream,output,false)
    }
    /**
     * Read or load the cached contents of a project file
     * @param projectName
     * @param path
     * @return
     */
    String readCachedProjectFileAsAstring(String projectName, String path) {
        Optional<String> cached = fileCache.get(ProjectFile.of(projectName, path))
        return cached.isPresent() ? cached.get() : null
    }
    /**
     * Read file contents as a string, or return null if not found
     * @param projectName
     * @param path
     * @return contents as string or null
     */
    String readProjectFileResourceAsString(String projectName, String path){
        if (!existsProjectFileResource(projectName, path)) {
            return null
        }
        def baos = new ByteArrayOutputStream()
        def len = readProjectFileResource(projectName, path, baos)
        return baos.toString()
    }
    /**
     * List the full paths of file resources in the directory at the given path
     * @param projectName projectname
     * @param path path directory path
     * @param pattern pattern match
     * @return
     */
    List<String> listProjectDirPaths(String projectName, String path, String pattern=null) {
        def prefix = 'projects/' + projectName
        def storagePath = prefix + (path.startsWith("/")?path:"/${path}")
        def resources = getStorage().listDirectory(storagePath)
        def outprefix=path.endsWith('/')?path.substring(0,path.length()-1):path
        resources.collect{Resource<ResourceMeta> res->
            def pathName=res.path.name + (res.isDirectory()?'/':'')
            (!pattern || pathName ==~ pattern) ? (outprefix+'/'+pathName) : null
        }.findAll{it}
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
        def res=getStorage().
                updateResource(storagePath, DataUtil.withStream(input, meta, StorageUtil.factory()))
        sourceCache.invalidate(ProjectFile.of(projectName, path))
        res
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

        def res=getStorage().
                createResource(storagePath, DataUtil.withStream(input, meta, StorageUtil.factory()))
        sourceCache.invalidate(ProjectFile.of(projectName, path))
        res
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
        fileCache.invalidate(ProjectFile.of(projectName, path))
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
        sourceCache.invalidate(ProjectFile.of(projectName, path))
        fileCache.invalidate(ProjectFile.of(projectName, path))
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
        properties['project.name']=projectName
        properties.store(baos, MIME_TYPE_PROJECT_PROPERTIES+";name=" + projectName)
        def bais = new ByteArrayInputStream(baos.toByteArray())

        def metadata = [(StorageUtil.RES_META_RUNDECK_CONTENT_TYPE):MIME_TYPE_PROJECT_PROPERTIES]
        def resource = writeProjectFileResource(projectName, storagePath, bais, metadata)

        projectCache.invalidate(projectName)
        nodeService.expireProjectNodes(projectName)
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
        nodeService.expireProjectNodes(projectName)
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
        def rdprojectconfig = new RundeckProjectConfig(projectName,
                                                       createProjectPropertyLookup(projectName, res.config ?: new Properties()),
                                                       createDirectProjectPropertyLookup(projectName, res.config ?: new Properties()),
                                                       res.lastModified
        )

        def newproj= new RundeckProject(rdprojectconfig, this)
        newproj.info = new ProjectInfo(
                projectName: projectName,
                projectService: this,
                description: rdprojectconfig.hasProperty('project.description') ? rdprojectconfig.getProperty(
                        'project.description'
                ) : null
        )
        newproj.nodesFactory = nodeService
        return newproj
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
        def rdprojectconfig = new RundeckProjectConfig(project.name,
                                                       createProjectPropertyLookup(project.name, resource.config ?: new Properties()),
                                                       createDirectProjectPropertyLookup(project.name, resource.config ?: new Properties()),
                                                       resource.lastModified
        )
        project.projectConfig=rdprojectconfig
        project.nodesFactory = nodeService
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
        def rdprojectconfig = new RundeckProjectConfig(project.name,
                                                       createProjectPropertyLookup(project.name, resource.config ?: new Properties()),
                                                       createDirectProjectPropertyLookup(project.name, resource.config ?: new Properties()),
                                                       resource.lastModified
        )
        project.projectConfig=rdprojectconfig
        project.nodesFactory = nodeService
    }
    Map setProjectProperties(final String projectName, final Properties properties) {
        Project found = Project.findByName(projectName)
        if (!found) {
            throw new IllegalArgumentException("project does not exist: " + projectName)
        }
        Map resource=storeProjectConfig(projectName, properties)
        resource
    }
    //basic creation, created via spec string in afterPropertiesSet()
    private LoadingCache<ProjectFile, CacheableYamlSource> sourceCache =
            CacheBuilder.newBuilder()
                        .refreshAfterWrite(2, TimeUnit.MINUTES)
                        .build(
                    new CacheLoader<ProjectFile, CacheableYamlSource>() {
                        public CacheableYamlSource load(ProjectFile key) {
                            return loadYamlSource(key);
                        }
                    }
            );

    private CacheableYamlSource loadYamlSource(ProjectFile key){
        def exists = existsProjectFileResource(key.project,key.path)
        log.debug("ProjectManagerService.loadYamlSource. path: ${key.project}, exists: ${exists}")
        if(!exists){
            return null
        }
        def resource = getProjectFileResource(key.project,key.path)
        def file = resource.contents
        def text =file.inputStream.getText()
        YamlProvider.sourceFromString("[project:${key.project}]${key.path}", text, file.modificationTime)
    }
    /**
     * Create Authorization using project-owned aclpolicies
     * @param projectName name of the project
     * @return authorization
     */
    public Authorization getProjectAuthorization(String projectName) {
        log.debug("ProjectManagerService.getProjectAuthorization for ${projectName}")
        //TODO: list of files is always reloaded?
        def paths = listProjectDirPaths(projectName, "acls", ".*\\.aclpolicy")
        log.debug("ProjectManagerService.getProjectAuthorization. paths= ${paths}")
        def sources = paths.collect { String zpath ->
            sourceCache.get(ProjectFile.of(projectName, zpath))
        }.findAll { it != null }
        def context = AuthorizationUtil.projectContext(projectName)
        return AclsUtil.createAuthorization(new Policies(PoliciesCache.fromSources(sources,context)))
    }
    boolean needsReloadAcl(ProjectFile key,CacheableYamlSource source) {

        boolean needsReload=true
        Storage.withNewSession {
            def exists=existsProjectFileResource(key.project,key.path)
            def resource=exists?getProjectFileResource(key.project,key.path):null
            needsReload = resource == null ||
                    source.lastModified == null ||
                    resource.contents.modificationTime > source.lastModified
        }
        needsReload
    }

    /**
     * Load the project config and node support
     * @param project
     * @return
     */
    IRundeckProjectConfig loadProjectConfig(final String project) {
        def resource = loadProjectConfigResource(project)
        def rdprojectconfig = new RundeckProjectConfig(project,
                                                 createProjectPropertyLookup(
                                                         project,
                                                         resource.config ?: new Properties()
                                                 ),
                                                 createDirectProjectPropertyLookup(
                                                         project,
                                                         resource.config ?: new Properties()
                                                 ),
                                                 resource.lastModified
        )
        return rdprojectconfig
    }
    /**
     * Load the project config and node support
     * @param project
     * @return
     */
    IRundeckProject loadProject(final String project) {
        if (!existsFrameworkProject(project)) {
            return null
        }
        long start=System.currentTimeMillis()
        log.info("Loading project definition for ${project}...")
        def rdproject = new RundeckProject(loadProjectConfig(project), this)
        //preload cached readme/motd
        String readme = readCachedProjectFileAsAstring(project,"readme.md")
        String motd = readCachedProjectFileAsAstring(project,"motd.md")
        rdproject.info = new ProjectInfo(
                projectName: project,
                projectService: this,
                description:
                        rdproject.hasProperty('project.description')
                                ? rdproject.getProperty('project.description')
                                : null
        )
        rdproject.nodesFactory = nodeService
        log.info("Loaded project ${project} in ${System.currentTimeMillis()-start}ms")
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

    //basic creation, created via spec string in afterPropertiesSet()
    private LoadingCache<ProjectFile, Optional<String>> fileCache =
            CacheBuilder.newBuilder()
                        .refreshAfterWrite(5, TimeUnit.MINUTES)
                        .build(
                    new CacheLoader<ProjectFile, Optional<String>>() {
                        public Optional<String> load(ProjectFile key) {
                            return Optional.fromNullable(readProjectFileResourceAsString(key.project,key.path));
                        }
                    }
            );

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
