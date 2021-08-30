databaseChangeLog = {
    changeSet(author: "rundeckuser (generated)", id: "tag-3.4.4") {
        tagDatabase(tag: "3.4.4")
        empty()
        rollback()
    }
}