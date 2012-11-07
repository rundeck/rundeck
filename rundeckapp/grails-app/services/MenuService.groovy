import com.dtolabs.rundeck.app.support.ReportQuery

/**
 * Service for menu views
 */
class MenuService {

    def frameworkService
	
	boolean transactional = true
	
	def getReports(ReportQuery query, Map params) {

        def txtfilters = [
            obj:'name',
            type:'type',
            cmd:'command',
            user:'user',
            node:'node',
            proj:'project']

        def filters = [ stat:'status',job:'jobName']
        filters.putAll(txtfilters)


        if(params['Clear']){
            filters.each{key,val ->
                params.remove("${key}Filter")
                params["${key}Filter"]=null
            }
            query=null

        }
        def options=[:]
        if(params['customView']){
              params.each{String k, v ->
                def m = k =~ /^(.*)Show$/
                if(m.matches() && v=='true'){
                log.info("saw view option: ${m.group(1)}")
                    options[m.group(1)]=true
                }
              }
            session.menu_reports_options=options
        }
        if(query && query.startafterFilter){
            log.info("startafter: ${query.startafterFilter}, params: ${params.startafterFilter}")
        }

        def crit = Execution.createCriteria()
        def runlist = crit.list{
            if(params.max){
                maxResults(params.max.toInteger())
            }else{
                maxResults(10)
            }
            if(params.offset){
                firstResult(params.offset.toInteger())
            }

            txtfilters.each{ key,val ->
                if(params["${key}Filter"]){
                    ilike(val,'%'+params["${key}Filter"]+'%')
                }
            }
            if(params['statFilter']){
                eq('status',params['statFilter']=='true'||params['statFilter']=='Succeeded' ? 'true' : 'false')
            }
            if(params['jobFilter']){
                scheduledExecution{
                    ilike('jobName','%'+params['jobFilter']+'%')
                }
            }
            if(query ){

                if(query.dostartafterFilter && query.dostartbeforeFilter && query.startbeforeFilter && query.startafterFilter){
                    between('dateStarted',query.startafterFilter,query.startbeforeFilter)
                }
                else if(query.dostartbeforeFilter && query.startbeforeFilter ){
                    le('dateStarted',query.startbeforeFilter)
                }else if (query.dostartafterFilter && query.startafterFilter ){
                    ge('dateStarted',query.startafterFilter)
                }

                if(query.doendafterFilter && query.doendbeforeFilter && query.endafterFilter && query.endbeforeFilter){
                    between('dateCompleted',query.endafterFilter,query.endbeforeFilter)
                }
                else if(query.doendbeforeFilter && query.endbeforeFilter ){
                    le('dateCompleted',query.endbeforeFilter)
                }
                if(query.doendafterFilter && query.endafterFilter ){
                    ge('dateCompleted',query.endafterFilter)
                }
            }
            isNotNull("dateCompleted")
            order("dateStarted","desc")
        };
        def executions=[]
        runlist.each{
            executions<<it
        }

        def jobs =[:]
        executions.each{
            if(it.scheduledExecution && !jobs[it.scheduledExecution.id.toString()]){
                jobs[it.scheduledExecution.id.toString()] = ScheduledExecution.get(it.scheduledExecution.id)
            }
        }

        log.info("executions: ${executions}")

        def total = Execution.createCriteria().count{

            txtfilters.each{ key,val ->
                if(params["${key}Filter"]){
                    ilike(val,'%'+params["${key}Filter"]+'%')
                }
            }
            if(params['statFilter']){
                eq('status',params['statFilter']=='true'||params['statFilter']=='Succeeded' ? 'true' : 'false')
            }
            if(params['jobFilter']){
                scheduledExecution{
                    ilike('jobName','%'+params['jobFilter']+'%')
                }
            }

            if(query ){

                if(query.dostartafterFilter && query.dostartbeforeFilter && query.startbeforeFilter && query.startafterFilter){
                    between('dateStarted',query.startafterFilter,query.startbeforeFilter)
                }
                else if(query.dostartbeforeFilter && query.startbeforeFilter ){
                    le('dateStarted',query.startbeforeFilter)
                }else if (query.dostartafterFilter && query.startafterFilter ){
                    ge('dateStarted',query.startafterFilter)
                }

                if(query.doendafterFilter && query.doendbeforeFilter && query.endafterFilter && query.endbeforeFilter){
                    between('dateCompleted',query.endafterFilter,query.endbeforeFilter)
                }
                else if(query.doendbeforeFilter && query.endbeforeFilter ){
                    le('dateCompleted',query.endbeforeFilter)
                }
                if(query.doendafterFilter && query.endafterFilter ){
                    ge('dateCompleted',query.endafterFilter)
                }
            }
            isNotNull("dateCompleted")
        };
//        total = total.adaptee



        if(!params.max){
            params.max=10
        }
        if(!params.offset){
            params.offset=0
        }

        def paginateParams=[:]

        filters.each{ key,val ->
            if(params["${key}Filter"]){
                paginateParams["${key}Filter"]=params["${key}Filter"]
            }
        }
        def displayParams = [:]
        displayParams.putAll(paginateParams)

        params.each{key,val ->
            if(key ==~ /^(start|end)(do|set|before|after)Filter.*/){
                paginateParams[key]=val
            }
        }
        if(query){
            if(query.dostartafterFilter && query.startafterFilter){
                displayParams.put("startafterFilter",query.startafterFilter)
                paginateParams.put("dostartafterFilter","true")
            }
            if(query.dostartbeforeFilter && query.startbeforeFilter){
                displayParams.put("startbeforeFilter",query.startbeforeFilter)
                paginateParams.put("dostartbeforeFilter","true")
            }
            if(query.doendafterFilter && query.endafterFilter){
                displayParams.put("endafterFilter",query.endafterFilter)
                paginateParams.put("doendafterFilter","true")
            }
            if(query.doendbeforeFilter && query.endbeforeFilter){
                displayParams.put("endbeforeFilter",query.endbeforeFilter)
                paginateParams.put("doendbeforeFilter","true")
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

        return [
            query:query,
            jobs:jobs,
            executions:executions,
            total: total,
            paginateParams:paginateParams,
            displayParams:displayParams,
            max: params.max?params.max:10,
            offset:params.offset?params.offset:0]
	}

}

