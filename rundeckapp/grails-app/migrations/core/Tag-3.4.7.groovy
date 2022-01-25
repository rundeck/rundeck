databaseChangeLog = {
    changeSet(author: "rundeckuser (generated)", id: "tag-3.4.7") {
        tagDatabase(tag: "3.4.7")
        empty()
        rollback()
    }
}