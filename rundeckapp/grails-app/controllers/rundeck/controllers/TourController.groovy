package rundeck.controllers

import grails.converters.JSON

class TourController {

    def tourLoaderService

    def index() { }

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
