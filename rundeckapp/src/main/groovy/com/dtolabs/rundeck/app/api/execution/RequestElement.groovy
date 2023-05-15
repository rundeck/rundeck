package com.dtolabs.rundeck.app.api.execution

import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.Schema

import javax.xml.bind.annotation.XmlAttribute

@CompileStatic
@Schema
class RequestElement {
    @XmlAttribute
    String id
}
