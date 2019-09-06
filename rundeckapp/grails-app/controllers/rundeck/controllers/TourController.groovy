package rundeck.controllers

import grails.converters.JSON

class TourController {

    def tourLoaderService

    def index() { }

    def listAllTourManifests() {
        String project = request.getHeader("X-Tour-Project")
        render tourLoaderService.listAllTourManifests(project) as JSON
    }

    def list() {
        String project = request.getHeader("X-Tour-Project")
        render tourLoaderService.listTours(params.loaderName,project) as JSON
    }

    def getTour() {
        String project = request.getHeader("X-Tour-Project")
        render tourLoaderService.getTour(params.loaderName,params.tour,project) as JSON
    }


}
