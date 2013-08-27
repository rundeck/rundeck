package rundeck.services

import com.dtolabs.rundeck.app.support.ExecQuery
import com.dtolabs.rundeck.app.support.ReportQuery
import rundeck.ExecReport
import rundeck.BaseReport

class ReportService  {

    def grailsApplication

    public Map reportExecutionResult(Map fields) {
        /**
         * allowed fields are specified
         */

        if (fields['rundeckEpochDateStarted']) {
            def long dstart = Long.parseLong(fields['rundeckEpochDateStarted'])
            if (dstart > 0) {
                fields['dateStarted'] = new Date(dstart)
            }
        } else if (fields['epochDateStarted']) {
            def long dstart = Long.parseLong(fields['epochDateStarted'])
            if (dstart > 0) {
                fields['dateStarted'] = new Date(dstart)
            }
        }
        if (fields['rundeckEpochDateEnded']) {
            def long dstart = Long.parseLong(fields['rundeckEpochDateEnded'])
            if (dstart > 0) {
                fields['dateCompleted'] = new Date(dstart)
            }
        } else if (fields['epochDateEnded']) {
            def long dstart = Long.parseLong(fields['epochDateEnded'])
            if (dstart > 0) {
                fields['dateCompleted'] = new Date(dstart)
            }
        }

        if (!fields['dateStarted']) {
            fields['dateStarted'] = fields['dateCompleted']
        }
        if(!fields.message){
            fields.message="[no message]"
        }
        fields.actionType=fields.status in ['succeed','fail','cancel'] ? fields.status : 'fail'
        def rep = new ExecReport(fields)

        //TODO: authorize event creation?

        if (rep && !rep.save(flush: true)) {
//            System.err.println("error saving report: ${fields}")
//            rep.errors.allErrors.each {
//                System.err.println(it)
//            }
            return [error:true,report:rep]
        }else{
            return [success:true]
        }
    }

     def public  finishquery = { query,params,model->

        if(!params.max){
            params.max=grailsApplication.config.reportservice.pagination.default?grailsApplication.config.reportservice.pagination.default.toInteger():20
        }
        if(!params.offset){
            params.offset=0
        }

        def paginateParams=[:]
        if(query){
            model._filters.each{ key,val ->
                if(query."${key}Filter"){
                    paginateParams["${key}Filter"]=query."${key}Filter"
                }
            }
        }
        def displayParams = [:]
        displayParams.putAll(paginateParams)

//        params.each{key,val ->
//            if(key ==~ /^(start|end)(do|set|before|after)Filter.*/ && query[key]){
//                paginateParams[key]=val
//            }
//        }
        if(query){
            if(query.recentFilter && query.recentFilter!='-'){
                displayParams.put("recentFilter",query.recentFilter)
                paginateParams.put("recentFilter",query.recentFilter)
                query.dostartafterFilter=false
                query.dostartbeforeFilter=false
                query.doendafterFilter=false
                query.doendbeforeFilter=false
            }else{
                if(query.dostartafterFilter && query.startafterFilter){
                    displayParams.put("startafterFilter",query.startafterFilter)
                    paginateParams.put("startafterFilter",query.startafterFilter)
                    paginateParams.put("dostartafterFilter","true")
                }
                if(query.dostartbeforeFilter && query.startbeforeFilter){
                    displayParams.put("startbeforeFilter",query.startbeforeFilter)
                    paginateParams.put("startbeforeFilter",query.startbeforeFilter)
                    paginateParams.put("dostartbeforeFilter","true")
                }
                if(query.doendafterFilter && query.endafterFilter){
                    displayParams.put("endafterFilter",query.endafterFilter)
                    paginateParams.put("endafterFilter",query.endafterFilter)
                    paginateParams.put("doendafterFilter","true")
                }
                if(query.doendbeforeFilter && query.endbeforeFilter){
                    displayParams.put("endbeforeFilter",query.endbeforeFilter)
                    paginateParams.put("endbeforeFilter",query.endbeforeFilter)
                    paginateParams.put("doendbeforeFilter","true")
                }
            }
        }
        def remkeys=[]
        if(!query || !query.dostartafterFilter){
            paginateParams.each{key,val ->
                if(key ==~ /^startafterFilter.*/){
                    remkeys.add(key)
                }
            }
        }
        if(!query || !query.dostartbeforeFilter){
            paginateParams.each{key,val ->
                if(key ==~ /^startbeforeFilter.*/){
                    remkeys.add(key)
                }
            }
        }
        if(!query || !query.doendafterFilter){
            paginateParams.each{key,val ->
                if(key ==~ /^endafterFilter.*/){
                    remkeys.add(key)
                }
            }
        }
        if(!query || !query.doendbeforeFilter){
            paginateParams.each{key,val ->
                if(key ==~ /^endbeforeFilter.*/){
                    remkeys.add(key)
                }
            }
        }
        remkeys.each{
            paginateParams.remove(it)
        }

        def tmod=[max: query?.max?query.max:grailsApplication.config.reportservice.pagination.default?grailsApplication.config.reportservice.pagination.default.toInteger():20,
            offset:query?.offset?query.offset:0,
            paginateParams:paginateParams,
            displayParams:displayParams]
        model.putAll(tmod)
        return model
    }

