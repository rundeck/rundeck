package rundeck.jetty.auth

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory
import rundeck.webrealms.config.WebreamlsConfigLoader

class RundeckJettyContainerCustomizer implements EmbeddedServletContainerCustomizer {
    private static final Logger log = LoggerFactory.getLogger(this)

    @Override
    void customize(final ConfigurableEmbeddedServletContainer container) {

        if(container instanceof JettyEmbeddedServletContainerFactory) {
            log.info("Adding webrealms jetty customizer")
            container.addServerCustomizers(new WebrealmsJettyCustomizer(WebreamlsConfigLoader.loadWebrealmsConfig()))
        }
    }
}
