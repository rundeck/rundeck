package rundeck


class ExecReport extends BaseReport{

    String ctxCommand
    String ctxController
    String jcExecId
    String jcJobId
    Boolean adhocExecution
    String adhocScript
    String abortedByUser

    static mapping = {
        adhocScript type: 'text'
    }

    static constraints = {
        adhocExecution(nullable:true)
        ctxCommand(nullable:true,blank:true)
        ctxController(nullable:true,blank:true)
        jcExecId(nullable:true,blank:true)
        jcJobId(nullable:true,blank:true)
        adhocScript(nullable:true,blank:true)
        abortedByUser(nullable:true,blank:true)
    }

    public static final ArrayList<String> exportProps = BaseReport.exportProps +[
            'jcExecId',
            'jcJobId',
            'adhocExecution',
            'adhocScript',
            'abortedByUser'
    ]
    def Map toMap(){
        def map = this.properties.subMap(exportProps)
        if (map.status == 'timeout') {
            map.status = 'timedout'
        }
        if (map.actionType == 'timeout') {
            map.actionType = 'timedout'
        }
        map
    }

    static buildFromMap(ExecReport obj, Map map) {
        BaseReport.buildFromMap(obj, map)
    }
    /**
     * Generate an ExecReport based off of an existing execution
     * @param exec
     * @return
     */
    static ExecReport fromExec(Execution exec){
        def failedCount = exec.failedNodeList ?exec.failedNodeList.split(',').size():0
        def successCount=exec.succeededNodeList? exec.succeededNodeList.split(',').size():0;
        def totalCount = failedCount+successCount;
        def adhocScript = null
        if(
            null == exec.scheduledExecution
            && exec.workflow.commands
            && exec.workflow.commands.size()==1
            && exec.workflow.commands[0] instanceof CommandExec
        ){
            adhocScript=exec.workflow.commands[0].adhocRemoteString
        }
        def summary = "[${exec.workflow.commands?exec.workflow.commands.size():0} steps]"
        def issuccess = exec.statusSucceeded()
        def iscancelled = exec.cancelled
        def istimedout = exec.timedOut
        def status = issuccess ? "succeed" : iscancelled ? "cancel" : exec.willRetry ? "retry" : istimedout ?
            "timedout" : "fail"
        return fromMap([
                jcExecId:exec.id,
                jcJobId: exec.scheduledExecution?.id,
                adhocExecution: null==exec.scheduledExecution,
                adhocScript: adhocScript,
                abortedByUser: iscancelled? exec.abortedby ?: exec.user:null,
                node:"${successCount}/${failedCount}/${totalCount}",
                title: adhocScript?adhocScript:summary,
                status: status,
                ctxProject: exec.project,
                reportId: exec.scheduledExecution?( exec.scheduledExecution.groupPath ? exec.scheduledExecution.generateFullName() : exec.scheduledExecution.jobName): 'adhoc',
                author: exec.user,
                message: (issuccess ? 'Job completed successfully' : iscancelled ? ('Job killed by: ' + (exec.abortedby ?: exec.user)) : 'Job failed'),
                dateStarted: exec.dateStarted,
                dateCompleted: exec.dateCompleted,
                actionType: status
        ])
    }
    static ExecReport fromMap(Map map) {
        def report = new ExecReport()
        buildFromMap(report, map.subMap( exportProps))
        report
    }
}