    boolean transactional = true

    private def getStartsWithFilters() {
        return [
            job: 'jobFullName',
        ]
    }
    private def getTxtFilters() {
        def txtfilters = [
            obj: 'ctxName',
            type: 'ctxType',
            user: 'author',
            abortedBy: 'abortedByUser',
            node: 'node',
            message: 'message',
            title: 'title',
            tags: 'tags',
        ]
        return txtfilters
    }

    private def getEqFilters() {
        def eqfilters = [
            maprefUri: 'maprefUri',
            stat: 'status',
                jobFullName: 'jobFullName',
            jobId:'jcJobId',
            proj: 'ctxProject',
        ]
        return eqfilters
    }

    /**
     * Count the query results matching the filter
     */
    def countCombinedReports(ReportQuery query) {

        def total = BaseReport.createCriteria().count {

            applyReportsCriteria(query,delegate)

        }
        return total
    }

    /**
     * Add criteria query elements for the ReportQuery
     * @param query the query
     * @param delegate the criteria closure's delegate
     * @return
     */
    private applyReportsCriteria(ReportQuery query, delegate){
        def eqfilters = getEqFilters()
        def txtfilters = getTxtFilters()
        def startFilters = getStartsWithFilters()
        delegate.with{

            if (query) {
                txtfilters.each {key, val ->
                    if (query["${key}Filter"]) {
                        ilike(val, '%' + query["${key}Filter"] + '%')
                    }
                }
                startFilters.each { key, val ->
                    def qval = query["${key}Filter"]
                    if (qval) {
                        ilike(val, qval.contains('%') ? qval : (qval + (qval.endsWith('/')? '%': '/%')))
                    }
                }

                eqfilters.each {key, val ->
                    if (query["${key}Filter"] == 'null') {
                        isNull(val)
                    } else if (query["${key}Filter"] == '!null') {
                        isNotNull(val)
                    } else if (query["${key}Filter"]) {
                        eq(val, query["${key}Filter"])
                    }
                }

                if (query.jobListFilter || query.excludeJobListFilter) {
                    and {
                        if (query.jobListFilter) {
                            or {
                                query.jobListFilter.each {
                                    eq('jobFullName', it)
                                }
                            }
                        }
                        if (query.excludeJobListFilter) {
                            not {
                                or {
                                    query.excludeJobListFilter.each {
                                        eq('jobFullName', it)
                                    }

                                }
                            }
                        }
                    }
                }

                if (query.dostartafterFilter && query.dostartbeforeFilter && query.startbeforeFilter && query.startafterFilter) {
                    between('dateStarted', query.startafterFilter, query.startbeforeFilter)
                }
                else if (query.dostartbeforeFilter && query.startbeforeFilter) {
                    le('dateStarted', query.startbeforeFilter)
                } else if (query.dostartafterFilter && query.startafterFilter) {
                    ge('dateStarted', query.startafterFilter)
                }

                if (query.doendafterFilter && query.doendbeforeFilter && query.endafterFilter && query.endbeforeFilter) {
                    between('dateCompleted', query.endafterFilter, query.endbeforeFilter)
                }
                else if (query.doendbeforeFilter && query.endbeforeFilter) {
                    le('dateCompleted', query.endbeforeFilter)
                }
                if (query.doendafterFilter && query.endafterFilter) {
                    ge('dateCompleted', query.endafterFilter)
                }
            }

        }
    }

