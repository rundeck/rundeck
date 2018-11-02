package rundeck.services

import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.tours.Tour
import com.dtolabs.rundeck.plugins.tours.TourLoaderPlugin
import com.dtolabs.rundeck.plugins.tours.TourManifest

class TourLoaderService {

    def pluginService
    def tourLoaderPluginProviderService
    def frameworkService

    def listAllTourManifests() {
        def tourManifest = []
        pluginService.listPlugins(TourLoaderPlugin,tourLoaderPluginProviderService).each {
            TourLoaderPlugin tourLoader = pluginService.configurePlugin(it.key, tourLoaderPluginProviderService, frameworkService.getFrameworkPropertyResolver(), PropertyScope.Instance).instance
            tourManifest.add([provider:it.key,loader:tourLoader.loaderName,tours:tourLoader.tourManifest.tours])
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
