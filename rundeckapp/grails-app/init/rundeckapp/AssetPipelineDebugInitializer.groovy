package rundeckapp

import groovy.util.logging.Slf4j
import jakarta.servlet.ServletContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import org.springframework.web.context.ServletContextAware

/**
 * Debug initializer to log asset pipeline and servlet context state
 * Pattern #42: Asset Pipeline Servlet Context Debugging
 * 
 * Logs critical information about servlet context initialization
 * to diagnose "Cannot get property 'contextPath' on null object" errors
 * in Selenium/functional tests.
 */
@Slf4j
@Component
class AssetPipelineDebugInitializer implements ServletContextAware, ApplicationListener<ApplicationReadyEvent> {
    
    ServletContext servletContext
    
    @Autowired(required = false)
    def assetProcessorService
    
    @Override
    void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext
        log.info("═══════════════════════════════════════════════════════")
        log.info("ASSET PIPELINE DEBUG: ServletContext set")
        log.info("  Context Path: ${servletContext?.contextPath ?: 'NULL'}")
        log.info("  Context Name: ${servletContext?.servletContextName ?: 'NULL'}")
        log.info("  Server Info: ${servletContext?.serverInfo ?: 'NULL'}")
        log.info("  Init Params: ${servletContext?.initParameterNames?.toList()}")
        log.info("═══════════════════════════════════════════════════════")
    }
    
    @Override
    void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("═══════════════════════════════════════════════════════")
        log.info("ASSET PIPELINE DEBUG: Application Ready")
        log.info("  ServletContext Available: ${servletContext != null}")
        log.info("  Context Path: ${servletContext?.contextPath ?: 'NULL'}")
        log.info("  AssetProcessorService Available: ${assetProcessorService != null}")
        
        // Try to access the config that asset pipeline uses
        try {
            def grailsApplication = event.applicationContext.getBean('grailsApplication')
            def assetsConfig = grailsApplication?.config?.grails?.assets
            log.info("  Grails Assets Config: ${assetsConfig}")
            log.info("  Assets URL: ${assetsConfig?.url ?: 'NOT SET'}")
        } catch (Exception e) {
            log.warn("  Could not access grails assets config: ${e.message}")
        }
        
        log.info("═══════════════════════════════════════════════════════")
    }
}

