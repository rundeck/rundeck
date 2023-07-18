package rundeck.services

import com.dtolabs.rundeck.core.common.NodeSourceLoader
import com.dtolabs.rundeck.core.common.NodeSourceLoaderConfig
import com.dtolabs.rundeck.core.common.SourceDefinition
import com.dtolabs.rundeck.core.plugins.CloseableProvider
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.resources.ResourceModelSource
import com.dtolabs.rundeck.core.resources.ResourceModelSourceService
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import org.rundeck.app.spi.Services

class NodeSourceLoaderService implements NodeSourceLoader{

    PluginService pluginService
    FrameworkService frameworkService
    ProjectManagerService projectManagerService
    Services rundeckSpiBaseServicesProvider

    @Override
    NodeSourceLoaderConfig getSourceForConfiguration(String project, SourceDefinition definition) {
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

            CloseableProvider<ResourceModelSource> closeableProvider = retained.closeable.convert(
                    ResourceModelSourceService.factoryConverter(
                            rundeckSpiBaseServicesProvider.combine(services),
                            definition.properties
                    )
            )

            return new NodeSourceLoaderConfigImpl(closeableProvider, services)
        } else {
            return null
        }
        return null
    }


    class NodeSourceLoaderConfigImpl implements NodeSourceLoaderConfig{

        CloseableProvider<ResourceModelSource> closeableProvider
        Services services

        NodeSourceLoaderConfigImpl(CloseableProvider<ResourceModelSource> closeableProvider, Services services) {
            this.closeableProvider = closeableProvider
            this.services = services
        }

        @Override
        CloseableProvider<ResourceModelSource> getCloseableProvider() {
            return closeableProvider
        }

        @Override
        Services getServices() {
            return services
        }
    }

}
