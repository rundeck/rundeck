package rundeck.services

import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.tours.TourLoaderPlugin

class TourLoaderService {

    def rundeckPluginRegistry
    def pluginService
    def frameworkService

    def listAllTourManifests() {
        def tourManifest = []
        PluggableProviderService tourLoaderProviderService = rundeckPluginRegistry.createPluggableService(TourLoaderPlugin.class)

        pluginService.listPlugins(TourLoaderPlugin).each { prov ->
            TourLoaderPlugin tourLoader = pluginService.configurePlugin(prov.key, tourLoaderProviderService, frameworkService.getFrameworkPropertyResolver(), PropertyScope.Instance).instance
            def title = getPluginTitle(tourLoader)
            def manifest = tourLoader.tourManifest
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

    Map listTours(String loaderName) {
        PluggableProviderService tourLoaderProviderService = rundeckPluginRegistry.createPluggableService(TourLoaderPlugin.class)
        TourLoaderPlugin tourLoader = pluginService.configurePlugin(loaderName, tourLoaderProviderService, frameworkService.getFrameworkPropertyResolver(), PropertyScope.Instance).instance
        tourLoader.tourManifest
    }

    Map getTour(String loaderName, String tourKey) {
        PluggableProviderService tourLoaderProviderService = rundeckPluginRegistry.createPluggableService(TourLoaderPlugin.class)
        TourLoaderPlugin tourLoader = pluginService.configurePlugin(loaderName, tourLoaderProviderService, frameworkService.getFrameworkPropertyResolver(), PropertyScope.Instance).instance
        tourLoader.getTour(tourKey)
    }
}
