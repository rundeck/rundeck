package org.rundeck.app.components

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import org.rundeck.app.components.jobs.JobDefinitionException
import org.rundeck.app.components.jobs.JobFormat

@CompileStatic
class JobJSONFormat implements JobFormat {
    final String format = 'json'

    Boolean trimSpacesFromLines = false

    @Override
    List<Map> decode(final Reader reader) throws JobDefinitionException {

        final ObjectMapper mapper = new ObjectMapper()
        def data = mapper.readValue(reader, List.class)
        if (!(data instanceof List)) {
            throw new JobDefinitionException("Job Json Format: Expected list data")
        }
        if (!data.every { it instanceof Map }) {
            throw new JobDefinitionException("Job Json Format: Expected list of Maps")
        }
        return data
    }

    @Override
    void encode(final List<Map> list, final Options options, final Writer writer) {

        final ObjectMapper mapper = new ObjectMapper()

        mapper.writeValue(writer, list.collect { JobYAMLFormat.canonicalMap(it, trimSpacesFromLines) })
    }
}
