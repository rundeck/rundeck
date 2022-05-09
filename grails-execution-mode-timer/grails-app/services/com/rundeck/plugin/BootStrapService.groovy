package com.rundeck.plugin

import grails.events.annotation.Subscriber

class BootStrapService {

    def executionModeService
    def updateModeProjectService

    def timer(String name,Closure clos){
        long bstart=System.currentTimeMillis()
        log.debug("BEGIN: ${name}")
        def res=clos()
        log.debug("${name} in ${System.currentTimeMillis()-bstart}ms")
        return res
    }

    @Subscriber("rundeck.bootstrap")
    void init() {
        timer("executionModeService.init") {
            executionModeService.initProcess()
        }

        timer("updateModeProjectService.init") {
            updateModeProjectService.initProcess()
        }
    }
}
