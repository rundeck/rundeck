package rundeck
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.common.ProjectNodeSupport
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
import org.rundeck.storage.data.DataUtil
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import rundeck.services.FrameworkService

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Transactional
class ProjectManagerService implements ProjectManager, ApplicationContextAware, InitializingBean {
    def FrameworkService frameworkService
    private StorageTree rundeckConfigStorageTree
    ApplicationContext applicationContext
    def grailsApplication

    private StorageTree getStorage(){
        if(null==rundeckConfigStorageTree){
            rundeckConfigStorageTree=applicationContext.getBean("rundeckConfigStorageTree",StorageTree)
        }
        return rundeckConfigStorageTree
    }

    @Override
    Collection<IRundeckProject> listFrameworkProjects() {
        return Project.findAll().collect{
            getFrameworkProject(it.name)
        }
    }

    @Override
    IRundeckProject getFrameworkProject(final String name) {
        if(!existsFrameworkProject(name)){
            throw new IllegalArgumentException("Project does not exist: "+name)
        }
        def result = projectCache.get(name)
        if(!result){
            throw new IllegalArgumentException("Project does not exist: "+name)
        }
        return result
    }

    @Override
    boolean existsFrameworkProject(final String project) {
        return Project.findByName(project)?true:false
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
    }

    /**
     * Loads or returns cached properties
     * @param projectName
     * @return
     */
    public Properties getProjectConfig(String projectName) {
        projectCache.get(projectName).getProperties()
    }

    Date getProjectConfigLastModified(String projectName) {

        def storagePath = "projects/" + projectName + "/etc/project.properties"
        if(!getStorage().hasResource(storagePath)){
            return null
        }
        def resource = getStorage().getResource(storagePath)
        //load as properties file
        def properties = new Properties()
        try{
            properties.load(resource.contents.inputStream)
        }catch(IOException e){
            log.error("Failed loading project properties from storage: ${storagePath}: "+e.message,e)
        }
        resource.getContents().modificationTime
    }
    private Map loadProjectConfigResource(String projectName) {
        def storagePath = "projects/" + projectName + "/etc/project.properties"
        if(!getStorage().hasResource(storagePath)){
            return [:]
        }
        def resource = getStorage().getResource(storagePath)
        //load as properties file
        def properties = new Properties()
        try{
            properties.load(resource.contents.inputStream)
        }catch(IOException e){
            log.error("Failed loading project properties from storage: ${storagePath}: "+e.message,e)
        }


        return [config: properties, lastModified: resource.contents.modificationTime, creationTime: resource.contents.creationTime]

    }
    private Date storeProjectConfig(String projectName, Properties properties){
        def storagePath = "projects/" + projectName + "/etc/project.properties"
        def baos=new ByteArrayOutputStream()
        properties.store(baos,"project config "+projectName)
        def bais = new ByteArrayInputStream(baos.toByteArray())

        def metadata = [:]
        def resource
        if(getStorage().hasResource(storagePath)){
            resource=getStorage().updateResource(storagePath,DataUtil.withStream(bais,
                                                                                    metadata, StorageUtil.factory()))
        } else {
            resource=getStorage().createResource(storagePath,DataUtil.withStream(bais,
                                                                                    metadata, StorageUtil.factory()))
        }
        projectCache.invalidate(projectName)
        resource.contents.modificationTime
    }
    private void deleteProjectConfig(String projectName){
        def storagePath = "projects/" + projectName + "/etc/project.properties"
        if(!getStorage().hasResource(storagePath)){
            throw new IllegalArgumentException("Project config does not exist: "+projectName)
        }
        //TODO: recursively delete storage path
        getStorage().deleteResource(storagePath)
        projectCache.invalidate(projectName)
    }

    private IPropertyLookup createProjectPropertyLookup(String projectName, Properties config){
        final Properties ownProps = new Properties();
        ownProps.setProperty("project.name", projectName);
        def create = PropertyLookup.create(
                config,
                PropertyLookup.create(ownProps, frameworkService.getRundeckFramework().propertyLookup)
        )

        create.expand()
        return create
    }
    @Override
    IRundeckProject createFrameworkProject(final String projectName, final Properties properties) {
        Project found = Project.findByName(projectName)
        if(!found) {
            def project=new Project(name: projectName)
            project.save()
        }
        Date lastmod=storeProjectConfig(projectName,properties)

        return new RundeckProject(projectName, createProjectPropertyLookup(projectName,getProjectConfig(projectName)),this,lastmod)
    }

    @Override
    void removeFrameworkProject(final String projectName) {
        Project found = Project.findByName(projectName)
        if(!found) {
            throw new IllegalArgumentException("project does not exist: " + projectName)
        }
        found.delete(flush:true)
        deleteProjectConfig(projectName)
    }

    @Override
    IRundeckProject createFrameworkProjectStrict(final String projectName, final Properties properties) {
        Project found = Project.findByName(projectName)
        if(found) {
            throw new IllegalArgumentException("project exists: " + projectName)
        }
        return createFrameworkProject(projectName, properties)
    }


    IPropertyLookup mergeProjectProperties(final String projectName, final Properties properties, final Set<String> removePrefixes) {
        Project found = Project.findByName(projectName)
        if(!found) {
            throw new IllegalArgumentException("project does not exist: " + projectName)
        }
        def newprops = new Properties()
        def oldprops=getProjectConfig(projectName)
        if(removePrefixes) {
            oldprops.propertyNames().each { String k ->
                if(!removePrefixes.find{k.startsWith(it)}){
                    newprops.put(k,oldprops.getProperty(k))
                }
            }
        }
        newprops.putAll(properties)
        storeProjectConfig(projectName,newprops)
        createProjectPropertyLookup(projectName,newprops)
    }

    IPropertyLookup setProjectProperties(final String projectName, final Properties properties) {
        Project found = Project.findByName(projectName)
        if(!found) {
            throw new IllegalArgumentException("project does not exist: " + projectName)
        }
        storeProjectConfig(projectName,properties)
        createProjectPropertyLookup(projectName,properties)
    }

    /**
     * Load the project config and node support
     * @param project
     * @return
     */
    IRundeckProject loadProject(final String project) {
        if(!existsFrameworkProject(project)){
            throw new IllegalArgumentException("Project does not exist: "+project)
        }
        def resource = loadProjectConfigResource(project)
        def rdproject=new RundeckProject(project,createProjectPropertyLookup(project,resource.config), this,resource.lastModified)

        def framework = frameworkService.getRundeckFramework()
        def nodes=new ProjectNodeSupport(
                rdproject,
                framework.getResourceFormatGeneratorService(),
                framework.getResourceModelSourceService()
        )
        rdproject.projectNodes=nodes
        return rdproject
    }

    boolean needsReload(IRundeckProject project) {
        Project rdproject = Project.findByName(project.name)
        boolean needsReload= rdproject == null ||
                project.configLastModifiedTime == null ||
                getProjectConfigLastModified(project.name) > project.configLastModifiedTime
        needsReload
    }

}
