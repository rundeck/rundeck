package rundeck

import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.NodeFileParserException
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.storage.StorageTree
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.dtolabs.rundeck.server.projects.RundeckProject
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import grails.transaction.Transactional
import org.rundeck.storage.data.DataUtil
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

import java.util.concurrent.TimeUnit

@Transactional
class ProjectManagerService implements ProjectManager, InitializingBean, ApplicationContextAware{
    private StorageTree rundeckConfigStorageTree
    ApplicationContext applicationContext

    @Override
    void afterPropertiesSet() throws Exception {
    }
    private StorageTree getStorage(){
        if(null==rundeckConfigStorageTree){
            rundeckConfigStorageTree=applicationContext.getBean("rundeckConfigStorageTree",StorageTree)
        }
        return rundeckConfigStorageTree
    }

    @Override
    Collection<IRundeckProject> listFrameworkProjects() {
        return Project.findAll().collect{
            new RundeckProject(it.name,null,this)
        }
    }

    @Override
    IRundeckProject getFrameworkProject(final String name) {
        if(!existsFrameworkProject(name)){
            throw new IllegalArgumentException("Project does not exist: "+name)
        }
        new RundeckProject(name,getProjectConfig(name), this)
    }

    @Override
    boolean existsFrameworkProject(final String project) {
        return Project.findByName(project)?true:false
    }

    @Override
    IRundeckProject createFrameworkProject(final String projectName) {
        return createFrameworkProject(projectName, new Properties())
    }

    //TODO: use spec for cache
    private LoadingCache<String, Properties> projectPropertiesCache = CacheBuilder.newBuilder()
//                                                  .maximumSize(1000)
                                                                                  .expireAfterAccess(10, TimeUnit.MINUTES)
                                                                                  .refreshAfterWrite(10, TimeUnit.MINUTES)
//                                                  .removalListener(MY_LISTENER)
                                                                                  .build(
            new CacheLoader<String, Properties>() {
                public Properties load(String key)  {
                    return loadProjectConfig(key);
                }
            });
    /**
     * Loads or returns cached properties
     * @param projectName
     * @return
     */
    public Properties getProjectConfig(String projectName) {
        projectPropertiesCache.get(projectName)
    }
    private Properties loadProjectConfig(String projectName) {
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
        return properties
    }
    private void storeProjectConfig(String projectName, Properties properties){
        def storagePath = "projects/" + projectName + "/etc/project.properties"
        def baos=new ByteArrayOutputStream()
        properties.store(baos,"project config "+projectName)
        def bais = new ByteArrayInputStream(baos.toByteArray())

        def metadata = [:]
        if(getStorage().hasResource(storagePath)){
            getStorage().updateResource(storagePath,DataUtil.withStream(bais,
                                                                                    metadata, StorageUtil.factory()))
        } else {
            getStorage().createResource(storagePath,DataUtil.withStream(bais,
                                                                                    metadata, StorageUtil.factory()))
        }
        projectPropertiesCache.put(projectName, properties)

    }
    private void deleteProjectConfig(String projectName){
        def storagePath = "projects/" + projectName + "/etc/project.properties"
        if(!getStorage().hasResource(storagePath)){
            throw new IllegalArgumentException("Project config does not exist: "+projectName)
        }
        getStorage().deleteResource(storagePath)
        projectPropertiesCache.invalidate(projectName)
    }

    @Override
    IRundeckProject createFrameworkProject(final String projectName, final Properties properties) {
        Project found = Project.findByName(projectName)
        if(!found) {
            def project=new Project(name: projectName)
            project.save()
        }
        storeProjectConfig(projectName,properties)

        return new RundeckProject(projectName, getProjectConfig(projectName),this)
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

    List<Map> listResourceModelConfigurations(final String project) {
        return FrameworkProject.listResourceModelConfigurations(project,getProjectConfig(project))
    }

    INodeSet getNodeSet(final String project) throws NodeFileParserException {
        throw new RuntimeException("not implemented")
    }

    void mergeProjectProperties(final String projectName, final Properties properties, final Set<String> removePrefixes) {
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
    }

    void setProjectProperties(final String projectName, final Properties properties) {
        Project found = Project.findByName(projectName)
        if(!found) {
            throw new IllegalArgumentException("project does not exist: " + projectName)
        }
        storeProjectConfig(projectName,properties)
    }
}
