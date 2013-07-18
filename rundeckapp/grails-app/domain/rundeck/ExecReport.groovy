package rundeck

import org.codehaus.groovy.grails.commons.ConfigurationHolder

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
        this.properties.subMap(exportProps)
    }

    static buildFromMap(ExecReport obj, Map map) {
        BaseReport.buildFromMap(obj, map)
    }

    static ExecReport fromMap(Map map) {
        def report = new ExecReport()
        buildFromMap(report, map.subMap( exportProps))
        report
    }
}
