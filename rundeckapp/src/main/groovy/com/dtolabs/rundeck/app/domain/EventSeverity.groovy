package com.dtolabs.rundeck.app.domain

import com.fasterxml.jackson.annotation.JsonFormat
import groovy.transform.CompileStatic

@JsonFormat(shape = JsonFormat.Shape.STRING)
@CompileStatic
enum EventSeverity {
    ERROR(0),
    WARN(1),
    INFO(2),
    DEBUG(3),
    TRACE(4)

    final int id
    private EventSeverity(int id) { this.id = id }
}