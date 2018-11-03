package com.rundeck.plugin.localtours

import spock.lang.Shared
import spock.lang.Specification

class LocalToursSpec extends Specification {

    @Shared
    File tourDir
    @Shared
    String manifestFileName = "tour-manifest.json"
    @Shared
    String tourSubDir = "tours"

    def setupSpec() {
        tourDir = File.createTempDir()
        new File(tourDir,"tour-manifest.json") << getClass().getClassLoader().getResourceAsStream("test-manifest.json")
        File tsd = new File(tourDir,tourSubDir)
        tsd.mkdir()
        new File(tsd,"tour1.json") << getClass().getClassLoader().getResourceAsStream("tour1.json")
        new File(tsd,"tour2.json") << getClass().getClassLoader().getResourceAsStream("tour2.json")
    }


    def "Get Tours"() {
        when:
        LocalTours tours = new LocalTours()
        tours.tourBaseDir = tourDir.absolutePath
        tours.manifestFileName = manifestFileName
        tours.toursSubDir = tourSubDir

        then:
        tours.tourManifest
    }

    def "Get Tour"() {
        when:
        LocalTours tours = new LocalTours()
        tours.tourBaseDir = tourDir.absolutePath
        tours.manifestFileName = manifestFileName
        tours.toursSubDir = tourSubDir

        then:
        tours.getTour("tour1")
    }

}