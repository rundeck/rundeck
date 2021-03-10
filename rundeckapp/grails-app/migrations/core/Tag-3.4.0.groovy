databaseChangeLog = {
    changeSet(author: "rundeckuser (generated)", id: "tag-3.4.0") {
        tagDatabase(tag: "3.4.0")
        empty()
        rollback()
    }
}