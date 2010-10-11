class FeedController {
    ReportService reportService

    def index = {ReportQuery query ->
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
        if ('true' == servletContext.getAttribute('RSS_ENABLED')) {

        } else {
            redirect(controller: 'menu', action: 'index')
        }
    }
}