    /**
     * Count the query results matching the filter
     */
    def countExecutionReports(ExecQuery query) {

        def total = ExecReport.createCriteria().count {

            applyExecutionCriteria(query, delegate)

        }
        return total
    }
    def applyExecutionCriteria(ExecQuery query, delegate, boolean isJobs=true){
        def eqfilters = [
                stat: 'status',
                jobFullName: 'jobFullName',
                jobId: 'jcJobId',
        ]
        def txtfilters = [
                proj: 'ctxProject',
                user: 'author',
                node: 'node',
                message: 'message',
                job: 'jobFullName',
                title: 'title',
                tags: 'tags',
        ]

        def filters = [:]
        filters.putAll(txtfilters)
        filters.putAll(eqfilters)


        delegate.with {

            if (query) {
                txtfilters.each { key, val ->
                    if (query["${key}Filter"]) {
                        ilike(val, '%' + query["${key}Filter"] + '%')
                    }
                }

                eqfilters.each { key, val ->
                    if (query["${key}Filter"]) {
                        eq(val, query["${key}Filter"])
                    }
                }
                if (query.titleFilter) {
                    or {
                        eq('jcJobId', '')
                        isNull('jcJobId')
                    }
                }
                if (query.jobListFilter || query.excludeJobListFilter) {
                    and {
                        if (query.jobListFilter) {
                            or {
                                query.jobListFilter.each {
                                    eq('jobFullName', it)
                                }
                            }
                        }
                        if (query.excludeJobListFilter) {
                            not {
                                or {
                                    query.excludeJobListFilter.each {
                                        eq('jobFullName', it)
                                    }

                                }
                            }
                        }
                    }
                }

                if (query.dostartafterFilter && query.dostartbeforeFilter && query.startbeforeFilter && query.startafterFilter) {
                    between('dateStarted', query.startafterFilter, query.startbeforeFilter)
                } else if (query.dostartbeforeFilter && query.startbeforeFilter) {
                    le('dateStarted', query.startbeforeFilter)
                } else if (query.dostartafterFilter && query.startafterFilter) {
                    ge('dateStarted', query.startafterFilter)
                }

                if (query.doendafterFilter && query.doendbeforeFilter && query.endafterFilter && query.endbeforeFilter) {
                    between('dateCompleted', query.endafterFilter, query.endbeforeFilter)
                } else if (query.doendbeforeFilter && query.endbeforeFilter) {
                    le('dateCompleted', query.endbeforeFilter)
                }
                if (query.doendafterFilter && query.endafterFilter) {
                    ge('dateCompleted', query.endafterFilter)
                }
            }

            if (isJobs) {
                or {
                    isNotNull("jobId")
                    isNotNull("execId")
                }
            } else {
                isNull("jobId")
                isNull("execId")
            }
        }
    }
    def getExecutionReports(ExecQuery query, boolean isJobs) {
        def eqfilters = [
                stat: 'status',
                jobFullName: 'jobFullName',
                jobId: 'jobId',
        ]
        def txtfilters = [
                proj: 'ctxProject',
                user: 'author',
                node: 'node',
                message: 'message',
                job: 'jobFullName',
                title: 'title',
                tags: 'tags',
        ]

        def filters = [:]
        filters.putAll(txtfilters)
        filters.putAll(eqfilters)
        def runlist=ExecReport.createCriteria().list {

            if (query?.max) {
                maxResults(query?.max.toInteger())
            } else {
                maxResults(grailsApplication.config.reportservice.pagination.default ? grailsApplication.config.reportservice.pagination.default.toInteger() : 20)
            }
            if (query?.offset) {
                firstResult(query.offset.toInteger())
            }

            applyExecutionCriteria(query, delegate,isJobs)

            if (query && query.sortBy && filters[query.sortBy]) {
                order(filters[query.sortBy], query.sortOrder == 'ascending' ? 'asc' : 'desc')
            } else {
                order("dateCompleted", 'desc')
                order("dateStarted",'desc')
                order("id", 'desc')
            }
        }
        def executions=[]
        def lastDate = -1
        runlist.each{
            executions<<it
            if (it.dateCompleted.time > lastDate) {
                lastDate = it.dateCompleted.time
            }
        }


        def total = ExecReport.createCriteria().count{
            applyExecutionCriteria(query, delegate,isJobs)
        };

        return [
            query:query,
            reports:executions,
            total: total,
            lastDate: lastDate,
            _filters:filters
            ]
	}

}
