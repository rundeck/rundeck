databaseChangeLog = {
    changeSet(author: "rundeckuser (generated)", id: "tag-4.13.0") {
        tagDatabase(tag: "4.13.0")
        empty()
        rollback()
    }
}