package com.dtolabs.rundeck.app.api.execution

import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.Schema

import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlRootElement

@CompileStatic
@Schema
@XmlRootElement(name = "executions")
class DeleteBulkRequestXml {
    @XmlElement(name = "execution")
    List<RequestElement> ids = []
}
