package com.dtolabs.rundeck.app.api.jobs.browse

import groovy.transform.CompileStatic
import org.rundeck.app.components.jobs.ComponentMeta

@CompileStatic
class ItemMeta {
    String name
    Map<String,Object> data

    static ItemMeta from(ComponentMeta meta) {
        new ItemMeta(name: meta.name, data: meta.data)
    }
}
