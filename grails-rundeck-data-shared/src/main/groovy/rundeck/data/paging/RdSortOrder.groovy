package rundeck.data.paging

import org.rundeck.app.data.model.v1.page.SortOrder

class RdSortOrder implements SortOrder {
    String column
    String direction

    static constraints={
        direction(inList:["asc","desc"],nullable: true)
        column(nullable: true)
    }
}
