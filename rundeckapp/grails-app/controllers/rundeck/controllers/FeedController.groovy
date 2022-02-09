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

import com.dtolabs.rundeck.app.support.ExecQuery
import rundeck.services.ReportService

import javax.servlet.http.HttpServletResponse

class FeedController  extends ControllerBase{
    ReportService reportService

    def index(ExecQuery query){
        if(!checkEnabled()){
            return
        }
        if(null!=query && !params.find{ it.key.endsWith('Filter')}){
            //no default filter
        }
        if (params.max != null && params.max != query.max.toString()) {
            query.errors.rejectValue('max', 'typeMismatch.java.lang.Integer', ['max'] as Object[], 'invalid')
        }
        if (params.offset != null && params.offset != query.offset.toString()) {
            query.errors.rejectValue('offset', 'typeMismatch.java.lang.Integer', ['offset'] as Object[], 'invalid')
        }
        if (query.hasErrors()) {
            response.status = 400
            return render(view: '/common/error', model: [beanErrors: query.errors])
        }
        if(null!=query){
            query.configureFilter()
        }
        def model=reportService.getExecutionReports(query,true)
        model = reportService.finishquery(query,params,model)
        return model
    }

    protected checkEnabled() {
        if ('true' != servletContext.getAttribute('RSS_ENABLED')) {
            response.status=HttpServletResponse.SC_NOT_FOUND
            renderErrorView("Not found")
            return false
        }
        return true
    }
}

