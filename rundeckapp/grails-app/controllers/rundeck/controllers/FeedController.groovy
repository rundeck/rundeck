package rundeck.controllers

import com.dtolabs.rundeck.app.support.ExecQuery
import rundeck.services.ReportService

class FeedController {
    ReportService reportService

    def index = {ExecQuery query ->
        if(!checkEnabled()){
            return 
        }
        if(null!=query && !params.find{ it.key.endsWith('Filter')}){
            //no default filter
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
            response.setStatus 404
            flash.error="Not found"
            render(template:"/common/error")
            return false
        }
        return true
    }
}

