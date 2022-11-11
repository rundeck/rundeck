package rundeck.data.paging

import org.rundeck.app.data.model.v1.page.Page
import org.rundeck.app.data.model.v1.page.Pageable

class RdPage<T> implements Page<T> {
    List<T> results = []
    Long total = 0
    RdPageable pageable
}
