package org.rundeck.app.data

import groovy.transform.CompileStatic
import org.rundeck.app.data.model.v1.page.SortOrder

@CompileStatic
class RdSortOrder implements SortOrder {
    String column;
    String direction;
}
