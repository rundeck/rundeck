package com.rundeck.plugin

class UrlMappings {

    static mappings = {
        "/menu/executionMode/executionLater"(controller: 'executionMode', action: 'getExecutionLater')
        "/menu/executionMode/executionLater/nextTime"(controller: 'executionMode', action: 'getNextExecutionChangeStatus')

        "/project/$project/configure/executionLater"(controller: 'editProject', action: 'getExecutionLater')
        "/project/$project/configure/executionLater/nextTime"(controller: 'editProject', action: 'getNextExecutionChangeStatus')

        "/api/$api_version/project/$project/enable/later"(controller: 'editProject', action: [POST:'apiProjectEnableLater'])
        "/api/$api_version/project/$project/disable/later"(controller: 'editProject', action: [POST:'apiProjectDisableLater'])

        "/api/$api_version/system/executions/enable/later"(controller: 'executionMode', action: [POST:'apiExecutionModeLaterActive'])
        "/api/$api_version/system/executions/disable/later"(controller: 'executionMode', action: [POST:'apiExecutionModeLaterPassive'])


        "/"(view:"/index")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
