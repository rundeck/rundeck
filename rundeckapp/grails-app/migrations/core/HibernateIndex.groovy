databaseChangeLog = {
    changeSet(author: "rundeckuser (generated)", id: "3.4.0-1", dbms: "oracle,postgresql") {
        createSequence(incrementBy: "1", sequenceName: "hibernate_sequence", startValue: "1")
    }
}