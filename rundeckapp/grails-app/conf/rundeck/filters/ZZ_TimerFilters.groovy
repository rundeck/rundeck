package rundeck.filters

class ZZ_TimerFilters {

    def dependsOn = [AA_TimerFilters]

    def filters = {
        all(controller:'user', action:'logout',invert:true) {
            after = {
                AA_TimerFilters.afterRequest(request,response,session)
            }
        }
    }
    
}
