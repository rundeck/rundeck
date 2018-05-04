package rundeck

class ReferencedExecution {
    ScheduledExecution scheduledExecution
    String status
    Execution execution

    static belongsTo=[Execution]

    static constraints = {
        scheduledExecution(nullable:true)
        status(nullable:true)
        execution(nullable: false)

    }

    static List<ScheduledExecution> parentList(ScheduledExecution se, int max = 0){
        def refExecs = findAllByScheduledExecution(se)?.collect{ re ->
            re.execution?.scheduledExecution
        }?.unique()
        refExecs.subList(0, (max!=0 && refExecs.size()>max)?max:refExecs.size())

    }
}
