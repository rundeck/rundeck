package rundeck

class BaseReport {

    String node
    String title
    String status
    String actionType
    String ctxProject
    String ctxType
    String ctxName
    String maprefUri
    String reportId
    String tags
    String author
    Date dateStarted
    Date dateCompleted 
    String message

    static mapping = {
        message type: 'text'
        title type: 'text'
    }
   static constraints = {
        reportId(nullable:true, maxSize: 1024+2048 /*jobName + groupPath size limitations from ScheduledExecution*/)
        tags(nullable:true)
        node(nullable:true)
        maprefUri(nullable:true)
        ctxName(nullable:true)
        ctxType(nullable:true)
        status(nullable:false, maxSize: 256)
        actionType(nullable:false, maxSize: 256)
    }
    public static final ArrayList<String> exportProps = [
            'node',
            'title',
            'status',
            'actionType',
            'ctxProject',
            'reportId',
            'tags',
            'author',
            'message',
            'dateStarted',
            'dateCompleted'
    ]

    def Map toMap(){
        def map=this.properties.subMap(exportProps)
        if(map.status=='timeout'){
            map.status='timedout'
        }
        if(map.actionType=='timeout'){
            map.actionType='timedout'
        }
        map
    }

    static buildFromMap(BaseReport obj, Map data) {
        data.each { k, v ->
            if ((k == 'status' || k == 'actionType') && v == 'timedout') {
                //XXX: use 'timeout' internally for timedout status, due to previous varchar(7) length limitations on
                // the field :-Σ
                v='timeout'
            }
            obj[k] = v
        }
    }

    static BaseReport fromMap(Map data) {
        def BaseReport report = new BaseReport()
        buildFromMap(report, data.subMap(exportProps))
        report
    }
}
