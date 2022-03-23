databaseChangeLog = {
    changeSet(author: "rundeckuser (generated)", id: "tag-4.0.0") {
        tagDatabase(tag: "4.0.0")
        empty()
        rollback()
    }
}