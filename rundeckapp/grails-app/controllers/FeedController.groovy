import com.dtolabs.rundeck.app.support.ExecQuery
import com.dtolabs.rundeck.app.support.ReportQuery

class FeedController {
    ReportService reportService

    def index = {ReportQuery query ->
        if(!checkEnabled()){
            return 
        }
        if(null!=query && !params.find{ it.key.endsWith('Filter')}){
            query.recentFilter="1d"
            params.recentFilter="1d"
        }

        if(null!=query){
            query.configureFilter()
        }
        def model=reportService.getCombinedReports(query)
        model = reportService.finishquery(query,params,model)
        return model
    }


    def commands = {ExecQuery query ->
        if (!checkEnabled()) {
            return
        }
        if(null!=query && !params.find{ it.key.endsWith('Filter')}){
            query.recentFilter="1d"
            params.recentFilter="1d"
        }

        if(null!=query){
            query.configureFilter()
        }
        def model=reportService.getExecutionReports(query, false)
        model = reportService.finishquery(query,params,model)
        return model
    }

    def jobs = {ExecQuery query ->
        if (!checkEnabled()) {
            return
        }
        if(null!=query && !params.find{ it.key.endsWith('Filter')}){
            query.recentFilter="1d"
            params.recentFilter="1d"
        }

        if(null!=query){
            query.configureFilter()
        }
        def model = reportService.getExecutionReports(query, true)
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

