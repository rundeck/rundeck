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
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import grails.converters.JSON
import org.rundeck.core.auth.AuthConstants
import org.springframework.context.ApplicationContext
import org.springframework.web.multipart.MultipartFile
import rundeck.services.AuthorizationService

import rundeck.services.FrameworkService

import javax.servlet.http.HttpServletResponse

class ProjectSchedulesController extends ControllerBase{

    static final String ACTION_ADMIN = "admin"
    static final String RESOURCE_TYPE_SYSTEM = "system"
    FrameworkService frameworkService
    def AuthorizationService authorizationService
    def ApplicationContext applicationContext
    def schedulerService
    static allowedMethods = [
    ]

    def boolean requireAuth(String project) {

        def authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, project)
        if (!frameworkService.authorizeApplicationResource(
                authContext,
                AuthorizationUtil.resourceType(RESOURCE_TYPE_SYSTEM),
                ACTION_ADMIN
        )) {
            request.errorCode = 'request.error.unauthorized.message'
            request.errorArgs = ['Schedule Definitions (admin)', 'Project', project]
            response.status = HttpServletResponse.SC_FORBIDDEN
            request.titleCode = 'request.error.unauthorized.title'

            render(view: "/common/error", model: [:])
            return false
        }
        return true
    }

    def index(){
        if (!requireAuth(params.project)) {
            return
        }
    }

    def reassociate() {
        if (!requireAuth(params.project)) {
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
        if (!requireAuth(params.project)) {
            return
        }
        def offset = 0
        if(params.offset){
            offset = params.offset
        }

        int max = 10
        def filteredNames = null
        if(request.JSON.filteredNames != null){
            filteredNames = request.JSON.filteredNames
        }
        def result = schedulerService.retrieveProjectSchedulesDefinitionsWithFilters(params.project, params.name, [max: max, offset: offset], filteredNames)
        result?.schedulesMap = result?.schedules?.collect{
            return it.toMap()
        }

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

        if (!requireAuth(params.project)) {
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
        if (!requireAuth(params.project)) {
            return
        }
        def result = schedulerService.delete(request.JSON.schedule)
        if(result.err) response.status = 400
        render result as JSON
    }

    def uploadFileDefinition (){
        if (!requireAuth(params.project)) {
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

    def bulkScheduleDelete(){
        if (!requireAuth(params.project)) {
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

    def getJobsAssociated(){
        if (!requireAuth(params.project)) {
            return
        }

        def offset = 0
        if(params.offset){
            offset = params.offset
        }

        int max = 10

        def result = schedulerService.findJobsAssociatedToSchedule(params.project, params.scheduleName, [max: max, offset: offset])
        result?.scheduledExecutions = result?.scheduledExecutions?.collect{
            return it.toMap()
        }
        render(contentType:'application/json',text:
                ([
                        scheduledExecutions : result.scheduledExecutions,
                        totalRecords        : result.totalRecords,
                        offset              : offset,
                        maxRows             : max

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