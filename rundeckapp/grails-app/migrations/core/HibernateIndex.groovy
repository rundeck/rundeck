databaseChangeLog = {
    changeSet(author: "rundeckuser (generated)", id: "3.4.0-1", dbms: "oracle,postgresql") {
        preConditions(onFail: "MARK_RAN"){
            not{
                sequenceExists (sequenceName:"hibernate_sequence")
            }
        }
        createSequence(incrementBy: "1", sequenceName: "hibernate_sequence", startValue: "1")
    }
}
