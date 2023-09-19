package rundeck

import org.rundeck.app.data.providers.v1.UserDataProvider

class UserDataTagLib {
    static namespace = "user"

    UserDataProvider userDataProvider

    def getReportFilters = { attrs, body ->
        def reportFilters = userDataProvider.findByLogin(attrs.user)?.reportfilters
        def var = attrs.var ?: "filters"
        if(reportFilters) {
            out << body((var): reportFilters)
        }
    }
}
