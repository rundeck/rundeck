package rundeck.data.paging

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.validation.Validateable
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.rundeck.app.data.model.v1.page.Pageable
import org.rundeck.app.data.model.v1.page.SortOrder

@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(["errors"])
class RdPageable implements Pageable, Validateable {
    Integer max = 200
    Integer offset = 0
    List<SortOrder> sortOrders = []

    static constraints={
        max(min:0, max: 500,nullable: true)
        offset(min:0,nullable: true)
        sortOrders(nullable: true)
    }

    RdPageable withOrder(String column, String sortDir) {
        sortOrders.add(new RdSortOrder(column:column,direction: sortDir))
        return this
    }
}
