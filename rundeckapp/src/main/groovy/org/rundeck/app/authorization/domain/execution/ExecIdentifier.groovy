package org.rundeck.app.authorization.domain.execution

import groovy.transform.CompileStatic
import org.rundeck.core.auth.access.ProjectIdentifier

@CompileStatic
interface ExecIdentifier extends ProjectIdentifier {
    String getId()
}