package org.rundeck.app.authorization.domain

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.ProjectIdentifier

@CompileStatic
interface ExecIdentifier extends ProjectIdentifier {
    String getId()

}