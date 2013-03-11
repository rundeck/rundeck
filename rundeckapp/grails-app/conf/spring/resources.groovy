import com.dtolabs.rundeck.core.utils.GrailsServiceInjectorJobListener
import groovy.io.FileType

beans={
    defaultGrailsServiceInjectorJobListener(GrailsServiceInjectorJobListener){
        name= 'defaultGrailsServiceInjectorJobListener'
        services=[executionService: ref('executionService')]
        quartzScheduler=ref('quartzScheduler')
    }


    //**** Plugin behavior
    // implementation via netflix asgard

    xmlns lang: 'http://www.springframework.org/schema/lang'

    File pluginDir = new File("${application.config.rdeck.base}/libext/server/")
    if (pluginDir.exists()) {
        pluginDir.eachFileMatch(FileType.FILES, ~/.*\.groovy/) { File plugin ->
            String beanName = plugin.name.replace('.groovy', '')
            lang.groovy(id: beanName, 'script-source': "file:${application.config.rdeck.base}/libext/server/${plugin.name}",
                    'refresh-check-delay': application.config.plugin.refreshDelay ?: -1)
        }
    }
}
