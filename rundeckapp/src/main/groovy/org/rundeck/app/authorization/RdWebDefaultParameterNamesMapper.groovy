package org.rundeck.app.authorization

import groovy.transform.CompileStatic
import org.rundeck.core.auth.web.WebDefaultParameterNamesMapper

@CompileStatic
class RdWebDefaultParameterNamesMapper implements WebDefaultParameterNamesMapper {
    Map<String, String> webDefaultParameterNames

    RdWebDefaultParameterNamesMapper(final Map<String, String> webDefaultParameterNames) {
        this.webDefaultParameterNames = webDefaultParameterNames
    }
}
