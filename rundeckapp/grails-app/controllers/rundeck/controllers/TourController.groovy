package rundeck.controllers

import com.dtolabs.rundeck.plugins.tours.TourLoader
import com.dtolabs.rundeck.server.plugins.tours.HttpTourLoader
import grails.converters.JSON

class TourController {

    private static final String RUNDECK_LOADER = "RUNDECK"

    private static Map<String, TourLoader> tourLoaders = [:]
    static {
        HttpTourLoader rundeckTourLoader = new HttpTourLoader()
        rundeckTourLoader.loaderName = RUNDECK_LOADER
        rundeckTourLoader.tourManifestEndpoint = "https://s3.amazonaws.com/tours.rundeck.com"
        tourLoaders[RUNDECK_LOADER] = rundeckTourLoader
    }

    def index() { }

    def list() {
        def tours = []
        tourLoaders.values().each {
            tours.addAll(it.tourManifest.tours)
        }
        render tours as JSON
    }

    def getTour() {
        String tourLoaderName = RUNDECK_LOADER
        String tourName = params.tour

        render tourLoaders[tourLoaderName].getTour(tourName) as JSON
    }


}
