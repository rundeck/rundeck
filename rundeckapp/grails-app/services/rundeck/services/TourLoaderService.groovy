package rundeck.services

import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.tours.TourLoaderPlugin

class TourLoaderService {
    def ServiceProviderLoader rundeckServerServiceProviderLoader
    def pluginService
    def frameworkService

    def listAllTourManifests(String project = null) {
        def tourManifest = []
        PluggableProviderService tourLoaderProviderService = rundeckServerServiceProviderLoader.createPluggableService(TourLoaderPlugin.class)

       pluginService.listPlugins(TourLoaderPlugin).each { prov ->
            TourLoaderPlugin tourLoader = pluginService.configurePlugin(prov.key, tourLoaderProviderService, frameworkService.getFrameworkPropertyResolver(project), PropertyScope.Instance).instance
            def title = getPluginTitle(tourLoader)

            def manifest = project ? tourLoader.getTourManifest(project) : tourLoader.tourManifest

            if(manifest){
                def tours = manifest.tours
                def groupedTours = tours.findAll { it.group }
                def ungroupedTours = tours.findAll { !it.group }
                groupedTours.groupBy{ it.group }.each { group, gtours  ->
                    tourManifest.add([provider:prov.key,loader:group,tours:gtours])
                }
                if(!ungroupedTours.isEmpty()) {
                    tourManifest.add([provider:prov.key,loader:manifest.name ?: title ?: prov.key,tours:ungroupedTours])
                }
            }

        }
        return tourManifest
    }

    String getPluginTitle(final TourLoaderPlugin tourLoaderPlugin) {
        if(tourLoaderPlugin instanceof Describable) return tourLoaderPlugin.description?.title
        if(tourLoaderPlugin.class.isAnnotationPresent(PluginDescription)) {
            PluginDescription desc = (PluginDescription)tourLoaderPlugin.class.getAnnotation(PluginDescription)
            return desc.title()
        }
        return null
    }

    Map listTours(String loaderName, String project = null) {
        PluggableProviderService tourLoaderProviderService = rundeckServerServiceProviderLoader.createPluggableService(TourLoaderPlugin.class)
        TourLoaderPlugin tourLoader = pluginService.configurePlugin(loaderName, tourLoaderProviderService, frameworkService.getFrameworkPropertyResolver(project), PropertyScope.Instance).instance

        if(project) return tourLoader.getTourManifest(project)
        else return tourLoader.getTourManifest()
    }

    Map getTour(String loaderName, String tourKey, String project = null) {
        PluggableProviderService tourLoaderProviderService = rundeckServerServiceProviderLoader.createPluggableService(TourLoaderPlugin.class)
        TourLoaderPlugin tourLoader = pluginService.configurePlugin(loaderName, tourLoaderProviderService, frameworkService.getFrameworkPropertyResolver(project), PropertyScope.Instance).instance

        if(tourKey && project) return tourLoader.getTour(tourKey,project)
        else return tourLoader.getTour(tourKey)


    }
}
