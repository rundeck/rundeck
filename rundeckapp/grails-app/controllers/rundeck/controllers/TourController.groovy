package rundeck.controllers

import grails.converters.JSON
import rundeck.services.TourLoaderService

class TourController {

    TourLoaderService tourLoaderService

    def index() { }

    def listTourLoaders() {
        render tourLoaderService.listTourLoaderPlugins() as JSON
    }

    def listAllTourManifests() {
        render tourLoaderService.listAllTourManifests() as JSON
    }

    def list() {
        render tourLoaderService.listTours(params.loaderName) as JSON
    }

    def getTour() {
        render tourLoaderService.getTour(params.loaderName,params.tour) as JSON
    }


}
