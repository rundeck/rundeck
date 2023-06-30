package rundeck.services

import com.dtolabs.rundeck.core.common.NodeSourceLoader
import com.dtolabs.rundeck.core.common.SourceDefinition
import com.dtolabs.rundeck.core.plugins.CloseableProvider
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.resources.ResourceModelSource
import com.dtolabs.rundeck.core.resources.ResourceModelSourceService
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import org.rundeck.app.spi.Services
import org.springframework.beans.factory.annotation.Autowired

class NodeSourceLoaderService implements NodeSourceLoader{

    PluginService pluginService
    FrameworkService frameworkService
    ProjectManagerService projectManagerService
    Services rundeckSpiBaseServicesProvider

    @Override
    CloseableProvider<ResourceModelSource> getSourceForConfiguration(String project, SourceDefinition definition) {
        def framework = frameworkService.getRundeckFramework()
        def resourceModelSourceService = framework.getResourceModelSourceService()
        def rdprojectconfig = framework.getFrameworkProjectMgr().loadProjectConfig(project)

        def retained = pluginService.rundeckPluginRegistry.retainConfigurePluginByName(
                definition.type,
                resourceModelSourceService,
                PropertyResolverFactory.pluginPrefixedScoped(
                        PropertyResolverFactory.instanceRetriever(definition.properties),
                        PropertyResolverFactory.instanceRetriever(rdprojectconfig.getProjectProperties()),
                        framework.getPropertyRetriever()
                ),
                PropertyScope.Instance
        )

        if (null != retained && null != retained.closeable) {
            //load services
            def services = projectManagerService.getNonAuthorizingProjectServicesForPlugin(
                    project,
                    ServiceNameConstants.ResourceModelSource,
                    definition.type
            )
            return retained.closeable.convert(
                    ResourceModelSourceService.factoryConverter(
                            rundeckSpiBaseServicesProvider.combine(services),
                            definition.properties
                    )
            )
        } else {
            return null
        }
        return null
    }
}
