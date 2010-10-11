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

   static constraints = {
        reportId(nullable:true)
        tags(nullable:true)
        node(nullable:true)
        maprefUri(nullable:true)
        ctxName(nullable:true)
        ctxType(nullable:true)
        status(nullable:false,inList:['succeed','fail','cancel'])
        actionType(nullable:false,inList:['create','update','delete','succeed','fail','cancel'])
    }
}
