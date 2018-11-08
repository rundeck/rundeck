package rundeck.services

import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.tours.TourLoaderPlugin

class TourLoaderService {

    def pluginService
    def tourLoaderPluginProviderService
    def frameworkService

    def listAllTourManifests() {
        def tourManifest = []
        pluginService.listPlugins(TourLoaderPlugin,tourLoaderPluginProviderService).each {
            TourLoaderPlugin tourLoader = pluginService.configurePlugin(it.key, tourLoaderPluginProviderService, frameworkService.getFrameworkPropertyResolver(), PropertyScope.Instance).instance
            def tours = tourLoader.tourManifest.tours
            def groupedTours = tours.findAll { it.group }
            def ungroupedTours = tours.findAll { !it.group }
            groupedTours.groupBy{ it.group }.each { group, gtours  ->
                tourManifest.add([provider:it.key,loader:group,tours:gtours])
            }
            tourManifest.add([provider:it.key,loader:tourLoader.loaderName,tours:ungroupedTours])
        }
        return tourManifest
    }

    Map listTours(String loaderName) {
        TourLoaderPlugin tourLoader = pluginService.configurePlugin(loaderName, tourLoaderPluginProviderService, frameworkService.getFrameworkPropertyResolver(), PropertyScope.Instance).instance
        tourLoader.tourManifest
    }

    Map getTour(String loaderName, String tourKey) {
        TourLoaderPlugin tourLoader = pluginService.configurePlugin(loaderName, tourLoaderPluginProviderService, frameworkService.getFrameworkPropertyResolver(), PropertyScope.Instance).instance
        tourLoader.getTour(tourKey)
    }
}
