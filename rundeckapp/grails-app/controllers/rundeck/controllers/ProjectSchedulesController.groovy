/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import grails.converters.JSON
import org.rundeck.core.auth.AuthConstants
import org.springframework.context.ApplicationContext
import org.springframework.web.multipart.MultipartFile
import rundeck.services.AuthorizationService

import rundeck.services.FrameworkService

class ProjectSchedulesController extends ControllerBase{

    FrameworkService frameworkService
    def AuthorizationService authorizationService
    def ApplicationContext applicationContext
    def schedulerService
    static allowedMethods = [
            //deleteFilter    : 'POST',
    ]

    def index(){
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, params.project)

        if (unauthorizedResponse(
                frameworkService.authorizeProjectResourceAll(
                        authContext,
                        AuthorizationUtil.resourceType('event'),
                        [AuthConstants.ACTION_READ],
                        params.project
                ),
                AuthConstants.ACTION_ADMIN,
                'schedules',
                params.project
        )) {
            return
        }
    }

    def reassociate() {
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, params.project)
        if (unauthorizedResponse(
                frameworkService.authorizeProjectResourceAll(
                        authContext,
                        AuthorizationUtil.resourceType('event'),
                        [AuthConstants.ACTION_READ],
                        params.project
                ),
                AuthConstants.ACTION_ADMIN,
                'schedules',
                params.project
        )) {
            return
        }
        schedulerService.reassociate( request.JSON.scheduleDefId, request.JSON.jobUuidsToAssociate, request.JSON.jobUuidsToDeassociate );

        render(contentType:'application/json',text:
                ([
                        result: 'ok'
                ] )as JSON
        )
    }

    def filteredProjectSchedules() {
        def offset = 0
        if(params.offset){
            offset = params.offset
        }

        int max = 10

        def result = schedulerService.retrieveProjectSchedulesDefinitionsWithFilters(params.project, params.name, [max: max, offset: offset])
        result?.schedulesMap = result?.schedules?.collect{
            return it.toMap()
        }
        //TODO: implement auth
        render(contentType:'application/json',text:
                ([
                        schedules       : result.schedulesMap,
                        totalRecords    : result.totalRecords,
                        offset          : offset,
                        maxRows         : max,
                        schedulesMap    : result.schedulesMap

                ] )as JSON
        )
    }

    def persistSchedule(){

        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, params.project)
        if (unauthorizedResponse(
                frameworkService.authorizeProjectResourceAll(
                        authContext,
                        AuthorizationUtil.resourceType('event'),
                        [AuthConstants.ACTION_READ],
                        params.project
                ),
                AuthConstants.ACTION_ADMIN,
                'schedules',
                params.project
        )) {
            return
        }
        def result = schedulerService.persistScheduleDef(request.JSON.schedule)
        def errors
        if(result.failed){
            errors = result.schedule.errors.allErrors.collect {g.message(error: it)}.join(", ")
        }
        result?.schedulesMap = result?.schedule?.collect{
            return it.toMap()
        }
        render(contentType:'application/json',text:
                ([
                        schedule        : result.schedulesMap,
                        errors          : errors,
                        failed          : result.failed

                ] )as JSON
        )
    }

    def deleteSchedule(){
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, params.project)
        if (unauthorizedResponse(
                frameworkService.authorizeProjectResourceAll(
                        authContext,
                        AuthorizationUtil.resourceType('event'),
                        [AuthConstants.ACTION_READ],
                        params.project
                ),
                AuthConstants.ACTION_ADMIN,
                'schedules',
                params.project
        )) {
            return
        }
        schedulerService.delete(request.JSON.schedule)
        render(contentType:'application/json',text:
                ([
                        result: 'ok'
                ] )as JSON
        )
    }

    def uploadFileDefinition (){
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, params.project)
        if (unauthorizedResponse(
                frameworkService.authorizeProjectResourceAll(
                        authContext,
                        AuthorizationUtil.resourceType('event'),
                        [AuthConstants.ACTION_READ],
                        params.project
                ),
                AuthConstants.ACTION_ADMIN,
                'schedules',
                params.project
        )) {
            return
        }
        def result = [:]
        result.success = true
        def file = request.getFile("scheduleUploadSelect")
        if (!file || file.empty) {
            result.success = false
            result.errors = ["No file was uploaded."]
        }

        if(result.success){
            result = schedulerService.parseUploadedFile(file.getInputStream(), params.project, params.update)
        }
        render(contentType:'application/json',text:
                ([
                        success: result.success,
                        errors : result.errors
                ] )as JSON
        )
    }

    def massiveScheduleDelete(){
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, params.project)
        if (unauthorizedResponse(
                frameworkService.authorizeProjectResourceAll(
                        authContext,
                        AuthorizationUtil.resourceType('event'),
                        [AuthConstants.ACTION_READ],
                        params.project
                ),
                AuthConstants.ACTION_ADMIN,
                'schedules',
                params.project
        )) {
            return
        }
        def schedulesId = request.JSON.schedulesId
        def result = schedulerService.massiveScheduleDelete(schedulesId, params.project)
        render(contentType:'application/json',text:
                ([
                        success     : result.success,
                        messages    : result.messages
                ] )as JSON
        )
    }

}

class ScheduleDefYAMLException extends Exception{

    public ScheduleDefYAMLException() {
        super();
    }

    public ScheduleDefYAMLException(String s) {
        super(s);
    }

    public ScheduleDefYAMLException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public ScheduleDefYAMLException(Throwable throwable) {
        super(throwable);
    }

}