package rundeck.utils

import org.rundeck.app.data.providers.v1.UserDataProvider
import org.rundeck.spi.data.DataManager

class UserDataTagLib {
    static namespace = "user"

    UserDataProvider userDataProvider

    def getJobFilters = { attrs, body ->
        def jobFilters = userDataProvider.findByLogin(attrs.user)?.jobfilters
        def var = attrs.var ?: "filters"
        if(jobFilters) {
            out << body((var): jobFilters)
        }
    }

    def getNodeFilters = { attrs, body ->
        def nodeFilters = userDataProvider.findByLogin(attrs.user)?.nodefilters
        def var = attrs.var ?: "filters"
        if(nodeFilters) {
            out << body((var): nodeFilters)
        }
    }

    def getReportFilters = { attrs, body ->
        def reportFilters = userDataProvider.findByLogin(attrs.user)?.reportfilters
        def var = attrs.var ?: "filters"
        if(reportFilters) {
            out << body((var): reportFilters)
        }
    }
}
