package rundeck.filters

class ZZ_TimerFilters {

    def filters = {
        all(controller:'*', action:'*') {
            after = {
                AA_TimerFilters.afterRequest(request,response,session)
            }
        }
    }
    
}
