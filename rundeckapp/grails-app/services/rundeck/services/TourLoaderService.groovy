package rundeck.services

import com.dtolabs.rundeck.plugins.tours.Tour
import com.dtolabs.rundeck.plugins.tours.TourLoaderPlugin
import com.dtolabs.rundeck.plugins.tours.TourManifest
import grails.gorm.transactions.Transactional

@Transactional
class TourLoaderService {

    def pluginService

    def listAllTourManifests() {
        def tourManifest = []
        pluginService.listPlugins(TourLoaderPlugin).each {
            tourManifest.add([loader:it.value.instance.loaderName,tours:it.value.instance.tourManifest.tours])
        }
        return tourManifest
    }

    def listTourLoaderPlugins() {
        pluginService.listPlugins(TourLoaderPlugin)*.collect { it.value.instance.loaderName }
    }

    TourManifest listTours(String loaderName) {
        TourLoaderPlugin tourLoader = pluginService.listPlugins(TourLoaderPlugin).find { it.value.instance.loaderName == loaderName }.value.instance
        tourLoader.tourManifest
    }

    Tour getTour(String loaderName, String tourKey) {
        TourLoaderPlugin tourLoader = pluginService.listPlugins(TourLoaderPlugin).find { it.value.instance.loaderName == loaderName }.value.instance
        tourLoader.getTour(tourKey)
    }
}
