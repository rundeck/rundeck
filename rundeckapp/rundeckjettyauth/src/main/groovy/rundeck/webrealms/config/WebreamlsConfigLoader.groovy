package rundeck.webrealms.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class WebreamlsConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(this)
    static final String DEFAULT_WEBREALMS_CONFIG_NAME = "DefaultWebrealmsConfig"
    static final String WEBREALMS_CONFIG_NAME = "WebrealmsConfig"


    static def loadWebrealmsConfig() {
        ClassLoader parent = WebreamlsConfigLoader.getClassLoader()
        GroovyClassLoader loader = new GroovyClassLoader(parent)

        def config = null

        try {
            config = loadAndParseConfigFromClass(loader,DEFAULT_WEBREALMS_CONFIG_NAME)

            try {
                def appConfig = loadAndParseConfigFromClass(loader,WEBREALMS_CONFIG_NAME)
                if (appConfig) {
                    log.debug("Configuring WebrealmsConfig")
                    config = config.merge(appConfig)
                }
            } catch (ClassNotFoundException cnfe) {
                //Ignore error when webrealms app level config file was not found
                log.warn("Unable to find webrealms config class: ${WEBREALMS_CONFIG_NAME}")
            }
        } catch (ClassNotFoundException e) {
            //Ignore error when default webrealms config file was not found
            log.warn("Unable to find default webrealms config class: ${DEFAULT_WEBREALMS_CONFIG_NAME}")
        }

        config?.webrealms
    }

    private static def loadAndParseConfigFromClass(GroovyClassLoader gcLoader,String configClassName) {
        def configClass = gcLoader.loadClass(configClassName)
        new ConfigSlurper().parse(configClass)
    }
}
