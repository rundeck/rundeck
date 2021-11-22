databaseChangeLog = {
    changeSet(author: "rundeckuser (generated)", id: "tag-3.4.8") {
        tagDatabase(tag: "3.4.8")
        empty()
        rollback()
    }
}