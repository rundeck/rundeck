class ExecReport extends BaseReport{

    String ctxCommand
    String ctxController
    String jcExecId
    String jcJobId
    Boolean adhocExecution
    String adhocScript

    static constraints = {
        adhocExecution(nullable:true)
        ctxCommand(nullable:true,blank:true)
        ctxController(nullable:true,blank:false)
        jcExecId(nullable:true,blank:false)
        jcJobId(nullable:true,blank:false)
        adhocScript(nullable:true,blank:true)
    }
}
